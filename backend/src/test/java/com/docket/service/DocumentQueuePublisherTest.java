package com.docket.service;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.docket.config.RabbitMqConfig;
import com.docket.dto.DocumentProcessingMessage;
import com.docket.entity.Document;
import com.docket.entity.DocumentStatus;
import com.docket.entity.DocumentType;
import com.docket.entity.Workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentQueuePublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private DocumentQueuePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new DocumentQueuePublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("publish sends DocumentProcessingMessage with documentId to RabbitMQ exchange")
    void testPublishSendsMessage() throws Exception {
        Workspace workspace = new Workspace("Test WS");
        Document doc = new Document(workspace, DocumentType.INVOICE, "/uploads/test.pdf", DocumentStatus.PENDING);
        Field field = doc.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(doc, 77);

        publisher.publish(doc);

        ArgumentCaptor<DocumentProcessingMessage> messageCaptor = ArgumentCaptor.forClass(DocumentProcessingMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.EXCHANGE_NAME),
                eq(RabbitMqConfig.ROUTING_KEY),
                messageCaptor.capture()
        );

        assertEquals(77, messageCaptor.getValue().documentId());
    }
}
