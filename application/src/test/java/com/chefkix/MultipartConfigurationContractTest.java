package com.chefkix;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.unit.DataSize;

class MultipartConfigurationContractTest {

  @Test
  void applicationDefaultsPermitCanonicalImageAndVideoUploadLimits() throws IOException {
    StandardEnvironment environment = new StandardEnvironment();
    List<PropertySource<?>> propertySources =
        new YamlPropertySourceLoader()
            .load("application.yml", new ClassPathResource("application.yml"));
    propertySources.forEach(environment.getPropertySources()::addLast);

    MultipartProperties properties =
        Binder.get(environment)
            .bind("spring.servlet.multipart", MultipartProperties.class)
            .orElseThrow(() -> new AssertionError("Multipart configuration was not bound"));

    assertThat(properties.getMaxFileSize()).isEqualTo(DataSize.ofMegabytes(50));
    assertThat(properties.getMaxRequestSize()).isEqualTo(DataSize.ofMegabytes(105));
  }
}
