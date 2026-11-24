package com.chefkix.culinary.features.knowledge.product;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
public class FoodbProductGraphService {
    public static final String EXPECTED_MANIFEST_SHA256 =
            "a187fe108dc2e716169344f3cb120c881a8e481b923351acfd49881cf5a6a01f";
    private static final String FOOD_PREFIX = "foodb:1.0:food-id:";
    private static final String COMPOUND_PREFIX = "foodb:1.0:compound-id:";
    private static final String FOOD_MEMBER = "foodb_2020_04_07_json/Food.json";
    private static final String COMPOUND_MEMBER = "foodb_2020_04_07_json/Compound.json";
    private static final Pattern SHA256 = Pattern.compile("(?i)[a-f0-9]{64}");
    private static final Pattern SOURCE_ID = Pattern.compile("[1-9][0-9]{0,18}");
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    private static final int MAX_OUTPUT_BYTES = 64 * 1024 * 1024;
    private static final int MAX_SEARCH_RESULTS = 20;
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build());

    private final FoodbProductGraphProperties properties;
    private final String requiredManifestSha256;
    private final AtomicReference<LoadedGraph> loaded = new AtomicReference<>();

    @Autowired
    public FoodbProductGraphService(FoodbProductGraphProperties properties) {
        this(properties, EXPECTED_MANIFEST_SHA256);
    }

    FoodbProductGraphService(FoodbProductGraphProperties properties, String requiredManifestSha256) {
        this.properties = properties;
        this.requiredManifestSha256 = requiredManifestSha256;
    }

    public FoodSearchResponse searchFoods(String query, int limit) {
        if (query == null || query.isBlank() || query.trim().length() > 120
                || limit < 1 || limit > MAX_SEARCH_RESULTS) {
            throw new IllegalArgumentException("invalid product graph search");
        }
        LoadedGraph graph = graph();
        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<FoodCandidate> matches = graph.foods().values().stream()
                .filter(FoodNode::exportToFoodb)
                .filter(food -> graph.edgesByFood().getOrDefault(food.id(), List.of()).stream()
                        .anyMatch(Edge::exportEligible))
                .filter(food -> food.name().toLowerCase(Locale.ROOT).contains(needle))
                .sorted(Comparator.comparing(FoodNode::name).thenComparing(FoodNode::id))
                .limit(limit)
                .map(food -> new FoodCandidate(food.id().substring(FOOD_PREFIX.length()), food.name()))
                .toList();
        return new FoodSearchResponse(graph.provenance(), matches);
    }

    public FoodCompositionResponse getFoodComposition(String foodId) {
        if (foodId == null || !SOURCE_ID.matcher(foodId).matches()) {
            throw new IllegalArgumentException("invalid source Food ID");
        }
        LoadedGraph graph = graph();
        String sourceNodeId = FOOD_PREFIX + foodId;
        FoodNode food = graph.foods().get(sourceNodeId);
        if (food == null || !food.exportToFoodb()) {
            return new FoodCompositionResponse(
                    "UNKNOWN_OR_OUT_OF_SCOPE", graph.provenance(), null, List.of(), 0);
        }

        List<CompoundRelation> relations = graph.edgesByFood().getOrDefault(sourceNodeId, List.of())
                .stream()
                .filter(Edge::exportEligible)
                .map(edge -> {
                    CompoundNode compound = graph.compounds().get(edge.target());
                    return new CompoundRelation(
                            compound.id(), compound.name(), "has_compound", "PRESENT",
                            edge.contentRowSupport(), edge.exportEligibleSupport());
                })
                .toList();
        int filtered = graph.edgesByFood().getOrDefault(sourceNodeId, List.of()).size() - relations.size();
        String status = relations.isEmpty() ? "NO_ELIGIBLE_RELATION_UNKNOWN" : "AVAILABLE";
        return new FoodCompositionResponse(status, graph.provenance(),
                new Food(food.id(), food.name(), "Food"), relations, filtered);
    }

    private LoadedGraph graph() {
        if (!properties.isEnabled()) {
            throw new ProductGraphUnavailableException();
        }
        LoadedGraph current = loaded.get();
        if (current != null) return current;
        synchronized (loaded) {
            current = loaded.get();
            if (current != null) return current;
            try {
                current = loadVerifiedGraph();
                loaded.set(current);
                return current;
            } catch (IOException | NoSuchAlgorithmException | RuntimeException rejected) {
                throw new ProductGraphUnavailableException();
            }
        }
    }

    private LoadedGraph loadVerifiedGraph() throws IOException, NoSuchAlgorithmException {
        Path bundle = requireSafeAbsolutePath(properties.getBundlePath(), true);
        Path archive = requireSafeAbsolutePath(properties.getSourceArchivePath(), false);
        if (!requiredManifestSha256.equalsIgnoreCase(properties.getExpectedManifestSha256())) {
            throw new IOException("unexpected product manifest pin");
        }

        byte[] manifestBytes = readBounded(bundle.resolve("manifest.json"), MAX_MANIFEST_BYTES);
        String manifestHash = sha256(manifestBytes);
        if (!requiredManifestSha256.equalsIgnoreCase(manifestHash)) {
            throw new IOException("manifest hash mismatch");
        }
        JsonNode manifest = JSON.readTree(manifestBytes);
        if (!"graph_product_v4".equals(text(manifest, "destination_graph"))
                || !"chefkix.graph_product_v4.foodb_native_snapshot.v1".equals(text(manifest, "schema"))
                || !"FooDB 1.0 JSON 2020-04-07".equals(text(manifest, "release"))
                || !"presence only; no concentration, equivalence, flavor, nutrient, allergen, or safety inference"
                        .equals(text(manifest, "semantics"))
                || !"not represented; missing relations remain UNKNOWN".equals(text(manifest, "absence_semantics"))) {
            throw new IOException("unsupported product manifest contract");
        }

        JsonNode outputs = manifest.path("outputs");
        if (!outputs.isObject() || outputs.size() != 2
                || !validDigest(text(outputs, "nodes.jsonl"))
                || !validDigest(text(outputs, "edges.jsonl"))) {
            throw new IOException("invalid product output inventory");
        }
        byte[] nodesBytes = readBounded(bundle.resolve("nodes.jsonl"), MAX_OUTPUT_BYTES);
        byte[] edgesBytes = readBounded(bundle.resolve("edges.jsonl"), MAX_OUTPUT_BYTES);
        String nodesHash = sha256(nodesBytes);
        String edgesHash = sha256(edgesBytes);
        if (!text(outputs, "nodes.jsonl").equalsIgnoreCase(nodesHash)
                || !text(outputs, "edges.jsonl").equalsIgnoreCase(edgesHash)) {
            throw new IOException("product output hash mismatch");
        }

        JsonNode inputs = manifest.path("inputs");
        String expectedArchiveHash = text(inputs, "archive_sha256");
        if (!validDigest(expectedArchiveHash) || !expectedArchiveHash.equalsIgnoreCase(sha256(archive))) {
            throw new IOException("source archive hash mismatch");
        }
        JsonNode memberHashes = inputs.path("archive_members_sha256");
        String foodMemberHash = text(memberHashes, "Food.json");
        String compoundMemberHash = text(memberHashes, "Compound.json");
        if (!validDigest(foodMemberHash) || !validDigest(compoundMemberHash)) {
            throw new IOException("source label member pins missing");
        }

        Map<String, FoodNode> foods = new TreeMap<>();
        Map<String, CompoundNode> compounds = new TreeMap<>();
        parseNodes(nodesBytes, foods, compounds);
        if (foods.isEmpty() || compounds.isEmpty()) throw new IOException("empty product graph");

        Map<String, List<Edge>> edgesByFood = parseEdges(edgesBytes, foods, compounds);
        if (edgesByFood.isEmpty()) throw new IOException("empty product relation set");

        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Map<Long, SourceName> foodNames = readSourceNames(
                    zip, FOOD_MEMBER, foodMemberHash, foods.keySet(), true);
            Map<Long, SourceName> compoundNames = readSourceNames(
                    zip, COMPOUND_MEMBER, compoundMemberHash, compounds.keySet(), false);
            foods.replaceAll((id, node) -> {
                SourceName source = foodNames.get(parseSourceId(id, FOOD_PREFIX));
                if (source == null || source.exportToFoodb() == null
                        || source.exportToFoodb() != node.exportToFoodb()) {
                    throw new ProductGraphUnavailableException();
                }
                return new FoodNode(node.id(), source.name(), node.exportToFoodb());
            });
            compounds.replaceAll((id, node) -> {
                SourceName source = compoundNames.get(parseSourceId(id, COMPOUND_PREFIX));
                if (source == null) throw new ProductGraphUnavailableException();
                return new CompoundNode(node.id(), source.name());
            });
        }

        long expectedEdges = manifest.path("counts").path("unique_native_food_compound_pairs").asLong(-1);
        long observedEdges = edgesByFood.values().stream().mapToLong(List::size).sum();
        if (expectedEdges <= 0 || expectedEdges != observedEdges) {
            throw new IOException("product manifest count mismatch");
        }
        Provenance provenance = new Provenance(
                manifestHash, nodesHash, edgesHash, "FooDB 1.0", "2020-04-07",
                "CC BY-NC 4.0", "Top-255 profile scope; export-eligible edges only",
                "Missing relations are unknown; presence is not quantity or substitution evidence.");
        return new LoadedGraph(Map.copyOf(foods), Map.copyOf(compounds),
                immutableEdges(edgesByFood), provenance);
    }

    private static void parseNodes(byte[] bytes, Map<String, FoodNode> foods,
                                   Map<String, CompoundNode> compounds) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) throw new IOException("blank node record");
                JsonNode node = JSON.readTree(line);
                String id = text(node, "node_id");
                String type = text(node, "node_type");
                if ("Food".equals(type)) {
                    if (!hasSourceId(id, FOOD_PREFIX) || !node.path("export_to_foodb").isBoolean())
                        throw new IOException("invalid Food node");
                    FoodNode value = new FoodNode(id, "", node.path("export_to_foodb").asBoolean());
                    if (foods.putIfAbsent(id, value) != null) throw new IOException("duplicate Food node");
                } else if ("Compound".equals(type)) {
                    if (!hasSourceId(id, COMPOUND_PREFIX)) throw new IOException("invalid Compound node");
                    if (compounds.putIfAbsent(id, new CompoundNode(id, "")) != null)
                        throw new IOException("duplicate Compound node");
                } else {
                    throw new IOException("unknown product node type");
                }
            }
        }
    }

    private static Map<String, List<Edge>> parseEdges(byte[] bytes, Map<String, FoodNode> foods,
                                                       Map<String, CompoundNode> compounds) throws IOException {
        Map<String, List<Edge>> result = new HashMap<>();
        TreeSet<String> unique = new TreeSet<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) throw new IOException("blank edge record");
                JsonNode row = JSON.readTree(line);
                String source = text(row, "source");
                String target = text(row, "target");
                long support = positiveLong(row, "content_row_support");
                long exportSupport = nonNegativeLong(row, "export_eligible_support");
                String state = text(row, "export_filter_state");
                if (!hasSourceId(source, FOOD_PREFIX) || !hasSourceId(target, COMPOUND_PREFIX)
                        || !foods.containsKey(source) || !compounds.containsKey(target)
                        || !"has_compound".equals(text(row, "predicate"))
                        || !"PRESENT".equals(text(row, "presence"))
                        || state.isBlank() || exportSupport > support
                        || (exportSupport > 0 && !foods.get(source).exportToFoodb())) {
                    throw new IOException("invalid native presence relation");
                }
                String key = source + "\u0000" + target;
                if (!unique.add(key)) throw new IOException("duplicate product edge");
                result.computeIfAbsent(source, ignored -> new ArrayList<>())
                        .add(new Edge(source, target, support, exportSupport, state));
            }
        }
        result.values().forEach(values -> values.sort(Comparator.comparing(Edge::target)));
        return result;
    }

    private static Map<Long, SourceName> readSourceNames(ZipFile zip, String memberName, String expectedHash,
                                                          Iterable<String> selectedIds, boolean food)
            throws IOException, NoSuchAlgorithmException {
        ZipEntry entry = zip.getEntry(memberName);
        if (entry == null || entry.isDirectory()) throw new IOException("source member missing");
        Map<Long, Boolean> wanted = new HashMap<>();
        for (String id : selectedIds) wanted.put(parseSourceId(id, food ? FOOD_PREFIX : COMPOUND_PREFIX), true);
        Map<Long, SourceName> found = new HashMap<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream raw = zip.getInputStream(entry);
             DigestInputStream hashed = new DigestInputStream(raw, digest);
             BufferedReader reader = new BufferedReader(new InputStreamReader(hashed, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) throw new IOException("blank source row");
                JsonNode row = JSON.readTree(line);
                long id = positiveLong(row, "id");
                if (!wanted.containsKey(id)) continue;
                String name = text(row, "name").trim();
                if (name.isEmpty()) throw new IOException("selected source label missing");
                Boolean export = food
                        ? (row.path("export_to_foodb").isBoolean() ? row.path("export_to_foodb").asBoolean() : null)
                        : null;
                if (food && export == null) throw new IOException("selected Food export flag missing");
                if (found.putIfAbsent(id, new SourceName(name, export)) != null)
                    throw new IOException("duplicate selected source ID");
            }
        }
        String observedHash = HexFormat.of().formatHex(digest.digest());
        if (!expectedHash.equalsIgnoreCase(observedHash) || found.size() != wanted.size()) {
            throw new IOException("source member hash or selected inventory mismatch");
        }
        return found;
    }

    private static Map<String, List<Edge>> immutableEdges(Map<String, List<Edge>> edges) {
        Map<String, List<Edge>> copy = new TreeMap<>();
        edges.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        return Map.copyOf(copy);
    }

    private static Path requireSafeAbsolutePath(Path path, boolean directory) throws IOException {
        if (path == null || !path.isAbsolute()) throw new IOException("absolute path required");
        Path normalized = path.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(normalized)
                || (directory
                    ? !Files.isDirectory(normalized, LinkOption.NOFOLLOW_LINKS)
                    : !Files.isRegularFile(normalized, LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("invalid artifact path");
        }
        return normalized;
    }

    private static byte[] readBounded(Path path, int maxBytes) throws IOException {
        if (Files.isSymbolicLink(path) || !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("artifact file missing");
        long size = Files.size(path);
        if (size < 1 || size > maxBytes) throw new IOException("artifact file size outside bounds");
        return Files.readAllBytes(path);
    }

    private static String sha256(Path path) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024 * 1024];
            int count;
            while ((count = input.read(buffer)) >= 0) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private static boolean validDigest(String value) {
        return value != null && SHA256.matcher(value).matches();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && value.isTextual() ? value.textValue() : "";
    }

    private static long positiveLong(JsonNode node, String field) throws IOException {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.canConvertToLong() || value.longValue() <= 0)
            throw new IOException("invalid positive integer field");
        return value.longValue();
    }

    private static long nonNegativeLong(JsonNode node, String field) throws IOException {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || !value.canConvertToLong() || value.longValue() < 0)
            throw new IOException("invalid non-negative integer field");
        return value.longValue();
    }

    private static boolean hasSourceId(String value, String prefix) {
        return value != null && value.startsWith(prefix)
                && SOURCE_ID.matcher(value.substring(prefix.length())).matches();
    }

    private static long parseSourceId(String value, String prefix) {
        if (!hasSourceId(value, prefix)) throw new ProductGraphUnavailableException();
        try {
            return Long.parseLong(value.substring(prefix.length()));
        } catch (NumberFormatException invalid) {
            throw new ProductGraphUnavailableException();
        }
    }

    private static final class LoadedGraph {
        private final Map<String, FoodNode> foods;
        private final Map<String, CompoundNode> compounds;
        private final Map<String, List<Edge>> edgesByFood;
        private final Provenance provenance;

        private LoadedGraph(Map<String, FoodNode> foods, Map<String, CompoundNode> compounds,
                            Map<String, List<Edge>> edgesByFood, Provenance provenance) {
            this.foods = foods;
            this.compounds = compounds;
            this.edgesByFood = edgesByFood;
            this.provenance = provenance;
        }

        Map<String, FoodNode> foods() { return foods; }
        Map<String, CompoundNode> compounds() { return compounds; }
        Map<String, List<Edge>> edgesByFood() { return edgesByFood; }
        Provenance provenance() { return provenance; }
    }

    private record FoodNode(String id, String name, boolean exportToFoodb) {}
    private record CompoundNode(String id, String name) {}
    private record Edge(String source, String target, long contentRowSupport,
                        long exportEligibleSupport, String exportFilterState) {
        boolean exportEligible() { return exportEligibleSupport > 0; }
    }
    private record SourceName(String name, Boolean exportToFoodb) {}

    public record Provenance(String manifestSha256, String nodesSha256, String edgesSha256,
                             String source, String releaseDate, String license,
                             String scope, String interpretation) {}
    public record FoodCandidate(String foodId, String name) {}
    public record FoodSearchResponse(Provenance provenance, List<FoodCandidate> foods) {}
    public record Food(String id, String name, String type) {}
    public record CompoundRelation(String id, String name, String predicate, String presence,
                                   long contentRowSupport, long exportEligibleSupport) {}
    public record FoodCompositionResponse(String status, Provenance provenance, Food food,
                                          List<CompoundRelation> compounds,
                                          int filteredKnownRelationCount) {}

    public static final class ProductGraphUnavailableException extends RuntimeException {
        private ProductGraphUnavailableException() {
            super("product graph unavailable");
        }
    }
}
