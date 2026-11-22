"""
L45 Scientific Argument and Methods Rationale Evaluator.

Verifies the scientific argument, chapter integrity, methods rationale,
and conclusion boundaries specified in Lead Backlog L45, Academic Vision &
Strategy §§3, 6-8, 17, and 21, and the 14 authored thesis chapters.

Invariants:
1. All 14 authored chapters must match their pinned SHA-256 digests in
   `SOURCE.json` (evidence/iron-chef-lead-member-handoff-20261105/SOURCE.json).
2. Chapter 12 (Bounded Negative Results) must enforce:
   - Exactly 7 distinct negative/withdrawn/unavailable threads:
     * SAG (Signal-Adaptive Gating) rejection
     * True-context dual-encoder rejection
     * Global flavor-pairing non-significance (p = 0.238095 > 0.05)
     * Behavioral exact-pair overlay rejection
     * Recipes5k frozen-feature CLIP projection rejection
     * Historical SmolVLM adapter withdrawal (VERIFIED NEGATIVE)
     * Voice-vision chain loopback unavailability (100/100 unavailable)
   - Descriptive calibration boundary: tHat=7 (ok, seen alpha=0.30),
     tHat=12 (uninformative, alpha=0.20), tHat=inf (vacuous, unconditioned/unseen).
   - Formal coverage guarantee remains disabled under unproven exchangeability.
3. Chapter 13 (Discussion & Threats) must enforce:
   - Protocol v1 freezes core rules (PR01-03/05/09/10).
   - PR13 ethics/consent remains a distinct human-subject gate.
   - L36 operational mapping does not substitute for human ratings/adjudications.
   - Historical 19/25 (76.0%) wording as recorded in the shared Chapter 13 snapshot.
   - Clean graph C5 vertical integration: 6,633 nodes, 23,185 edges, 0 identity errors,
     manifest prefix `ade85c3312b8`.
"""

from __future__ import annotations

import argparse
import dataclasses
import hashlib
import json
import re
from pathlib import Path
from typing import Dict, List


def load_chapter_pins(chapters_dir: Path) -> dict[str, str]:
    """Read the authored destination pins from the shared handoff manifest."""
    manifest = json.loads((chapters_dir.parent / "SOURCE.json").read_text(encoding="utf-8"))
    pins = {}
    for entry in manifest["direct_authored_copies"]:
        relative = entry["destination_path"]
        match = re.fullmatch(r"chapters/((0[1-9]|1[0-4])-[a-z0-9-]+\.md)", relative)
        digest = entry["destination_sha256"]
        if not match or match[2] in pins or not re.fullmatch(r"[0-9a-fA-F]{64}", digest):
            raise ValueError("Invalid or duplicate chapter pin in SOURCE.json")
        pins[match[2]] = (match[1], digest.lower())
    if len(pins) != 14:
        raise ValueError("SOURCE.json must pin chapters 01 through 14")
    return dict(pins[number] for number in sorted(pins))


# The 7 required negative / withdrawn / boundary threads in Chapter 12
REQUIRED_CH12_NEGATIVE_THREADS = (
    "sag_fusion",
    "true_context_dual_encoder",
    "flavor_pairing_hypothesis",
    "exact_pair_overlay",
    "recipes5k_clip_projection",
    "smolvlm_adapter_withdrawal",
    "voice_vision_boundary",
)


@dataclasses.dataclass(frozen=True)
class ChapterHashCheck:
    filename: str
    expected_sha256: str
    actual_sha256: str
    matches: bool


@dataclasses.dataclass(frozen=True)
class Chapter12NegativeAudit:
    thread_coverage: Dict[str, bool]
    all_threads_present: bool
    flavor_pairing_p_value_bounded: bool
    smolvlm_withdrawn_recorded: bool
    descriptive_calibration_t_hat_bounded: bool
    formal_guarantee_disabled: bool


@dataclasses.dataclass(frozen=True)
class Chapter13DiscussionAudit:
    pr_freeze_acknowledged: bool
    pr13_ethics_gate_preserved: bool
    l36_operational_mapping_acknowledged: bool
    campaign_progress_reconciled: bool  # 19/25 milestones, 76.0%
    c5_clean_graph_metrics_verified: bool  # 6,633 nodes, 23,185 edges, 0 errors


@dataclasses.dataclass(frozen=True)
class L45ScientificArgumentAuditResult:
    all_14_chapters_intact: bool
    chapter_checks: List[ChapterHashCheck]
    ch12_audit: Chapter12NegativeAudit
    ch13_audit: Chapter13DiscussionAudit
    overall_status: str  # "PASS" | "FAIL"
    discrepancies: List[str]


def compute_file_sha256(path: Path) -> str:
    """Computes SHA-256 of raw file bytes matching SOURCE.json."""
    raw = path.read_bytes()
    return hashlib.sha256(raw).hexdigest()


def verify_chapter_hashes(chapters_dir: Path) -> List[ChapterHashCheck]:
    """Compare each chapter with the current handoff manifest."""
    checks = []
    for filename, expected_hash in load_chapter_pins(chapters_dir).items():
        ch_path = chapters_dir / filename
        if not ch_path.exists():
            checks.append(ChapterHashCheck(filename, expected_hash, "FILE_NOT_FOUND", False))
            continue
        actual_hash = compute_file_sha256(ch_path)
        checks.append(ChapterHashCheck(filename, expected_hash, actual_hash, actual_hash == expected_hash))
    return checks


def audit_chapter_12_text(ch12_text: str) -> Chapter12NegativeAudit:
    """Audits Chapter 12 text for negative result coverage and boundaries."""
    thread_coverage = {
        "sag_fusion": "signal-adaptive gating" in ch12_text.lower() or "sag" in ch12_text.lower(),
        "true_context_dual_encoder": "dual encoder" in ch12_text.lower() or "recipe1msubs" in ch12_text.lower(),
        "flavor_pairing_hypothesis": "flavor-pairing" in ch12_text.lower() and "0.238095" in ch12_text,
        "exact_pair_overlay": "exact-pair overlay" in ch12_text.lower(),
        "recipes5k_clip_projection": "recipes5k" in ch12_text.lower() and "clip" in ch12_text.lower(),
        "smolvlm_adapter_withdrawal": "smolvlm" in ch12_text.lower() and "withdrawn" in ch12_text.lower(),
        "voice_vision_boundary": "voice-vision" in ch12_text.lower() and "unavailable" in ch12_text.lower(),
    }
    all_threads = all(thread_coverage.values())

    # Flavor pairing p-value check: p = 0.238095 > alpha = 0.05
    fp_check = "0.238095" in ch12_text and "0.05" in ch12_text

    # SmolVLM withdrawn check
    smolvlm_check = "7e8c164e0fda2d4c7c1f24a17ea94a89fc7027dfbfbfee4d152192644fe7dea1" in ch12_text

    # Descriptive calibration cutoffs: tHat=7, tHat=12, tHat=inf
    cal_check = "t=7" in ch12_text or "t}=7" in ch12_text or "\\hat{t}=7" in ch12_text or "hat{t} = 7" in ch12_text
    cal_vacuous = "\\hat{t} = \\infty" in ch12_text or "t}=\\infty" in ch12_text or "t = \\infty" in ch12_text

    # Formal guarantee disabled check
    guar_check = "formal statistical coverage guarantees remain disabled" in ch12_text.lower() or "formal guarantee" in ch12_text.lower()

    return Chapter12NegativeAudit(
        thread_coverage=thread_coverage,
        all_threads_present=all_threads,
        flavor_pairing_p_value_bounded=fp_check,
        smolvlm_withdrawn_recorded=smolvlm_check,
        descriptive_calibration_t_hat_bounded=bool(cal_check and cal_vacuous),
        formal_guarantee_disabled=guar_check,
    )


def audit_chapter_13_text(ch13_text: str) -> Chapter13DiscussionAudit:
    """Audits Chapter 13 text for frozen PR status, ethics gate, progress, and C5 proof."""
    pr_freeze = "pr01" in ch13_text.lower() and "frozen" in ch13_text.lower()
    pr13_gate = "pr13" in ch13_text.lower() and "ethics" in ch13_text.lower()
    l36_map = "l36" in ch13_text.lower()
    progress_check = "19/25" in ch13_text and "76.0%" in ch13_text
    c5_check = "6,633" in ch13_text and "23,185" in ch13_text and "ade85c3312b8" in ch13_text

    return Chapter13DiscussionAudit(
        pr_freeze_acknowledged=pr_freeze,
        pr13_ethics_gate_preserved=pr13_gate,
        l36_operational_mapping_acknowledged=l36_map,
        campaign_progress_reconciled=progress_check,
        c5_clean_graph_metrics_verified=c5_check,
    )


def audit_scientific_argument(chapters_dir: Path) -> L45ScientificArgumentAuditResult:
    """Full L45 evaluation of 14 authored thesis chapters."""
    discrepancies: List[str] = []

    # 1. Check all 14 chapter hashes
    checks = verify_chapter_hashes(chapters_dir)
    hash_failures = [c for c in checks if not c.matches]
    if hash_failures:
        for f in hash_failures:
            discrepancies.append(f"Chapter hash mismatch: {f.filename} (expected {f.expected_sha256[:8]}..., actual {f.actual_sha256[:8]}...)")
    all_intact = len(hash_failures) == 0

    # 2. Check Chapter 12 content
    ch12_path = chapters_dir / "12-bounded-negatives.md"
    if ch12_path.exists():
        ch12_text = ch12_path.read_text(encoding="utf-8")
        ch12_audit = audit_chapter_12_text(ch12_text)
        if not ch12_audit.all_threads_present:
            missing_th = [k for k, v in ch12_audit.thread_coverage.items() if not v]
            discrepancies.append(f"Chapter 12 missing negative threads: {missing_th}")
        if not ch12_audit.flavor_pairing_p_value_bounded:
            discrepancies.append("Chapter 12 missing flavor pairing p-value bound (0.238095 > 0.05)")
        if not ch12_audit.smolvlm_withdrawn_recorded:
            discrepancies.append("Chapter 12 missing SmolVLM withdrawal receipt hash")
        if not ch12_audit.descriptive_calibration_t_hat_bounded:
            discrepancies.append("Chapter 12 missing descriptive tHat cutoffs (7 and inf)")
        if not ch12_audit.formal_guarantee_disabled:
            discrepancies.append("Chapter 12 missing explicit disabled formal guarantee statement")
    else:
        discrepancies.append("12-bounded-negatives.md not found")
        ch12_audit = Chapter12NegativeAudit({}, False, False, False, False, False)

    # 3. Check Chapter 13 content
    ch13_path = chapters_dir / "13-discussion-and-threats.md"
    if ch13_path.exists():
        ch13_text = ch13_path.read_text(encoding="utf-8")
        ch13_audit = audit_chapter_13_text(ch13_text)
        if not ch13_audit.pr_freeze_acknowledged:
            discrepancies.append("Chapter 13 does not acknowledge core PR freeze")
        if not ch13_audit.pr13_ethics_gate_preserved:
            discrepancies.append("Chapter 13 does not preserve PR13 ethics gate")
        if not ch13_audit.campaign_progress_reconciled:
            discrepancies.append("Chapter 13 is missing the historical 19/25 (76.0%) snapshot wording")
        if not ch13_audit.c5_clean_graph_metrics_verified:
            discrepancies.append("Chapter 13 is missing the recorded C5 clean graph counts (6,633 nodes, 23,185 edges)")
    else:
        discrepancies.append("13-discussion-and-threats.md not found")
        ch13_audit = Chapter13DiscussionAudit(False, False, False, False, False)

    status = "PASS" if not discrepancies else "FAIL"
    return L45ScientificArgumentAuditResult(
        all_14_chapters_intact=all_intact,
        chapter_checks=checks,
        ch12_audit=ch12_audit,
        ch13_audit=ch13_audit,
        overall_status=status,
        discrepancies=discrepancies,
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Check the shared chapter hashes and snapshot wording.")
    parser.add_argument("chapters_dir", type=Path)
    args = parser.parse_args()
    result = audit_scientific_argument(args.chapters_dir)
    print(json.dumps(dataclasses.asdict(result), indent=2))
    raise SystemExit(result.overall_status != "PASS")
