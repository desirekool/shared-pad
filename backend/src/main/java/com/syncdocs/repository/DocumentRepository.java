package com.syncdocs.repository;

import com.syncdocs.model.Document;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByOwnerAndStatus(User owner, DocumentStatus status);

    @Query("SELECT d FROM Document d WHERE d.owner = :user AND d.status = :status ORDER BY d.updatedAt DESC")
    List<Document> findOwnedDocuments(User user, DocumentStatus status);

    @Query("""
            SELECT DISTINCT d FROM Document d
            LEFT JOIN DocumentPermission p ON p.document = d AND p.user = :user
            WHERE d.status = :status
              AND (d.owner = :user OR p.user = :user)
            ORDER BY d.updatedAt DESC
            """)
    List<Document> findAccessibleDocuments(User user, DocumentStatus status);

    Optional<Document> findByIdAndStatus(Long id, DocumentStatus status);
}
