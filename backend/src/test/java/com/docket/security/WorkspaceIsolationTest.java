package com.docket.security;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.User;
import com.docket.entity.Workspace;
import com.docket.exception.ApiException;
import com.docket.repository.AnomalyFlagRepository;
import com.docket.repository.DocumentRepository;
import com.docket.repository.ExtractionRepository;
import com.docket.repository.SummaryRepository;
import com.docket.repository.UserRepository;
import com.docket.service.DocumentProcessingService;
import com.docket.service.DocumentService;
import com.docket.service.ExportService;
import com.docket.service.StorageService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceIsolationTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StorageService storageService;
    @Mock
    private DocumentProcessingService documentProcessingService;
    @Mock
    private AnomalyFlagRepository anomalyFlagRepository;
    @Mock
    private ExtractionRepository extractionRepository;
    @Mock
    private SummaryRepository summaryRepository;
    @Mock
    private ExportService exportService;

    private DocumentService documentService;

    private Workspace workspaceA;
    private Workspace workspaceB;
    private User userA;
    private Document docInWorkspaceB;

    @BeforeEach
    void setUp() throws Exception {
        documentService = new DocumentService(
                documentRepository,
                userRepository,
                storageService,
                documentProcessingService,
                anomalyFlagRepository,
                extractionRepository,
                summaryRepository,
                exportService
        );

        workspaceA = new Workspace("Workspace Alpha");
        setEntityId(workspaceA, 1);

        workspaceB = new Workspace("Workspace Beta");
        setEntityId(workspaceB, 2);

        userA = new User("user.a@alpha.com", "hash", workspaceA);
        setEntityId(userA, 10);

        docInWorkspaceB = new Document(workspaceB, DocumentType.INVOICE, "/uploads/beta-secret.pdf", DocumentStatus.PROCESSED);
        setEntityId(docInWorkspaceB, 200);
    }

    private void setEntityId(Object entity, Integer id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("Cross-workspace document access is blocked with 404 NOT FOUND")
    void testCrossWorkspaceDocumentAccessReturns404() {
        when(userRepository.findById(10)).thenReturn(Optional.of(userA));
        when(documentRepository.findById(200)).thenReturn(Optional.of(docInWorkspaceB));

        ApiException exception = assertThrows(ApiException.class, () -> {
            documentService.getDocumentForWorkspace(10, 200);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("DOCUMENT_NOT_FOUND", exception.getCode());
    }

    @Test
    @DisplayName("Cross-workspace file retrieval is blocked with 404 NOT FOUND")
    void testCrossWorkspaceFileRetrievalReturns404() {
        when(userRepository.findById(10)).thenReturn(Optional.of(userA));
        when(documentRepository.findById(200)).thenReturn(Optional.of(docInWorkspaceB));

        ApiException exception = assertThrows(ApiException.class, () -> {
            documentService.getDocumentFileForWorkspace(10, 200);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(storageService, never()).getResource(anyString());
    }

    @Test
    @DisplayName("Same-workspace document access succeeds")
    void testSameWorkspaceAccessSucceeds() throws Exception {
        Document docInWorkspaceA = new Document(workspaceA, DocumentType.INVOICE, "/uploads/alpha-doc.pdf", DocumentStatus.PROCESSED);
        setEntityId(docInWorkspaceA, 100);

        when(userRepository.findById(10)).thenReturn(Optional.of(userA));
        when(documentRepository.findById(100)).thenReturn(Optional.of(docInWorkspaceA));

        Document result = documentService.getDocumentForWorkspace(10, 100);

        assertNotNull(result);
        assertEquals(100, result.getId());
        assertEquals(workspaceA.getId(), result.getWorkspace().getId());
    }
}
