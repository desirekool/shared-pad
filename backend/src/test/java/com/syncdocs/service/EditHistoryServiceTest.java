package com.syncdocs.service;

import com.syncdocs.model.EditHistory;
import com.syncdocs.repository.EditHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditHistoryServiceTest {

    @Mock private EditHistoryRepository editHistoryRepository;
    @InjectMocks private EditHistoryService editHistoryService;

    @Captor private ArgumentCaptor<EditHistory> historyCaptor;

    @Test
    void recordEdit_ShouldSaveEditHistory() {
        editHistoryService.recordEdit(1L, "user1", "INSERT", 5, "abc", 0, 1L);

        verify(editHistoryRepository).save(historyCaptor.capture());
        EditHistory saved = historyCaptor.getValue();
        assertEquals(1L, saved.getDocumentId());
        assertEquals("user1", saved.getUserId());
        assertEquals("INSERT", saved.getOperationType());
        assertEquals(5, saved.getPosition());
        assertEquals("abc", saved.getText());
        assertEquals(1L, saved.getVersion());
    }

    @Test
    void getHistory_ShouldDelegateToRepository() {
        when(editHistoryRepository.findByDocumentIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(EditHistory.builder().id(1L).build()));

        List<EditHistory> result = editHistoryService.getHistory(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(editHistoryRepository).findByDocumentIdOrderByCreatedAtAsc(1L);
    }

    @Test
    void getHistorySince_ShouldDelegateToRepository() {
        when(editHistoryRepository.findByDocumentIdAndVersionGreaterThanOrderByCreatedAtAsc(1L, 2L))
                .thenReturn(List.of(EditHistory.builder().id(2L).build()));

        List<EditHistory> result = editHistoryService.getHistorySince(1L, 2L);

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getId());
        verify(editHistoryRepository).findByDocumentIdAndVersionGreaterThanOrderByCreatedAtAsc(1L, 2L);
    }
}
