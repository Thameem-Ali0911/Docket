package com.docket.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

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
 *
 * <p>Dispatches via RabbitMQ queue (if available) or @Async thread pool (default),
 * matching the application's configured processing mode.</p>
 */
@Service
public class DocumentReconciliationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DocumentReconciliationScheduler.class);
    private static final int STUCK_THRESHOLD_MINUTES = 5;

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;
    private final Optional<DocumentQueuePublisher> queuePublisher;

    public DocumentReconciliationScheduler(DocumentRepository documentRepository,
                                           DocumentProcessingService documentProcessingService,
                                           Optional<DocumentQueuePublisher> queuePublisher) {
        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
        this.queuePublisher = queuePublisher;
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
                    if (queuePublisher.isPresent()) {
                        queuePublisher.get().publish(doc);
                    } else {
                        documentProcessingService.processDocumentAsync(doc);
                    }
                } catch (Exception e) {
                    log.error("Failed to re-trigger processing for document id={}", doc.getId(), e);
                }
            }
        }
    }
}

