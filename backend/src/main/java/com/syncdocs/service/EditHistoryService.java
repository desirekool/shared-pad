package com.syncdocs.service;

import com.syncdocs.model.EditHistory;
import com.syncdocs.repository.EditHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EditHistoryService {

    private final EditHistoryRepository editHistoryRepository;

    @Transactional
    public void recordEdit(Long documentId, String userId, String operationType,
                           int position, String text, int length, long version) {
        EditHistory history = EditHistory.builder()
                .documentId(documentId)
                .userId(userId)
                .operationType(operationType)
                .position(position)
                .text(text)
                .length(length)
                .version(version)
                .build();

        editHistoryRepository.save(history);
    }

    public List<EditHistory> getHistory(Long documentId) {
        return editHistoryRepository.findByDocumentIdOrderByCreatedAtAsc(documentId);
    }

    public List<EditHistory> getHistorySince(Long documentId, Long version) {
        return editHistoryRepository.findByDocumentIdAndVersionGreaterThanOrderByCreatedAtAsc(
                documentId, version);
    }
}
