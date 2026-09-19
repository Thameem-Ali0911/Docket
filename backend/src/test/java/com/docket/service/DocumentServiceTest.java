package com.docket.service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.docket.dto.document.DocumentListItemDto;
import com.docket.entity.AnomalyFlag;
import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.User;
import com.docket.entity.Workspace;
import com.docket.repository.AnomalyFlagRepository;
import com.docket.repository.DocumentRepository;
import com.docket.repository.ExtractionRepository;
import com.docket.repository.SummaryRepository;
import com.docket.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

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
    @Mock
    private LlmBudgetService llmBudgetService;
    @Mock
    private ComparativeAnomalyService comparativeAnomalyService;

    private DocumentService documentService;

    private Workspace workspace;
    private User user;

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
                exportService,
                llmBudgetService,
                comparativeAnomalyService,
                Optional.empty()
        );

        workspace = new Workspace("Acme Workspace");
        setEntityId(workspace, 1);

        user = new User("alice@acme.com", "hash", workspace);
        setEntityId(user, 10);
    }

    private void setEntityId(Object entity, Integer id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("Upload document stores file, persists PENDING document, and triggers async processing")
    void testUploadDocument() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "dummy pdf content".getBytes());

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(storageService.store(file)).thenReturn("/uploads/uuid_invoice.pdf");

        Document savedDoc = new Document(workspace, DocumentType.INVOICE, "/uploads/uuid_invoice.pdf", DocumentStatus.PENDING);
        setEntityId(savedDoc, 101);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);

        Document result = documentService.uploadDocument(10, DocumentType.INVOICE, file);

        assertNotNull(result);
        assertEquals(101, result.getId());
        assertEquals(DocumentStatus.PENDING, result.getStatus());
        verify(documentProcessingService).processDocumentAsync(savedDoc);
    }

    @Test
    @DisplayName("getEnrichedDocumentsForWorkspace aggregates anomaly counts accurately")
    void testGetEnrichedDocumentsForWorkspace() throws Exception {
        Document doc1 = new Document(workspace, DocumentType.INVOICE, "/uploads/doc1.pdf", DocumentStatus.PROCESSED);
        setEntityId(doc1, 101);
        Document doc2 = new Document(workspace, DocumentType.CONTRACT, "/uploads/doc2.pdf", DocumentStatus.PROCESSED);
        setEntityId(doc2, 102);

        AnomalyFlag flag1 = new AnomalyFlag(doc1, "vendorName", "Mismatch", "HIGH");
        AnomalyFlag flag2 = new AnomalyFlag(doc1, "totalAmount", "Over budget", "MEDIUM");

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(documentRepository.findByWorkspaceIdOrderByUploadedAtDesc(1)).thenReturn(List.of(doc1, doc2));
        when(anomalyFlagRepository.findByDocumentIdIn(List.of(101, 102))).thenReturn(List.of(flag1, flag2));

        List<DocumentListItemDto> dtos = documentService.getEnrichedDocumentsForWorkspace(10);

        assertEquals(2, dtos.size());
        assertEquals(101, dtos.get(0).id());
        assertEquals(2L, dtos.get(0).anomalyCount());
        assertEquals(102, dtos.get(1).id());
        assertEquals(0L, dtos.get(1).anomalyCount());
    }

    @Test
    @DisplayName("reprocessDocument resets status to PENDING and re-triggers async pipeline")
    void testReprocessDocument() throws Exception {
        Document failedDoc = new Document(workspace, DocumentType.INVOICE, "/uploads/doc1.pdf", DocumentStatus.FAILED);
        setEntityId(failedDoc, 101);
        failedDoc.setFailedReason("OCR failed");

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(documentRepository.findById(101)).thenReturn(Optional.of(failedDoc));
        when(documentRepository.save(failedDoc)).thenReturn(failedDoc);

        Document reprocessed = documentService.reprocessDocument(10, 101);

        assertEquals(DocumentStatus.PENDING, reprocessed.getStatus());
        assertNull(reprocessed.getFailedReason());
        verify(documentProcessingService).processDocumentAsync(failedDoc);
    }

    @Test
    @DisplayName("uploadDocuments batch stores multiple files, persists documents, and triggers async processing")
    void testUploadDocumentsBatch() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("files", "inv1.pdf", "application/pdf", "dummy 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("files", "inv2.pdf", "application/pdf", "dummy 2".getBytes());

        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(storageService.store(file1)).thenReturn("/uploads/inv1.pdf");
        when(storageService.store(file2)).thenReturn("/uploads/inv2.pdf");

        Document doc1 = new Document(workspace, DocumentType.INVOICE, "/uploads/inv1.pdf", DocumentStatus.PENDING);
        setEntityId(doc1, 201);
        Document doc2 = new Document(workspace, DocumentType.INVOICE, "/uploads/inv2.pdf", DocumentStatus.PENDING);
        setEntityId(doc2, 202);

        when(documentRepository.saveAll(any())).thenReturn(List.of(doc1, doc2));

        List<Document> result = documentService.uploadDocuments(10, DocumentType.INVOICE, List.of(file1, file2));

        assertEquals(2, result.size());
        assertEquals(201, result.get(0).getId());
        assertEquals(202, result.get(1).getId());
        verify(documentProcessingService, times(2)).processDocumentAsync(any(Document.class));
    }

    @Test
    @DisplayName("uploadDocument dispatches to RabbitMQ when queue publisher is present")
    void testUploadDocumentWithQueuePublisher() throws Exception {
        DocumentQueuePublisher publisher = mock(DocumentQueuePublisher.class);
        DocumentService queueModeService = new DocumentService(
                documentRepository,
                userRepository,
                storageService,
                documentProcessingService,
                anomalyFlagRepository,
                extractionRepository,
                summaryRepository,
                exportService,
                llmBudgetService,
                comparativeAnomalyService,
                Optional.of(publisher)
        );

        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "dummy pdf content".getBytes());
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        when(storageService.store(file)).thenReturn("/uploads/uuid_invoice.pdf");

        Document savedDoc = new Document(workspace, DocumentType.INVOICE, "/uploads/uuid_invoice.pdf", DocumentStatus.PENDING);
        setEntityId(savedDoc, 101);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);

        Document result = queueModeService.uploadDocument(10, DocumentType.INVOICE, file);

        assertNotNull(result);
        verify(publisher).publish(savedDoc);
        verify(documentProcessingService, never()).processDocumentAsync(any());
    }

    @Test
    @DisplayName("getWorkspaceTrends delegates to comparativeAnomalyService with user's workspace ID")
    void testGetWorkspaceTrends() {
        when(userRepository.findById(10)).thenReturn(Optional.of(user));
        com.docket.dto.document.WorkspaceTrendsDto mockTrends = new com.docket.dto.document.WorkspaceTrendsDto(
                1, 2, 0, 0, 0, List.of()
        );
        when(comparativeAnomalyService.calculateWorkspaceTrends(1)).thenReturn(mockTrends);

        var result = documentService.getWorkspaceTrends(10);

        assertNotNull(result);
        assertEquals(1, result.totalVendors());
        verify(comparativeAnomalyService).calculateWorkspaceTrends(1);
    }
}
