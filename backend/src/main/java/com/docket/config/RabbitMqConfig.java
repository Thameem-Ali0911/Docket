package com.docket.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ infrastructure beans — only created when {@code docket.processing.mode=queue}.
 *
 * <p>Declares a durable queue, a direct exchange, and a JSON message converter so
 * {@link com.docket.dto.DocumentProcessingMessage} is serialized/deserialized
 * automatically by Spring AMQP.</p>
 *
 * <p>When running with the default {@code mode=async}, this entire configuration class
 * is skipped and no RabbitMQ connection is attempted.</p>
 */
@Configuration
@ConditionalOnProperty(name = "docket.processing.mode", havingValue = "queue")
public class RabbitMqConfig {

    public static final String QUEUE_NAME = "docket.document.processing";
    public static final String EXCHANGE_NAME = "docket.exchange";
    public static final String ROUTING_KEY = "document.process";

    @Bean
    public Queue documentProcessingQueue() {
        // durable = true → queue (and its messages) survive broker restarts
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public DirectExchange docketExchange() {
        return new DirectExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding documentProcessingBinding(Queue documentProcessingQueue, DirectExchange docketExchange) {
        return BindingBuilder.bind(documentProcessingQueue)
                .to(docketExchange)
                .with(ROUTING_KEY);
    }

    /**
     * Jackson-based message converter so payloads are human-readable JSON in the
     * RabbitMQ Management UI, and deserialization uses the same ObjectMapper Spring
     * Boot already configures globally.
     */
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
