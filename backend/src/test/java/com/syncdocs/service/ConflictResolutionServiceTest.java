package com.syncdocs.service;

import com.syncdocs.model.Document;
import com.syncdocs.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConflictResolutionServiceTest {

    @Mock private DocumentRepository documentRepository;

    @InjectMocks private ConflictResolutionService conflictResolutionService;

    private Document document;

    @BeforeEach
    void setUp() {
        document = Document.builder()
                .id(1L)
                .title("Test Doc")
                .version(5L)
                .build();
    }

    @Test
    void validateEdit_ShouldAcceptWhenVersionMatches() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        boolean result = conflictResolutionService.validateEdit("1", 5L);

        assertTrue(result);
    }

    @Test
    void validateEdit_ShouldRejectWhenVersionMismatch() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        boolean result = conflictResolutionService.validateEdit("1", 3L);

        assertFalse(result);
    }

    @Test
    void validateEdit_ShouldRejectWhenDocumentNotFound() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = conflictResolutionService.validateEdit("99", 1L);

        assertFalse(result);
    }
}
