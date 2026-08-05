package com.syncdocs.repository;

import com.syncdocs.model.StoredEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredEventRepository extends JpaRepository<StoredEvent, Long> {

    List<StoredEvent> findByDocumentIdAndIdGreaterThanOrderByIdAsc(Long documentId, Long afterEventId);

    List<StoredEvent> findByDocumentIdOrderByIdAsc(Long documentId);
}
