#!/usr/bin/env python3
"""
audit_ssot_appendix_a1.py
Machine audit of the reference statistics listing in `academic_vision_and_strategy.md`
Appendix A.1.

Why this exists
---------------
R41 ("Reference statistics synthetic self-test") is recorded as *source author claim
only; Appendix A is not validated here*. The listing is committed text, so the claim
can be audited from bytes without inventing evidence. Appendix A.1 itself names the
defects that a passing self-test does not repair:

  - the evaluator counts infinity <= infinity as covered;
  - absent calibration entries are treated as infinity;
  - the sampler can return fewer than requested;
  - repeated candidates overwrite ranks;
  - synthetic ranks are not bounded by M;
  - the scoreboard uses a different field name.

This tool locates each defect in the committed listing and emits line-anchored
evidence. Every check is written so that it reports NOT_FOUND if the source is
corrected; the audit therefore fails closed instead of decaying into a rubber stamp.

What it does NOT do
-------------------
- It does not install, patch, or adopt the listing as protocol code.
- It does not treat a self-test pass as approval (PR15).
- It does not verify the source author's original file hash
  `2ac5e6fe5eb991dffb85e66fb936da71c78deb9a1774e5848a591b9e22efc91e`; only the
  document listing is auditable here.
- Optional `--execute-selftest` runs the extracted listing in a throwaway temp
  directory as a bounded diagnostic. The SSoT labels A.1 a NON-EXECUTABLE
  REFERENCE, so the result is reported as diagnostic only and never as a gate.

Output: JSON report. Exit 0 when the audit ran and every check resolved to a
known state; exit 1 on extraction failure.
"""

from __future__ import annotations

import argparse
import ast
import hashlib
import json
import re
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

SECTION_MARKER = "### A.1 Statistics module"
FENCE_OPEN = "```python"
FENCE_CLOSE = "```"
SELFTEST_SENTINEL = "ALL SELF-TESTS PASSED"
CANONICAL_REGISTER_COLUMN = "Evidence_status"
SCOREBOARD_READ_COLUMN = "Status"


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def extract_listing(document: str) -> Tuple[str, int, int]:
    """Return (source, first_line, last_line), both 1-based inclusive."""
    marker = document.find(SECTION_MARKER)
    if marker < 0:
        raise ValueError("Appendix A.1 section marker not found")
    open_at = document.find(FENCE_OPEN, marker)
    if open_at < 0:
        raise ValueError("no ```python fence after the A.1 marker")
    body_start = document.find("\n", open_at) + 1
    close_at = document.find("\n" + FENCE_CLOSE, body_start)
    if close_at < 0:
        raise ValueError("unterminated ```python fence in A.1")
    source = document[body_start:close_at]
    first_line = document.count("\n", 0, body_start) + 1
    last_line = first_line + source.count("\n")
    return source, first_line, last_line


def _function_spans(source: str) -> Dict[str, Tuple[int, int]]:
    """Map function name -> (first_line, last_line) relative to the listing."""
    tree = ast.parse(source)
    spans: Dict[str, Tuple[int, int]] = {}
    for node in tree.body:
        if isinstance(node, ast.FunctionDef):
            spans[node.name] = (node.lineno, node.end_lineno)
    return spans


def _lines(source: str) -> List[str]:
    return source.split("\n")


def _find(
    lines: List[str], span: Tuple[int, int], pattern: str
) -> List[Dict[str, Any]]:
    """Line-anchored evidence for a regex inside a 1-based inclusive span."""
    rx = re.compile(pattern)
    hits: List[Dict[str, Any]] = []
    start, end = span
    for line_no in range(start, min(end, len(lines)) + 1):
        text = lines[line_no - 1]
        if rx.search(text):
            hits.append({"line": line_no, "text": text.strip()})
    return hits


def _check(
    code: str,
    name: str,
    consequence: str,
    falsifier: str,
    requirements: List[List[Dict[str, Any]]],
    extra: Optional[Dict[str, Any]] = None,
) -> Dict[str, Any]:
    """Confirm only when every required source fragment is present.

    `requirements` is a list of evidence groups. A defect is confirmed only when
    every group is non-empty, so correcting any one fragment turns the check into
    NOT_FOUND instead of leaving a partially-matching green.
    """
    confirmed = all(group for group in requirements)
    evidence: List[Dict[str, Any]] = []
    for group in requirements:
        evidence.extend(group)
    entry: Dict[str, Any] = {
        "code": code,
        "name": name,
        "status": "CONFIRMED_IN_SOURCE" if confirmed else "NOT_FOUND",
        "evidence": evidence,
        "consequence": consequence,
        "falsifier": falsifier,
    }
    if extra:
        entry.update(extra)
    return entry


def audit(source: str, register_header: Optional[List[str]] = None) -> Dict[str, Any]:
    lines = _lines(source)
    spans = _function_spans(source)

    def span(name: str) -> Tuple[int, int]:
        if name not in spans:
            return (1, 0)  # empty span -> no evidence -> NOT_FOUND
        return spans[name]

    defects: List[Dict[str, Any]] = []

    # 1. infinity <= infinity counted as covered.
    evaluator = span("evaluate_cells")
    inf_default = _find(lines, evaluator, r'\.get\(\s*"threshold"\s*,\s*math\.inf\s*\)')
    bare_le = _find(lines, evaluator, r"^\s*if s <= t:\s*$")
    defects.append(_check(
        "EVALUATOR_INF_LE_INF_COVERED",
        "evaluate_cells counts an infinite score against an infinite threshold as a hit",
        "A case whose first acceptable rank never appears (score=inf) is reported as covered "
        "when the cell threshold is inf, inflating empirical coverage to 1.0.",
        "Fix by excluding non-finite scores/thresholds (e.g. require math.isfinite(t) and "
        "math.isfinite(s)) and re-run; this check then reports NOT_FOUND.",
        [inf_default, bare_le],
    ))

    # 2. Absent calibration entries treated as infinity.
    defects.append(_check(
        "ABSENT_CALIBRATION_AS_INFINITY",
        "evaluate_cells silently substitutes math.inf for cells missing from calibration",
        "A cell that was never calibrated is scored with an infinite threshold, so every test "
        "case counts as covered instead of the cell being flagged uncalibrated/no_guarantee.",
        "Fix by rejecting unknown cells (e.g. skip or raise) and re-run; this check then "
        "reports NOT_FOUND.",
        [inf_default],
    ))

    # 3. Sampler silent shortfall.
    sampler = span("stratified_sample")
    guard = _find(lines, sampler, r"^\s*if k \* min_per_stratum > n_total:\s*$")
    min_cap = _find(lines, sampler, r"^\s*m = min\(alloc\[s\], len\(v\)\)\s*$")
    alloc_min = _find(lines, sampler, r"max\(min_per_stratum, round\(")
    post_check = _find(lines, sampler, r"len\(out\)|sum\(alloc\.values\(\)\)\s*==\s*n_total")
    defects.append(_check(
        "SAMPLER_SILENT_SHORTFALL",
        "stratified_sample can return fewer than n_total without raising",
        "The only guard is k*min_per_stratum > n_total. Allocation uses "
        "max(min_per_stratum, ...), then m = min(alloc, len(stratum)) truncates silently, so a "
        "small stratum makes the returned sample shorter than requested.",
        "Fix by raising when sum(m) != n_total, or by redistributing the shortfall, and re-run; "
        "`m = alloc[s]` (no silent truncation) makes this check report NOT_FOUND.",
        [alloc_min + guard, min_cap],
        extra={"has_post_length_check": bool(post_check)},
    ))

    # 4. Repeated candidate overwrites rank.
    blinded = span("blinded_pool")
    last_write = _find(lines, blinded, r"^\s*key\[cand\.strip\(\)\.lower\(\)\]\[arm\] = rank\s*$")
    enumerate_rank = _find(lines, blinded, r"for rank, cand in enumerate\(lst, start=1\)")
    defects.append(_check(
        "DUPLICATE_CANDIDATE_RANK_OVERWRITE",
        "blinded_pool keeps only the last rank of a repeated candidate",
        "Assigning key[...][arm] = rank inside the enumerate loop means a candidate listed "
        "twice in one arm overwrites its earlier rank; the first/best rank is lost from the "
        "private key.",
        "Fix with setdefault (preserve first) or explicit duplicate rejection and re-run; this "
        "check then reports NOT_FOUND.",
        [last_write, enumerate_rank],
    ))

    # 5. Synthetic rank not bounded by M.
    synth = span("_synthetic_scores")
    synth_sig = _find(lines, synth, r"^def _synthetic_scores\(rng, n, p_none, geo_p\):\s*$")
    synth_loop = _find(lines, synth, r"^\s*while rng\.random\(\) > geo_p:\s*$")
    defects.append(_check(
        "SYNTHETIC_RANK_UNBOUNDED_BY_M",
        "_synthetic_scores has no M bound and can emit ranks beyond any pool size",
        "The generator draws geometric ranks with no upper bound, so the synthetic self-test "
        "exercises ranks that a bounded-M candidate pool cannot produce.",
        "Fix by adding an M bound/truncation and re-run; a signature carrying an M parameter "
        "makes this check report NOT_FOUND.",
        [synth_sig, synth_loop],
    ))

    # 6. Scoreboard field-name divergence.
    board = span("scoreboard")
    read_literal = _find(lines, board, r'r\["Status"\]')
    header_has_status = bool(register_header) and SCOREBOARD_READ_COLUMN in register_header
    header_has_canonical = bool(register_header) and CANONICAL_REGISTER_COLUMN in register_header
    divergence = read_literal if (header_has_canonical and not header_has_status) else []
    defects.append(_check(
        "SCOREBOARD_FIELD_NAME_DIVERGENCE",
        "scoreboard reads `Status` while the canonical register column is `Evidence_status`",
        "Applied to a real register row the scoreboard raises KeyError; applied to a converted "
        "row it silently scores a different field, so its count is not a register count.",
        "Fix by reading Evidence_status (or accepting both) and re-run; this check reports "
        "NOT_FOUND when the register header itself uses Status.",
        [read_literal, divergence],
        extra={
            "register_header_has_Evidence_status": header_has_canonical,
            "register_header_has_Status": header_has_status,
            "reads": read_literal[0]["text"] if read_literal else None,
        },
    ))

    # 7. Normalization collision collapse.
    collapse = _find(lines, blinded, r"^\s*cands = sorted\(key\)\s*$")
    normalize = _find(lines, blinded, r"key\[cand\.strip\(\)\.lower\(\)\]\[arm\] = rank")
    defects.append(_check(
        "NORMALIZATION_COLLISION_COLLAPSE",
        "blinded_pool collapses distinct surface forms to one normalized candidate",
        "`cand.strip().lower()` merges candidates that differ only by case/whitespace, so the "
        "rater pool silently loses alternatives and the private key retains one rank set.",
        "Fix by keeping an explicit surface-form -> normalized map and re-run; this check "
        "reports NOT_FOUND if normalization is removed.",
        [collapse, normalize],
    ))

    # 8. Degenerate kappa -> NaN without status.
    kappa = span("cohen_kappa")
    bootstrap = span("cluster_bootstrap_kappa")
    nan_return = _find(lines, kappa, r"return float\(\"nan\"\)")
    nan_filter = _find(lines, bootstrap, r"if not math\.isnan\(v\):")
    nan_fallback = _find(lines, bootstrap, r"return cohen_kappa\(a, b\), float\(\"nan\"\), float\(\"nan\"\)")
    defects.append(_check(
        "DEGENERATE_KAPPA_NAN_VACUOUS_BOOTSTRAP",
        "degenerate agreement yields NaN kappa and an empty bootstrap without an explicit status",
        "When pe == 1.0 cohen_kappa returns NaN; the bootstrap filters those resamples and falls "
        "back to (NaN, NaN, NaN, 0), which a downstream consumer can mistake for a measurement.",
        "Fix by returning an explicit `undefined` status and re-run; this check reports "
        "NOT_FOUND if NaN is surfaced as a status instead of a float.",
        [nan_return, nan_filter, nan_fallback],
    ))

    # 9. Unvalidated conformal alpha.
    conformal = span("conformal_threshold")
    k_expr = _find(lines, conformal, r"k = math\.ceil\(\(n \+ 1\) \* \(1 - alpha\)\)")
    alpha_guard = _find(lines, conformal, r"if not \(0\.0 < alpha < 1\.0\)")
    alpha_check = _check(
        "CONFORMAL_ALPHA_UNVALIDATED",
        "conformal_threshold accepts alpha outside (0, 1) without validation",
        "alpha <= 0 yields k > n (infinite threshold); alpha >= 1 yields k <= 0 and a QoI-1 "
        "order statistic, i.e. a silent 100% coverage claim instead of an invalid-parameter error.",
        "Fix by validating 0 < alpha < 1 and re-run; this check reports NOT_FOUND.",
        [k_expr],
        extra={"alpha_guard_present": bool(alpha_guard)},
    )
    if alpha_guard:
        alpha_check["status"] = "NOT_FOUND"
    defects.append(alpha_check)

    confirmed = [d for d in defects if d["status"] == "CONFIRMED_IN_SOURCE"]
    return {
        "listing": {
            "line_count": len(lines),
            "source_sha256": sha256_text(source),
            "selftest_harness_present": "def _selftest(" in source,
            "selftest_sentinel_present": SELFTEST_SENTINEL in source,
            "function_count": len(spans),
        },
        "defects": defects,
        "summary": {
            "confirmed": len(confirmed),
            "not_found": len(defects) - len(confirmed),
            "codes": [d["code"] for d in confirmed],
        },
        "state": "DEFECTS_CONFIRMED" if confirmed else "NO_NAMED_DEFECT_FOUND",
        "claim_boundary": (
            "This is a line-anchored audit of the committed Appendix A.1 listing. It confirms "
            "that the named defects are present in the document text. It does not execute the "
            "listing as an acceptance gate, does not install or repair it, does not verify the "
            "source author's original file hash, and does not establish any protocol property."
        ),
    }


def run_selftest(source: str, timeout: int = 180) -> Dict[str, Any]:
    """Bounded diagnostic execution of the extracted listing. Never a gate."""
    with tempfile.TemporaryDirectory(prefix="r41-selftest-") as tmp:
        path = Path(tmp) / "iron_chef_stats_extract.py"
        with path.open("w", encoding="utf-8", newline="\n") as stream:
            stream.write(source if source.endswith("\n") else source + "\n")
        try:
            proc = subprocess.run(
                [sys.executable, str(path)],
                cwd=tmp,
                capture_output=True,
                text=True,
                timeout=timeout,
            )
        except subprocess.TimeoutExpired:
            return {"executed": True, "passed": False, "reason": "TIMEOUT", "timeout_s": timeout}
        stdout = proc.stdout or ""
        return {
            "executed": True,
            "passed": proc.returncode == 0 and SELFTEST_SENTINEL in stdout,
            "returncode": proc.returncode,
            "sentinel_present": SELFTEST_SENTINEL in stdout,
            "stdout_tail": stdout.strip().splitlines()[-12:],
            "stderr_tail": (proc.stderr or "").strip().splitlines()[-6:],
        }


def register_header(path: Path) -> Optional[List[str]]:
    if not path.exists():
        return None
    import csv

    with path.open(encoding="utf-8", newline="") as stream:
        reader = csv.reader(stream)
        for row in reader:
            return [cell.strip() for cell in row]
    return None


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--ssot", type=Path, default=Path("academic_vision_and_strategy.md"))
    parser.add_argument("--register", type=Path, default=None,
                        help="claims register used to check the scoreboard field name")
    parser.add_argument("--execute-selftest", action="store_true",
                        help="bounded diagnostic run of the extracted listing (never a gate)")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--repo-root", type=Path, default=Path.cwd())
    args = parser.parse_args(argv)

    document = args.ssot.read_text(encoding="utf-8")
    try:
        source, first_line, last_line = extract_listing(document)
    except ValueError as exc:
        sys.stderr.write("EXTRACTION_FAILED: %s\n" % exc)
        return 1

    header = register_header(args.register) if args.register else None
    body = audit(source, header)

    try:
        rel_ssot = str(args.ssot.resolve().relative_to(args.repo_root.resolve())).replace("\\", "/")
    except ValueError:
        rel_ssot = str(args.ssot)

    report: Dict[str, Any] = {
        "schema_version": 1,
        "produced_at_utc": datetime.now(timezone.utc).isoformat(),
        "tool": "ai/tools/audit_ssot_appendix_a1.py",
        "ssot_path": rel_ssot,
        "ssot_sha256": hashlib.sha256(args.ssot.read_bytes()).hexdigest(),
        "listing": dict(body["listing"], start_line=first_line, end_line=last_line),
        "register_path": str(args.register) if args.register else None,
        "register_header": header,
        "defects": body["defects"],
        "summary": body["summary"],
        "state": body["state"],
        "selftest_execution": (
            run_selftest(source) if args.execute_selftest
            else {
                "executed": False,
                "reason": "NOT_REQUESTED",
                "note": "SSoT labels Appendix A.1 a NON-EXECUTABLE REFERENCE; execution is diagnostic only.",
            }
        ),
        "claim_boundary": body["claim_boundary"],
    }

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8", newline="\n") as stream:
        json.dump(report, stream, indent=2, ensure_ascii=True)
        stream.write("\n")

    summary = {
        "state": report["state"],
        "listing_lines": "%d-%d" % (first_line, last_line),
        "listing_sha256": report["listing"]["source_sha256"],
        "confirmed": report["summary"]["confirmed"],
        "not_found": report["summary"]["not_found"],
        "codes": report["summary"]["codes"],
        "selftest_executed": report["selftest_execution"]["executed"],
        "selftest_passed": report["selftest_execution"].get("passed"),
        "report_sha256": hashlib.sha256(args.output.read_bytes()).hexdigest(),
        "output": str(args.output),
    }
    sys.stdout.buffer.write((json.dumps(summary, indent=2) + "\n").encode("utf-8"))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
