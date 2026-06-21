package com.syncdocs.controller;

import com.syncdocs.events.UserPresence;
import com.syncdocs.service.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{id}/presence")
@RequiredArgsConstructor
public class PresenceRestController {

    private final PresenceService presenceService;

    @GetMapping
    public ResponseEntity<List<UserPresence>> getActiveUsers(@PathVariable Long id) {
        return ResponseEntity.ok(presenceService.getActiveUsers(String.valueOf(id)));
    }

    @GetMapping("/count")
    public ResponseEntity<Integer> getActiveCount(@PathVariable Long id) {
        return ResponseEntity.ok(presenceService.getActiveCount(String.valueOf(id)));
    }
}
