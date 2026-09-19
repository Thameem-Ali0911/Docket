package com.docket.service;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.docket.dto.document.WorkspaceTrendsDto;
import com.docket.entity.AnomalyFlag;
import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.Extraction;
import com.docket.entity.Workspace;
import com.docket.repository.AnomalyFlagRepository;
import com.docket.repository.ExtractionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparativeAnomalyServiceTest {

    @Mock
    private ExtractionRepository extractionRepository;

    @Mock
    private AnomalyFlagRepository anomalyFlagRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ComparativeAnomalyService service;
    private Workspace workspace;

    @BeforeEach
    void setUp() throws Exception {
        service = new ComparativeAnomalyService(extractionRepository, anomalyFlagRepository, objectMapper);
        workspace = new Workspace("Acme Workspace");
        setEntityId(workspace, 1);
    }

    private void setEntityId(Object entity, Integer id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }

    @Test
    @DisplayName("detectComparativeAnomalies flags duplicate invoice numbers in the same workspace")
    void testDuplicateInvoiceDetection() throws Exception {
        Document currentDoc = new Document(workspace, DocumentType.INVOICE, "/uploads/doc1.pdf", DocumentStatus.PROCESSED);
        setEntityId(currentDoc, 101);

        String currentJson = """
            {
                "vendorName": "CloudHost Inc",
                "invoiceNumber": "INV-9999",
                "invoiceDate": "2026-01-15",
                "dueDate": "2026-02-15",
                "totalAmount": "$500.00",
                "lineItems": [{"description": "VPS Hosting", "amount": "500.00"}]
            }
            """;
        Extraction currentExt = new Extraction(currentDoc, currentJson);

        Document existingDoc = new Document(workspace, DocumentType.INVOICE, "/uploads/doc2.pdf", DocumentStatus.PROCESSED);
        setEntityId(existingDoc, 102);
        String existingJson = """
            {
                "vendorName": "CloudHost Inc",
                "invoiceNumber": "INV-9999",
                "invoiceDate": "2025-12-15",
                "dueDate": "2026-01-15",
                "totalAmount": "$500.00",
                "lineItems": [{"description": "VPS Hosting", "amount": "500.00"}]
            }
            """;
        Extraction existingExt = new Extraction(existingDoc, existingJson);

        when(extractionRepository.findByDocumentId(101)).thenReturn(Optional.of(currentExt));
        when(anomalyFlagRepository.findByDocumentId(101)).thenReturn(List.of());
        when(extractionRepository.findByDocumentWorkspaceId(1)).thenReturn(List.of(currentExt, existingExt));

        service.detectComparativeAnomalies(currentDoc);

        ArgumentCaptor<List<AnomalyFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(anomalyFlagRepository).saveAll(captor.capture());

        List<AnomalyFlag> saved = captor.getValue();
        assertFalse(saved.isEmpty());
        assertTrue(saved.stream().anyMatch(f ->
                f.getFieldName().contains("Duplicate") &&
                f.getSeverity().equals("HIGH") &&
                f.getDescription().contains("INV-9999") &&
                f.getDescription().contains("102")
        ));
    }

    @Test
    @DisplayName("detectComparativeAnomalies flags price surge when amount is >50% higher than historical vendor average")
    void testPriceSurgeDetection() throws Exception {
        Document currentDoc = new Document(workspace, DocumentType.INVOICE, "/uploads/doc3.pdf", DocumentStatus.PROCESSED);
        setEntityId(currentDoc, 103);

        String currentJson = """
            {
                "vendorName": "SaaS Tools",
                "invoiceNumber": "INV-103",
                "invoiceDate": "2026-03-01",
                "dueDate": "2026-03-31",
                "totalAmount": "$3,000.00",
                "lineItems": [{"description": "Enterprise Tier", "amount": "3000.00"}]
            }
            """;
        Extraction currentExt = new Extraction(currentDoc, currentJson);

        // Prior invoices averaging $1,000
        Document priorDoc1 = new Document(workspace, DocumentType.INVOICE, "/uploads/p1.pdf", DocumentStatus.PROCESSED);
        setEntityId(priorDoc1, 101);
        Extraction priorExt1 = new Extraction(priorDoc1, """
            {"vendorName": "SaaS Tools", "invoiceNumber": "INV-101", "totalAmount": "$950.00", "invoiceDate": "2026-01-01"}
            """);

        Document priorDoc2 = new Document(workspace, DocumentType.INVOICE, "/uploads/p2.pdf", DocumentStatus.PROCESSED);
        setEntityId(priorDoc2, 102);
        Extraction priorExt2 = new Extraction(priorDoc2, """
            {"vendorName": "SaaS Tools", "invoiceNumber": "INV-102", "totalAmount": "$1,050.00", "invoiceDate": "2026-02-01"}
            """);

        when(extractionRepository.findByDocumentId(103)).thenReturn(Optional.of(currentExt));
        when(anomalyFlagRepository.findByDocumentId(103)).thenReturn(List.of());
        when(extractionRepository.findByDocumentWorkspaceId(1)).thenReturn(List.of(currentExt, priorExt1, priorExt2));

        service.detectComparativeAnomalies(currentDoc);

        ArgumentCaptor<List<AnomalyFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(anomalyFlagRepository).saveAll(captor.capture());

        List<AnomalyFlag> saved = captor.getValue();
        assertFalse(saved.isEmpty());
        assertTrue(saved.stream().anyMatch(f ->
                f.getFieldName().contains("Price Surge") &&
                f.getDescription().contains("higher than the historical average")
        ));
    }

    @Test
    @DisplayName("detectComparativeAnomalies flags invalid payment window when dueDate is before invoiceDate")
    void testInvalidPaymentWindow() throws Exception {
        Document currentDoc = new Document(workspace, DocumentType.INVOICE, "/uploads/doc4.pdf", DocumentStatus.PROCESSED);
        setEntityId(currentDoc, 104);

        String currentJson = """
            {
                "vendorName": "Logistics Co",
                "invoiceNumber": "INV-104",
                "invoiceDate": "2026-05-15",
                "dueDate": "2026-05-01",
                "totalAmount": "$150.00",
                "lineItems": [{"description": "Shipping", "amount": "150.00"}]
            }
            """;
        Extraction currentExt = new Extraction(currentDoc, currentJson);

        when(extractionRepository.findByDocumentId(104)).thenReturn(Optional.of(currentExt));
        when(anomalyFlagRepository.findByDocumentId(104)).thenReturn(List.of());
        when(extractionRepository.findByDocumentWorkspaceId(1)).thenReturn(List.of(currentExt));

        service.detectComparativeAnomalies(currentDoc);

        ArgumentCaptor<List<AnomalyFlag>> captor = ArgumentCaptor.forClass(List.class);
        verify(anomalyFlagRepository).saveAll(captor.capture());

        List<AnomalyFlag> saved = captor.getValue();
        assertTrue(saved.stream().anyMatch(f ->
                f.getFieldName().contains("Invalid Window") &&
                f.getSeverity().equals("HIGH")
        ));
    }

    @Test
    @DisplayName("calculateWorkspaceTrends aggregates multi-document metrics per vendor accurately")
    void testCalculateWorkspaceTrends() throws Exception {
        Document doc1 = new Document(workspace, DocumentType.INVOICE, "/uploads/inv1.pdf", DocumentStatus.PROCESSED);
        setEntityId(doc1, 201);
        Extraction ext1 = new Extraction(doc1, """
            {"vendorName": "Acme Supplies", "invoiceNumber": "INV-01", "totalAmount": "$200.00", "invoiceDate": "2026-01-10"}
            """);

        Document doc2 = new Document(workspace, DocumentType.INVOICE, "/uploads/inv2.pdf", DocumentStatus.PROCESSED);
        setEntityId(doc2, 202);
        Extraction ext2 = new Extraction(doc2, """
            {"vendorName": "Acme Supplies", "invoiceNumber": "INV-02", "totalAmount": "$400.00", "invoiceDate": "2026-02-10"}
            """);

        Document doc3 = new Document(workspace, DocumentType.INVOICE, "/uploads/inv3.pdf", DocumentStatus.PROCESSED);
        setEntityId(doc3, 203);
        Extraction ext3 = new Extraction(doc3, """
            {"vendorName": "Beta Corp", "invoiceNumber": "B-99", "totalAmount": "$1,000.00", "invoiceDate": "2026-02-15"}
            """);

        when(extractionRepository.findByDocumentWorkspaceId(1)).thenReturn(List.of(ext1, ext2, ext3));
        when(anomalyFlagRepository.findByDocumentIdIn(anyList())).thenReturn(List.of(
                new AnomalyFlag(doc2, "totalAmount (Price Surge)", "Surge detected", "MEDIUM")
        ));

        WorkspaceTrendsDto trends = service.calculateWorkspaceTrends(1);

        assertNotNull(trends);
        assertEquals(2, trends.totalVendors());
        assertEquals(3, trends.totalInvoicesAnalyzed());
        assertEquals(1, trends.priceSurgesCount());

        // Beta Corp has 1000, Acme Supplies has 600 total
        assertEquals("Beta Corp", trends.vendors().get(0).vendorName());
        assertEquals(1000.0, trends.vendors().get(0).totalSpend());

        assertEquals("Acme Supplies", trends.vendors().get(1).vendorName());
        assertEquals(600.0, trends.vendors().get(1).totalSpend());
        assertEquals(300.0, trends.vendors().get(1).averageAmount());
        assertEquals(2, trends.vendors().get(1).documentCount());
        assertEquals(1, trends.vendors().get(1).anomalyCount());
    }
}
