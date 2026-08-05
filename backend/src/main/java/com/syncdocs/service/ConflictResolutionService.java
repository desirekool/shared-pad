package com.syncdocs.service;

import com.syncdocs.model.Document;
import com.syncdocs.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConflictResolutionService {

    private final DocumentRepository documentRepository;

    @Transactional
    public boolean validateEdit(String documentId, long operationVersion) {
        try {
            Long docId = Long.valueOf(documentId);
            Document document = documentRepository.findById(docId).orElse(null);
            if (document == null) {
                log.warn("Document not found: {}", documentId);
                return false;
            }

            long currentVersion = document.getVersion() != null ? document.getVersion() : 0;
            if (operationVersion > currentVersion + 1) {
                log.warn("Edit rejected (future version): doc={} opVersion={} currentVersion={}",
                        documentId, operationVersion, currentVersion);
                return false;
            }

            if (operationVersion <= currentVersion) {
                log.debug("Edit accepted (replay/retry): doc={} opVersion={} currentVersion={}",
                        documentId, operationVersion, currentVersion);
                return true;
            }

            log.debug("Edit accepted: doc={} opVersion={} currentVersion={}",
                    documentId, operationVersion, currentVersion);
            return true;
        } catch (NumberFormatException e) {
            log.warn("Non-numeric document ID (likely not yet created): {}", documentId);
            return false;
        }
    }
}
