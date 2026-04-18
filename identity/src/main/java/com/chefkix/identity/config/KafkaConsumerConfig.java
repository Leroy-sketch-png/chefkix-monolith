package com.chefkix.identity.config;

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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration("identityKafkaConsumerConfig")
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers:localhost:9094}")
  private String bootstrapServers;

  /** Generic factory builder with error handling — 3 retries, 1s between each. */
  private <T> ConcurrentKafkaListenerContainerFactory<String, T> createFactory(
      Class<T> eventClass, String groupId, KafkaOperations<Object, Object> kafkaOperations) {
    Map<String, Object> props = new HashMap<>();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.chefkix.shared.event,com.chefkix.identity.entity,com.chefkix.identity.enums");

    ConsumerFactory<String, T> consumerFactory =
        new DefaultKafkaConsumerFactory<>(
            props, new StringDeserializer(), new JsonDeserializer<>(eventClass, false));

    ConcurrentKafkaListenerContainerFactory<String, T> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    DeadLetterPublishingRecoverer recoverer =
      new DeadLetterPublishingRecoverer(
        kafkaOperations,
        (record, ex) -> new TopicPartition(record.topic() + ".dlt", record.partition()));
    // 3 retries, 1s between each. Prevents permanent message loss on transient failures.
    factory.setCommonErrorHandler(new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L)));

    return factory;
  }

  // ================================
  // 🧱 Specific factories for each event type
  // ================================

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, com.chefkix.shared.event.PostCreatedEvent>
      postCreatedKafkaListenerContainerFactory(KafkaOperations<Object, Object> kafkaOperations) {
    return createFactory(com.chefkix.shared.event.PostCreatedEvent.class, "post-created-group", kafkaOperations);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, com.chefkix.shared.event.PostDeletedEvent>
      postDeletedKafkaListenerContainerFactory(KafkaOperations<Object, Object> kafkaOperations) {
    return createFactory(com.chefkix.shared.event.PostDeletedEvent.class, "post-deleted-group", kafkaOperations);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, com.chefkix.shared.event.XpRewardEvent>
      xpRewardedKafkaListenerContainerFactory(KafkaOperations<Object, Object> kafkaOperations) {
    return createFactory(com.chefkix.shared.event.XpRewardEvent.class, "xp-rewarded-group", kafkaOperations);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, com.chefkix.identity.entity.UserEvent>
      userEventKafkaListenerContainerFactory(KafkaOperations<Object, Object> kafkaOperations) {
    return createFactory(com.chefkix.identity.entity.UserEvent.class, "user-events-group", kafkaOperations);
  }
}
