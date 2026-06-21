package com.syncdocs.controller;

import com.syncdocs.dto.request.ShareRequest;
import com.syncdocs.dto.response.ErrorResponse;
import com.syncdocs.dto.response.PermissionResponse;
import com.syncdocs.model.Document;
import com.syncdocs.model.DocumentPermission;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.DocumentStatus;
import com.syncdocs.model.enums.PermissionLevel;
import com.syncdocs.repository.DocumentPermissionRepository;
import com.syncdocs.repository.DocumentRepository;
import com.syncdocs.repository.UserRepository;
import com.syncdocs.service.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents/{documentId}/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private User getUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private Document getDocument(Long id) {
        return documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Document not found"));
    }

    private void requireOwner(Document document, User user) {
        if (!document.getOwner().getId().equals(user.getId())) {
            DocumentPermission perm = permissionRepository
                    .findByDocumentAndUser(document, user)
                    .orElseThrow(() -> new RuntimeException("Access denied"));
            if (perm.getPermissionLevel() != PermissionLevel.OWNER) {
                throw new RuntimeException("Only the owner can manage permissions");
            }
        }
    }

    @GetMapping
    public ResponseEntity<?> listPermissions(@PathVariable Long documentId, Authentication auth) {
        try {
            User user = getUser(auth);
            Document document = getDocument(documentId);
            requireOwner(document, user);

            List<PermissionResponse> permissions = permissionRepository.findByDocument(document).stream()
                    .map(p -> PermissionResponse.builder()
                            .id(p.getId())
                            .documentId(document.getId())
                            .username(p.getUser().getUsername())
                            .permissionLevel(p.getPermissionLevel().name())
                            .build())
                    .toList();

            return ResponseEntity.ok(permissions);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> share(@PathVariable Long documentId,
                                    @Valid @RequestBody ShareRequest request,
                                    Authentication auth) {
        try {
            User currentUser = getUser(auth);
            Document document = getDocument(documentId);
            requireOwner(document, currentUser);

            User targetUser = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found: " + request.getUsername()));

            if (document.getOwner().getId().equals(targetUser.getId())) {
                throw new RuntimeException("Cannot change owner permissions");
            }

            PermissionLevel level = PermissionLevel.valueOf(request.getPermissionLevel());

            DocumentPermission existing = permissionRepository
                    .findByDocumentAndUser(document, targetUser).orElse(null);

            if (existing != null) {
                existing.setPermissionLevel(level);
                permissionRepository.save(existing);
            } else {
                permissionRepository.save(DocumentPermission.builder()
                        .document(document)
                        .user(targetUser)
                        .permissionLevel(level)
                        .build());
            }

            auditService.logDocumentEvent("PERMISSION_CHANGED", currentUser.getUsername(),
                    documentId, "Shared with " + request.getUsername() + " as " + request.getPermissionLevel());

            return ResponseEntity.ok(new ErrorResponse(200, "Shared with " + request.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> revoke(@PathVariable Long documentId,
                                     @PathVariable Long userId,
                                     Authentication auth) {
        try {
            User currentUser = getUser(auth);
            Document document = getDocument(documentId);
            requireOwner(document, currentUser);

            User targetUser = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            DocumentPermission perm = permissionRepository
                    .findByDocumentAndUser(document, targetUser)
                    .orElseThrow(() -> new RuntimeException("Permission not found"));

            permissionRepository.delete(perm);

            auditService.logDocumentEvent("PERMISSION_REVOKED", currentUser.getUsername(),
                    documentId, "Revoked access from " + targetUser.getUsername());

            return ResponseEntity.ok(new ErrorResponse(200, "Access revoked from " + targetUser.getUsername()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }
}
