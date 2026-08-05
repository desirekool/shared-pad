package com.syncdocs.controller;

import com.syncdocs.dto.request.LocalShareRequest;
import com.syncdocs.service.LocalShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/local-shares")
@RequiredArgsConstructor
public class LocalShareController {

    private final LocalShareService localShareService;

    @GetMapping
    public ResponseEntity<?> getSharedWith(Authentication auth) {
        return ResponseEntity.ok(localShareService.getSharedWith(auth.getName()));
    }

    @PostMapping
    public ResponseEntity<?> share(@Valid @RequestBody LocalShareRequest req, Authentication auth) {
        localShareService.share(req.getLocalDocId(), req.getTitle(), auth.getName(), req.getSharedWith(), req.getFilePath());
        return ResponseEntity.ok(Map.of("status", "shared"));
    }

    @DeleteMapping("/{localDocId}")
    public ResponseEntity<?> removeShare(@PathVariable String localDocId) {
        localShareService.removeShare(localDocId);
        return ResponseEntity.ok(Map.of("status", "removed"));
    }
}
