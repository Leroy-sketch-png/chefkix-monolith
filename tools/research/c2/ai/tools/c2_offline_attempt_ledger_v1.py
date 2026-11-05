#!/usr/bin/env python3
"""Prepare or verify a local C2 case inventory and empty attempt journal.

This versioned offline adapter binds T253's validated provider candidate to
T208's local disposition order and T219's one-attempt case contract. It never
creates dispatch-intent or remote-submit events and never contacts a provider.
"""

from __future__ import annotations

import hashlib
import importlib.util
import json
import os
import re
import tempfile
from pathlib import Path
from typing import Any

CONTRACT = "C2_OFFLINE_ATTEMPT_LEDGER_V1"
PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
T253_SHA256 = "24091fe506bb91c4225de4c3d9052e2e00e8518b5810666d78afeb0f151742ac"
T219_SHA256 = "d2a0a1c06f3a0f12c74c0bf3a766367ee93a2d0e5b3d26b4f35c2d2b1a71c36b"
EXPECTED_COUNTS = {"denominator_cases": 10_747, "ready_requests": 10_499,
                   "retained_preparation_errors": 248}
SHA256_RE = re.compile(r"[0-9a-f]{64}\Z")
OUTPUT_NAMES = ("case_inventory.jsonl", "attempt_journal.jsonl", "ledger_manifest.json")


class LedgerError(ValueError):
    """Sanitized offline package or attempt-ledger failure."""


def sha256(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True,
                      separators=(",", ":"), allow_nan=False).encode("utf-8")


def _json(raw: bytes) -> Any:
    def pairs(values: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in values:
            if key in result:
                raise LedgerError("duplicate JSON property")
            result[key] = value
        return result

    try:
        return json.loads(raw.decode("utf-8", errors="strict"), object_pairs_hook=pairs,
                          parse_constant=lambda _: (_ for _ in ()).throw(LedgerError("invalid constant")))
    except (UnicodeError, json.JSONDecodeError, LedgerError):
        raise LedgerError("local package JSON is invalid") from None


def _jsonl(raw: bytes) -> list[dict[str, Any]]:
    if not raw or not raw.endswith(b"\n") or b"\r" in raw:
        raise LedgerError("local package JSONL is incomplete or noncanonical")
    rows = [_json(line) for line in raw[:-1].split(b"\n")]
    if any(not isinstance(row, dict) for row in rows):
        raise LedgerError("local package JSONL rows must be objects")
    if b"".join(canonical(row) + b"\n" for row in rows) != raw:
        raise LedgerError("local package JSONL is not canonical")
    return rows


def _load_pinned(root: Path, relative: str, expected_sha256: str, module_name: str):
    path = root / relative
    try:
        raw = path.read_bytes()
    except OSError:
        raise LedgerError("pinned offline dependency is unavailable") from None
    if b"\r" in raw.replace(b"\r\n", b""):
        raise LedgerError("pinned offline dependency has noncanonical line endings")
    source = raw.replace(b"\r\n", b"\n")
    if sha256(source) != expected_sha256:
        raise LedgerError("pinned offline dependency digest mismatch")
    spec = importlib.util.spec_from_file_location(module_name, path)
    if spec is None:
        raise LedgerError("pinned offline dependency cannot be loaded")
    module = importlib.util.module_from_spec(spec)
    exec(compile(source, str(path), "exec"), module.__dict__)
    return module


def _dependencies(root: Path):
    t253 = _load_pinned(root, "tools/research/c2/ai/tools/build_senath_c2_batch_transport_candidate.py",
                        T253_SHA256, "c2_ledger_t253")
    t219 = _load_pinned(root, "tools/research/c2/ai/tools/c2_attempt_resume.py",
                        T219_SHA256, "c2_ledger_t219")
    return t253, t219


def _inputs(root: Path, package_dir: Path, t253, t219) -> tuple[dict[str, Any], bytes, bytes, bytes]:
    """Rebuild the exact T219 inventory from pinned T208/T253 artifacts."""
    root = root.resolve()
    package_dir = package_dir.resolve()
    try:
        package_result = t253.validate_saved_package(root, package_dir)
        package_manifest_raw = (package_dir / "manifest.json").read_bytes()
        package_manifest = _json(package_manifest_raw)
        mapping_raw = (package_dir / "local_only_mapping.jsonl").read_bytes()
        batch_raw = (package_dir / "batch_requests.jsonl").read_bytes()
        t208_dir = root / t253.T208_DIR
        t208_manifest_raw = (t208_dir / "manifest.json").read_bytes()
        t208_dispositions_raw = (t208_dir / "dispositions.jsonl").read_bytes()
    except (OSError, ValueError, KeyError, TypeError):
        raise LedgerError("pinned T208/T253 offline package is unavailable or invalid") from None

    if (sha256(t208_manifest_raw) != t253.T208_MANIFEST_SHA256
            or sha256(t208_dispositions_raw) != t253.T208_DISPOSITIONS_SHA256):
        raise LedgerError("pinned T208 manifest or disposition digest mismatch")
    t208_manifest = _json(t208_manifest_raw)
    if (not isinstance(t208_manifest, dict)
            or t208_manifest.get("protocol_sha256") != PROTOCOL_SHA256
            or t208_manifest.get("cohort_count") != EXPECTED_COUNTS["denominator_cases"]
            or t208_manifest.get("ready_count") != EXPECTED_COUNTS["ready_requests"]
            or t208_manifest.get("preparation_error_count") != EXPECTED_COUNTS["retained_preparation_errors"]):
        raise LedgerError("pinned T208 manifest does not match the frozen cohort")
    if (not isinstance(package_manifest, dict)
            or package_manifest.get("protocol_sha256") != PROTOCOL_SHA256
            or package_manifest.get("parent_t208_manifest_sha256") != t253.T208_MANIFEST_SHA256
            or package_manifest.get("parent_dispositions_sha256") != t253.T208_DISPOSITIONS_SHA256
            or package_result.get("manifest_sha256") != sha256(package_manifest_raw)
            or package_result.get("batch_requests_sha256") != sha256(batch_raw)
            or package_result.get("local_mapping_sha256") != sha256(mapping_raw)):
        raise LedgerError("T253 package does not bind the pinned T208 inputs")
    if package_result.get("dispatch_authorized") is not False or package_result.get("provider_called") is not False:
        raise LedgerError("T253 package has an unexpected execution state")

    dispositions = t253.read_jsonl(t208_dir / "dispositions.jsonl")[0]
    ready = t253.validate_dispositions(dispositions)
    mapping = _jsonl(mapping_raw)
    batch_rows = _jsonl(batch_raw)
    if len(mapping) != EXPECTED_COUNTS["ready_requests"] or len(batch_rows) != len(mapping):
        raise LedgerError("T253 candidate and identity map counts differ")

    ready_cases: dict[str, dict[str, Any]] = {}
    ready_refs = [row["case_ref"] for row in ready]
    for ordinal, (identity_row, candidate_row) in enumerate(zip(mapping, batch_rows)):
        if (set(identity_row) != {"custom_id", "case_ref"}
                or identity_row.get("case_ref") != ready_refs[ordinal]
                or candidate_row.get("custom_id") != identity_row.get("custom_id")
                or candidate_row.get("method") != t253.METHOD
                or candidate_row.get("url") != t253.ENDPOINT
                or not isinstance(candidate_row.get("body"), dict)):
            raise LedgerError("T253 map/order does not match the pinned T208 READY records")
        ready_cases[identity_row["case_ref"]] = {
            "case_id": identity_row["case_ref"],
            "package_manifest_sha256": t253.T208_MANIFEST_SHA256,
            "request_body_sha256": sha256(t219.canonical_json(candidate_row["body"])),
            "disposition": "READY",
        }

    errors_by_ref = {row["case_ref"]: row for row in dispositions if row.get("status") == "PREPARATION_ERROR"}
    if len(errors_by_ref) != EXPECTED_COUNTS["retained_preparation_errors"]:
        raise LedgerError("T208 preparation-error identities are incomplete or duplicated")
    error_cases: dict[str, dict[str, Any]] = {}
    for case_ref, row in errors_by_ref.items():
        if row.get("reason") != "TARGET_SLOT_MATCH_MULTIPLE":
            raise LedgerError("T208 error reason is outside the T219 allowlist")
        error_cases[case_ref] = {
            "case_id": case_ref,
            "package_manifest_sha256": t253.T208_MANIFEST_SHA256,
            "disposition": "PREPARATION_ERROR",
            "reason_code": row["reason"],
        }
    case_rows: list[dict[str, Any]] = []
    for row in dispositions:
        case_ref = row["case_ref"]
        if row["status"] == "READY":
            case = ready_cases.get(case_ref)
        else:
            case = error_cases.get(case_ref)
        if case is None:
            raise LedgerError("T208 disposition has no matching T253/T219 case")
        case_rows.append(case)
    if (len(case_rows) != EXPECTED_COUNTS["denominator_cases"]
            or len({row["case_id"] for row in case_rows}) != len(case_rows)):
        raise LedgerError("T208/T253 identities do not form the fixed unique denominator")

    cases_raw = t219.jsonl_bytes(case_rows)
    events_raw = b""  # Preparation is not a dispatch; no intent/submit events are invented.
    states = t219.reconcile_attempts(
        case_rows, [], t253.T208_MANIFEST_SHA256,
        expected_case_inventory_sha256=sha256(cases_raw),
        event_ledger_sha256=sha256(events_raw),
    )
    counts = states.get("state_counts")
    if (states.get("case_count") != EXPECTED_COUNTS["denominator_cases"]
            or counts != {"NOT_STARTED": EXPECTED_COUNTS["ready_requests"],
                          "PREPARATION_ERROR": EXPECTED_COUNTS["retained_preparation_errors"]}):
        raise LedgerError("T219 did not reconcile the prepared fixed cohort")
    return {
        "schema": CONTRACT,
        "protocol_sha256": PROTOCOL_SHA256,
        "t219_source_sha256": T219_SHA256,
        "t253_source_sha256": T253_SHA256,
        "parent_t208_manifest_sha256": t253.T208_MANIFEST_SHA256,
        "parent_t208_dispositions_sha256": t253.T208_DISPOSITIONS_SHA256,
        "t253_manifest_sha256": package_result["manifest_sha256"],
        "t253_batch_requests_sha256": package_result["batch_requests_sha256"],
        "t253_local_mapping_sha256": package_result["local_mapping_sha256"],
        "case_inventory_sha256": sha256(cases_raw),
        "attempt_journal_sha256": sha256(events_raw),
        "counts": EXPECTED_COUNTS,
        "initial_state_counts": counts,
        "remote_attempt_events_created": 0,
        "provider_called": False,
        "dispatch_authorized": False,
        "cohort_selected": False,
    }, cases_raw, events_raw, package_manifest_raw


def _require_separate_paths(root: Path, package_dir: Path, ledger_dir: Path, t253) -> None:
    candidates = [
        root / t253.T208_DIR / "manifest.json",
        root / t253.T208_DIR / "dispositions.jsonl",
        package_dir / "manifest.json",
        package_dir / "batch_requests.jsonl",
        package_dir / "local_only_mapping.jsonl",
    ]
    out = ledger_dir.resolve(strict=False)
    protected_dirs = (package_dir.resolve(), (root / t253.T208_DIR).resolve())
    if any(out == protected or protected in out.parents for protected in protected_dirs):
        raise LedgerError("ledger output directory aliases a pinned input")
    for source in candidates:
        resolved = source.resolve(strict=False)
        if out == resolved or out in resolved.parents or resolved in out.parents:
            raise LedgerError("ledger output directory aliases a pinned input")
        if source.exists() and (source.is_symlink() or not source.is_file()):
            raise LedgerError("pinned package inputs must be ordinary files")
    if package_dir.is_symlink() or not package_dir.is_dir():
        raise LedgerError("T253 package path must be an ordinary directory")


def prepare(root: Path, package_dir: Path, ledger_dir: Path) -> dict[str, Any]:
    if package_dir.is_symlink() or ledger_dir.is_symlink():
        raise LedgerError("package and ledger destinations must not be symlinks")
    root, package_dir = root.resolve(), package_dir.resolve()
    t253, t219 = _dependencies(root)
    _require_separate_paths(root, package_dir, ledger_dir, t253)
    manifest, cases_raw, events_raw, _ = _inputs(root, package_dir, t253, t219)
    ledger_dir = ledger_dir.resolve()
    if ledger_dir.exists() or not ledger_dir.parent.is_dir():
        raise LedgerError("ledger destination exists or parent directory is missing")
    temp_dir = Path(tempfile.mkdtemp(prefix=f".{ledger_dir.name}.pending-", dir=ledger_dir.parent))
    contents = {
        "case_inventory.jsonl": cases_raw,
        "attempt_journal.jsonl": events_raw,
        "ledger_manifest.json": canonical(manifest) + b"\n",
    }
    for name, raw in contents.items():
        with (temp_dir / name).open("xb") as stream:
            stream.write(raw)
            stream.flush()
            os.fsync(stream.fileno())
    # Refuse replacement. The pending directory is deliberately preserved if
    # publication fails so an interrupted preparation is diagnosable.
    try:
        os.rename(temp_dir, ledger_dir)
    except OSError:
        raise LedgerError("ledger publication failed; preserve pending output and stop") from None
    return {"status": "OFFLINE_ATTEMPT_LEDGER_PREPARED", **manifest}


def validate(root: Path, package_dir: Path, ledger_dir: Path) -> dict[str, Any]:
    if package_dir.is_symlink() or ledger_dir.is_symlink():
        raise LedgerError("package and ledger destinations must not be symlinks")
    root, package_dir, ledger_dir = root.resolve(), package_dir.resolve(), ledger_dir.resolve()
    t253, t219 = _dependencies(root)
    _require_separate_paths(root, package_dir, ledger_dir, t253)
    expected, cases_raw, events_raw, _ = _inputs(root, package_dir, t253, t219)
    if any(path.is_symlink() or not path.is_file() for path in ledger_dir.iterdir()):
        raise LedgerError("prepared attempt ledger must contain only ordinary files")
    try:
        inventory_raw = (ledger_dir / "case_inventory.jsonl").read_bytes()
        journal_raw = (ledger_dir / "attempt_journal.jsonl").read_bytes()
        manifest_raw = (ledger_dir / "ledger_manifest.json").read_bytes()
        manifest = _json(manifest_raw)
    except (OSError, ValueError):
        raise LedgerError("prepared attempt ledger is incomplete or invalid") from None
    if (set(path.name for path in ledger_dir.iterdir()) != set(OUTPUT_NAMES)
            or inventory_raw != cases_raw or journal_raw != events_raw
            or manifest != expected or manifest_raw != canonical(manifest) + b"\n"):
        raise LedgerError("prepared attempt ledger differs from its pinned package")
    return {"status": "OFFLINE_ATTEMPT_LEDGER_VALIDATED", **expected}


def run_action(*, root: Path, package_dir: Path, ledger_dir: Path, action: str) -> dict[str, Any]:
    if action == "prepare":
        return prepare(root, package_dir, ledger_dir)
    if action == "validate":
        return validate(root, package_dir, ledger_dir)
    raise LedgerError("offline ledger action is not allowlisted")
