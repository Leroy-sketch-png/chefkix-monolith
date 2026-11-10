"""C1 Adjudication Integrator and Estimand Pipeline.

Joins the private L36 adjudicator return back with the 717 agreed candidate
judgments via the sealed opaque identity key, produces the unified 867-judgment
matrix, classifies all 150 cases under iron-chef-l36-case-rule-v1, and computes
the partial-identification point bounds and bootstrap intervals on the unseen strata.
"""
from __future__ import annotations

import argparse
import dataclasses
import hashlib
import json
from pathlib import Path
from typing import Any, Mapping, Sequence

import sys
_monolith_root = Path(__file__).resolve().parents[3]
if str(_monolith_root) not in sys.path:
    sys.path.insert(0, str(_monolith_root))
_l36_dir = Path(__file__).resolve().parents[1] / "l36"
if str(_l36_dir) not in sys.path:
    sys.path.insert(0, str(_l36_dir))

try:
    from audit_estimand_evaluator import (
        ALLOWED_LABELS,
        BOOTSTRAP_DRAWS,
        AuditCase,
        CaseValue,
        CaseEvaluationResult,
        L36AuditReport,
        PartialIdentificationBounds,
        PROTOCOL_SHA256,
        SEED,
        SPECIFICATION_ID,
        TOTAL_SAMPLE,
        UNSEEN_SAMPLE,
        compute_partial_bounds,
        evaluate_audit_estimand,
        evaluate_case_rule,
        FROZEN_FRAME_SIZES,
        FROZEN_SAMPLE_SIZES,
    )
except ModuleNotFoundError:
    from tools.research.l36.audit_estimand_evaluator import (
        ALLOWED_LABELS,
        BOOTSTRAP_DRAWS,
        AuditCase,
        CaseValue,
        CaseEvaluationResult,
        L36AuditReport,
        PartialIdentificationBounds,
        PROTOCOL_SHA256,
        SEED,
        SPECIFICATION_ID,
        TOTAL_SAMPLE,
        UNSEEN_SAMPLE,
        compute_partial_bounds,
        evaluate_audit_estimand,
        evaluate_case_rule,
        FROZEN_FRAME_SIZES,
        FROZEN_SAMPLE_SIZES,
    )

TOTAL_CANDIDATE_JUDGMENTS = 867
DISAGREEMENT_COUNT = 150
AGREED_COUNT = 717


@dataclasses.dataclass(frozen=True)
class IntegratedCandidateJudgment:
    case_id: str
    identity_id: str
    final_label: str
    source_type: str  # "AGREED" or "ADJUDICATED"
    rationale: str = ""


@dataclasses.dataclass(frozen=True)
class C1AdjudicationIntegrationResult:
    total_cases_evaluated: int
    total_candidate_judgments: int
    agreed_judgments_count: int
    adjudicated_judgments_count: int
    unseen_cases_count: int
    seen_cases_count: int
    case_outcomes: Mapping[str, CaseEvaluationResult]
    point_bounds: PartialIdentificationBounds
    p_ac_sensitivity: float
    v1_count_total: int
    v0_count_total: int
    vu_count_total: int
    receipt_format: str
    is_denominator_preserved: bool

    def to_receipt_dict(self) -> dict[str, Any]:
        payload = {
            "format": self.receipt_format,
            "specification_id": SPECIFICATION_ID,
            "protocol_sha256": PROTOCOL_SHA256,
            "total_cases_evaluated": self.total_cases_evaluated,
            "total_candidate_judgments": self.total_candidate_judgments,
            "agreed_judgments_count": self.agreed_judgments_count,
            "adjudicated_judgments_count": self.adjudicated_judgments_count,
            "unseen_cases_count": self.unseen_cases_count,
            "seen_cases_count": self.seen_cases_count,
            "v1_count_total": self.v1_count_total,
            "v0_count_total": self.v0_count_total,
            "vu_count_total": self.vu_count_total,
            "is_denominator_preserved": self.is_denominator_preserved,
            "partial_identification_bounds": {
                "p_lower": round(self.point_bounds.p_lower, 6),
                "p_upper": round(self.point_bounds.p_upper, 6),
                "weighted_unknown_mass": round(self.point_bounds.weighted_unknown_mass, 6),
                "total_unseen_cases": self.point_bounds.total_unseen_cases,
                "n_unknown_cases": self.point_bounds.n_unknown_cases,
                "total_weighted_denominator": round(self.point_bounds.total_weighted_denominator, 2),
                "stratum_sample_counts": dict(self.point_bounds.stratum_sample_counts),
            },
            "p_ac_sensitivity": round(self.p_ac_sensitivity, 6),
        }
        canonical_bytes = json.dumps(payload, sort_keys=True, separators=(",", ":")).encode("utf-8")
        receipt_sha256 = hashlib.sha256(canonical_bytes).hexdigest()
        payload["receipt_sha256"] = receipt_sha256
        return payload


def evaluate_audit_case(case: AuditCase) -> CaseEvaluationResult:
    """Evaluates a single AuditCase under iron-chef-l36-case-rule-v1."""
    v, ok, err = evaluate_case_rule(case.candidate_labels, case.is_operational_failure)
    l_i = 1 if v == CaseValue.SUCCESS else 0
    u_i = 1 if v in {CaseValue.SUCCESS, CaseValue.UNKNOWN} else 0
    is_unk = (v == CaseValue.UNKNOWN)
    return CaseEvaluationResult(
        case_id=case.case_id,
        stratum=case.stratum,
        recipe_cluster_id=case.recipe_cluster_id,
        case_value=v,
        l_i=l_i,
        u_i=u_i,
        is_unknown=is_unk,
        integrity_passed=ok,
        failure_reason=err,
    )


def extract_agreed_map_from_returns(
    key_dict: dict[str, Any],
    r1_return_dict: dict[str, Any],
    r2_return_dict: dict[str, Any],
) -> dict[tuple[str, str], str]:
    """Extracts agreed (case_id, identity_id) -> label mappings where R1 and R2 agree.
    
    Verifies that total pairs across key is 867, exactly 717 agree, and 150 disagree.
    """
    if key_dict.get("format") != "c1-private-key-v1":
        raise ValueError(f"Invalid private key format: {key_dict.get('format')}")
    if r1_return_dict.get("format") != "c1-reviewer-return-v1" or r1_return_dict.get("reviewer") != "R1":
        raise ValueError("Invalid R1 return format or reviewer identifier")
    if r2_return_dict.get("format") != "c1-reviewer-return-v1" or r2_return_dict.get("reviewer") != "R2":
        raise ValueError("Invalid R2 return format or reviewer identifier")

    key_entries = key_dict.get("entries", [])
    entry_lookup: dict[tuple[str, str, str], tuple[str, str]] = {}
    distinct_pairs: set[tuple[str, str]] = set()
    for e in key_entries:
        rev = e["reviewer"]
        vid = e["view_id"]
        bcand = e["blind_candidate"]
        cid = e["case_id"]
        iid = e["identity_id"]
        entry_lookup[(rev, vid, bcand)] = (cid, iid)
        distinct_pairs.add((cid, iid))

    if len(distinct_pairs) != TOTAL_CANDIDATE_JUDGMENTS:
        raise ValueError(f"Expected {TOTAL_CANDIDATE_JUDGMENTS} distinct candidate pairs, got {len(distinct_pairs)}")

    r1_judgments = {
        entry_lookup[("R1", j["view_id"], j["blind_candidate"])]: j["label"]
        for j in r1_return_dict.get("judgments", [])
        if ("R1", j["view_id"], j["blind_candidate"]) in entry_lookup
    }
    r2_judgments = {
        entry_lookup[("R2", j["view_id"], j["blind_candidate"])]: j["label"]
        for j in r2_return_dict.get("judgments", [])
        if ("R2", j["view_id"], j["blind_candidate"]) in entry_lookup
    }

    if len(r1_judgments) != TOTAL_CANDIDATE_JUDGMENTS or len(r2_judgments) != TOTAL_CANDIDATE_JUDGMENTS:
        raise ValueError(
            f"Expected {TOTAL_CANDIDATE_JUDGMENTS} judgments from each reviewer, "
            f"got R1={len(r1_judgments)}, R2={len(r2_judgments)}"
        )

    agreed_map: dict[tuple[str, str], str] = {}
    disagreements = 0

    for pair in distinct_pairs:
        l1 = r1_judgments.get(pair)
        l2 = r2_judgments.get(pair)
        if l1 is None or l2 is None:
            raise ValueError(f"Missing judgment for pair {pair}")
        if l1 not in ALLOWED_LABELS or l2 not in ALLOWED_LABELS:
            raise ValueError(f"Invalid label for pair {pair}: R1={l1}, R2={l2}")
        if l1 == l2:
            agreed_map[pair] = l1
        else:
            disagreements += 1

    if len(agreed_map) != AGREED_COUNT:
        raise ValueError(f"Expected {AGREED_COUNT} agreed items, got {len(agreed_map)} (disagreements={disagreements})")
    if disagreements != DISAGREEMENT_COUNT:
        raise ValueError(f"Expected {DISAGREEMENT_COUNT} disagreements, got {disagreements}")

    return agreed_map


def integrate_judgments(
    sealed_key: dict[str, Any],
    adjudication_return: dict[str, Any],
    agreed_map: Mapping[tuple[str, str], str],  # (case_id, identity_id) -> label
) -> dict[tuple[str, str], IntegratedCandidateJudgment]:
    """Integrate agreed labels and adjudicated labels into unified matrix."""
    if sealed_key.get("format") != "c1-l36-adjudication-key-v1":
        raise ValueError(f"Invalid sealed key format: {sealed_key.get('format')}")
    if adjudication_return.get("format") != "c1-l36-adjudication-return-v1":
        raise ValueError(f"Invalid adjudication return format: {adjudication_return.get('format')}")

    if len(agreed_map) != AGREED_COUNT:
        raise ValueError(f"Expected exactly {AGREED_COUNT} agreed items, got {len(agreed_map)}")

    decisions = {d["item_id"]: d for d in adjudication_return.get("decisions", [])}
    key_items = {item["item_id"]: item for item in sealed_key.get("items", [])}

    if len(key_items) != DISAGREEMENT_COUNT:
        raise ValueError(f"Sealed key must contain exactly {DISAGREEMENT_COUNT} items, got {len(key_items)}")

    integrated: dict[tuple[str, str], IntegratedCandidateJudgment] = {}

    # 1. Add agreed judgments
    for (case_id, identity_id), label in agreed_map.items():
        if label not in ALLOWED_LABELS:
            raise ValueError(f"Invalid agreed label: {label}")
        integrated[(case_id, identity_id)] = IntegratedCandidateJudgment(
            case_id=case_id,
            identity_id=identity_id,
            final_label=label,
            source_type="AGREED",
        )

    # 2. Add adjudicated judgments
    for item_id, item in key_items.items():
        case_id = item["case_id"]
        identity_id = item["identity_id"]
        dec = decisions.get(item_id)
        if dec is None:
            # Unanswered defaults to U (unknown) under L36 rule
            label = "U"
            rationale = "Unanswered adjudication defaulted to U"
        else:
            label = dec.get("final_label", "U")
            rationale = dec.get("rationale", "")

        if label not in ALLOWED_LABELS:
            raise ValueError(f"Invalid adjudicated label: {label}")

        pair_key = (case_id, identity_id)
        if pair_key in integrated:
            raise ValueError(f"Collision: item {pair_key} already in agreed map")

        integrated[pair_key] = IntegratedCandidateJudgment(
            case_id=case_id,
            identity_id=identity_id,
            final_label=label,
            source_type="ADJUDICATED",
            rationale=rationale,
        )

    if len(integrated) != TOTAL_CANDIDATE_JUDGMENTS:
        raise ValueError(
            f"Expected {TOTAL_CANDIDATE_JUDGMENTS} total judgments, got {len(integrated)}"
        )

    return integrated


def evaluate_integrated_audit(
    integrated_judgments: Mapping[tuple[str, str], IntegratedCandidateJudgment],
    case_manifest: Sequence[dict[str, Any]],
) -> C1AdjudicationIntegrationResult:
    """Evaluate all 150 cases and compute L36 point bounds."""
    if len(case_manifest) != TOTAL_SAMPLE:
        raise ValueError(f"Expected {TOTAL_SAMPLE} cases in manifest, got {len(case_manifest)}")

    audit_cases: list[AuditCase] = []
    evaluated_cases: list[CaseEvaluationResult] = []
    case_outcomes: dict[str, CaseEvaluationResult] = {}

    v1_cnt = 0
    v0_cnt = 0
    vu_cnt = 0

    for c in case_manifest:
        case_id = c["case_id"]
        stratum = c["stratum"]
        cluster_id = c.get("recipe_cluster_id", c.get("recipe_id", case_id))

        if "candidate_identities" in c:
            candidate_ids = [str(x) for x in c["candidate_identities"]]
        elif "candidates" in c:
            candidate_ids = [
                str(cand.get("identity_id", cand.get("candidate_id", cand)))
                for cand in c["candidates"]
            ]
        else:
            raise KeyError(f"Case {case_id} missing candidate identities or candidates")

        if len(candidate_ids) != 5:
            raise ValueError(f"Case {case_id} must have exactly 5 candidates")

        labels = []
        for cand_id in candidate_ids:
            rec = integrated_judgments.get((case_id, cand_id))
            if rec is None:
                labels.append("U")
            else:
                labels.append(rec.final_label)

        audit_case = AuditCase(
            case_id=case_id,
            stratum=stratum,
            recipe_cluster_id=cluster_id,
            candidate_labels=labels,
            is_operational_failure=c.get("is_operational_failure", False),
            is_reference_disputed=c.get("is_reference_disputed", False),
        )
        audit_cases.append(audit_case)

        res = evaluate_audit_case(audit_case)
        evaluated_cases.append(res)
        case_outcomes[case_id] = res

        if res.case_value == CaseValue.SUCCESS:
            v1_cnt += 1
        elif res.case_value == CaseValue.FAILURE:
            v0_cnt += 1
        else:
            vu_cnt += 1

    # Validate unseen sample size
    unseen_cases = [c for c in audit_cases if c.stratum.startswith("unseen_")]
    if len(unseen_cases) != UNSEEN_SAMPLE:
        raise ValueError(f"Expected {UNSEEN_SAMPLE} unseen cases, got {len(unseen_cases)}")

    # Compute point bounds on unseen stratum via frozen evaluator
    point_bounds, _, p_ac = compute_partial_bounds(audit_cases)

    agreed_count = sum(1 for j in integrated_judgments.values() if j.source_type == "AGREED")
    adj_count = sum(1 for j in integrated_judgments.values() if j.source_type == "ADJUDICATED")

    return C1AdjudicationIntegrationResult(
        total_cases_evaluated=len(case_manifest),
        total_candidate_judgments=len(integrated_judgments),
        agreed_judgments_count=agreed_count,
        adjudicated_judgments_count=adj_count,
        unseen_cases_count=len(unseen_cases),
        seen_cases_count=len(case_manifest) - len(unseen_cases),
        case_outcomes=case_outcomes,
        point_bounds=point_bounds,
        p_ac_sensitivity=p_ac,
        v1_count_total=v1_cnt,
        v0_count_total=v0_cnt,
        vu_count_total=vu_cnt,
        receipt_format="c1-l36-adjudication-integration-receipt-v1",
        is_denominator_preserved=True,
    )


def evaluate_full_audit(
    integrated_judgments: Mapping[tuple[str, str], IntegratedCandidateJudgment],
    case_manifest: Sequence[dict[str, Any]],
    rater_pairs: list[tuple[str, str]] | None = None,
    n_bootstrap_draws: int = BOOTSTRAP_DRAWS,
    seed: int = SEED,
) -> tuple[C1AdjudicationIntegrationResult, L36AuditReport]:
    """Computes both integration result and full L36 audit report with bootstrap intervals."""
    integration_result = evaluate_integrated_audit(integrated_judgments, case_manifest)

    audit_cases: list[AuditCase] = []
    for c in case_manifest:
        case_id = c["case_id"]
        stratum = c["stratum"]
        cluster_id = c.get("recipe_cluster_id", c.get("recipe_id", case_id))

        if "candidate_identities" in c:
            candidate_ids = [str(x) for x in c["candidate_identities"]]
        elif "candidates" in c:
            candidate_ids = [
                str(cand.get("identity_id", cand.get("candidate_id", cand)))
                for cand in c["candidates"]
            ]
        else:
            raise KeyError(f"Case {case_id} missing candidate identities or candidates")

        labels = []
        for cand_id in candidate_ids:
            rec = integrated_judgments.get((case_id, cand_id))
            labels.append("U" if rec is None else rec.final_label)

        audit_cases.append(AuditCase(
            case_id=case_id,
            stratum=stratum,
            recipe_cluster_id=cluster_id,
            candidate_labels=labels,
            is_operational_failure=c.get("is_operational_failure", False),
            is_reference_disputed=c.get("is_reference_disputed", False),
        ))

    audit_report = evaluate_audit_estimand(
        cases=audit_cases,
        rater_pairs=rater_pairs,
        n_bootstrap_draws=n_bootstrap_draws,
        seed=seed,
    )

    return integration_result, audit_report


def parse_args(args: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sealed-key", type=Path, required=True, help="Path to sealed adjudication key JSON")
    parser.add_argument("--adjudication-return", type=Path, required=True, help="Path to adjudication return JSON")
    parser.add_argument("--case-manifest", type=Path, required=True, help="Path to 150-case manifest JSON")
    parser.add_argument("--agreed-map", type=Path, default=None, help="Path to pre-extracted agreed map JSON")
    parser.add_argument("--private-key", type=Path, default=None, help="Path to c1-private-key-v1 JSON")
    parser.add_argument("--r1-return", type=Path, default=None, help="Path to R1 return JSON")
    parser.add_argument("--r2-return", type=Path, default=None, help="Path to R2 return JSON")
    parser.add_argument("--output", type=Path, default=None, help="Path to write integration receipt JSON")
    parser.add_argument("--bootstrap-draws", type=int, default=BOOTSTRAP_DRAWS, help="Number of bootstrap draws")
    parser.add_argument("--seed", type=int, default=SEED, help="Random seed for bootstrap")
    return parser.parse_args(args)


def main(argv: Sequence[str] | None = None) -> int:
    opts = parse_args(argv)

    sealed_key = json.loads(opts.sealed_key.read_text(encoding="utf-8"))
    adj_return = json.loads(opts.adjudication_return.read_text(encoding="utf-8"))
    manifest_raw = opts.case_manifest.read_text(encoding="utf-8").strip()
    if opts.case_manifest.suffix == ".jsonl" or ("\n" in manifest_raw and not manifest_raw.startswith("[")):
        manifest = [json.loads(line) for line in manifest_raw.splitlines() if line.strip()]
    elif manifest_raw.startswith("["):
        manifest = json.loads(manifest_raw)
    else:
        parsed = json.loads(manifest_raw)
        if "cases" in parsed:
            manifest = parsed["cases"]
        elif "selected_cases" in parsed:
            manifest = parsed["selected_cases"]
        else:
            manifest = [parsed]

    if opts.agreed_map:
        raw_agreed = json.loads(opts.agreed_map.read_text(encoding="utf-8"))
        if isinstance(raw_agreed, dict) and "items" in raw_agreed:
            agreed_map = {(item["case_id"], item["identity_id"]): item["final_label"] for item in raw_agreed["items"]}
        elif isinstance(raw_agreed, list):
            agreed_map = {(item["case_id"], item["identity_id"]): item["final_label"] for item in raw_agreed}
        elif isinstance(raw_agreed, dict):
            agreed_map = {}
            for k, v in raw_agreed.items():
                if isinstance(k, str) and "::" in k:
                    parts = k.split("::", 1)
                    agreed_map[(parts[0], parts[1])] = v
                else:
                    raise ValueError(f"Unrecognized agreed map key format: {k}")
        else:
            raise ValueError("Unsupported agreed_map format")
    elif opts.private_key and opts.r1_return and opts.r2_return:
        priv_key = json.loads(opts.private_key.read_text(encoding="utf-8"))
        r1_ret = json.loads(opts.r1_return.read_text(encoding="utf-8"))
        r2_ret = json.loads(opts.r2_return.read_text(encoding="utf-8"))
        agreed_map = extract_agreed_map_from_returns(priv_key, r1_ret, r2_ret)
    else:
        raise ValueError("Either --agreed-map or (--private-key, --r1-return, --r2-return) must be provided")

    integrated = integrate_judgments(sealed_key, adj_return, agreed_map)
    result, report = evaluate_full_audit(
        integrated_judgments=integrated,
        case_manifest=manifest,
        n_bootstrap_draws=opts.bootstrap_draws,
        seed=opts.seed,
    )

    receipt = result.to_receipt_dict()
    receipt["h1_decision"] = report.h1_decision.value
    if report.bootstrap_interval and not report.bootstrap_interval.cluster_crosses_strata:
        receipt["bootstrap_ci_95"] = {
            "p_lower": list(report.bootstrap_interval.p_lower_ci_95),
            "p_upper": list(report.bootstrap_interval.p_upper_ci_95),
            "valid_draws": report.bootstrap_interval.valid_draws,
        }

    output_str = json.dumps(receipt, indent=2, sort_keys=True)
    if opts.output:
        opts.output.write_text(output_str + "\n", encoding="utf-8")
    else:
        print(output_str)

    return 0


if __name__ == "__main__":
    sys.exit(main())
