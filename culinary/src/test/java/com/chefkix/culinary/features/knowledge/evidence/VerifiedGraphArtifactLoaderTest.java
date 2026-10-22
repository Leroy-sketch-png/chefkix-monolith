package com.chefkix.culinary.features.knowledge.evidence;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VerifiedGraphArtifactLoaderTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Map<String, byte[]> VALID_OUTPUTS = Map.of(
            "nodes.jsonl", "{}\n".getBytes(StandardCharsets.UTF_8),
            "edges.jsonl", "{}\n".getBytes(StandardCharsets.UTF_8),
            "edge_evidence.jsonl", "{}\n".getBytes(StandardCharsets.UTF_8),
            "row_errors.jsonl", new byte[0]);

    @TempDir
    Path tempDir;

    private final VerifiedGraphArtifactLoader loader = new VerifiedGraphArtifactLoader();

    @Test
    void loadsOnlyPinnedExactInventoryAndReturnsDefensiveByteSnapshots() throws Exception {
        Path root = writeBundle(tempDir.resolve("valid"), VALID_OUTPUTS);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));

        var result = loader.load(root, pin);

        var loaded = assertInstanceOf(VerifiedGraphArtifactLoader.Loaded.class, result).snapshot();
        assertEquals(pin, loaded.manifestSha256());
        assertEquals(Set.of("manifest.json", "nodes.jsonl", "edges.jsonl",
                "edge_evidence.jsonl", "row_errors.jsonl"), loaded.fileNames());
        byte[] returned = loaded.fileBytes("nodes.jsonl");
        returned[0] = 'X';
        assertArrayEquals("{}\n".getBytes(StandardCharsets.UTF_8), loaded.fileBytes("nodes.jsonl"));
    }

    @Test
    void rejectsInvalidOrMismatchedManifestPin() throws Exception {
        Path root = writeBundle(tempDir.resolve("wrong-pin"), VALID_OUTPUTS);

        assertEquals(VerifiedGraphArtifactLoader.Refusal.INVALID_EXPECTED_MANIFEST_HASH,
                assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class,
                        loader.load(root, "not-a-hash")).reason());
        assertEquals(VerifiedGraphArtifactLoader.Refusal.MANIFEST_HASH_MISMATCH,
                assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class,
                        loader.load(root, "0".repeat(64))).reason());
    }

    @Test
    void rejectsTamperedOutputWithoutReturningAnyBundleBytes() throws Exception {
        Path root = writeBundle(tempDir.resolve("tampered"), VALID_OUTPUTS);
        String pin = sha256(Files.readAllBytes(root.resolve("manifest.json")));
        Files.writeString(root.resolve("edges.jsonl"), "tampered\n");

        var result = assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class, loader.load(root, pin));
        assertEquals(VerifiedGraphArtifactLoader.Refusal.OUTPUT_HASH_MISMATCH, result.reason());
    }

    @Test
    void rejectsMissingAndExtraFilesRatherThanReadingAnOlderBundle() throws Exception {
        Path missing = writeBundle(tempDir.resolve("missing"), VALID_OUTPUTS);
        String missingPin = sha256(Files.readAllBytes(missing.resolve("manifest.json")));
        Files.delete(missing.resolve("row_errors.jsonl"));
        assertEquals(VerifiedGraphArtifactLoader.Refusal.INVENTORY_MISMATCH,
                assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class,
                        loader.load(missing, missingPin)).reason());

        Path extra = writeBundle(tempDir.resolve("extra"), VALID_OUTPUTS);
        String extraPin = sha256(Files.readAllBytes(extra.resolve("manifest.json")));
        Files.writeString(extra.resolve("old-seed.jsonl"), "must-not-be-used\n");
        assertEquals(VerifiedGraphArtifactLoader.Refusal.INVENTORY_MISMATCH,
                assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class,
                        loader.load(extra, extraPin)).reason());
    }

    @Test
    void rejectsManifestPathTraversalAndUnexpectedOutputRoles() throws Exception {
        Path root = tempDir.resolve("path-traversal");
        Files.createDirectories(root);
        Map<String, String> outputs = new LinkedHashMap<>();
        VALID_OUTPUTS.forEach((name, bytes) -> outputs.put(name, sha256(bytes)));
        outputs.put("../old-seed.jsonl", "a".repeat(64));
        Files.write(root.resolve("manifest.json"), manifest(outputs));

        var result = assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class,
                loader.load(root, sha256(Files.readAllBytes(root.resolve("manifest.json")))));
        assertEquals(VerifiedGraphArtifactLoader.Refusal.INVALID_MANIFEST, result.reason());
    }

    @Test
    void rejectsDuplicateJsonKeysAndSymbolicLinks() throws Exception {
        Path duplicate = tempDir.resolve("duplicate");
        Files.createDirectories(duplicate);
        byte[] duplicateBytes = "{\"schema_id\":\"chefkix.graph_eval_v4\",\"schema_id\":\"chefkix.graph_eval_v4\"}"
                .getBytes(StandardCharsets.UTF_8);
        Files.write(duplicate.resolve("manifest.json"), duplicateBytes);
        assertEquals(VerifiedGraphArtifactLoader.Refusal.INVALID_MANIFEST,
                assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class,
                        loader.load(duplicate, sha256(duplicateBytes))).reason());

        Path linked = writeBundle(tempDir.resolve("linked"), VALID_OUTPUTS);
        String pin = sha256(Files.readAllBytes(linked.resolve("manifest.json")));
        Path target = tempDir.resolve("external-output.txt");
        Files.writeString(target, "not in bundle\n");
        try {
            Files.delete(linked.resolve("edges.jsonl"));
            Files.createSymbolicLink(linked.resolve("edges.jsonl"), target);
        } catch (UnsupportedOperationException | IOException exception) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "symbolic links unavailable on this host");
        }
        assertEquals(VerifiedGraphArtifactLoader.Refusal.INVENTORY_MISMATCH,
                assertInstanceOf(VerifiedGraphArtifactLoader.Refused.class,
                        loader.load(linked, pin)).reason());
    }

    private static Path writeBundle(Path root, Map<String, byte[]> outputs) throws IOException {
        Files.createDirectories(root);
        Map<String, String> hashes = new LinkedHashMap<>();
        outputs.forEach((name, bytes) -> hashes.put(name, sha256(bytes)));
        Files.write(root.resolve("manifest.json"), manifest(hashes));
        outputs.forEach((name, bytes) -> {
            try {
                Files.write(root.resolve(name), bytes);
            } catch (IOException exception) {
                throw new IllegalStateException(exception);
            }
        });
        return root;
    }

    private static byte[] manifest(Map<String, String> outputHashes) throws IOException {
        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema_id", "chefkix.graph_eval_v4");
        manifest.put("schema_version", 4);
        manifest.put("outputs_sha256", outputHashes);
        return JSON.writeValueAsBytes(manifest);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
