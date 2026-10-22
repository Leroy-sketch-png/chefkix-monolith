package com.chefkix.culinary.features.knowledge.evidence;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.nio.file.Path;
import java.util.regex.Pattern;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GraphEvidenceProperties.class)
public class GraphEvidenceConfiguration {
    @Bean
    @ConditionalOnProperty(
            prefix = "chefkix.knowledge.graph-evidence", name = "enabled", havingValue = "true")
    @Conditional(ValidGraphArtifactConfiguration.class)
    GraphEvidenceProvider manifestBackedGraphEvidenceProvider(GraphEvidenceProperties properties) {
        return new ManifestBackedGraphEvidenceProvider(properties, new VerifiedGraphArtifactLoader());
    }

    static final class ValidGraphArtifactConfiguration implements Condition {
        private static final Pattern SHA256 = Pattern.compile("(?i)[0-9a-f]{64}");

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String path = context.getEnvironment().getProperty(
                    "chefkix.knowledge.graph-evidence.bundle-path");
            String manifestHash = context.getEnvironment().getProperty(
                    "chefkix.knowledge.graph-evidence.expected-manifest-sha256");
            if (path == null || manifestHash == null || !SHA256.matcher(manifestHash).matches()) {
                return false;
            }
            try {
                return Path.of(path).isAbsolute();
            } catch (RuntimeException invalidPath) {
                return false;
            }
        }
    }
}
