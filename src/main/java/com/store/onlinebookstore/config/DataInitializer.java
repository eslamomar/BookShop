package com.store.onlinebookstore.config;

import com.store.onlinebookstore.model.Customer;
import com.store.onlinebookstore.model.Role;
import com.store.onlinebookstore.repository.CustomerRepository;
import com.store.onlinebookstore.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.data.enabled:true}")
    private boolean initEnabled;

    @Value("${app.admin.email:admin@bookstore.com}")
    private String adminEmail;

    // No insecure default here — force setting this via env/secret
    @Value("${app.admin.password:}")
    private String adminPassword;

    @Value("${app.admin.name:Store Administrator}")
    private String adminName;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    public DataInitializer(CustomerRepository customerRepository,
                           RoleRepository roleRepository,
                           PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!initEnabled) {
            log.info("Data initializer disabled (app.init.data.enabled=false). Skipping.");
            return;
        }

        // In any environment, we refuse to create an admin with a blank password.
        if (adminPassword == null || adminPassword.isBlank()) {
            // Be explicit in prod-like profiles
            if (isProdLike()) {
                throw new IllegalStateException("Admin password must be provided in production (app.admin.password).");
            }
            throw new IllegalStateException("Admin password is missing (app.admin.password).");
        }

        long count = customerRepository.count();
        log.info("DataInitializer starting. Customer count={}", count);

        ensureRole("ROLE_ADMIN");
        ensureRole("ROLE_CUSTOMER");

        // Create admin if not present — race-safe with unique email constraint in DB
        if (customerRepository.findByEmail(adminEmail).isEmpty()) {
            createAdminUserSafely();
        } else {
            log.info("Admin already exists for {}", maskEmail(adminEmail));
        }
    }

    private void ensureRole(String roleName) {
        Role existing = roleRepository.findByName(roleName);
        if (existing == null) {
            roleRepository.save(new Role(roleName));
            log.info("Created role {}", roleName);
        } else {
            log.debug("Role {} already exists", roleName);
        }
    }

    private void createAdminUserSafely() {
        Role adminRole = Optional.ofNullable(roleRepository.findByName("ROLE_ADMIN"))
                .orElseThrow(() -> new IllegalStateException("ROLE_ADMIN missing"));

        Customer admin = new Customer();
        admin.setName(adminName);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));

        admin.setRoles(Collections.singletonList(adminRole));

        try {
            Customer saved = customerRepository.save(admin);
            log.info("Admin user ensured. id={}, email={}", saved.getId(), maskEmail(adminEmail));
        } catch (DataIntegrityViolationException e) {
            // If two instances race, one will win; the other hits unique constraint — safe to continue
            log.warn("Admin creation raced with another instance (likely unique email). Continuing. {}", e.getMessage());
        }
    }

    private boolean isProdLike() {
        if (activeProfiles == null || activeProfiles.isBlank()) return false;
        String p = activeProfiles.toLowerCase();
        return p.contains("prod") || p.contains("production");
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}
