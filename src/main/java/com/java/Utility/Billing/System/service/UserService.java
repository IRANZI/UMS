package com.java.Utility.Billing.System.service;

import com.java.Utility.Billing.System.dto.request.AdminCreateUserRequest;
import com.java.Utility.Billing.System.dto.request.UserUpdateRequest;
import com.java.Utility.Billing.System.dto.response.CredentialsDeliveryResponse;
import com.java.Utility.Billing.System.dto.response.PageResponse;
import com.java.Utility.Billing.System.dto.response.UserResponse;
import com.java.Utility.Billing.System.entity.Role;
import com.java.Utility.Billing.System.entity.User;
import com.java.Utility.Billing.System.enums.RoleName;
import com.java.Utility.Billing.System.enums.UserStatus;
import com.java.Utility.Billing.System.exception.BadRequestException;
import com.java.Utility.Billing.System.exception.ResourceNotFoundException;
import com.java.Utility.Billing.System.mapper.EntityMapper;
import com.java.Utility.Billing.System.repository.RoleRepository;
import com.java.Utility.Billing.System.repository.UserRepository;
import com.java.Utility.Billing.System.util.PageMapper;
import com.java.Utility.Billing.System.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Admin creates Finance/Operator/Customer staff.
     * Generates a temporary password and emails it (no OTP verification needed).
     */
    @Transactional
    public UserResponse createByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new BadRequestException("Phone number already registered");
        }

        validateStaffRoles(request.getRoles());

        String temporaryPassword = PasswordGenerator.generateTemporaryPassword();
        Set<Role> roles = resolveRoles(request.getRoles());

        User user = User.builder()
                .fullNames(request.getFullNames())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .password(passwordEncoder.encode(temporaryPassword))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mustChangePassword(true)
                .roles(roles)
                .build();

        user = userRepository.save(user);

        Set<RoleName> roleNames = request.getRoles();
        deliverCredentials(user, temporaryPassword, roleNames);
        return EntityMapper.toUserResponse(user);
    }

    @Transactional
    public CredentialsDeliveryResponse resendCredentials(Long id) {
        User user = findUser(id);
        if (user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN)) {
            throw new BadRequestException("Cannot resend credentials for admin accounts");
        }

        String temporaryPassword = PasswordGenerator.generateTemporaryPassword();
        user.setPassword(passwordEncoder.encode(temporaryPassword));
        user.setMustChangePassword(true);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        Set<RoleName> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return deliverCredentials(user, temporaryPassword, roleNames);
    }

    /**
     * Tries to email credentials; if SMTP fails, returns the temp password in the API response
     * so the admin can share it manually. Password is always saved before this runs.
     */
    private CredentialsDeliveryResponse deliverCredentials(User user, String temporaryPassword,
                                                           Set<RoleName> roleNames) {
        boolean emailSent = emailService.sendAccountCredentialsEmail(
                user.getEmail(), user.getFullNames(), temporaryPassword, roleNames);

        String deliveryNote;
        String returnedPassword = null;
        if (emailSent) {
            deliveryNote = "Credentials email sent to " + user.getEmail();
            log.info("Credentials delivered by email to {}", user.getEmail());
        } else if (!emailService.isMailEnabled()) {
            deliveryNote = "Mail is disabled. Use the temporary password returned below.";
            returnedPassword = temporaryPassword;
            log.warn("Mail disabled. Credentials for {} returned in API response.", user.getEmail());
        } else {
            deliveryNote = "Password was reset but Gmail SMTP failed. Use the temporary password below, " +
                    "then fix spring.mail.password in application-local.properties and restart the app.";
            returnedPassword = temporaryPassword;
            log.warn("SMTP delivery failed. Credentials for {} returned in API response.", user.getEmail());
        }

        return CredentialsDeliveryResponse.builder()
                .user(EntityMapper.toUserResponse(user))
                .emailSent(emailSent)
                .temporaryPassword(returnedPassword)
                .deliveryNote(deliveryNote)
                .build();
    }

    public UserResponse getById(Long id) {
        return EntityMapper.toUserResponse(findUser(id));
    }

    public PageResponse<UserResponse> getAll(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(EntityMapper::toUserResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<UserResponse> search(String query, Pageable pageable) {
        Page<UserResponse> page = userRepository.search(query, pageable).map(EntityMapper::toUserResponse);
        return PageMapper.toPageResponse(page);
    }

    public PageResponse<UserResponse> getByStatus(UserStatus status, Pageable pageable) {
        Page<UserResponse> page = userRepository.findByStatus(status, pageable).map(EntityMapper::toUserResponse);
        return PageMapper.toPageResponse(page);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findUser(id);
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        Set<RoleName> previousRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        user.setFullNames(request.getFullNames());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            user.setRoles(resolveRoles(request.getRoles()));

            Set<RoleName> newRoles = request.getRoles();
            if (!previousRoles.equals(newRoles)) {
                emailService.sendRoleChangeEmail(user.getEmail(), user.getFullNames(), newRoles);
                log.info("Role change email sent to {}", user.getEmail());
            }
        }

        user = userRepository.save(user);
        log.info("User updated: {}", user.getEmail());
        return EntityMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse assignRoles(Long id, Set<RoleName> roleNames) {
        User user = findUser(id);
        Set<RoleName> previousRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        user.setRoles(resolveRoles(roleNames));
        user = userRepository.save(user);

        if (!previousRoles.equals(roleNames)) {
            emailService.sendRoleChangeEmail(user.getEmail(), user.getFullNames(), roleNames);
            log.info("Roles assigned to {}: {}", user.getEmail(), roleNames);
        }

        return EntityMapper.toUserResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
        log.info("User deleted: {}", user.getEmail());
    }

    public User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private void validateStaffRoles(Set<RoleName> roles) {
        for (RoleName role : roles) {
            if (role != RoleName.ROLE_OPERATOR
                    && role != RoleName.ROLE_FINANCE
                    && role != RoleName.ROLE_CUSTOMER) {
                throw new BadRequestException(
                        "Admin user creation supports OPERATOR, FINANCE, and CUSTOMER roles only. " +
                        "Use a separate process for additional administrators.");
            }
        }
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
}
