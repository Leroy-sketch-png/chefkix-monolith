package com.chefkix.culinary.features.knowledge.evidence;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.*;

/**
 * Read-only adapter over one caller-pinned graph_eval_v4 snapshot. It never resolves aliases,
 * ranks candidates, reads profiles, or falls back to the legacy seed graph.
 */
public final class ManifestBackedGraphEvidenceProvider implements GraphEvidenceProvider {
    private static final Pattern SHA256 = Pattern.compile("(?i)[0-9a-f]{64}");
    private static final Pattern NODE_ID = Pattern.compile("0|[1-9][0-9]*");
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build());

    private final GraphEvidenceProperties properties;
    private final VerifiedGraphArtifactLoader loader;
    private volatile LoadState cached;

    public ManifestBackedGraphEvidenceProvider(
            GraphEvidenceProperties properties, VerifiedGraphArtifactLoader loader) {
        this.properties = properties;
        this.loader = loader;
    }

    @Override
    public ProviderResult findEvidence(Request request) {
        LoadState loaded = loadOnce();
        if (loaded.integrity().status() != ArtifactIntegrityStatus.VERIFIED) {
            return new ProviderResult(loaded.integrity(), null);
        }
        if (request == null || blank(request.sourceIngredientId()) || blank(request.candidateIngredientId())) {
            throw new IllegalArgumentException("graph evidence request must contain exact ingredient IDs");
        }

        boolean sourceKnown = loaded.nodes().contains(request.sourceIngredientId());
        boolean candidateKnown = loaded.nodes().contains(request.candidateIngredientId());
        IdentityStatus identityStatus = sourceKnown && candidateKnown
                ? IdentityStatus.RESOLVED : IdentityStatus.UNKNOWN;
        IdentityResolution identity = new IdentityResolution(
                identityStatus,
                request.sourceIngredientId(),
                sourceKnown ? request.sourceIngredientId() : null,
                List.of(),
                request.candidateIngredientId(),
                candidateKnown ? request.candidateIngredientId() : null,
                List.of());

        Long pairCount = null;
        if (identityStatus == IdentityStatus.RESOLVED) {
            pairCount = loaded.pairCounts().getOrDefault(
                    new Pair(request.sourceIngredientId(), request.candidateIngredientId()), 0L);
        }
        TrainPairEvidence pairEvidence = new TrainPairEvidence(
                pairCount == null ? null : pairCount > 0, pairCount, "TRAIN_PAIRS");
        Evidence evidence = new Evidence(
                identity,
                pairEvidence,
                List.of(new Provenance(
                        "recipe1msubs-train",
                        "UNKNOWN_NOT_REVIEWED",
                        "TRAIN",
                        loaded.state(),
                        loaded.trainArtifactSha256(),
                        loaded.sourceManifestSha256())),
                new Availability(AvailabilityStatus.NOT_IN_ARTIFACT, null),
                new Availability(AvailabilityStatus.NOT_IN_ARTIFACT, null),
                new DeclaredGroupScreening(
                        ScreeningStatus.UNAVAILABLE,
                        "This graph contains no declared-allergen evidence; it makes no safety or clinical claim."),
                new Calibration(null, null, CalibrationStatus.NO_GUARANTEE, null),
                new DecisionResult(
                        Decision.ABSTAIN,
                        "Graph pair evidence alone does not establish a substitution recommendation."));
        return new ProviderResult(loaded.integrity(), evidence);
    }

    private LoadState loadOnce() {
        LoadState current = cached;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            current = cached;
            if (current == null) {
                current = loadConfiguredSnapshot();
                cached = current;
            }
        }
        return current;
    }

    private LoadState loadConfiguredSnapshot() {
        String expected = properties.getExpectedManifestSha256();
        if (properties.getBundlePath() == null || blank(expected)) {
            return failed(ArtifactIntegrityStatus.MISSING, expected, null);
        }
        if (!SHA256.matcher(expected).matches()) {
            return failed(ArtifactIntegrityStatus.INVALID, expected, null);
        }
        VerifiedGraphArtifactLoader.Result result = loader.load(properties.getBundlePath(), expected);
        if (result instanceof VerifiedGraphArtifactLoader.Refused refused) {
            ArtifactIntegrityStatus status = switch (refused.reason()) {
                case INVALID_ROOT, IO_FAILURE -> ArtifactIntegrityStatus.MISSING;
                default -> ArtifactIntegrityStatus.INVALID;
            };
            return failed(status, expected, null);
        }

        VerifiedGraphArtifactLoader.Snapshot snapshot =
                ((VerifiedGraphArtifactLoader.Loaded) result).snapshot();
        ArtifactIntegrity verified = new ArtifactIntegrity(
                ArtifactIntegrityStatus.VERIFIED, expected, snapshot.manifestSha256());
        try {
            ParsedGraph graph = parse(snapshot);
            return new LoadState(verified, graph.nodes(), graph.pairCounts(), graph.state(),
                    graph.trainArtifactSha256(), graph.sourceManifestSha256());
        } catch (IOException | RuntimeException exception) {
            return failed(ArtifactIntegrityStatus.INVALID, expected, snapshot.manifestSha256());
        }
    }

    private ParsedGraph parse(VerifiedGraphArtifactLoader.Snapshot snapshot) throws IOException {
        JsonNode manifest = JSON.readTree(snapshot.fileBytes("manifest.json"));
        JsonNode source = objectField(manifest, "source");
        if (!"train".equals(text(source, "split"))) {
            throw new IllegalArgumentException("graph source is not train-only");
        }
        String trainHash = hashField(source, "train_csv_sha256");
        String sourceManifestHash = hashField(source, "source_manifest_sha256");
        String state = text(manifest, "state");
        if (state == null || !state.startsWith("PROVISIONAL_PRIVATE_CANDIDATE")) {
            throw new IllegalArgumentException("unsupported graph acceptance state");
        }
        JsonNode counts = objectField(manifest, "counts");
        if (requiredLong(counts, "identity_error_rows") != 0) {
            throw new IllegalArgumentException("graph candidate contains unresolved identity rows");
        }
        String edgeArtifactHash = hashField(objectField(manifest, "outputs_sha256"), "edges.jsonl");
        if (!edgeArtifactHash.equalsIgnoreCase(sha256(snapshot.fileBytes("edges.jsonl")))) {
            throw new IllegalArgumentException("verified edge bytes disagree with manifest digest");
        }

        Set<String> nodes = readNodes(snapshot.fileBytes("nodes.jsonl"));
        Map<Pair, EdgeSummary> edges = readEdges(snapshot.fileBytes("edges.jsonl"), nodes);
        validateEvidence(snapshot.fileBytes("edge_evidence.jsonl"), edges, trainHash, manifest);
        if (snapshot.fileBytes("row_errors.jsonl").length != 0
                || requiredLong(counts, "nodes") != nodes.size()
                || requiredLong(counts, "edges") != edges.size()) {
            throw new IllegalArgumentException("manifest counts or row-error inventory do not reconcile");
        }
        Map<Pair, Long> pairCounts = new HashMap<>();
        edges.forEach((pair, summary) -> pairCounts.put(pair, summary.count()));
        return new ParsedGraph(Set.copyOf(nodes), Map.copyOf(pairCounts), state, trainHash, sourceManifestHash);
    }

    private static Set<String> readNodes(byte[] bytes) throws IOException {
        Set<String> nodes = new HashSet<>();
        for (JsonNode row : jsonLines(bytes, false)) {
            requireFields(row, Set.of("canonical_id", "display_name", "identity_status", "node_type", "source_refs"));
            String id = text(row, "canonical_id");
            if (id == null || !NODE_ID.matcher(id).matches() || blank(text(row, "display_name"))
                    || !"resolved".equals(text(row, "identity_status"))
                    || !"ingredient".equals(text(row, "node_type"))
                    || !stringArray(row.get("source_refs")).equals(List.of("gismo_vocabulary", "gismo_node_catalog"))
                    || !nodes.add(id)) {
                throw new IllegalArgumentException("invalid or duplicate graph node");
            }
        }
        if (nodes.isEmpty()) {
            throw new IllegalArgumentException("graph node inventory is empty");
        }
        return nodes;
    }

    private static Map<Pair, EdgeSummary> readEdges(byte[] bytes, Set<String> nodes) throws IOException {
        Map<Pair, EdgeSummary> edges = new TreeMap<>((left, right) -> {
            int source = compareIds(left.sourceId(), right.sourceId());
            return source != 0 ? source : compareIds(left.targetId(), right.targetId());
        });
        for (JsonNode row : jsonLines(bytes, false)) {
            requireFields(row, Set.of("distinct_context_count", "edge_ref", "evidence_refs", "relation",
                    "seen", "source_id", "target_id", "train_pair_count"));
            String source = text(row, "source_id");
            String target = text(row, "target_id");
            long pairCount = requiredLong(row, "train_pair_count");
            long contextCount = requiredLong(row, "distinct_context_count");
            List<String> refs = stringArray(row.get("evidence_refs"));
            Pair pair = new Pair(source, target);
            if (source == null || target == null || !nodes.contains(source) || !nodes.contains(target)
                    || !"substitution".equals(text(row, "relation"))
                    || !(source + ":" + target + ":substitution").equals(text(row, "edge_ref"))
                    || pairCount <= 0 || contextCount <= 0 || contextCount > pairCount
                    || !row.path("seen").isBoolean() || !row.path("seen").booleanValue()
                    || refs.isEmpty() || !refs.equals(new ArrayList<>(new TreeSet<>(refs)))
                    || edges.putIfAbsent(pair, new EdgeSummary(pairCount, contextCount, refs)) != null) {
                throw new IllegalArgumentException("invalid or duplicate graph edge");
            }
        }
        return edges;
    }

    private static void validateEvidence(
            byte[] bytes, Map<Pair, EdgeSummary> edges, String trainHash, JsonNode manifest) throws IOException {
        Map<Pair, Set<String>> rowKeys = new HashMap<>();
        Map<Pair, Set<String>> contextHashes = new HashMap<>();
        Map<Pair, Set<String>> evidenceIds = new HashMap<>();
        JsonNode transformations = manifest.get("transformations");
        if (transformations == null || !transformations.isArray() || transformations.size() != 1) {
            throw new IllegalArgumentException("manifest must pin exactly one transformation");
        }
        JsonNode transform = transformations.get(0);
        String transformationId = text(transform, "id");
        String transformationHash = hashField(transform, "sha256");
        Set<String> allEvidenceIds = new HashSet<>();
        for (JsonNode row : jsonLines(bytes, false)) {
            requireFields(row, Set.of("context_sha256", "edge_ref", "evidence_id", "relation",
                    "source_artifact_sha256", "source_id", "source_row_key", "split",
                    "target_id", "transformation_id", "transformation_sha256"));
            String source = text(row, "source_id");
            String target = text(row, "target_id");
            Pair pair = new Pair(source, target);
            EdgeSummary edge = edges.get(pair);
            String rowKey = text(row, "source_row_key");
            String contextHash = hashField(row, "context_sha256");
            String evidenceId = hashField(row, "evidence_id");
            if (edge == null || !"substitution".equals(text(row, "relation"))
                    || !(source + ":" + target + ":substitution").equals(text(row, "edge_ref"))
                    || !"train".equals(text(row, "split"))
                    || !trainHash.equalsIgnoreCase(text(row, "source_artifact_sha256"))
                    || !transformationId.equals(text(row, "transformation_id"))
                    || !transformationHash.equalsIgnoreCase(text(row, "transformation_sha256"))
                    || blank(rowKey) || !allEvidenceIds.add(evidenceId)) {
                throw new IllegalArgumentException("invalid or duplicate train evidence record");
            }
            String expectedId = sha256(JSON.writeValueAsBytes(List.of(source, target, "substitution", trainHash,
                    rowKey, transformationId, transformationHash, contextHash)));
            if (!expectedId.equalsIgnoreCase(evidenceId)) {
                throw new IllegalArgumentException("evidence ID does not bind its train-row provenance");
            }
            rowKeys.computeIfAbsent(pair, ignored -> new HashSet<>()).add(rowKey);
            contextHashes.computeIfAbsent(pair, ignored -> new HashSet<>()).add(contextHash);
            evidenceIds.computeIfAbsent(pair, ignored -> new TreeSet<>()).add(evidenceId);
        }
        for (Map.Entry<Pair, EdgeSummary> entry : edges.entrySet()) {
            Pair pair = entry.getKey();
            EdgeSummary edge = entry.getValue();
            if (rowKeys.getOrDefault(pair, Set.of()).size() != edge.count()
                    || contextHashes.getOrDefault(pair, Set.of()).size() != edge.contextCount()
                    || !new ArrayList<>(evidenceIds.getOrDefault(pair, Set.of())).equals(edge.evidenceRefs())) {
                throw new IllegalArgumentException("edge support does not reconcile to its evidence records");
            }
        }
        if (requiredLong(objectField(manifest, "counts"), "evidence_records") != allEvidenceIds.size()) {
            throw new IllegalArgumentException("manifest evidence count does not reconcile");
        }
    }

    private static List<JsonNode> jsonLines(byte[] bytes, boolean allowEmpty) throws IOException {
        if (bytes.length == 0) {
            if (allowEmpty) return List.of();
            throw new IllegalArgumentException("required JSONL inventory is empty");
        }
        String content = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString();
        if (!content.endsWith("\n") || content.contains("\r")) {
            throw new IllegalArgumentException("JSONL must be UTF-8 with LF terminators only");
        }
        List<JsonNode> rows = new ArrayList<>();
        for (String line : content.substring(0, content.length() - 1).split("\n", -1)) {
            if (line.isEmpty()) throw new IllegalArgumentException("blank JSONL record");
            JsonNode row = JSON.readTree(line);
            if (row == null || !row.isObject()) throw new IllegalArgumentException("JSONL record must be an object");
            rows.add(row);
        }
        return rows;
    }

    private static void requireFields(JsonNode node, Set<String> fields) {
        Set<String> actual = new TreeSet<>();
        node.fieldNames().forEachRemaining(actual::add);
        if (!actual.equals(new TreeSet<>(fields))) throw new IllegalArgumentException("unexpected artifact record fields");
    }

    private static List<String> stringArray(JsonNode node) {
        if (node == null || !node.isArray()) throw new IllegalArgumentException("expected string array");
        List<String> values = new ArrayList<>();
        for (JsonNode value : node) {
            if (!value.isTextual()) throw new IllegalArgumentException("expected string array");
            values.add(value.textValue());
        }
        return values;
    }

    private static JsonNode objectField(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isObject()) throw new IllegalArgumentException("missing object field: " + field);
        return value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() ? value.textValue() : null;
    }

    private static String hashField(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || !SHA256.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid SHA-256 field: " + field);
        }
        return value;
    }

    private static long requiredLong(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) {
            throw new IllegalArgumentException("invalid nonnegative integer field: " + field);
        }
        return value.longValue();
    }

    private static int compareIds(String left, String right) {
        return Long.compare(Long.parseLong(left), Long.parseLong(right));
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static LoadState failed(ArtifactIntegrityStatus status, String expected, String observed) {
        return new LoadState(new ArtifactIntegrity(status, expected, observed), Set.of(), Map.of(), null, null, null);
    }

    private record Pair(String sourceId, String targetId) {}
    private record EdgeSummary(long count, long contextCount, List<String> evidenceRefs) {}
    private record ParsedGraph(Set<String> nodes, Map<Pair, Long> pairCounts, String state,
                               String trainArtifactSha256, String sourceManifestSha256) {}
    private record LoadState(ArtifactIntegrity integrity, Set<String> nodes, Map<Pair, Long> pairCounts,
                             String state, String trainArtifactSha256, String sourceManifestSha256) {}
}
