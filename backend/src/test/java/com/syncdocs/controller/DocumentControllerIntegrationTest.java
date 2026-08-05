package com.syncdocs.controller;

import tools.jackson.databind.ObjectMapper;
import com.syncdocs.dto.request.DocumentCreateRequest;
import com.syncdocs.model.Document;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentPermissionRepository permissionRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private com.syncdocs.service.MinioService minioService;

    private String token;
    private User user;
    private Long docId;

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());

        user = User.builder()
                .username("docowner")
                .email("owner@example.com")
                .password("encoded")
                .build();
        user = userRepository.save(user);
        token = jwtTokenProvider.generateToken(user.getUsername());
    }

    private void createTestDoc() {
        Document doc = Document.builder()
                .title("Existing Doc")
                .owner(user)
                .mimeType("text/plain")
                .status(com.syncdocs.model.enums.DocumentStatus.ACTIVE)
                .version(1L)
                .build();
        doc = documentRepository.save(doc);
        docId = doc.getId();

        org.mockito.Mockito.when(minioService.getObject(docId + "/1"))
                .thenReturn("test content".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void createDocument_ShouldReturn201() throws Exception {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setTitle("Integration Test Doc");
        request.setContent("Test content");
        request.setMimeType("text/plain");

        org.mockito.Mockito.doNothing().when(minioService).putObject(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Integration Test Doc"))
                .andExpect(jsonPath("$.owner").value("docowner"));
    }

    @Test
    void createDocument_ShouldRejectEmptyTitle() throws Exception {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setTitle("");
        request.setContent("content");

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDocument_NonExistent_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/documents/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteDocument_NonExistent_ShouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/documents/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateDocument_NonExistent_ShouldReturn400() throws Exception {
        String body = "{\"title\":\"Updated\"}";
        mockMvc.perform(put("/api/documents/99999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadDocument_NonExistent_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/documents/99999/download")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listDocuments_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getEvents_ShouldReturn200ForExistingDoc() throws Exception {
        createTestDoc();

        mockMvc.perform(get("/api/documents/" + docId + "/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void getEvents_NonExistentDoc_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/documents/99999/events")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getEvents_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/documents/1/events"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDocuments_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }
}
