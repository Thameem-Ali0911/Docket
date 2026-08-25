package com.docket.service;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.Extraction;
import com.docket.entity.Workspace;
import com.docket.repository.ExtractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractionServiceTest {

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private ExtractionRepository extractionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ExtractionService extractionService;

    private Document document;

    @BeforeEach
    void setUp() throws Exception {
        extractionService = new ExtractionService(geminiClient, extractionRepository, objectMapper);

        Workspace workspace = new Workspace("Acme Workspace");
        document = new Document(workspace, DocumentType.INVOICE, "/uploads/invoice.pdf", DocumentStatus.PROCESSED);
        document.setExtractedText("Invoice #INV-2026-001\nVendor: Acme Inc\nTotal: $1,250.00");
        setEntityId(document, 101);
    }

    private void setEntityId(Object entity, Integer id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("Valid Gemini extraction saves extraction JSON and clears failure reason")
    void testExtractInvoiceFieldsSuccess() throws Exception {
        String validJson = """
                {
                    "vendorName": "Acme Inc",
                    "invoiceNumber": "INV-2026-001",
                    "invoiceDate": "2026-08-01",
                    "dueDate": "2026-08-31",
                    "totalAmount": "$1,250.00",
                    "lineItems": [
                        {
                            "description": "Consulting Services",
                            "quantity": "1",
                            "unitPrice": "$1,250.00",
                            "amount": "$1,250.00"
                        }
                    ]
                }
                """;

        when(geminiClient.generateStructuredJson(anyString(), anyString())).thenReturn(validJson);
        when(extractionRepository.findByDocumentId(101)).thenReturn(Optional.empty());

        extractionService.extractInvoiceFields(document);

        ArgumentCaptor<Extraction> captor = ArgumentCaptor.forClass(Extraction.class);
        verify(extractionRepository).save(captor.capture());

        Extraction saved = captor.getValue();
        assertNotNull(saved);
        assertNull(saved.getFailedReason());
        assertTrue(saved.getFieldsJson().contains("Acme Inc"));
    }

    @Test
    @DisplayName("NUL bytes in LLM JSON are stripped before saving extraction")
    void testStripsNulBytesFromGeminiResponse() throws Exception {
        String jsonWithNul = """
                {
                    "vendorName": "Acme\u0000 Inc",
                    "invoiceNumber": "INV-001",
                    "invoiceDate": "2026-08-01",
                    "dueDate": "2026-08-31",
                    "totalAmount": "$1,250.00",
                    "lineItems": [
                        {
                            "description": "Consulting\u0000 Services",
                            "quantity": "1",
                            "unitPrice": "$1,250.00",
                            "amount": "$1,250.00"
                        }
                    ]
                }
                """;

        when(geminiClient.generateStructuredJson(anyString(), anyString())).thenReturn(jsonWithNul);
        when(extractionRepository.findByDocumentId(101)).thenReturn(Optional.empty());

        extractionService.extractInvoiceFields(document);

        ArgumentCaptor<Extraction> captor = ArgumentCaptor.forClass(Extraction.class);
        verify(extractionRepository).save(captor.capture());

        Extraction saved = captor.getValue();
        assertFalse(saved.getFieldsJson().contains("\u0000"));
        assertTrue(saved.getFieldsJson().contains("Acme Inc"));
    }

    @Test
    @DisplayName("Extraction with empty text logs failure without calling Gemini")
    void testExtractWithEmptyText() throws Exception {
        document.setExtractedText("");
        when(extractionRepository.findByDocumentId(101)).thenReturn(Optional.empty());

        extractionService.extractInvoiceFields(document);

        verify(geminiClient, never()).generateStructuredJson(anyString(), anyString());
        verify(extractionRepository).save(any(Extraction.class));
    }
}
