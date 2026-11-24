package com.chefkix.culinary.features.knowledge.product;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "chefkix.knowledge.product.foodb")
public class FoodbProductGraphProperties {
    private boolean enabled;
    private Path bundlePath;
    private Path sourceArchivePath;
    private String expectedManifestSha256;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Path getBundlePath() {
        return bundlePath;
    }

    public void setBundlePath(Path bundlePath) {
        this.bundlePath = bundlePath;
    }

    public Path getSourceArchivePath() {
        return sourceArchivePath;
    }

    public void setSourceArchivePath(Path sourceArchivePath) {
        this.sourceArchivePath = sourceArchivePath;
    }

    public String getExpectedManifestSha256() {
        return expectedManifestSha256;
    }

    public void setExpectedManifestSha256(String expectedManifestSha256) {
        this.expectedManifestSha256 = expectedManifestSha256;
    }
}
