package com.syncdocs.repository;

import com.syncdocs.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByDocumentIdOrderByCreatedAtDesc(Long documentId);

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(String userId);

    List<AuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType);
}
