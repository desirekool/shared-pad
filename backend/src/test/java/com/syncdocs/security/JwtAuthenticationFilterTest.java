package com.syncdocs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @InjectMocks private JwtAuthenticationFilter filter;

    @Test
    void doFilter_ShouldContinueChainWhenNoToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    void doFilter_ShouldSetAuthContextWhenValidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid-token")).thenReturn("testuser");
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(
                new User("testuser", "pass", Collections.emptyList()));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider).validateToken("valid-token");
        verify(userDetailsService).loadUserByUsername("testuser");
    }

    @Test
    void doFilter_ShouldClearContextWhenDeletedUser() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-for-deleted");
        when(jwtTokenProvider.validateToken("token-for-deleted")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("token-for-deleted")).thenReturn("deleteduser");
        when(userDetailsService.loadUserByUsername("deleteduser"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_ShouldNotSetAuthWhenInvalidToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userDetailsService);
    }
}
