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
        Long docId = Long.valueOf(documentId);
        Document document = documentRepository.findById(docId).orElse(null);
        if (document == null) {
            log.warn("Document not found: {}", documentId);
            return false;
        }

        long currentVersion = document.getVersion();

        if (operationVersion == currentVersion) {
            log.debug("Edit accepted: doc={} opVersion={} serverVersion={}",
                    documentId, operationVersion, currentVersion);
            return true;
        }

        log.warn("Edit rejected (stale version): doc={} opVersion={} serverVersion={}",
                documentId, operationVersion, currentVersion);
        return false;
    }
}
