package com.docket.dto;

/**
 * Lightweight message payload published to RabbitMQ for document processing.
 *
 * <p>Only carries the document ID — the consumer re-loads the full {@code Document}
 * entity from the database. This avoids serializing Hibernate-proxied JPA entities
 * (which can cause {@code InvalidDefinitionException} / lazy-init issues) and keeps
 * messages small and resilient to entity schema changes.</p>
 */
public record DocumentProcessingMessage(Integer documentId) {
}
