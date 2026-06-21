package com.syncdocs.controller;

import com.syncdocs.model.AuditLog;
import com.syncdocs.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<List<AuditLog>> getDocumentAudit(@PathVariable Long documentId) {
        return ResponseEntity.ok(auditService.getDocumentAudit(documentId));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<AuditLog>> getUserAudit(@PathVariable String userId) {
        return ResponseEntity.ok(auditService.getUserAudit(userId));
    }
}
