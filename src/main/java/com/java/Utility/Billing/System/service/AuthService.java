package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.*;
import com.java.Utility.Billing.System.dto.response.AuthResponse;
import com.java.Utility.Billing.System.dto.response.UserResponse;
import com.java.Utility.Billing.System.entity.RefreshToken;
import com.java.Utility.Billing.System.entity.Role;
import com.java.Utility.Billing.System.entity.TokenBlacklist;
import com.java.Utility.Billing.System.entity.User;
import com.java.Utility.Billing.System.enums.OtpType;
import com.java.Utility.Billing.System.enums.RoleName;
import com.java.Utility.Billing.System.enums.UserStatus;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.exception.UnauthorizedException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.RefreshTokenRepository;
import com.java.Utility.Billing.System.repository.RoleRepository;
import com.java.Utility.Billing.System.repository.TokenBlacklistRepository;
import com.java.Utility.Billing.System.repository.UserRepository;
import com.java.Utility.Billing.System.security.JwtService;
import com.java.Utility.Billing.System.security.SecurityUtils;
import com.java.Utility.Billing.System.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final SecurityUtils securityUtils;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        Set<Role> roles = resolveRoles(request.getRoles());
        User user = User.builder()
                .fullNames(request.getFullNames())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        // Self-registration: send welcome + OTP (user must call POST /api/auth/verify-email)
        String otp = otpService.generateOtp(user.getEmail(), OtpType.EMAIL_VERIFICATION);
        emailService.sendWelcomeEmail(user.getEmail(), user.getFullNames());
        emailService.sendVerificationEmail(user.getEmail(), user.getFullNames(), otp);

        log.info("User registered: {}", user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedException("Account is inactive");
        }

        log.info("User logged in: {} (mustChangePassword={})", user.getEmail(), user.isMustChangePassword());
        return buildAuthResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = securityUtils.getCurrentUser();
        if (principal == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        User user = userRepository.findByEmail(principal.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);

        log.info("Password changed for: {}", user.getEmail());
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (refreshToken.isRevoked() || refreshToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String accessToken) {
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }
        LocalDateTime expiry = jwtService.extractExpiration(accessToken).toInstant()
                .atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        tokenBlacklistRepository.save(TokenBlacklist.builder()
                .token(accessToken)
                .expiryDate(expiry)
                .build());

        UserPrincipal principal = getCurrentUserFromToken(accessToken);
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getEmail()).orElse(null);
            if (user != null) {
                refreshTokenRepository.deleteByUser(user);
            }
        }
        log.info("User logged out");
    }

    @Transactional
    public void verifyEmail(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.EMAIL_VERIFICATION);
        user.setEmailVerified(true);
        userRepository.save(user);
        log.info("Email verified for: {}", user.getEmail());
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with this email"));
        // Generate OTP and email it (also logged to console if SMTP fails)
        String otp = otpService.generateOtp(user.getEmail(), OtpType.PASSWORD_RESET);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullNames(), otp);
        log.info("Password reset OTP sent to: {}", user.getEmail());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        otpService.verifyOtp(request.getEmail(), request.getOtp(), OtpType.PASSWORD_RESET);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        refreshTokenRepository.deleteByUser(user);
        log.info("Password reset for: {}", user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration())
                .mustChangePassword(user.isMustChangePassword())
                .user(EntityMapper.toUserResponse(user))
                .build();
    }

    private String createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusSeconds(jwtService.getRefreshExpiration() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException("Role not found: " + roleName));
            roles.add(role);
        }
        return roles;
    }

    private UserPrincipal getCurrentUserFromToken(String token) {
        try {
            String email = jwtService.extractUsername(token);
            User user = userRepository.findByEmail(email).orElse(null);
            return user != null ? new UserPrincipal(user) : null;
        } catch (Exception e) {
            return null;
        }
    }
}
