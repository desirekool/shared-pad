package com.syncdocs.controller;

import com.syncdocs.model.Document;
import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.DocumentStatus;
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
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PermissionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentPermissionRepository permissionRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private String ownerToken;
    private User owner;
    private User editor;
    private Document doc;

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());

        owner = userRepository.save(User.builder()
                .username("docowner")
                .email("owner@test.com")
                .password("encoded")
                .build());
        ownerToken = jwtTokenProvider.generateToken(owner.getUsername());

        editor = userRepository.save(User.builder()
                .username("editoruser")
                .email("editor@test.com")
                .password("encoded")
                .build());

        doc = documentRepository.save(Document.builder()
                .title("Test Doc")
                .owner(owner)
                .status(DocumentStatus.ACTIVE)
                .version(1L)
                .mimeType("text/plain")
                .build());
    }

    @Test
    void shareAndRevoke_ShouldWork() throws Exception {
        // 1. Share with editor
        mockMvc.perform(post("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"editoruser\",\"permissionLevel\":\"EDITOR\"}"))
                .andExpect(status().isOk());

        // 2. List permissions - get the permission ID dynamically
        String permJson = mockMvc.perform(get("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").exists())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode permArr = mapper.readTree(permJson);
        int permissionId = -1;
        for (JsonNode node : permArr) {
            if (!"OWNER".equals(node.get("permissionLevel").asText())) {
                permissionId = node.get("id").asInt();
                break;
            }
        }

        // 3. Revoke using the permission's ID (not user ID)
        mockMvc.perform(delete("/api/documents/{documentId}/permissions/{permissionId}", doc.getId(), permissionId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        // 4. Verify permission list no longer includes editor
        mockMvc.perform(get("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='editoruser')]").doesNotExist());
    }

    @Test
    void revoke_ShouldFailForNonOwner() throws Exception {
        String editorToken = jwtTokenProvider.generateToken(editor.getUsername());

        // First share
        mockMvc.perform(post("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"editoruser\",\"permissionLevel\":\"EDITOR\"}"))
                .andExpect(status().isOk());

        // Get the editor's permission ID
        String permJson = mockMvc.perform(get("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode permArr = mapper.readTree(permJson);
        int permissionId = -1;
        for (JsonNode node : permArr) {
            if (!"OWNER".equals(node.get("permissionLevel").asText())) {
                permissionId = node.get("id").asInt();
                break;
            }
        }

        // Editor tries to revoke - should fail
        mockMvc.perform(delete("/api/documents/{documentId}/permissions/{permissionId}", doc.getId(), permissionId)
                        .header("Authorization", "Bearer " + editorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void revoke_WithInvalidPermissionId_ShouldReturn400() throws Exception {
        mockMvc.perform(delete("/api/documents/{documentId}/permissions/{permissionId}", doc.getId(), 99999)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void share_ToNonExistentUser_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nonexistent\",\"permissionLevel\":\"EDITOR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void share_InvalidDocumentId_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/documents/99999/permissions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"editoruser\",\"permissionLevel\":\"EDITOR\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void share_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(post("/api/documents/{documentId}/permissions", doc.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"editoruser\",\"permissionLevel\":\"EDITOR\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void share_InvalidPermissionLevel_ShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"editoruser\",\"permissionLevel\":\"INVALID\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void share_DuplicateShare_ShouldUpdateLevel() throws Exception {
        // Share as EDITOR first
        mockMvc.perform(post("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"editoruser\",\"permissionLevel\":\"EDITOR\"}"))
                .andExpect(status().isOk());

        // Share again as VIEWER - should update level, not create duplicate
        mockMvc.perform(post("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"editoruser\",\"permissionLevel\":\"VIEWER\"}"))
                .andExpect(status().isOk());

        // Verify only one permission entry exists for editoruser
        String permJson = mockMvc.perform(get("/api/documents/{documentId}/permissions", doc.getId())
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        tools.jackson.databind.ObjectMapper mapper = new tools.jackson.databind.ObjectMapper();
        tools.jackson.databind.JsonNode permArr = mapper.readTree(permJson);
        long editorCount = 0;
        for (tools.jackson.databind.JsonNode node : permArr) {
            if ("editoruser".equals(node.get("username").asText())) {
                editorCount++;
                assertEquals("VIEWER", node.get("permissionLevel").asText());
            }
        }
        assertEquals(1, editorCount, "Should have exactly one permission entry");
    }
}
