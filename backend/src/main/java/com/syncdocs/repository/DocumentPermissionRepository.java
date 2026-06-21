package com.syncdocs.repository;

import com.syncdocs.model.Document;
import com.syncdocs.model.DocumentPermission;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.PermissionLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentPermissionRepository extends JpaRepository<DocumentPermission, Long> {

    List<DocumentPermission> findByDocument(Document document);

    List<DocumentPermission> findByUser(User user);

    Optional<DocumentPermission> findByDocumentAndUser(Document document, User user);

    boolean existsByDocumentAndUserAndPermissionLevel(Document document, User user, PermissionLevel level);
}
