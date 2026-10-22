package com.chefkix.culinary.features.knowledge.evidence;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonValue;

/** Versioned, read-only contract kept separate from the legacy knowledge graph API. */
public final class GraphEvidenceContractV1 {

    public static final String VERSION = "graph-evidence.v1";
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
