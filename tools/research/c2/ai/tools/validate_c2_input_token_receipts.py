#!/usr/bin/env python3
"""Offline consistency checks for caller-supplied C2 token-count evidence.

This tool performs no network access, does not read credentials, and never
authorizes a cohort, reservation, or provider dispatch. It verifies that a
separately obtained count receipt set covers exactly the READY request hashes
in a caller-supplied blocked preflight report, and each receipt matches its
captured endpoint response. This does not authenticate that report, the count,
the source endpoint, or the transport; it establishes byte/hash consistency
only.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import Any


MODEL = "gpt-4o-2024-08-06"
ENDPOINT = "https://api.openai.com/v1/responses/input_tokens"
PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
HEX_SHA256 = re.compile(r"^[0-9a-f]{64}$")


class ReceiptError(ValueError):
    """Raised when local count evidence does not match the frozen requests."""


def _reject_duplicate_keys(pairs: list[tuple[str, Any]], label: str) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise ReceiptError(f"{label} contains duplicate JSON key {key!r}")
        result[key] = value
    return result


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def read_object(path: Path, label: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=lambda pairs: _reject_duplicate_keys(pairs, label))
    except ReceiptError:
        raise
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ReceiptError(f"{label} is not readable UTF-8 JSON") from exc
    if not isinstance(value, dict):
        raise ReceiptError(f"{label} must be a JSON object")
    return value


def safe_pinned_file(root: Path, ref: Any, label: str) -> Path:
    if not isinstance(ref, dict) or set(ref) != {"path", "sha256"}:
        raise ReceiptError(f"{label} must contain exactly path and sha256")
    rel, expected = ref["path"], ref["sha256"]
    if not isinstance(rel, str) or not rel or not isinstance(expected, str) or not HEX_SHA256.fullmatch(expected):
        raise ReceiptError(f"{label} has an invalid path or SHA-256")
    path = (root / rel).resolve()
    try:
        path.relative_to(root.resolve())
    except ValueError as exc:
        raise ReceiptError(f"{label} path escapes the evidence directory") from exc
    if not path.is_file() or sha256_file(path) != expected:
        raise ReceiptError(f"{label} is missing or its SHA-256 does not match")
    return path


def validate_utc_timestamp(value: Any, label: str) -> None:
    if not isinstance(value, str) or not value.endswith("Z"):
        raise ReceiptError(f"{label} must be an ISO-8601 UTC timestamp ending in Z")
    try:
        parsed = datetime.fromisoformat(value[:-1] + "+00:00")
    except ValueError as exc:
        raise ReceiptError(f"{label} is not a valid ISO-8601 timestamp") from exc
    if parsed.utcoffset() is None:
        raise ReceiptError(f"{label} must include UTC")


def load_request_hashes(report_path: Path) -> dict[str, str]:
    report = read_object(report_path, "preflight report")
    if (
        report.get("protocol_sha256") != PROTOCOL_SHA256
        or report.get("model") != MODEL
        or report.get("status") != "BLOCKED"
        or report.get("dispatch_authorized") is not False
        or report.get("cohort_selected") is not False
        or report.get("selected_cohort", "absent") is not None
        or report.get("conditional_scenario_if_receipts_are_true", "absent") is not None
        or report.get("reservation_installed", False) is not False
    ):
        raise ReceiptError("input is not shaped like a blocked, non-dispatch-authorizing C2 report")
    evidence = report.get("evidence")
    if not isinstance(evidence, dict) or not isinstance(evidence.get("manifest_sha256"), str):
        raise ReceiptError("input report lacks the expected preflight evidence envelope")
    serializer_hash = report.get("serializer_source_sha256")
    if not isinstance(serializer_hash, str) or not HEX_SHA256.fullmatch(serializer_hash):
        raise ReceiptError("input report lacks a valid serializer-source hash field")
    hashes = report.get("request_body_sha256_by_case")
    if not isinstance(hashes, dict) or not hashes:
        raise ReceiptError("preflight report has no request-body hash map")
    for case_ref, digest in hashes.items():
        if not isinstance(case_ref, str) or not case_ref or not isinstance(digest, str) or not HEX_SHA256.fullmatch(digest):
            raise ReceiptError("preflight report contains an invalid request identity or hash")
    return hashes


def validate_receipts(report_path: Path, receipt_path: Path) -> dict[str, Any]:
    expected = load_request_hashes(report_path)
    evidence_root = receipt_path.resolve().parent
    seen: set[str] = set()
    total_tokens = 0
    token_counts: dict[str, int] = {}
    capture_hashes: dict[str, str] = {}
    lines = receipt_path.read_text(encoding="utf-8").splitlines()
    if len(lines) != len(expected):
        raise ReceiptError(f"receipt count {len(lines)} does not cover {len(expected)} READY requests")

    required = {
        "case_ref", "request_body_sha256", "model", "endpoint", "observed_at",
        "input_tokens", "response_capture",
    }
    for line_number, line in enumerate(lines, start=1):
        try:
            receipt = json.loads(line, object_pairs_hook=lambda pairs: _reject_duplicate_keys(pairs, f"receipt line {line_number}"))
        except ReceiptError:
            raise
        except json.JSONDecodeError as exc:
            raise ReceiptError(f"receipt line {line_number} is invalid JSON") from exc
        if not isinstance(receipt, dict) or set(receipt) != required:
            raise ReceiptError(f"receipt line {line_number} has an unsupported shape")
        case_ref = receipt["case_ref"]
        if not isinstance(case_ref, str) or case_ref not in expected:
            raise ReceiptError(f"receipt line {line_number} names an unknown case reference")
        if case_ref in seen:
            raise ReceiptError(f"receipt line {line_number} duplicates a case reference")
        seen.add(case_ref)
        if receipt["model"] != MODEL or receipt["endpoint"] != ENDPOINT:
            raise ReceiptError(f"receipt line {line_number} does not name the frozen model and official endpoint")
        if receipt["request_body_sha256"] != expected[case_ref]:
            raise ReceiptError(f"receipt line {line_number} is not bound to the frozen request body")
        validate_utc_timestamp(receipt["observed_at"], f"receipt line {line_number}.observed_at")
        count = receipt["input_tokens"]
        if isinstance(count, bool) or not isinstance(count, int) or count <= 0:
            raise ReceiptError(f"receipt line {line_number}.input_tokens must be a positive integer")

        capture_path = safe_pinned_file(evidence_root, receipt["response_capture"], f"receipt line {line_number} response capture")
        captured_response = read_object(capture_path, f"receipt line {line_number} response capture")
        if (
            set(captured_response) != {"object", "input_tokens"}
            or captured_response["object"] != "response.input_tokens"
            or isinstance(captured_response["input_tokens"], bool)
            or not isinstance(captured_response["input_tokens"], int)
            or captured_response["input_tokens"] <= 0
            or captured_response["input_tokens"] != count
        ):
            raise ReceiptError(f"receipt line {line_number} capture must match the official minimal input-token response")
        token_counts[case_ref] = count
        total_tokens += count
        capture_hashes[case_ref] = sha256_file(capture_path)

    missing = set(expected) - seen
    if missing:
        raise ReceiptError(f"receipt set is missing {len(missing)} READY request(s)")
    ordered_counts = list(token_counts.values())
    return {
            "status": "LOCAL_CONSISTENCY_CHECK_ONLY_UNAUTHENTICATED_INPUTS",
        "protocol_sha256": PROTOCOL_SHA256,
        "model": MODEL,
        "endpoint": ENDPOINT,
        "preflight_report_sha256": sha256_file(report_path),
        "receipt_jsonl_sha256": sha256_file(receipt_path),
        "requestable_cases": len(expected),
        "receipt_cases": len(token_counts),
        "input_tokens_total": total_tokens,
        "input_tokens_max_per_case": max(ordered_counts),
        "input_tokens_by_case_ref": token_counts,
        "response_capture_sha256_by_case_ref": capture_hashes,
        "cohort_selected": False,
        "reservation_installed": False,
        "dispatch_authorized": False,
        "billing_and_call_cap_treatment": "UNKNOWN_NOT_EVALUATED",
        "receipt_authenticity": "HASH_INTEGRITY_ONLY_REQUIRES_LEAD_EVIDENCE_REVIEW",
        "preflight_report_authenticity": "UNVERIFIED_CALLER_SUPPLIED_REPORT",
        "count_receipt_authenticity": "UNVERIFIED_CALLER_SUPPLIED_CAPTURE",
        "request_body_binding_claim": "MATCHES_HASH_MAP_IN_UNAUTHENTICATED_REPORT_ONLY",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preflight-report", required=True, type=Path)
    parser.add_argument("--receipts", required=True, type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        result = validate_receipts(args.preflight_report, args.receipts)
        code = 0
    except (OSError, ReceiptError, UnicodeDecodeError) as exc:
        result = {
            "status": "BLOCKED",
            "cohort_selected": False,
            "reservation_installed": False,
            "dispatch_authorized": False,
            "error": str(exc),
        }
        code = 2
    serialized = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.write_text(serialized, encoding="utf-8", newline="\n")
    else:
        sys.stdout.write(serialized)
    return code


if __name__ == "__main__":
    raise SystemExit(main())
