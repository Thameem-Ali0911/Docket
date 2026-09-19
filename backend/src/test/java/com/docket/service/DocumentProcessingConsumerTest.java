package com.docket.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.docket.dto.DocumentProcessingMessage;
import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.Workspace;
import com.docket.repository.DocumentRepository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingConsumerTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentProcessingService documentProcessingService;

    private DocumentProcessingConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new DocumentProcessingConsumer(documentRepository, documentProcessingService);
    }

    @Test
    @DisplayName("onMessage loads document and calls synchronous processDocument")
    void testOnMessageProcessesDocument() {
        Workspace workspace = new Workspace("Test WS");
        Document doc = new Document(workspace, DocumentType.INVOICE, "/uploads/test.pdf", DocumentStatus.PENDING);

        when(documentRepository.findById(42)).thenReturn(Optional.of(doc));

        consumer.onMessage(new DocumentProcessingMessage(42));

        verify(documentRepository).findById(42);
        verify(documentProcessingService).processDocument(doc);
    }

    @Test
    @DisplayName("onMessage skips gracefully when document not found in DB")
    void testOnMessageSkipsMissingDocument() {
        when(documentRepository.findById(999)).thenReturn(Optional.empty());

        consumer.onMessage(new DocumentProcessingMessage(999));

        verify(documentRepository).findById(999);
        verify(documentProcessingService, never()).processDocument(any());
    }

    @Test
    @DisplayName("onMessage catches unexpected exception and does not rethrow")
    void testOnMessageHandlesExceptionGracefully() {
        Workspace workspace = new Workspace("Test WS");
        Document doc = new Document(workspace, DocumentType.INVOICE, "/uploads/test.pdf", DocumentStatus.PENDING);

        when(documentRepository.findById(42)).thenReturn(Optional.of(doc));
        doThrow(new RuntimeException("Pipeline crash")).when(documentProcessingService).processDocument(doc);

        assertDoesNotThrow(() -> consumer.onMessage(new DocumentProcessingMessage(42)));
    }
}
