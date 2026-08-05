package com.syncdocs.controller;

import com.syncdocs.model.AuditLog;
import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.RoleRepository;
import com.syncdocs.repository.UserRepository;
import com.syncdocs.security.JwtTokenProvider;
import com.syncdocs.service.AuditService;
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
class AuditControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AuditService auditService;

    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());

        User user = userRepository.save(User.builder()
                .username("audituser")
                .email("audit@test.com")
                .password(passwordEncoder.encode("pass"))
                .build());
        token = jwtTokenProvider.generateToken(user.getUsername());
    }

    @Test
    void getDocumentAudit_ShouldReturnList() throws Exception {
        when(auditService.getDocumentAudit(1L)).thenReturn(List.of(
                AuditLog.builder().id(1L).eventType("EDIT").build()
        ));

        mockMvc.perform(get("/api/audit/documents/1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("EDIT"));
    }

    @Test
    void getUserAudit_ShouldReturnList() throws Exception {
        when(auditService.getUserAudit("user1")).thenReturn(List.of(
                AuditLog.builder().id(2L).eventType("LOGIN").build()
        ));

        mockMvc.perform(get("/api/audit/users/user1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventType").value("LOGIN"));
    }

    @Test
    void getDocumentAudit_NonExistent_ShouldReturnEmpty() throws Exception {
        when(auditService.getDocumentAudit(99999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/audit/documents/99999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getDocumentAudit_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/audit/documents/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserAudit_ShouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/audit/users/user1"))
                .andExpect(status().isUnauthorized());
    }
}
