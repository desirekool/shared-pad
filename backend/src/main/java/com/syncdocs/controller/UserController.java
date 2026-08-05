package com.syncdocs.controller;

import com.syncdocs.model.User;
import com.syncdocs.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    public ResponseEntity<List<String>> searchUsers(@RequestParam String q, Authentication auth) {
        User current = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<String> usernames = userRepository.findByUsernameContainingIgnoreCase(q)
                .stream()
                .filter(u -> !u.getId().equals(current.getId()))
                .map(User::getUsername)
                .limit(10)
                .toList();

        return ResponseEntity.ok(usernames);
    }
}
