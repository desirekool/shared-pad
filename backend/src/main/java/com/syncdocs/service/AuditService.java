package com.syncdocs.service;

import com.syncdocs.model.AuditLog;
import com.syncdocs.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String eventType, String userId, Long documentId, String sessionId, String details) {
        AuditLog entry = AuditLog.builder()
                .eventType(eventType)
                .userId(userId)
                .documentId(documentId)
                .sessionId(sessionId)
                .details(details)
                .build();

        auditLogRepository.save(entry);
        log.info("AUDIT: {} | user={} | doc={} | {}", eventType, userId, documentId, details);
    }

    public void logEvent(String eventType, String userId, String details) {
        log(eventType, userId, null, null, details);
    }

    public void logDocumentEvent(String eventType, String userId, Long documentId, String details) {
        log(eventType, userId, documentId, null, details);
    }

    public List<AuditLog> getDocumentAudit(Long documentId) {
        return auditLogRepository.findByDocumentIdOrderByCreatedAtDesc(documentId);
    }

    public List<AuditLog> getUserAudit(String userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
