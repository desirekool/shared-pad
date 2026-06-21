package com.syncdocs.service;

import com.syncdocs.dto.request.DocumentCreateRequest;
import com.syncdocs.dto.request.DocumentUpdateRequest;
import com.syncdocs.dto.response.DocumentResponse;
import com.syncdocs.model.Document;
import com.syncdocs.model.User;
import com.syncdocs.model.enums.DocumentStatus;
import com.syncdocs.model.enums.RoleName;
import com.syncdocs.repository.DocumentPermissionRepository;
import com.syncdocs.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentPermissionRepository permissionRepository;
    @Mock private MinioService minioService;
    @Mock private VersionHistoryService versionHistoryService;
    @Mock private AuditService auditService;

    @InjectMocks private DocumentService documentService;

    private User owner;
    private Document document;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("alice").build();
        document = Document.builder()
                .id(1L)
                .title("Test Doc")
                .owner(owner)
                .version(1L)
                .status(DocumentStatus.ACTIVE)
                .mimeType("text/plain")
                .build();
    }

    @Test
    void create_ShouldReturnDocumentResponse() {
        DocumentCreateRequest request = new DocumentCreateRequest();
        request.setTitle("New Doc");
        request.setContent("Hello World");
        request.setMimeType("text/plain");

        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));
        when(minioService.putObject(anyString(), any(), anyString())).thenAnswer(i -> null);
        when(versionHistoryService.recordVersion(any(), any(), any(), any())).thenAnswer(i -> null);
        when(auditService.logDocumentEvent(any(), any(), any(), any())).thenAnswer(i -> null);

        DocumentResponse response = documentService.create(request, owner);

        assertNotNull(response);
        assertEquals("New Doc", response.getTitle());
        assertEquals("alice", response.getOwner());
    }

    @Test
    void getById_ShouldReturnDocument() {
        when(documentRepository.findByIdAndStatus(1L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.of(document));
        when(minioService.getObject(anyString())).thenReturn("Hello".getBytes());
        when(auditService.logDocumentEvent(any(), any(), any(), any())).thenAnswer(i -> null);

        DocumentResponse response = documentService.getById(1L, owner);

        assertNotNull(response);
        assertEquals(1L, response.getId());
    }

    @Test
    void listOwn_ShouldReturnUserDocuments() {
        when(documentRepository.findOwnedDocuments(owner, DocumentStatus.ACTIVE))
                .thenReturn(List.of(document));

        List<DocumentResponse> docs = documentService.listOwn(owner);

        assertEquals(1, docs.size());
    }

    @Test
    void delete_ShouldSoftDelete() {
        when(documentRepository.findByIdAndStatus(1L, DocumentStatus.ACTIVE))
                .thenReturn(Optional.of(document));

        documentService.delete(1L, owner);

        assertEquals(DocumentStatus.DELETED, document.getStatus());
        verify(documentRepository).save(document);
    }
}
