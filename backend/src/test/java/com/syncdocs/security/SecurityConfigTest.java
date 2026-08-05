package com.syncdocs.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void authEndpoint_ShouldBePublic() throws Exception {
        // POST-only endpoint should not be 401/403 (meaning filter intercepted it)
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isMethodNotAllowed()); // 405 = endpoint reachable, wrong method
    }

    @Test
    void healthEndpoint_ShouldBePublic() throws Exception {
        // Should not return 401/403 (meaning it passed auth filter, even if status=503 w/o MinIO)
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assertTrue(status != 401 && status != 403,
                "Health endpoint should be accessible without auth, but got " + status);
    }

    @Test
    void protectedEndpoint_ShouldRejectWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cors_ShouldAllowConfiguredOrigins() throws Exception {
        mockMvc.perform(options("/api/documents")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }
}
