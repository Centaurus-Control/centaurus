package de.shadowsoft.centaurus.server.auth;

import de.shadowsoft.centaurus.server.user.User;
import de.shadowsoft.centaurus.server.user.UserRepository;
import de.shadowsoft.centaurus.server.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final BootstrapPasswordGenerator bootstrapPasswordGenerator;

    public BootstrapAdminInitializer(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        BootstrapAdminProperties bootstrapAdminProperties,
        BootstrapPasswordGenerator bootstrapPasswordGenerator
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminProperties = bootstrapAdminProperties;
        this.bootstrapPasswordGenerator = bootstrapPasswordGenerator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String adminUsername = bootstrapAdminProperties.getUsername();
        if (userRepository.existsByUsernameAndDeletedFalse(adminUsername)) {
            return;
        }
        String password = resolveBootstrapPassword();
        String passwordHash = passwordEncoder.encode(password);
        User admin = new User(adminUsername, passwordHash, UserRole.ADMIN);
        boolean generatedPassword = !StringUtils.hasText(bootstrapAdminProperties.getPassword());
        if (generatedPassword) {
            admin.requirePasswordChange();
        }
        userRepository.save(admin);
        logBootstrapAdminCreated(adminUsername, password, generatedPassword);
    }

    private String resolveBootstrapPassword() {
        if (StringUtils.hasText(bootstrapAdminProperties.getPassword())) {
            return bootstrapAdminProperties.getPassword();
        }
        return bootstrapPasswordGenerator.generatePassword();
    }

    private void logBootstrapAdminCreated(String username, String password, boolean generatedPassword) {
        if (generatedPassword) {
            LOGGER.warn("Created bootstrap admin user '{}'. Password change is required after first login.", username);
            LOGGER.warn("Bootstrap admin temporary password: {}", password);
            return;
        }
        LOGGER.info("Created bootstrap admin user '{}' from configured bootstrap password.", username);
    }
}
