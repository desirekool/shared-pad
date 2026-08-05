package com.syncdocs.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.AuthenticationException;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationEntryPointTest {

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private AuthenticationException authException;

    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint();

    @Test
    void commence_ShouldSend401() throws Exception {
        entryPoint.commence(request, response, authException);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
    }
}
