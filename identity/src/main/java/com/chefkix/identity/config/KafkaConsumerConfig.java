package com.chefkix.identity.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
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
      Class<T> eventClass, String groupId) {
    Map<String, Object> props = new HashMap<>();

    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.chefkix.shared.event,*");

    ConsumerFactory<String, T> consumerFactory =
        new DefaultKafkaConsumerFactory<>(
            props, new StringDeserializer(), new JsonDeserializer<>(eventClass, false));

    ConcurrentKafkaListenerContainerFactory<String, T> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    // 3 retries, 1s between each. Prevents permanent message loss on transient failures.
    factory.setCommonErrorHandler(new DefaultErrorHandler(
        (record, ex) -> log.error("Dead-letter (identity {}): topic={}, offset={}, error={}",
            eventClass.getSimpleName(), record.topic(), record.offset(), ex.getMessage()),
        new FixedBackOff(1000L, 3L)));

    return factory;
  }

  // ================================
  // 🧱 Các factory cụ thể cho từng event
  // ================================

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, com.chefkix.shared.event.PostCreatedEvent>
      postCreatedKafkaListenerContainerFactory() {
    return createFactory(com.chefkix.shared.event.PostCreatedEvent.class, "post-created-group");
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, com.chefkix.shared.event.PostDeletedEvent>
      postDeletedKafkaListenerContainerFactory() {
    return createFactory(com.chefkix.shared.event.PostDeletedEvent.class, "post-deleted-group");
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, com.chefkix.shared.event.XpRewardEvent>
      xpRewardedKafkaListenerContainerFactory() {
    return createFactory(com.chefkix.shared.event.XpRewardEvent.class, "xp-rewarded-group");
  }
}
