package com.syncdocs.security;

import com.syncdocs.model.Role;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_ShouldReturnUserDetails() {
        User user = User.builder()
                .username("alice")
                .password("encoded")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_EDITOR).build()))
                .build();

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails result = userDetailsService.loadUserByUsername("alice");

        assertEquals("alice", result.getUsername());
        assertEquals("encoded", result.getPassword());
        assertEquals(1, result.getAuthorities().size());
    }

    @Test
    void loadUserByUsername_ShouldThrowWhenNotFound() {
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("nobody"));
    }
}
