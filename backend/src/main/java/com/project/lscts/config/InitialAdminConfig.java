
package com.project.lscts.config;

import com.project.lscts.entity.Role;
import com.project.lscts.entity.UserEntity;
import com.project.lscts.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@Configuration
public class InitialAdminConfig {

    @Bean
    CommandLineRunner createInitialAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            String adminEmail = System.getenv("LSTS_ADMIN_EMAIL");
            String adminPassword = System.getenv("LSTS_ADMIN_PASSWORD");

            if (adminEmail == null || adminEmail.isBlank()
                    || adminPassword == null
                    || adminPassword.length() < 8) {

                throw new IllegalStateException(
                        "Set LSTS_ADMIN_EMAIL and LSTS_ADMIN_PASSWORD "
                                + "environment variables before starting the application."
                );
            }

            adminEmail = adminEmail.trim().toLowerCase(Locale.ROOT);

            if (userRepository.existsByEmail(adminEmail)) {
                return;
            }

            UserEntity admin = new UserEntity();

            admin.setName("System Admin");
            admin.setEmail(adminEmail);
            admin.setPhoneNumber("0123456789");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);

            System.out.println("Initial admin account created.");
        };
    }
}
