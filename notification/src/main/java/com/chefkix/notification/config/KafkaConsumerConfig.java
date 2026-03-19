package com.chefkix.notification.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.chefkix.shared.event.BaseEvent;
import com.chefkix.shared.event.EmailEvent;

import lombok.extern.slf4j.Slf4j;

/**
 * Kafka consumer configuration for the notification module.
 * Two consumer factories:
 * <ul>
 *   <li>{@code notificationEventListenerFactory} — polymorphic BaseEvent (bell notifications)</li>
 *   <li>{@code emailEventListenerFactory} — flat EmailEvent (OTP/transactional emails)</li>
 * </ul>
 */
@Slf4j
@Configuration("notificationKafkaConsumerConfig")
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9094}")
    private String bootstrapServers;

    // =========================================================================
    // FACTORY FOR POLYMORPHIC EVENTS (BaseEvent with @JsonSubTypes)
    // =========================================================================

    @Bean
    public ConsumerFactory<String, BaseEvent> notificationEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.chefkix.shared.event");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, BaseEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new JsonDeserializer<>(BaseEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BaseEvent> notificationEventListenerFactory(
            KafkaOperations<Object, Object> kafkaOperations) {
        ConcurrentKafkaListenerContainerFactory<String, BaseEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationEventConsumerFactory());
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaOperations,
                        (record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition()));
        // 3 retries, 1s between each. Prevents permanent message loss on transient failures.
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L)));
        return factory;
    }

    // =========================================================================
    // FACTORY FOR EMAIL EVENTS (Simple flat DTO, no polymorphism)
    // =========================================================================

    @Bean
    public ConsumerFactory<String, EmailEvent> emailEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.chefkix.shared.event");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, EmailEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new JsonDeserializer<>(EmailEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmailEvent> emailEventListenerFactory(
            KafkaOperations<Object, Object> kafkaOperations) {
        ConcurrentKafkaListenerContainerFactory<String, EmailEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(emailEventConsumerFactory());
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaOperations,
                        (record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition()));
        // 3 retries, 1s between each. Email delivery is important — don't lose OTPs.
        factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L)));
        return factory;
    }
}
