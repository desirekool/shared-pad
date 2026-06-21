package com.syncdocs.repository;

import com.syncdocs.model.EditHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EditHistoryRepository extends JpaRepository<EditHistory, Long> {

    List<EditHistory> findByDocumentIdOrderByCreatedAtAsc(Long documentId);

    List<EditHistory> findByDocumentIdAndVersionGreaterThanOrderByCreatedAtAsc(
            Long documentId, Long version);
}
