package com.docket.service;

import java.time.OffsetDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.repository.DocumentRepository;

/**
 * Background scheduler that identifies orphaned documents stuck in PENDING status
 * (e.g. from server restarts mid-processing) and re-enqueues them for processing.
 */
@Service
public class DocumentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentReconciliationScheduler.class);
    private static final int STUCK_THRESHOLD_MINUTES = 5;

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    public DocumentReconciliationScheduler(DocumentRepository documentRepository,
                                           DocumentProcessingService documentProcessingService) {
        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
    }

    /**
     * Runs every 5 minutes to detect and reprocess stuck PENDING documents.
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void reconcileStuckDocuments() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(STUCK_THRESHOLD_MINUTES);
        List<Document> stuckDocs = documentRepository.findByStatusAndUploadedAtBefore(DocumentStatus.PENDING, threshold);

        if (!stuckDocs.isEmpty()) {
            log.warn("Found {} documents stuck in PENDING status older than {} minutes. Triggering reconciliation...",
                    stuckDocs.size(), STUCK_THRESHOLD_MINUTES);

            for (Document doc : stuckDocs) {
                try {
                    log.info("Reconciling stuck document id={} (type={})", doc.getId(), doc.getType());
                    documentProcessingService.processDocumentAsync(doc);
                } catch (Exception e) {
                    log.error("Failed to re-trigger processing for document id={}", doc.getId(), e);
                }
            }
        }
    }
}
