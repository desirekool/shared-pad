package com.syncdocs.runner;

import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.RoleRepository;
import com.syncdocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("testuser", "test@example.com", "password123", RoleName.ROLE_OWNER);
        seedUser("user2", "user2@test.com", "password123", RoleName.ROLE_EDITOR);
    }

    private void seedUser(String username, String email, String password, RoleName roleName) {
        userRepository.findByUsername(username).ifPresentOrElse(
                existing -> {
                    existing.setEmail(email);
                    existing.setPassword(passwordEncoder.encode(password));
                    userRepository.save(existing);
                    log.info("Updated test user: {} / {}", username, password);
                },
                () -> {
                    Role role = roleRepository.findByName(roleName)
                            .orElseThrow(() -> new RuntimeException(roleName + " not found"));

                    User user = User.builder()
                            .username(username)
                            .email(email)
                            .password(passwordEncoder.encode(password))
                            .roles(Set.of(role))
                            .build();

                    userRepository.save(user);
                    log.info("Created test user: {} / {} ({})", username, password, roleName);
                }
        );
    }
}
