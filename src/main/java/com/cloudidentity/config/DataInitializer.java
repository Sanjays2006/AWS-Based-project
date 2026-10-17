package com.cloudidentity.config;

import com.cloudidentity.entity.Role;
import com.cloudidentity.entity.User;
import com.cloudidentity.repository.RoleRepository;
import com.cloudidentity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Runs once on application startup.
 * Creates the default admin user if no users exist in the database.
 * Roles and risk rules are already seeded via Flyway V6.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureAdminUserExists();
        ensureAnalystUserExists();
        ensureViewerUserExists();
    }

    private void ensureAdminUserExists() {
        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found. Ensure Flyway migration V6 has run."));

        User admin = userRepository.findByUsername("admin")
                .orElseGet(() -> userRepository.findByEmail("admin@cloudidentity.local").orElse(null));

        if (admin == null) {
            log.info("Admin account does not exist. Creating default admin account...");
            admin = User.builder()
                    .username("admin")
                    .email("admin@cloudidentity.local")
                    .passwordHash(passwordEncoder.encode("AdminPass123!"))
                    .fullName("System Administrator")
                    .isActive(true)
                    .mfaEnabled(false)
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Default admin user created — username: admin | email: admin@cloudidentity.local");
        } else {
            // Update admin credentials to ensure they match default configuration
            admin.setEmail("admin@cloudidentity.local");
            admin.setPasswordHash(passwordEncoder.encode("AdminPass123!"));
            if (admin.getRoles() == null || admin.getRoles().isEmpty()) {
                admin.setRoles(Set.of(adminRole));
            }
            userRepository.save(admin);
            log.info("Admin account credentials synchronized — username: admin | email: admin@cloudidentity.local");
        }
    }

    private void ensureAnalystUserExists() {
        Role analystRole = roleRepository.findByName("ANALYST").orElse(null);
        if (analystRole == null) return;

        User analyst = userRepository.findByUsername("analyst").orElse(null);
        if (analyst == null) {
            analyst = User.builder()
                    .username("analyst")
                    .email("analyst@cloudidentity.local")
                    .passwordHash(passwordEncoder.encode("AnalystPass123!"))
                    .fullName("Security Analyst")
                    .isActive(true)
                    .mfaEnabled(false)
                    .roles(Set.of(analystRole))
                    .build();
            userRepository.save(analyst);
            log.info("Default analyst user created — username: analyst");
        }
    }

    private void ensureViewerUserExists() {
        Role viewerRole = roleRepository.findByName("VIEWER").orElse(null);
        if (viewerRole == null) return;

        User viewer = User.builder()
                .username("viewer")
                .email("viewer@cloudidentity.local")
                .passwordHash(passwordEncoder.encode("ViewerPass123!"))
                .fullName("Security Viewer")
                .isActive(true)
                .mfaEnabled(false)
                .roles(Set.of(viewerRole))
                .build();
        if (userRepository.findByUsername("viewer").isEmpty()) {
            userRepository.save(viewer);
            log.info("Default viewer user created — username: viewer");
        }
    }
}
