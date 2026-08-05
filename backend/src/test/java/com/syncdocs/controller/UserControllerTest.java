package com.syncdocs.controller;

import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.RoleRepository;
import com.syncdocs.repository.UserRepository;
import com.syncdocs.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());

        userRepository.save(User.builder()
                .username("alice")
                .email("alice@test.com")
                .password(passwordEncoder.encode("pass"))
                .build());
        userRepository.save(User.builder()
                .username("bob")
                .email("bob@test.com")
                .password(passwordEncoder.encode("pass"))
                .build());

        User current = userRepository.save(User.builder()
                .username("searcher")
                .email("search@test.com")
                .password(passwordEncoder.encode("pass"))
                .build());
        token = jwtTokenProvider.generateToken(current.getUsername());
    }

    @Test
    void search_ShouldReturnMatchingUsernames() throws Exception {
        mockMvc.perform(get("/api/users/search?q=al")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("alice"));
    }

    @Test
    void search_ShouldExcludeCurrentUser() throws Exception {
        mockMvc.perform(get("/api/users/search?q=search")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_ShouldReturnEmptyForNoMatch() throws Exception {
        mockMvc.perform(get("/api/users/search?q=zzzz")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_WithEmptyQuery_ShouldNotFail() throws Exception {
        mockMvc.perform(get("/api/users/search?q=")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void search_WithLongQuery_ShouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/users/search?q=" + "a".repeat(100))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void search_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/users/search?q=alice"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void search_WithSqlInjection_ShouldReturnEmpty() throws Exception {
        mockMvc.perform(get("/api/users/search?q=' OR 1=1 --")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
