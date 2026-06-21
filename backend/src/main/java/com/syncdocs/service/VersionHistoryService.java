package com.syncdocs.service;

import com.syncdocs.dto.response.DocumentResponse;
import com.syncdocs.model.Document;
import com.syncdocs.model.DocumentVersion;
import com.syncdocs.repository.DocumentRepository;
import com.syncdocs.repository.DocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionHistoryService {

    private final DocumentVersionRepository versionRepository;
    private final DocumentRepository documentRepository;
    private final MinioService minioService;
    private final AuditService auditService;

    @Transactional
    public void recordVersion(Long documentId, Long versionNumber, String createdBy, String message) {
        DocumentVersion version = DocumentVersion.builder()
                .documentId(documentId)
                .versionNumber(versionNumber)
                .createdBy(createdBy)
                .message(message != null ? message : "Version " + versionNumber)
                .build();

        versionRepository.save(version);
        log.debug("Recorded version {} for document {}", versionNumber, documentId);
    }

    @Transactional
    public DocumentResponse restore(Long documentId, Long versionNumber, String requestedBy) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        DocumentVersion version = versionRepository
                .findByDocumentIdAndVersionNumber(documentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionNumber));

        String objectKey = documentId + "/" + versionNumber;
        byte[] content = minioService.getObject(objectKey);
        String contentStr = new String(content, StandardCharsets.UTF_8);

        document.setContent(null);
        document = documentRepository.save(document);

        long newVersion = document.getVersion();
        String newObjectKey = documentId + "/" + newVersion;
        minioService.putObject(newObjectKey, content, document.getMimeType());

        recordVersion(documentId, newVersion, requestedBy,
                "Restored from version " + versionNumber);

        auditService.logDocumentEvent("VERSION_RESTORED", requestedBy, documentId,
                "Restored version " + versionNumber + " as version " + newVersion);

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .owner(document.getOwner().getUsername())
                .mimeType(document.getMimeType())
                .size((long) content.length)
                .version(newVersion)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .content(contentStr)
                .build();
    }

    public List<DocumentVersion> getVersions(Long documentId) {
        return versionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId);
    }

    public byte[] getVersionContent(Long documentId, Long versionNumber) {
        String objectKey = documentId + "/" + versionNumber;
        return minioService.getObject(objectKey);
    }
}
