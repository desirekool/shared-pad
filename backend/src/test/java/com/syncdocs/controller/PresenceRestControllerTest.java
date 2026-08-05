package com.syncdocs.controller;

import com.syncdocs.events.UserPresence;
import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.RoleRepository;
import com.syncdocs.repository.UserRepository;
import com.syncdocs.security.JwtTokenProvider;
import com.syncdocs.service.PresenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PresenceRestControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private PresenceService presenceService;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());

        User user = userRepository.save(User.builder()
                .username("presenceuser")
                .email("presence@test.com")
                .password(passwordEncoder.encode("pass"))
                .build());
        token = jwtTokenProvider.generateToken(user.getUsername());
    }

    @Test
    void getActiveUsers_ShouldReturnUsers() throws Exception {
        when(presenceService.getActiveUsers("1")).thenReturn(List.of(
                UserPresence.builder().userId("alice").build()
        ));

        mockMvc.perform(get("/api/documents/1/presence")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("alice"));
    }

    @Test
    void getActiveCount_ShouldReturnCount() throws Exception {
        when(presenceService.getActiveCount("1")).thenReturn(3);

        mockMvc.perform(get("/api/documents/1/presence/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("3"));
    }

    @Test
    void getActiveUsers_NonExistentDoc_ShouldReturnEmpty() throws Exception {
        when(presenceService.getActiveUsers("99999")).thenReturn(List.of());

        mockMvc.perform(get("/api/documents/99999/presence")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getActiveCount_NoUsers_ShouldReturnZero() throws Exception {
        when(presenceService.getActiveCount("99999")).thenReturn(0);

        mockMvc.perform(get("/api/documents/99999/presence/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("0"));
    }

    @Test
    void getActiveUsers_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/documents/1/presence"))
                .andExpect(status().isUnauthorized());
    }
}
