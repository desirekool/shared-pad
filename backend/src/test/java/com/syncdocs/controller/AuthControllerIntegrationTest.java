package com.syncdocs.controller;

import tools.jackson.databind.ObjectMapper;
import com.syncdocs.dto.request.LoginRequest;
import com.syncdocs.dto.request.RegisterRequest;
import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.RoleRepository;
import com.syncdocs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        roleRepository.save(Role.builder().name(RoleName.ROLE_VIEWER).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_EDITOR).build());
        roleRepository.save(Role.builder().name(RoleName.ROLE_OWNER).build());
    }

    @Test
    void register_ShouldReturnToken() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void register_ShouldRejectDuplicateUsername() throws Exception {
        User user = User.builder()
                .username("existing")
                .email("existing@example.com")
                .password(passwordEncoder.encode("pass"))
                .build();
        userRepository.save(user);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setEmail("other@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturnToken() throws Exception {
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        userRepository.save(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_ShouldRejectBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // --- Input validation tests ---

    @Test
    void register_ShouldRejectMissingUsername() throws Exception {
        String body = """
                {"email":"test@example.com","password":"password123"}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectInvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("not-an-email");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectShortPassword() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("test@example.com");
        request.setPassword("ab");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- JSON structure tests ---

    @Test
    void register_ShouldReturnFullJsonStructure() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("jsonuser");
        request.setEmail("json@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("jsonuser"))
                .andExpect(jsonPath("$.email").value("json@example.com"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void register_ShouldReturnErrorJsonOnDuplicate() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("dupuser");
        request.setEmail("dup@example.com");
        request.setPassword("password123");

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same username fails
        RegisterRequest dup = new RegisterRequest();
        dup.setUsername("dupuser");
        dup.setEmail("other@example.com");
        dup.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // --- Login edge cases ---

    @Test
    void login_ShouldReturnFullJsonStructure() throws Exception {
        // Register user first
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("loginjson");
        reg.setEmail("loginjson@example.com");
        reg.setPassword("password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login and check response
        LoginRequest login = new LoginRequest();
        login.setUsername("loginjson");
        login.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.username").value("loginjson"))
                .andExpect(jsonPath("$.email").value("loginjson@example.com"))
                .andExpect(jsonPath("$.roles").isArray());
    }

    @Test
    void login_ShouldReturnErrorJsonOnWrongPassword() throws Exception {
        // Register user first
        RegisterRequest reg = new RegisterRequest();
        reg.setUsername("wrongpass");
        reg.setEmail("wrongpass@example.com");
        reg.setPassword("password123");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Login with wrong password
        LoginRequest login = new LoginRequest();
        login.setUsername("wrongpass");
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void login_ShouldRejectNonexistentUser() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // --- Public endpoint tests ---

    @Test
    void authEndpoints_ShouldBePublic_Login() throws Exception {
        // Login endpoint should be accessible without auth
        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized()); // Still 401 for bad creds, not 403
    }

    @Test
    void authEndpoints_ShouldBePublic_Register() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("publicuser");
        request.setEmail("public@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()); // Works without auth header
    }

    @Test
    void authEndpoints_ShouldNotRequireAuthHeader() throws Exception {
        // No Authorization header should still work for auth endpoints
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isUnauthorized()); // Auth fails, but not 403
    }

    @Test
    void register_ShouldRejectDuplicateEmail() throws Exception {
        RegisterRequest first = new RegisterRequest();
        first.setUsername("firstuser");
        first.setEmail("dupe@example.com");
        first.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = new RegisterRequest();
        second.setUsername("seconduser");
        second.setEmail("dupe@example.com");
        second.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectMissingEmail() throws Exception {
        String body = """
                {"username":"nouser","password":"password123"}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectMissingPassword() throws Exception {
        String body = """
                {"username":"nouser","email":"test@example.com"}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectAllEmpty() throws Exception {
        String body = """
                {"username":"","email":"","password":""}
                """;
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectTooLongUsername() throws Exception {
        String longName = "a".repeat(51);
        RegisterRequest request = new RegisterRequest();
        request.setUsername(longName);
        request.setEmail("long@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectTooLongEmail() throws Exception {
        String longEmail = "a".repeat(100) + "@example.com";
        RegisterRequest request = new RegisterRequest();
        request.setUsername("longemail");
        request.setEmail(longEmail);
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectEmptyUsername() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("empty@example.com");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldRejectEmptyJsonBody() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldRejectMissingUsername() throws Exception {
        String body = """
                {"password":"password123"}
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldRejectMissingPassword() throws Exception {
        String body = """
                {"username":"someuser"}
                """;
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
