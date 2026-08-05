package com.syncdocs.security;

import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.DocumentPermissionRepository;
import com.syncdocs.repository.DocumentRepository;
import com.syncdocs.repository.RoleRepository;
import com.syncdocs.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private DocumentRepository documentRepository;
    @Autowired private DocumentPermissionRepository permissionRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9ubHktbWluaW11bS0zMi1ieXRlcw==";
    private User user;

    @BeforeEach
    void setUp() {
        permissionRepository.deleteAll();
        documentRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());

        user = userRepository.save(User.builder()
                .username("authtest")
                .email("auth@test.com")
                .password("encoded")
                .build());
    }

    @Test
    void requestWithValidToken_ShouldReturn200() throws Exception {
        String token = jwtTokenProvider.generateToken(user.getUsername());
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithoutToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/documents"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithInvalidToken_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer invalid-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithExpiredToken_ShouldReturn401() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        String expiredToken = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .expiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key)
                .compact();

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithTamperedToken_ShouldReturn401() throws Exception {
        String token = jwtTokenProvider.generateToken(user.getUsername());
        // Tamper with the payload section (between first and second dot)
        int firstDot = token.indexOf('.');
        int secondDot = token.indexOf('.', firstDot + 1);
        String tampered = token.substring(0, firstDot + 1)
                + "X" + token.substring(firstDot + 2, secondDot)
                + token.substring(secondDot);

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithMalformedHeader_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithWrongScheme_ShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Basic dGVzdDp0ZXN0"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requestWithTokenForDeletedUser_ShouldReturn401() throws Exception {
        String token = jwtTokenProvider.generateToken(user.getUsername());
        userRepository.delete(user);

        mockMvc.perform(get("/api/documents")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }
}
