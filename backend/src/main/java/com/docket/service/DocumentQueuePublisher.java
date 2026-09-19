package com.docket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.docket.config.RabbitMqConfig;
import com.docket.dto.DocumentProcessingMessage;
import com.docket.entity.Document;

/**
 * Publishes document processing tasks to the RabbitMQ queue.
 *
 * <p>Only instantiated when {@code docket.processing.mode=queue}. Callers
 * (e.g. {@link DocumentService}) inject this as an {@code Optional<DocumentQueuePublisher>}
 * so the async fallback path still works when this bean is absent.</p>
 */
@Service
@ConditionalOnProperty(name = "docket.processing.mode", havingValue = "queue")
public class DocumentQueuePublisher {

    private static final Logger log = LoggerFactory.getLogger(DocumentQueuePublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public DocumentQueuePublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publishes a lightweight message to the processing queue.
     *
     * @param doc the persisted document entity (must have a non-null ID)
     */
    public void publish(Document doc) {
        DocumentProcessingMessage message = new DocumentProcessingMessage(doc.getId());
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NAME,
                RabbitMqConfig.ROUTING_KEY,
                message
        );
        log.info("Published document id={} (type={}) to processing queue", doc.getId(), doc.getType());
    }
}
