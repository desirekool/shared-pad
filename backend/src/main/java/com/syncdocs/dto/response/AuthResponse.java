package com.syncdocs.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String username;
    private String email;
    private Set<String> roles;
}
