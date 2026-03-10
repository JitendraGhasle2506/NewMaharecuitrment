package com.maharecruitment.gov.in.auth.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

@Component
public class AdminBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.name:System Admin}")
    private String adminName;

    @Value("${app.bootstrap.admin.password:ifms123}")
    private String adminPassword;

    public AdminBootstrapInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Admin bootstrap is disabled.");
            return;
        }

        String normalizedUsername = normalizeRequired(adminUsername, "Admin username");
        String normalizedAdminName = normalizeRequired(adminName, "Admin name");
        String resolvedPassword = normalizeRequired(adminPassword, "Admin password");

        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                .orElseGet(this::createAdminRole);

        User existingAdmin = userRepository.findByEmailIgnoreCase(normalizedUsername).orElse(null);
        if (existingAdmin == null) {
            User admin = new User();
            admin.setName(normalizedAdminName);
            admin.setEmail(normalizedUsername);
            admin.setPassword(passwordEncoder.encode(resolvedPassword));
            admin.setRoles(new ArrayList<>(List.of(adminRole)));
            userRepository.save(admin);
            log.warn("Bootstrap admin user created with username='{}'. Change password immediately.", normalizedUsername);
            return;
        }

        if (existingAdmin.getRoles() == null) {
            existingAdmin.setRoles(new ArrayList<>());
        }

        boolean hasAdminRole = existingAdmin.getRoles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));
        if (!hasAdminRole) {
            existingAdmin.getRoles().add(adminRole);
            userRepository.save(existingAdmin);
            log.info("Existing admin user '{}' updated with ADMIN role.", normalizedUsername);
        } else {
            log.info("Admin bootstrap completed; user '{}' already present.", normalizedUsername);
        }
    }

    private Role createAdminRole() {
        Role role = new Role();
        role.setName("ADMIN");
        Role saved = roleRepository.save(role);
        log.info("Bootstrap role created: {}", saved.getName());
        return saved;
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(label + " is required for admin bootstrap.");
        }
        return value.trim();
    }
}
