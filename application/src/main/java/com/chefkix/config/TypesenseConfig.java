package com.chefkix.config;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Data
@Configuration
@ConfigurationProperties(prefix = "chefkix.typesense")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TypesenseConfig {

    String host = "localhost";
    int port = 8108;
    String protocol = "http";
    String apiKey;

    @Bean("typesenseRestClient")
    public RestClient typesenseRestClient() {
        return RestClient.builder()
                .baseUrl(protocol + "://" + host + ":" + port)
                .defaultHeader("X-TYPESENSE-API-KEY", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
