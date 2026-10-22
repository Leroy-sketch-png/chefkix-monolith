package com.chefkix.culinary.features.knowledge.evidence;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

/** Evidence endpoint is explicitly disabled unless an operator opts in. */
@ConfigurationProperties(prefix = "chefkix.knowledge.graph-evidence")
public class GraphEvidenceProperties {
    private boolean enabled = false;
    private Path bundlePath;
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

    public String getExpectedManifestSha256() {
        return expectedManifestSha256;
    }

    public void setExpectedManifestSha256(String expectedManifestSha256) {
        this.expectedManifestSha256 = expectedManifestSha256;
    }
}
