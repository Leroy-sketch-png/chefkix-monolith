#!/usr/bin/env python3
"""Reproduce Claim R09: Global flavor test not above null.

Verifies the canonical flavor pairing test report:
  chefkix-fe/public/data/flavor_pairing_report.json
confirming its SHA-256 digest, 2,231,142 corpus rows, 1,905,776 eligible recipes,
372 FooDB profiles, observed mean Jaccard (0.545990), matched null mean (0.527733),
one-sided permutation p-value (0.238095 > alpha 0.05), and unsupported primary hypothesis.
"""

from __future__ import annotations

import hashlib
import json
import math
import sys
from pathlib import Path


EXPECTED_REPORT_SHA256 = "d8184a5e9b2b82131b698f2308f5ac256837ba54e64a2423410c129263095b04"
EXPECTED_CORPUS_SHA256 = "f376ee0bb9f99f323761abf6270d453f4eff2002c3625d04cabcf8b57dca0803"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def reproduce_r09(repo_root: Path) -> dict:
    report_path = repo_root / "chefkix-fe" / "public" / "data" / "flavor_pairing_report.json"
    if not report_path.is_file():
        raise FileNotFoundError(f"Missing report file: {report_path}")

    actual_hash = sha256_file(report_path)
    if actual_hash != EXPECTED_REPORT_SHA256:
        raise ValueError(f"Hash mismatch: expected {EXPECTED_REPORT_SHA256}, got {actual_hash}")

    data = json.loads(report_path.read_text(encoding="utf-8"))

    if data.get("schema_version") != 2:
        raise ValueError(f"Unexpected schema_version: {data.get('schema_version')}")
    if data.get("state") != "VERIFIED_E2_NEGATIVE_RIGHTS_LIMITED":
        raise ValueError(f"Unexpected state: {data.get('state')}")

    corpus = data.get("corpus", {})
    if corpus.get("rows") != 2231142:
        raise ValueError(f"Expected 2,231,142 corpus rows, got {corpus.get('rows')}")
    if corpus.get("sha256") != EXPECTED_CORPUS_SHA256:
        raise ValueError(f"Corpus sha256 mismatch: {corpus.get('sha256')}")

    coverage = data.get("coverage", {})
    if coverage.get("eligible_recipes") != 1905776:
        raise ValueError(f"Expected 1,905,776 eligible recipes, got {coverage.get('eligible_recipes')}")
    if coverage.get("distinct_fooDB_profiles") != 372:
        raise ValueError(f"Expected 372 FooDB profiles, got {coverage.get('distinct_fooDB_profiles')}")

    result_data = data.get("result", {})
    obs_jaccard = result_data.get("observed_unweighted_recipe_mean_jaccard")
    null_jaccard = result_data.get("matched_null_mean_jaccard")
    p_val = result_data.get("one_sided_permutation_p")
    alpha = result_data.get("alpha", 0.05)
    hyp_supported = result_data.get("primary_hypothesis_supported")

    if not math.isclose(obs_jaccard, 0.5459900765725983, rel_tol=1e-6):
        raise ValueError(f"Observed Jaccard mismatch: {obs_jaccard}")
    if not math.isclose(null_jaccard, 0.5277329267016719, rel_tol=1e-6):
        raise ValueError(f"Matched null Jaccard mismatch: {null_jaccard}")
    if not math.isclose(p_val, 0.23809523809523808, rel_tol=1e-6):
        raise ValueError(f"Permutation p-value mismatch: {p_val}")
    if hyp_supported is not False:
        raise ValueError(f"Expected primary_hypothesis_supported to be False, got {hyp_supported}")
    if p_val <= alpha:
        raise ValueError(f"Permutation p-value ({p_val}) unexpectedly statistically significant (<= {alpha})")

    result = {
        "schema_version": 1,
        "record_id": "r09-global-flavor-test-reproduction-20261102",
        "claim_id": "R09",
        "claim_text": "Global flavor test not above null",
        "report_file": "chefkix-fe/public/data/flavor_pairing_report.json",
        "report_sha256": actual_hash,
        "corpus_rows": corpus.get("rows"),
        "eligible_recipes": coverage.get("eligible_recipes"),
        "distinct_profiles": coverage.get("distinct_fooDB_profiles"),
        "observed_mean_jaccard": obs_jaccard,
        "matched_null_mean_jaccard": null_jaccard,
        "one_sided_permutation_p": p_val,
        "alpha": alpha,
        "primary_hypothesis_supported": hyp_supported,
        "status": "PASS",
        "reproduced_value": (
            f"2,231,142 corpus rows; 1,905,776 eligible recipes (85.417%); "
            f"372 FooDB profiles; observed Jaccard {obs_jaccard:.6f} vs null {null_jaccard:.6f}; "
            f"p={p_val:.6f} > alpha {alpha}; primary_hypothesis_supported: false."
        ),
    }

    return result


def main():
    repo_root = Path(__file__).resolve().parent.parent.parent
    try:
        report = reproduce_r09(repo_root)
        print(json.dumps(report, indent=2))

        # Write reproduction output json to evidence directory
        out_dir = repo_root / "ai" / "evidence" / "claims-register" / "instantiated-r01-r45-v1"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_file = out_dir / "r09-global-flavor-test-reproduction-20261102.json"
        out_file.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(f"Reproduction output written to: {out_file}")
        sys.exit(0)
    except Exception as e:
        print(f"Reproduction error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
