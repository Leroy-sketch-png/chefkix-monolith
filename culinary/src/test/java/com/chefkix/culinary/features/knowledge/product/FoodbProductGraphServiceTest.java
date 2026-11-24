package com.chefkix.culinary.features.knowledge.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Assumptions;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FoodbProductGraphServiceTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void serviceIsConstructibleBySpringWithItsRuntimeProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(ServiceContext.class)
                .run(context -> assertTrue(context.getStartupFailure() == null));
    }

    @Test
    void servesTypedSourcePresenceAndArchiveLabelsOnlyAfterAllPinsMatch() throws Exception {
        Fixture fixture = fixture(tempDir.resolve("valid"));
        FoodbProductGraphService service = service(fixture);

        var search = service.searchFoods("sample", 10);
        assertEquals(1, search.foods().size());
        assertEquals("1", search.foods().getFirst().foodId());
        assertEquals("Sample Food", search.foods().getFirst().name());
        assertEquals(fixture.manifestSha256(), search.provenance().manifestSha256());

        var composition = service.getFoodComposition("1");
        assertEquals("AVAILABLE", composition.status());
        assertEquals("foodb:1.0:food-id:1", composition.food().id());
        assertEquals("Food", composition.food().type());
        assertEquals(1, composition.compounds().size());
        assertEquals("foodb:1.0:compound-id:10", composition.compounds().getFirst().id());
        assertEquals("Sample Compound", composition.compounds().getFirst().name());
        assertEquals("has_compound", composition.compounds().getFirst().predicate());
        assertEquals("PRESENT", composition.compounds().getFirst().presence());
        assertEquals("CC BY-NC 4.0", composition.provenance().license());
    }

    @Test
    void refusesWrongManifestPinAndChangedOutputBytes() throws Exception {
        Fixture fixture = fixture(tempDir.resolve("tampered"));
        Files.writeString(fixture.bundle().resolve("edges.jsonl"), "{}\n", StandardCharsets.UTF_8);

        var wrongPin = new FoodbProductGraphProperties();
        wrongPin.setEnabled(true);
        wrongPin.setBundlePath(fixture.bundle());
        wrongPin.setSourceArchivePath(fixture.archive());
        wrongPin.setExpectedManifestSha256("0".repeat(64));
        assertThrows(FoodbProductGraphService.ProductGraphUnavailableException.class,
                () -> new FoodbProductGraphService(wrongPin, "0".repeat(64)).searchFoods("sample", 1));

        assertThrows(FoodbProductGraphService.ProductGraphUnavailableException.class,
                () -> service(fixture).searchFoods("sample", 1));
    }

    @Test
    void keepsUnknownFoodIdentityExplicitInsteadOfReportingAbsence() throws Exception {
        Fixture fixture = fixture(tempDir.resolve("unknown"));
        var result = service(fixture).getFoodComposition("999");
        assertEquals("UNKNOWN_OR_OUT_OF_SCOPE", result.status());
        assertEquals(null, result.food());
        assertTrue(result.compounds().isEmpty());
    }

    @Test
    void realPinnedFoodbBundleReturnsOneNonEmptyTypedSourceQueryWhenOptedIn() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("chefkix.productGraph.realArtifactTest"));
        Path workspace = findWorkspaceRoot();
        Path bundle = workspace.resolve("evidence/member-foodb-native-output-20261111");
        Path archive = workspace.resolve("evidence/member-foodb-source-20261114/foodb_2020_04_07_json.zip");
        var properties = new FoodbProductGraphProperties();
        properties.setEnabled(true);
        properties.setBundlePath(bundle);
        properties.setSourceArchivePath(archive);
        properties.setExpectedManifestSha256(FoodbProductGraphService.EXPECTED_MANIFEST_SHA256);
        var service = new FoodbProductGraphService(properties);

        var candidates = service.searchFoods("a", 20).foods();
        assertTrue(!candidates.isEmpty());
        var response = service.getFoodComposition(candidates.getFirst().foodId());
        assertEquals("AVAILABLE", response.status());
        assertTrue(!response.compounds().isEmpty());
        assertTrue(response.provenance().manifestSha256()
                .equals(FoodbProductGraphService.EXPECTED_MANIFEST_SHA256));
    }

    private FoodbProductGraphService service(Fixture fixture) {
        var properties = new FoodbProductGraphProperties();
        properties.setEnabled(true);
        properties.setBundlePath(fixture.bundle());
        properties.setSourceArchivePath(fixture.archive());
        properties.setExpectedManifestSha256(fixture.manifestSha256());
        return new FoodbProductGraphService(properties, fixture.manifestSha256());
    }

    private Fixture fixture(Path root) throws Exception {
        Path bundle = Files.createDirectories(root.resolve("bundle"));
        Path archive = root.resolve("foodb.zip");
        byte[] nodes = ("{\"node_id\":\"foodb:1.0:food-id:1\",\"node_type\":\"Food\",\"export_to_foodb\":true}\n"
                + "{\"node_id\":\"foodb:1.0:compound-id:10\",\"node_type\":\"Compound\"}\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] edges = ("{\"source\":\"foodb:1.0:food-id:1\",\"target\":\"foodb:1.0:compound-id:10\","
                + "\"content_row_support\":2,\"export_eligible_support\":1,"
                + "\"export_filter_state\":\"EXPORT_ELIGIBLE\",\"predicate\":\"has_compound\","
                + "\"presence\":\"PRESENT\"}\n").getBytes(StandardCharsets.UTF_8);
        byte[] foods = ("{\"id\":1,\"name\":\"Sample Food\",\"export_to_foodb\":true}\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] compounds = ("{\"id\":10,\"name\":\"Sample Compound\"}\n")
                .getBytes(StandardCharsets.UTF_8);
        Files.write(bundle.resolve("nodes.jsonl"), nodes);
        Files.write(bundle.resolve("edges.jsonl"), edges);
        writeZip(archive, Map.of(
                "foodb_2020_04_07_json/Food.json", foods,
                "foodb_2020_04_07_json/Compound.json", compounds));

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("destination_graph", "graph_product_v4");
        manifest.put("schema", "chefkix.graph_product_v4.foodb_native_snapshot.v1");
        manifest.put("release", "FooDB 1.0 JSON 2020-04-07");
        manifest.put("semantics", "presence only; no concentration, equivalence, flavor, nutrient, allergen, or safety inference");
        manifest.put("absence_semantics", "not represented; missing relations remain UNKNOWN");
        manifest.put("counts", Map.of("unique_native_food_compound_pairs", 1));
        manifest.put("outputs", Map.of("nodes.jsonl", sha256(nodes), "edges.jsonl", sha256(edges)));
        manifest.put("inputs", Map.of(
                "archive_sha256", sha256(Files.readAllBytes(archive)),
                "archive_members_sha256", Map.of("Food.json", sha256(foods), "Compound.json", sha256(compounds))));
        byte[] manifestBytes = JSON.writeValueAsBytes(manifest);
        Files.write(bundle.resolve("manifest.json"), manifestBytes);
        return new Fixture(bundle, archive, sha256(manifestBytes));
    }

    private Path findWorkspaceRoot() {
        Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        while (current != null) {
            if (Files.isDirectory(current.resolve("evidence/member-foodb-native-output-20261111")))
                return current;
            current = current.getParent();
        }
        throw new IllegalStateException("shared FooDB package is not present in this checkout");
    }

    private void writeZip(Path archive, Map<String, byte[]> entries) throws Exception {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (var entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }

    private record Fixture(Path bundle, Path archive, String manifestSha256) {}

    @Configuration(proxyBeanMethods = false)
    @Import({FoodbProductGraphConfiguration.class, FoodbProductGraphService.class})
    static class ServiceContext {}
}
