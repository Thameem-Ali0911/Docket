package com.docket.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.docket.config.RabbitMqConfig;
import com.docket.dto.DocumentProcessingMessage;
import com.docket.entity.Document;
import com.docket.repository.DocumentRepository;

/**
 * RabbitMQ consumer that listens for document processing messages and triggers
 * the OCR → extraction → summarization → anomaly pipeline.
 *
 * <p>Only instantiated when {@code docket.processing.mode=queue}. Uses the
 * synchronous {@link DocumentProcessingService#processDocument(Document)} method
 * (not the {@code @Async} variant) since the RabbitMQ listener thread is already
 * the "worker" thread.</p>
 *
 * <p>If a document ID in the message no longer exists (e.g. deleted between
 * publish and consume), the message is acknowledged and logged — no retry.</p>
 */
@Service
@ConditionalOnProperty(name = "docket.processing.mode", havingValue = "queue")
public class DocumentProcessingConsumer {

    private static final Logger log = LoggerFactory.getLogger(DocumentProcessingConsumer.class);

    private final DocumentRepository documentRepository;
    private final DocumentProcessingService documentProcessingService;

    public DocumentProcessingConsumer(DocumentRepository documentRepository,
                                      DocumentProcessingService documentProcessingService) {
        this.documentRepository = documentRepository;
        this.documentProcessingService = documentProcessingService;
    }

    /**
     * Receives a document processing message from the queue, loads the full entity,
     * and runs the synchronous processing pipeline.
     */
    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    public void onMessage(DocumentProcessingMessage message) {
        Integer docId = message.documentId();
        log.info("Received processing message for document id={}", docId);

        Optional<Document> optDoc = documentRepository.findById(docId);
        if (optDoc.isEmpty()) {
            log.warn("Document id={} not found in DB — may have been deleted. Acknowledging and skipping.", docId);
            return;
        }

        Document doc = optDoc.get();
        try {
            documentProcessingService.processDocument(doc);
            log.info("Completed queue-based processing for document id={} (status={})", doc.getId(), doc.getStatus());
        } catch (Throwable t) {
            // processDocument already handles Throwable internally and sets terminal status,
            // but catch here as a last-resort to prevent unacknowledged messages.
            log.error("Unexpected failure processing document id={} from queue", docId, t);
        }
    }
}
