package com.chefkix.culinary.features.knowledge.evidence;

import static com.chefkix.culinary.features.knowledge.evidence.GraphEvidenceContractV1.*;

/** Port for a future hash-verified artifact adapter. No seed-graph adapter is permitted. */
public interface GraphEvidenceProvider {
    ProviderResult findEvidence(Request request);

    /** Optional additive snapshot path; pair-only providers stay source and behavior compatible. */
    default SnapshotResult getSnapshot(SnapshotRequest request) {
        return null;
    }
}
