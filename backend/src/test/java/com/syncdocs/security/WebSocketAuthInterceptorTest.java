package com.syncdocs.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.User;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private MessageChannel channel;
    @Mock private Message<?> message;
    @Mock private StompHeaderAccessor accessor;

    @InjectMocks private WebSocketAuthInterceptor interceptor;

    @Test
    void preSend_ShouldAuthenticateConnectWithValidToken() {
        when(accessor.getCommand()).thenReturn(StompCommand.CONNECT);
        when(accessor.getFirstNativeHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.validateToken("valid-token")).thenReturn(true);
        when(jwtTokenProvider.getUsernameFromToken("valid-token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice"))
                .thenReturn(new User("alice", "pass", Collections.emptyList()));

        try (var mockedStatic = mockStatic(org.springframework.messaging.support.MessageHeaderAccessor.class)) {
            mockedStatic.when(() -> org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class))
                    .thenReturn(accessor);

            interceptor.preSend(message, channel);

            verify(jwtTokenProvider).validateToken("valid-token");
            verify(accessor).setUser(any());
        }
    }

    @Test
    void preSend_ShouldSkipForNonConnect() {
        when(accessor.getCommand()).thenReturn(StompCommand.SUBSCRIBE);

        try (var mockedStatic = mockStatic(org.springframework.messaging.support.MessageHeaderAccessor.class)) {
            mockedStatic.when(() -> org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class))
                    .thenReturn(accessor);

            interceptor.preSend(message, channel);

            verifyNoInteractions(jwtTokenProvider);
        }
    }

    @Test
    void preSend_ShouldSkipForMissingToken() {
        when(accessor.getCommand()).thenReturn(StompCommand.CONNECT);
        when(accessor.getFirstNativeHeader("Authorization")).thenReturn(null);

        try (var mockedStatic = mockStatic(org.springframework.messaging.support.MessageHeaderAccessor.class)) {
            mockedStatic.when(() -> org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class))
                    .thenReturn(accessor);

            interceptor.preSend(message, channel);

            verifyNoInteractions(jwtTokenProvider);
        }
    }

    @Test
    void preSend_ShouldSkipForInvalidToken() {
        when(accessor.getCommand()).thenReturn(StompCommand.CONNECT);
        when(accessor.getFirstNativeHeader("Authorization")).thenReturn("Bearer invalid");
        when(jwtTokenProvider.validateToken("invalid")).thenReturn(false);

        try (var mockedStatic = mockStatic(org.springframework.messaging.support.MessageHeaderAccessor.class)) {
            mockedStatic.when(() -> org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class))
                    .thenReturn(accessor);

            interceptor.preSend(message, channel);

            verify(jwtTokenProvider).validateToken("invalid");
            verifyNoInteractions(userDetailsService);
        }
    }
}
