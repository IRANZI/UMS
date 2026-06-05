package com.java.Utility.Billing.System.config;

import com.java.Utility.Billing.System.entity.Role;
import com.java.Utility.Billing.System.entity.User;
import com.java.Utility.Billing.System.enums.RoleName;
import com.java.Utility.Billing.System.enums.UserStatus;
import com.java.Utility.Billing.System.repository.RoleRepository;
import com.java.Utility.Billing.System.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "iradianah5@gmail.com";
    private static final String LEGACY_ADMIN_EMAIL = "admin@wasac.rw";
    private static final String ADMIN_PASSWORD = "Admin@12345";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                Role role = Role.builder().name(roleName).build();
                log.info("Seeding role: {}", roleName);
                return roleRepository.save(role);
            });
        }
    }

    private void seedAdminUser() {
        Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                .orElseThrow();

        Optional<User> existingAdmin = userRepository.findByEmail(ADMIN_EMAIL)
                .or(() -> userRepository.findByEmail(LEGACY_ADMIN_EMAIL))
                .or(() -> userRepository.findAll().stream()
                        .filter(u -> u.getRoles().stream()
                                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN))
                        .findFirst());

        if (existingAdmin.isPresent()) {
            User admin = existingAdmin.get();
            admin.setEmail(ADMIN_EMAIL);
            admin.setFullNames("System Administrator");
            admin.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmailVerified(true);
            admin.setMustChangePassword(false);
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
            log.info("Admin account ensured: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
            return;
        }

        User admin = User.builder()
                .fullNames("System Administrator")
                .email(ADMIN_EMAIL)
                .phoneNumber("+250788000001")
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .mustChangePassword(false)
                .roles(Set.of(adminRole))
                .build();
        userRepository.save(admin);
        log.info("Default admin user created: {} / {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }
}
