package com.maharecruitment.gov.in.auth.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.maharecruitment.gov.in.auth.entity.Role;
import com.maharecruitment.gov.in.auth.entity.User;
import com.maharecruitment.gov.in.auth.repository.RoleRepository;
import com.maharecruitment.gov.in.auth.repository.UserRepository;

@Component
@Order(1)
public class AdminBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.name:System Admin}")
    private String adminName;

    @Value("${app.bootstrap.admin.password:ifms123}")
    private String adminPassword;

    @Value("${app.bootstrap.hr.username:hr@mahait.org}")
    private String hrUsername;

    @Value("${app.bootstrap.hr.name:HR Manager}")
    private String hrName;

    @Value("${app.bootstrap.hr.password:ifms123}")
    private String hrPassword;

    @Value("${app.bootstrap.auditor.username:auditor@mahait.org}")
    private String auditorUsername;

    @Value("${app.bootstrap.auditor.name:Auditor User}")
    private String auditorName;

    @Value("${app.bootstrap.auditor.password:ifms123}")
    private String auditorPassword;

    public AdminBootstrapInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JdbcTemplate jdbcTemplate) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Admin bootstrap is disabled.");
            return;
        }

        bootstrapRoles();

        String normalizedUsername = normalizeRequired(adminUsername, "Admin username");
        String normalizedAdminName = normalizeRequired(adminName, "Admin name");
        String resolvedPassword = normalizeRequired(adminPassword, "Admin password");

        Role adminRole = roleRepository.findByNameIgnoreCase("ADMIN")
                .orElseThrow(() -> new IllegalStateException("ADMIN role should have been bootstrapped."));

        User existingAdmin = userRepository.findByEmailIgnoreCase(normalizedUsername).orElse(null);
        if (existingAdmin == null) {
            User admin = new User();
            admin.setName(normalizedAdminName);
            admin.setEmail(normalizedUsername);
            admin.setPassword(passwordEncoder.encode(resolvedPassword));
            admin.setRoles(new ArrayList<>(List.of(adminRole)));
            userRepository.save(admin);
            log.warn("Bootstrap admin user created with username='{}'. Change password immediately.",
                    normalizedUsername);
        } else {
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

        bootstrapHrUser();
        bootstrapAuditorUser();
    }

    private void bootstrapAuditorUser() {
        String normalizedAuditorUsername = normalizeRequired(auditorUsername, "Auditor username");
        String normalizedAuditorName = normalizeRequired(auditorName, "Auditor name");
        String resolvedAuditorPassword = normalizeRequired(auditorPassword, "Auditor password");

        Role auditorRole = roleRepository.findByNameIgnoreCase("ROLE_AUDITOR")
                .orElseThrow(() -> new IllegalStateException("ROLE_AUDITOR should have been bootstrapped."));

        User existingAuditor = userRepository.findByEmailIgnoreCase(normalizedAuditorUsername).orElse(null);
        if (existingAuditor == null) {
            User auditor = new User();
            auditor.setName(normalizedAuditorName);
            auditor.setEmail(normalizedAuditorUsername);
            auditor.setPassword(passwordEncoder.encode(resolvedAuditorPassword));
            auditor.setRoles(new ArrayList<>(List.of(auditorRole)));
            userRepository.save(auditor);
            log.warn("Bootstrap Auditor user created with username='{}'. Change password immediately.",
                    normalizedAuditorUsername);
        } else {
            if (existingAuditor.getRoles() == null) {
                existingAuditor.setRoles(new ArrayList<>());
            }
            boolean hasAuditorRole = existingAuditor.getRoles().stream()
                    .anyMatch(role -> "ROLE_AUDITOR".equalsIgnoreCase(role.getName()));
            if (!hasAuditorRole) {
                existingAuditor.getRoles().add(auditorRole);
                userRepository.save(existingAuditor);
                log.info("Existing Auditor user '{}' updated with ROLE_AUDITOR role.", normalizedAuditorUsername);
            } else {
                log.info("Auditor bootstrap completed; user '{}' already present.", normalizedAuditorUsername);
            }
        }
    }

    private void bootstrapHrUser() {
        String normalizedHrUsername = normalizeRequired(hrUsername, "HR username");
        String normalizedHrName = normalizeRequired(hrName, "HR name");
        String resolvedHrPassword = normalizeRequired(hrPassword, "HR password");

        Role hrRole = roleRepository.findByNameIgnoreCase("ROLE_HR")
                .orElseThrow(() -> new IllegalStateException("ROLE_HR should have been bootstrapped."));

        User existingHr = userRepository.findByEmailIgnoreCase(normalizedHrUsername).orElse(null);
        if (existingHr == null) {
            User hr = new User();
            hr.setName(normalizedHrName);
            hr.setEmail(normalizedHrUsername);
            hr.setPassword(passwordEncoder.encode(resolvedHrPassword));
            hr.setRoles(new ArrayList<>(List.of(hrRole)));
            userRepository.save(hr);
            log.warn("Bootstrap HR user created with username='{}'. Change password immediately.",
                    normalizedHrUsername);
        } else {
            if (existingHr.getRoles() == null) {
                existingHr.setRoles(new ArrayList<>());
            }
            boolean hasHrRole = existingHr.getRoles().stream()
                    .anyMatch(role -> "ROLE_HR".equalsIgnoreCase(role.getName()));
            if (!hasHrRole) {
                existingHr.getRoles().add(hrRole);
                userRepository.save(existingHr);
                log.info("Existing HR user '{}' updated with ROLE_HR role.", normalizedHrUsername);
            } else {
                log.info("HR bootstrap completed; user '{}' already present.", normalizedHrUsername);
            }
        }
    }

    private void bootstrapRoles() {
        String[] roles = {
                "ADMIN", "ROLE_ADMIN", "ROLE_USER", "ROLE_AGENCY", "ROLE_HR",
                "ROLE_STM", "ROLE_HOD2", "ROLE_HOD1", "ROLE_COO", "ROLE_PM",
                "ROLE_HOD3", "ROLE_STM1", "ROLE_DEPARTMENT", "ROLE_AUDITOR",
                "ROLE_EMPLOYEE", "ROLE_MAHAIT_ADMIN"
        };

        for (String roleName : roles) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM roles WHERE UPPER(name) = UPPER(?)",
                    Integer.class, roleName);

            if (count != null && count == 0) {
                jdbcTemplate.update("INSERT INTO roles (name) VALUES (?)", roleName);
                log.info("Bootstrapped role: {}", roleName);
            }
        }
    }

    private String normalizeRequired(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(label + " is required for admin bootstrap.");
        }
        return value.trim();
    }
}
