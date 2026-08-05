package com.syncdocs.controller;

import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.DocumentPermissionRepository;
import com.syncdocs.repository.DocumentRepository;
import com.syncdocs.repository.RoleRepository;
import com.syncdocs.repository.UserRepository;
import com.syncdocs.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LocalShareControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentPermissionRepository permissionRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private com.syncdocs.service.MinioService minioService;

    private String token;

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());

        User user = userRepository.save(User.builder()
                .username("localuser")
                .email("local@test.com")
                .password(passwordEncoder.encode("pass"))
                .build());
        token = jwtTokenProvider.generateToken(user.getUsername());
    }

    @Test
    void share_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/local-shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localDocId\":\"doc1\",\"title\":\"Test\",\"sharedWith\":\"other\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("shared"));
    }

    @Test
    void share_ShouldRejectMissingFields() throws Exception {
        mockMvc.perform(post("/api/local-shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localDocId\":\"doc1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getSharedWith_ShouldReturnEmptyInitially() throws Exception {
        mockMvc.perform(get("/api/local-shares")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void share_WithEmptyTitle_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/local-shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localDocId\":\"doc1\",\"title\":\"\",\"sharedWith\":\"other\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void share_WithLongTitle_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/local-shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localDocId\":\"doc1\",\"title\":\"" + "a".repeat(256) + "\",\"sharedWith\":\"other\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void removeShare_NonExistent_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/local-shares/nonexistent")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("removed"));
    }

    @Test
    void share_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/local-shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localDocId\":\"doc1\",\"title\":\"Test\",\"sharedWith\":\"other\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void share_WithNullSharedWith_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/local-shares")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"localDocId\":\"doc1\",\"title\":\"Test\"}"))
                .andExpect(status().isBadRequest());
    }
}
