package com.syncdocs.service;

import com.syncdocs.model.AuditLog;
import com.syncdocs.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks private AuditService auditService;

    @Test
    void log_ShouldSaveAuditEntry() {
        auditService.log("TEST_EVENT", "alice", 1L, "sess-1", "Test detail");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logEvent_ShouldSaveWithoutDocument() {
        auditService.logEvent("USER_LOGIN", "alice", "User logged in");

        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void getDocumentAudit_ShouldReturnEntries() {
        AuditLog entry = AuditLog.builder()
                .eventType("DOCUMENT_OPENED")
                .userId("alice")
                .documentId(1L)
                .details("Opened doc")
                .build();
        when(auditLogRepository.findByDocumentIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(entry));

        List<AuditLog> result = auditService.getDocumentAudit(1L);

        assertEquals(1, result.size());
        assertEquals("DOCUMENT_OPENED", result.get(0).getEventType());
    }
}
