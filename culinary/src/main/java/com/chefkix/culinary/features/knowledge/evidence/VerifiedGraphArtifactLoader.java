package com.chefkix.culinary.features.knowledge.evidence;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Integrity-only loader for a caller-pinned graph_eval_v4 bundle.
 * It has no dependency on, or fallback to, the legacy seed graph.
 */
public final class VerifiedGraphArtifactLoader {
    private static final String MANIFEST = "manifest.json";
    private static final Set<String> OUTPUTS = Set.of(
            "nodes.jsonl", "edges.jsonl", "edge_evidence.jsonl", "row_errors.jsonl");
    private static final Pattern SHA256 = Pattern.compile("(?i)[0-9a-f]{64}");
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build());

    public enum Refusal {
        INVALID_EXPECTED_MANIFEST_HASH,
        INVALID_ROOT,
        MANIFEST_HASH_MISMATCH,
        INVALID_MANIFEST,
        INVENTORY_MISMATCH,
        OUTPUT_HASH_MISMATCH,
        IO_FAILURE
    }

    public sealed interface Result permits Loaded, Refused {}

    public record Loaded(Snapshot snapshot) implements Result {}

    public record Refused(Refusal reason) implements Result {}

    /** Defensive byte ownership means callers consume the exact bytes that were hashed. */
    public static final class Snapshot {
        private final String manifestSha256;
        private final Map<String, byte[]> files;

        private Snapshot(String manifestSha256, Map<String, byte[]> files) {
            this.manifestSha256 = manifestSha256;
            Map<String, byte[]> owned = new LinkedHashMap<>();
            files.forEach((name, bytes) -> owned.put(name, bytes.clone()));
            this.files = Collections.unmodifiableMap(owned);
        }

        public String manifestSha256() {
            return manifestSha256;
        }

        public Set<String> fileNames() {
            return files.keySet();
        }

        public byte[] fileBytes(String name) {
            byte[] bytes = files.get(name);
            return bytes == null ? null : bytes.clone();
        }
    }

    public Result load(Path bundleRoot, String expectedManifestSha256) {
        if (expectedManifestSha256 == null || !SHA256.matcher(expectedManifestSha256).matches()) {
            return new Refused(Refusal.INVALID_EXPECTED_MANIFEST_HASH);
        }
        if (!validRoot(bundleRoot)) {
            return new Refused(Refusal.INVALID_ROOT);
        }

        try {
            Path root = bundleRoot.toAbsolutePath().normalize();
            byte[] manifestBytes = Files.readAllBytes(root.resolve(MANIFEST));
            String observedManifestHash = sha256(manifestBytes);
            if (!expectedManifestSha256.equalsIgnoreCase(observedManifestHash)) {
                return new Refused(Refusal.MANIFEST_HASH_MISMATCH);
            }

            JsonNode manifest;
            try {
                manifest = JSON.readTree(manifestBytes);
            } catch (IOException malformedJson) {
                return new Refused(Refusal.INVALID_MANIFEST);
            }
            if (!validManifest(manifest)) {
                return new Refused(Refusal.INVALID_MANIFEST);
            }
            JsonNode outputs = manifest.get("outputs_sha256");
            Set<String> declared = new TreeSet<>();
            outputs.fieldNames().forEachRemaining(declared::add);
            if (!declared.equals(new TreeSet<>(OUTPUTS))) {
                return new Refused(Refusal.INVALID_MANIFEST);
            }

            Set<String> expectedInventory = new TreeSet<>(OUTPUTS);
            expectedInventory.add(MANIFEST);
            Set<String> observedInventory = inventory(root);
            if (!expectedInventory.equals(observedInventory)) {
                return new Refused(Refusal.INVENTORY_MISMATCH);
            }

            Map<String, byte[]> verified = new LinkedHashMap<>();
            verified.put(MANIFEST, manifestBytes);
            for (String name : new TreeSet<>(OUTPUTS)) {
                byte[] bytes = Files.readAllBytes(root.resolve(name));
                String expected = outputs.get(name).textValue();
                if (!expected.equalsIgnoreCase(sha256(bytes))) {
                    return new Refused(Refusal.OUTPUT_HASH_MISMATCH);
                }
                verified.put(name, bytes);
            }
            return new Loaded(new Snapshot(observedManifestHash, verified));
        } catch (IOException | NoSuchAlgorithmException | RuntimeException exception) {
            return new Refused(Refusal.IO_FAILURE);
        }
    }

    private static boolean validRoot(Path root) {
        return root != null
                && !Files.isSymbolicLink(root)
                && Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)
                && Files.isRegularFile(root.resolve(MANIFEST), LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(root.resolve(MANIFEST));
    }

    private static boolean validManifest(JsonNode manifest) {
        if (manifest == null || !manifest.isObject()
                || !"chefkix.graph_eval_v4".equals(text(manifest, "schema_id"))
                || !manifest.has("schema_version") || !manifest.get("schema_version").isIntegralNumber()
                || manifest.get("schema_version").intValue() != 4) {
            return false;
        }
        JsonNode outputs = manifest.get("outputs_sha256");
        if (outputs == null || !outputs.isObject()) {
            return false;
        }
        var names = outputs.fieldNames();
        while (names.hasNext()) {
            JsonNode digest = outputs.get(names.next());
            if (digest == null || !digest.isTextual() || !SHA256.matcher(digest.textValue()).matches()) {
                return false;
            }
        }
        return true;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    private static Set<String> inventory(Path root) throws IOException {
        Set<String> names = new TreeSet<>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(root)) {
            for (Path entry : entries) {
                if (Files.isSymbolicLink(entry) || !Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) {
                    return Set.of("<invalid-entry>");
                }
                names.add(entry.getFileName().toString());
            }
        }
        return names;
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormatSupport.toHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static final class HexFormatSupport {
        private static final char[] HEX = "0123456789abcdef".toCharArray();

        private static String toHex(byte[] bytes) {
            char[] result = new char[bytes.length * 2];
            for (int i = 0; i < bytes.length; i++) {
                int value = bytes[i] & 0xff;
                result[i * 2] = HEX[value >>> 4];
                result[i * 2 + 1] = HEX[value & 0x0f];
            }
            return new String(result);
        }
    }
}
