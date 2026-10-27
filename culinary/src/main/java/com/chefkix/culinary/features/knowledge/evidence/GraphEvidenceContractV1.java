package com.chefkix.culinary.features.knowledge.evidence;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonValue;

/** Versioned, read-only contract kept separate from the legacy knowledge graph API. */
public final class GraphEvidenceContractV1 {

    public static final String VERSION = "graph-evidence.v1";
    public static final String SNAPSHOT_VERSION = "graph-evidence-snapshot.v1";
    public static final String TRAIN_PAIR_UNIT = "TRAIN_PAIRS";

    private GraphEvidenceContractV1() {}

    public record Request(String sourceIngredientId, String candidateIngredientId) {}

    public enum ResponseStatus { AVAILABLE, UNAVAILABLE, REFUSED }
    public enum UnavailableReason {
        FEATURE_DISABLED,
        PROVIDER_NOT_REGISTERED,
        ARTIFACT_MISSING,
        ARTIFACT_UNVERIFIED,
        IDENTITY_UNKNOWN,
        IDENTITY_AMBIGUOUS,
        PROVIDER_ERROR,
        TRAIN_PAIR_EVIDENCE_UNAVAILABLE,
        CALIBRATION_UNAVAILABLE,
        PROVIDER_RESULT_INVALID
    }
    public enum IdentityStatus { RESOLVED, UNKNOWN, AMBIGUOUS }
    public enum ArtifactIntegrityStatus { VERIFIED, MISSING, INVALID, UNVERIFIED }
    public enum AvailabilityStatus { AVAILABLE, UNAVAILABLE, NOT_IN_ARTIFACT }
    public enum ScreeningStatus { NOT_EVALUATED, EVIDENCE_PRESENT, EVIDENCE_ABSENT, UNAVAILABLE }
    public enum CalibrationStatus {
        OK("ok"),
        UNINFORMATIVE("uninformative"),
        VACUOUS("vacuous"),
        NO_GUARANTEE("no_guarantee"),
        ABSTAIN("abstain"),
        UNAVAILABLE("unavailable"),
        NOT_APPLICABLE("not_applicable");

        private final String wireValue;

        CalibrationStatus(String wireValue) {
            this.wireValue = wireValue;
        }

        @JsonValue
        public String wireValue() {
            return wireValue;
        }
    }
    public enum Decision { SHOW, ABSTAIN, UNAVAILABLE }

    public record IdentityResolution(
            IdentityStatus status,
            String requestedSourceIngredientId,
            String sourceIngredientId,
            List<String> sourceCandidates,
            String requestedCandidateIngredientId,
            String candidateIngredientId,
            List<String> candidateCandidates) {
        public IdentityResolution {
            sourceCandidates = sourceCandidates == null ? List.of() : List.copyOf(sourceCandidates);
            candidateCandidates = candidateCandidates == null ? List.of() : List.copyOf(candidateCandidates);
        }
    }

    /** Null pair/count values mean unavailable; count is a train-only record count, not confidence. */
    public record TrainPairEvidence(Boolean seenInTrain, Long trainCount, String unit) {
        public TrainPairEvidence {
            if (trainCount != null && trainCount < 0) {
                throw new IllegalArgumentException("trainCount must be nonnegative");
            }
            // The count is authoritative; never let a provider contradict the frozen invariant.
            seenInTrain = trainCount == null ? null : trainCount > 0;
        }
    }

    /** Provenance is per source; UNKNOWN is a valid state and must not be upgraded by the API. */
    public record Provenance(
            String sourceId,
            String licenseStatus,
            String split,
            String evidenceTier,
            String artifactSha256,
            String manifestSha256) {}

    public record Availability(AvailabilityStatus status, Long recordCount) {}

    public record DeclaredGroupScreening(ScreeningStatus status, String caveat) {}

    /** No confidence percentage is part of this contract. */
    public record Calibration(String armId, Double alpha, CalibrationStatus status, Double cutoff) {}

    public record DecisionResult(Decision decision, String reason) {}

    public record Evidence(
            IdentityResolution identityResolution,
            TrainPairEvidence trainPairEvidence,
            List<Provenance> provenance,
            Availability compoundAvailability,
            Availability nutrientAvailability,
            DeclaredGroupScreening declaredGroupScreening,
            Calibration calibration,
            DecisionResult decision) {
        public Evidence {
            provenance = provenance == null ? List.of() : List.copyOf(provenance);
        }
    }

    /** Hashes must be SHA-256 hex and equal; the provider computes observedSha256 from artifact bytes. */
    public record ArtifactIntegrity(
            ArtifactIntegrityStatus status,
            String expectedSha256,
            String observedSha256) {
        public boolean isVerified() {
            return status == ArtifactIntegrityStatus.VERIFIED
                    && isSha256(expectedSha256)
                    && isSha256(observedSha256)
                    && expectedSha256.equalsIgnoreCase(observedSha256);
        }

        private static boolean isSha256(String value) {
            return value != null && value.matches("(?i)[0-9a-f]{64}");
        }
    }

    public record ProviderResult(ArtifactIntegrity artifactIntegrity, Evidence evidence) {}

    public record SnapshotRequest(String centerIngredientId, int depth, int limit) {}

    public enum SnapshotStatus { AVAILABLE, UNAVAILABLE, REFUSED }

    public enum SnapshotUnavailableReason {
        FEATURE_DISABLED, PROVIDER_NOT_REGISTERED, ARTIFACT_MISSING, ARTIFACT_UNVERIFIED,
        CENTER_IDENTITY_UNKNOWN, INVALID_QUERY, PROVIDER_ERROR, PROVIDER_RESULT_INVALID
    }

    /** No C3 artifact is implied by the graph bundle; this status is deliberately explicit. */
    public enum C3CellStatus { NOT_SEPARATELY_PINNED }

    public record SnapshotCenter(IdentityStatus status, String requestedId, String canonicalId) {}

    /** Aggregate graph view; source-row and per-record evidence identifiers are not serialized. */
    public record SnapshotNode(
            String canonicalId, String displayName, String identityStatus, String nodeType, List<String> sourceRefs) {
        public SnapshotNode {
            sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        }
    }

    public record SnapshotEdge(
            String sourceId, String targetId, String relation, boolean seen,
            long trainPairCount, long distinctContextCount) {}

    public record SnapshotStats(
            long totalNodes, long totalEdges, int returnedNodes, int returnedEdges,
            int depth, int limit, boolean hasMore) {}

    /** Distinguishes the verified graph-bundle manifest from its upstream source-manifest reference. */
    public record SnapshotProvenance(
            String sourceId, String licenseStatus, String split, String evidenceTier,
            String manifestSha256, String sourceManifestSha256) {}

    public record GraphSnapshot(
            String acceptanceState, List<SnapshotNode> nodes, List<SnapshotEdge> edges,
            SnapshotStats stats, SnapshotProvenance provenance,
            C3CellStatus c3CellStatus, String c3CellCaveat) {
        public GraphSnapshot {
            nodes = nodes == null ? List.of() : List.copyOf(nodes);
            edges = edges == null ? List.of() : List.copyOf(edges);
        }
    }

    public record SnapshotResult(ArtifactIntegrity artifactIntegrity, SnapshotCenter center, GraphSnapshot snapshot) {}

    public record SnapshotResponse(
            String contractVersion, SnapshotStatus status, SnapshotUnavailableReason unavailableReason,
            ArtifactIntegrity artifactIntegrity, SnapshotCenter center, GraphSnapshot snapshot) {
        public static SnapshotResponse unavailable(SnapshotUnavailableReason reason) {
            return new SnapshotResponse(SNAPSHOT_VERSION, SnapshotStatus.UNAVAILABLE, reason, null, null, null);
        }

        public static SnapshotResponse unavailable(
                SnapshotUnavailableReason reason, ArtifactIntegrity integrity, SnapshotCenter center) {
            return new SnapshotResponse(SNAPSHOT_VERSION, SnapshotStatus.UNAVAILABLE,
                    reason, integrity, center, null);
        }

        public static SnapshotResponse refused(ArtifactIntegrity integrity) {
            return new SnapshotResponse(SNAPSHOT_VERSION, SnapshotStatus.REFUSED,
                    SnapshotUnavailableReason.ARTIFACT_UNVERIFIED, integrity, null, null);
        }

        public static SnapshotResponse available(SnapshotResult result) {
            return new SnapshotResponse(SNAPSHOT_VERSION, SnapshotStatus.AVAILABLE, null,
                    result.artifactIntegrity(), result.center(), result.snapshot());
        }
    }

    public record Response(
            String contractVersion,
            ResponseStatus status,
            UnavailableReason unavailableReason,
            ArtifactIntegrity artifactIntegrity,
            IdentityResolution identityResolution,
            Evidence evidence) {
        public static Response unavailable(UnavailableReason reason) {
            return unavailable(reason, null);
        }

        public static Response unavailable(UnavailableReason reason, IdentityResolution identityResolution) {
            return new Response(VERSION, ResponseStatus.UNAVAILABLE, reason, null, identityResolution, null);
        }

        public static Response refused(ArtifactIntegrity integrity) {
            return new Response(VERSION, ResponseStatus.REFUSED,
                    UnavailableReason.ARTIFACT_UNVERIFIED, integrity, null, null);
        }

        public static Response available(ProviderResult result) {
            return new Response(VERSION, ResponseStatus.AVAILABLE, null,
                    result.artifactIntegrity(), result.evidence().identityResolution(), result.evidence());
        }
    }
}
