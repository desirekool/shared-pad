package com.syncdocs.service;

import com.syncdocs.dto.request.DocumentCreateRequest;
import com.syncdocs.dto.request.DocumentUpdateRequest;
import com.syncdocs.dto.request.PromoteRequest;
import com.syncdocs.dto.response.DocumentResponse;
import com.syncdocs.model.Document;
import com.syncdocs.model.DocumentPermission;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.DocumentStatus;
import com.syncdocs.model.enums.PermissionLevel;
import com.syncdocs.repository.DocumentPermissionRepository;
import com.syncdocs.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentPermissionRepository permissionRepository;
    private final MinioService minioService;
    private final VersionHistoryService versionHistoryService;
    private final AuditService auditService;

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String resolvePermissionLevel(Document document, User user) {
        if (document.getOwner().getId().equals(user.getId())) {
            return "OWNER";
        }
        return permissionRepository.findByDocumentAndUser(document, user)
                .map(p -> p.getPermissionLevel().name())
                .orElse("VIEWER");
    }

    private String generateObjectKey(Long documentId, Long version) {
        return documentId + "/" + version;
    }

    @Transactional
    public DocumentResponse create(DocumentCreateRequest request, User owner) {
        byte[] content = request.getContent() != null
                ? request.getContent().getBytes(StandardCharsets.UTF_8)
                : new byte[0];

        String hash = sha256(content);
        String mimeType = request.getMimeType() != null ? request.getMimeType() : "text/plain";

        Document document = Document.builder()
                .title(request.getTitle())
                .owner(owner)
                .contentHash(hash)
                .mimeType(mimeType)
                .size((long) content.length)
                .version(0L)
                .build();

        document = documentRepository.save(document);

        String objectKey = generateObjectKey(document.getId(), document.getVersion());
        minioService.putObject(objectKey, content, mimeType);

        permissionRepository.save(DocumentPermission.builder()
                .document(document)
                .user(owner)
                .permissionLevel(PermissionLevel.OWNER)
                .build());

        versionHistoryService.recordVersion(document.getId(), document.getVersion(),
                owner.getUsername(), "Initial version");

        auditService.logDocumentEvent("DOCUMENT_CREATED", owner.getUsername(),
                document.getId(), "Created: " + document.getTitle());

        return toResponse(document, request.getContent());
    }

    @Transactional
    public DocumentResponse promote(PromoteRequest request, User user) {
        byte[] content = request.getContent().getBytes(StandardCharsets.UTF_8);
        String hash = sha256(content);
        String mimeType = request.getMimeType() != null ? request.getMimeType() : "text/plain";

        Document document = Document.builder()
                .title(request.getOriginalFilename())
                .owner(user)
                .contentHash(hash)
                .mimeType(mimeType)
                .size(request.getFileSize())
                .originalFilename(request.getOriginalFilename())
                .originalPath(request.getOriginalPath())
                .originalChecksum(request.getOriginalChecksum())
                .originalLastModified(request.getOriginalLastModified() != null
                        ? Instant.parse(request.getOriginalLastModified()) : null)
                .importedAt(Instant.now())
                .version(0L)
                .build();

        document = documentRepository.save(document);

        String objectKey = generateObjectKey(document.getId(), document.getVersion());
        minioService.putObject(objectKey, content, mimeType);

        permissionRepository.save(DocumentPermission.builder()
                .document(document)
                .user(user)
                .permissionLevel(PermissionLevel.OWNER)
                .build());

        versionHistoryService.recordVersion(document.getId(), document.getVersion(),
                user.getUsername(), "Imported from: " + request.getOriginalFilename());

        return toResponse(document, request.getContent());
    }

    public DocumentResponse getById(Long id, User user) {
        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        checkPermission(document, user, PermissionLevel.VIEWER);

        String objectKey = generateObjectKey(document.getId(), document.getVersion());
        byte[] content = minioService.getObject(objectKey);
        String contentStr = new String(content, StandardCharsets.UTF_8);

        String permissionLevel = resolvePermissionLevel(document, user);

        auditService.logDocumentEvent("DOCUMENT_OPENED", user.getUsername(),
                document.getId(), "Opened: " + document.getTitle());

        return toResponse(document, contentStr, permissionLevel);
    }

    public List<DocumentResponse> listOwn(User user) {
        return documentRepository.findOwnedDocuments(user, DocumentStatus.ACTIVE).stream()
                .map(doc -> toResponse(doc, null))
                .toList();
    }

    @Transactional
    public DocumentResponse update(Long id, DocumentUpdateRequest request, User user) {
        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        checkPermission(document, user, PermissionLevel.EDITOR);

        if (request.getTitle() != null && !request.getTitle().equals(document.getTitle())) {
            String oldTitle = document.getTitle();
            document.setTitle(request.getTitle());
            auditService.logDocumentEvent("DOCUMENT_RENAMED", user.getUsername(),
                    document.getId(), "Renamed from '" + oldTitle + "' to '" + request.getTitle() + "'");
        }

        document = documentRepository.save(document);

        if (request.getContent() != null) {
            byte[] content = request.getContent().getBytes(StandardCharsets.UTF_8);
            String hash = sha256(content);

            document.setContentHash(hash);
            document.setSize((long) content.length);

            String objectKey = generateObjectKey(document.getId(), document.getVersion());
            minioService.putObject(objectKey, content, document.getMimeType());

            document = documentRepository.save(document);

            versionHistoryService.recordVersion(document.getId(), document.getVersion(),
                    user.getUsername(), "Saved: " + document.getTitle());

            auditService.logDocumentEvent("DOCUMENT_SAVED", user.getUsername(),
                    document.getId(), "Version " + document.getVersion() + " saved");
        }

        String content = request.getContent();
        if (content == null) {
            String objectKey = generateObjectKey(document.getId(), document.getVersion());
            byte[] data = minioService.getObject(objectKey);
            content = new String(data, StandardCharsets.UTF_8);
        }

        return toResponse(document, content);
    }

    @Transactional
    public void delete(Long id, User user) {
        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        checkPermission(document, user, PermissionLevel.OWNER);

        document.setStatus(DocumentStatus.DELETED);
        documentRepository.save(document);

        auditService.logDocumentEvent("DOCUMENT_DELETED", user.getUsername(),
                document.getId(), "Deleted: " + document.getTitle());
    }

    public byte[] download(Long id, User user) {
        Document document = documentRepository.findByIdAndStatus(id, DocumentStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        checkPermission(document, user, PermissionLevel.VIEWER);

        String objectKey = generateObjectKey(document.getId(), document.getVersion());
        byte[] data = minioService.getObject(objectKey);

        auditService.logDocumentEvent("DOCUMENT_DOWNLOADED", user.getUsername(),
                document.getId(), "Downloaded: " + document.getTitle());

        return data;
    }

    @Transactional
    public DocumentResponse upload(String title, byte[] content, String mimeType, User user) {
        String hash = sha256(content);
        String mt = mimeType != null ? mimeType : "application/octet-stream";

        Document document = Document.builder()
                .title(title)
                .owner(user)
                .contentHash(hash)
                .mimeType(mt)
                .size((long) content.length)
                .version(0L)
                .build();

        document = documentRepository.save(document);

        String objectKey = generateObjectKey(document.getId(), document.getVersion());
        minioService.putObject(objectKey, content, mt);

        permissionRepository.save(DocumentPermission.builder()
                .document(document)
                .user(user)
                .permissionLevel(PermissionLevel.OWNER)
                .build());

        versionHistoryService.recordVersion(document.getId(), document.getVersion(),
                user.getUsername(), "Uploaded: " + title);

        auditService.logDocumentEvent("DOCUMENT_UPLOADED", user.getUsername(),
                document.getId(), "Uploaded: " + title);

        return toResponse(document, new String(content, StandardCharsets.UTF_8));
    }

    private void checkPermission(Document document, User user, PermissionLevel minimum) {
        if (document.getOwner().getId().equals(user.getId())) return;

        DocumentPermission permission = permissionRepository
                .findByDocumentAndUser(document, user)
                .orElseThrow(() -> new RuntimeException("Access denied"));

        if (permission.getPermissionLevel().ordinal() < minimum.ordinal()) {
            throw new RuntimeException("Insufficient permissions");
        }
    }

    private DocumentResponse toResponse(Document document, String content) {
        return toResponse(document, content, null);
    }

    private DocumentResponse toResponse(Document document, String content, String permissionLevel) {
        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .owner(document.getOwner().getUsername())
                .mimeType(document.getMimeType())
                .size(document.getSize())
                .version(document.getVersion())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .content(content)
                .originalFilename(document.getOriginalFilename())
                .permissionLevel(permissionLevel)
                .build();
    }
}
