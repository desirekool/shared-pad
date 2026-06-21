package com.syncdocs.controller;

import com.syncdocs.dto.request.DocumentCreateRequest;
import com.syncdocs.dto.request.DocumentUpdateRequest;
import com.syncdocs.dto.request.PromoteRequest;
import com.syncdocs.dto.response.DocumentResponse;
import com.syncdocs.dto.response.ErrorResponse;
import com.syncdocs.model.EditHistory;
import com.syncdocs.model.User;
import com.syncdocs.repository.UserRepository;
import com.syncdocs.service.DocumentService;
import com.syncdocs.service.EditHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final UserRepository userRepository;
    private final EditHistoryService editHistoryService;
    private final VersionHistoryService versionHistoryService;

    private User getUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody DocumentCreateRequest request,
                                     Authentication auth) {
        try {
            User user = getUser(auth);
            DocumentResponse response = documentService.create(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @PostMapping("/promote")
    public ResponseEntity<?> promote(@Valid @RequestBody PromoteRequest request,
                                      Authentication auth) {
        try {
            User user = getUser(auth);
            DocumentResponse response = documentService.promote(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list(Authentication auth) {
        User user = getUser(auth);
        return ResponseEntity.ok(documentService.listOwn(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, Authentication auth) {
        try {
            User user = getUser(auth);
            DocumentResponse response = documentService.getById(id, user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(404, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @Valid @RequestBody DocumentUpdateRequest request,
                                     Authentication auth) {
        try {
            User user = getUser(auth);
            DocumentResponse response = documentService.update(id, request, user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        try {
            User user = getUser(auth);
            documentService.delete(id, user);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<?> download(@PathVariable Long id, Authentication auth) {
        try {
            User user = getUser(auth);
            DocumentResponse doc = documentService.getById(id, user);
            byte[] content = documentService.download(id, user);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(doc.getMimeType() != null ? doc.getMimeType() : "application/octet-stream"));
            headers.setContentDisposition(ContentDisposition.attachment().filename(doc.getTitle()).build());

            return new ResponseEntity<>(content, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(404, e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                     Authentication auth) throws IOException {
        try {
            User user = getUser(auth);
            String title = file.getOriginalFilename() != null ? file.getOriginalFilename() : "untitled";
            DocumentResponse response = documentService.upload(title, file.getBytes(), file.getContentType(), user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<?> getHistory(@PathVariable Long id, Authentication auth) {
        try {
            User user = getUser(auth);
            documentService.getById(id, user);
            List<EditHistory> history = editHistoryService.getHistory(id);
            return ResponseEntity.ok(history);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<?> getVersions(@PathVariable Long id, Authentication auth) {
        try {
            User user = getUser(auth);
            documentService.getById(id, user);
            return ResponseEntity.ok(versionHistoryService.getVersions(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<?> getVersionContent(@PathVariable Long id,
                                                @PathVariable Long version,
                                                Authentication auth) {
        try {
            User user = getUser(auth);
            documentService.getById(id, user);
            byte[] content = versionHistoryService.getVersionContent(id, version);
            DocumentResponse doc = documentService.getById(id, user);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(java.util.Map.of(
                    "content", new String(content, java.nio.charset.StandardCharsets.UTF_8),
                    "version", version,
                    "title", doc.getTitle()
            ), headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }

    @PostMapping("/{id}/versions/{version}/restore")
    public ResponseEntity<?> restoreVersion(@PathVariable Long id,
                                             @PathVariable Long version,
                                             Authentication auth) {
        try {
            User user = getUser(auth);
            DocumentResponse response = versionHistoryService.restore(id, version, user.getUsername());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        }
    }
}
