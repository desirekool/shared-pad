package com.syncdocs.service;

import com.syncdocs.model.Document;
import com.syncdocs.model.DocumentVersion;
import com.syncdocs.repository.DocumentRepository;
import com.syncdocs.repository.DocumentVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VersionHistoryServiceTest {

    @Mock private DocumentVersionRepository versionRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private MinioService minioService;
    @Mock private AuditService auditService;

    @InjectMocks private VersionHistoryService versionHistoryService;

    @Test
    void recordVersion_ShouldSave() {
        versionHistoryService.recordVersion(1L, 3L, "alice", "Version 3");

        verify(versionRepository).save(any(DocumentVersion.class));
    }

    @Test
    void getVersions_ShouldReturnOrdered() {
        DocumentVersion v1 = DocumentVersion.builder().versionNumber(2L).build();
        DocumentVersion v2 = DocumentVersion.builder().versionNumber(1L).build();
        when(versionRepository.findByDocumentIdOrderByVersionNumberDesc(1L))
                .thenReturn(List.of(v1, v2));

        List<DocumentVersion> result = versionHistoryService.getVersions(1L);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getVersionNumber());
    }
}
