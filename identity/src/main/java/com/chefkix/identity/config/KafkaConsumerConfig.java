package com.chefkix.identity.config;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value; // Import thêm cái này
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers:localhost:9094}")
  private String bootstrapServers;

  /** 🔧 Hàm generic tạo factory cho bất kỳ event class nào */
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
