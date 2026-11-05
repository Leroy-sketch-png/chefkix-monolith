"""Build and verify a deterministic offline C2 request-boundary package.

This local-only utility reads only the hash-pinned T203 manifest, READY request
candidates, and companion disposition records. It never contacts a provider.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import subprocess
import tempfile
from pathlib import Path
from types import ModuleType
from typing import Any

PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
T90_DECISION_SHA256 = "d78c61486c26f21780f9830abdaf31078156fd0224e7645f750cd50728c0bd44"
T203_MANIFEST_SHA256 = "c84493e76240f041b4dccb01650be3f0aaa2033ad0b35b1ab9c88b135fa90fcb"
SERIALIZER_SHA256 = "4c5694f378d5b3ae6b1bda6e0733a25926dffaaa235e6ff2292f1b3d9385d829"
SERIALIZER_GIT_COMMIT = "217358f495c96f5c3bd655e0150844e4427f2bb1"
SERIALIZER_GIT_PATH = "ai/tools/preflight_c2_openai_cost.py"
EXPECTED_TOTAL = 10_747
EXPECTED_READY = 10_499
EXPECTED_ERRORS = 248
ALLOWED_ERROR_REASONS = {"TARGET_SLOT_MATCH_MULTIPLE"}
PAYLOAD_KEYS = {"recipe_title", "ingredient_list", "ingredient_to_replace"}
SOURCE_KEYS = {"recipe_title", "ingredients", "target_ingredient"}
SYSTEM_MESSAGE = (
    "You generate ranked ingredient substitutions for a recipe. Given only the recipe title, its ingredient list, "
    "and the ingredient to replace, return exactly 20 distinct ingredient names that could replace that ingredient "
    "in this recipe, ordered from most to least suitable in this recipe context. The ingredient list is an array of slots: "
    "each inner list is one ingredient slot, and its strings are aliases for the same ingredient, not separate ingredients. "
    "Do not infer that the first alias is canonical. The exact ingredient-to-replace spelling identifies one slot; do not return any alias from that entire slot. Do not include the ingredient to "
    "replace itself. Do not include explanations, scores, citations, or any text outside the required JSON object."
)


class PackageError(ValueError):
    """Sanitized package failure; never includes payload or identifier values."""


def load_verified_git_module(
    repository: Path,
    revision: str,
    source_path: str,
    expected_sha256: str,
    module_name: str,
    fallback_source: Path | None = None,
) -> tuple[ModuleType, tempfile.TemporaryDirectory[str]]:
    """Import the frozen serializer from Git, or its verified portable snapshot."""
    repository = repository.resolve()
    if Path(source_path).is_absolute() or ".." in Path(source_path).parts:
        raise PackageError("pinned serializer source path is not repository-relative")
    try:
        result = subprocess.run(
            ["git", "show", f"{revision}:{source_path}"],
            cwd=repository,
            check=False,
            capture_output=True,
            env={**os.environ, "GIT_NO_LAZY_FETCH": "1"},
        )
    except OSError as exc:
        raise PackageError("local Git is unavailable for the frozen serializer") from exc
    if result.returncode != 0:
        if fallback_source is None:
            raise PackageError("pinned serializer source is unavailable in local Git history")
        try:
            source_bytes = fallback_source.read_bytes()
        except OSError as exc:
            raise PackageError("pinned serializer source is unavailable in local Git history and bundle") from exc
    else:
        source_bytes = result.stdout
    if hashlib.sha256(source_bytes).hexdigest() != expected_sha256:
        raise PackageError("pinned serializer source digest mismatch")

    temporary = tempfile.TemporaryDirectory(prefix="chefkix-c2-pinned-serializer-")
    module_path = Path(temporary.name) / Path(source_path).name
    module_path.write_bytes(source_bytes)
    spec = importlib.util.spec_from_file_location(module_name, module_path)
    if spec is None or spec.loader is None:
        temporary.cleanup()
        raise PackageError("pinned serializer module could not be loaded")
    module = importlib.util.module_from_spec(spec)
    try:
        spec.loader.exec_module(module)
    except Exception as exc:
        temporary.cleanup()
        raise PackageError("pinned serializer module failed to load") from exc
    return module, temporary


_REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
serializer, _PINNED_SERIALIZER_TEMP = load_verified_git_module(
    _REPOSITORY_ROOT,
    SERIALIZER_GIT_COMMIT,
    SERIALIZER_GIT_PATH,
    SERIALIZER_SHA256,
    "_chefkix_pinned_c2_serializer",
    fallback_source=_REPOSITORY_ROOT / "_frozen" / "preflight_c2_openai_cost.py",
)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical(value: Any) -> bytes:
    return serializer.canonical_json(value)


def jsonl_bytes(rows: list[dict[str, Any]]) -> bytes:
    return b"".join(canonical(row) + b"\n" for row in rows)


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError):
        raise PackageError("pinned JSON input could not be parsed") from None
    if not isinstance(value, dict):
        raise PackageError("pinned JSON input is not an object")
    return value


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    rows = []
    try:
        with path.open("r", encoding="utf-8") as stream:
            for line in stream:
                item = json.loads(line)
                if not isinstance(item, dict):
                    raise PackageError("case record is not an object")
                rows.append(item)
    except (OSError, UnicodeError, json.JSONDecodeError):
        raise PackageError("pinned JSONL input could not be parsed") from None
    return rows


def validate_source_payload(source: Any, payload: Any) -> None:
    if not isinstance(source, dict) or set(source) != SOURCE_KEYS:
        raise PackageError("READY source fields differ from the exact allowlist")
    if not isinstance(payload, dict) or set(payload) != PAYLOAD_KEYS:
        raise PackageError("READY payload fields differ from the exact allowlist")
    if (
        payload.get("recipe_title") != source.get("recipe_title")
        or payload.get("ingredient_list") != source.get("ingredients")
        or payload.get("ingredient_to_replace") != source.get("target_ingredient")
    ):
        raise PackageError("READY payload is not the exact approved source mapping")
    if any(not isinstance(payload.get(key), str) or not payload[key].strip()
           for key in ("recipe_title", "ingredient_to_replace")):
        raise PackageError("READY text fields are malformed")
    groups = payload.get("ingredient_list")
    if not isinstance(groups, list) or not groups or any(
        not isinstance(group, list) or not group
        or any(not isinstance(alias, str) or not alias.strip() for alias in group)
        for group in groups
    ):
        raise PackageError("READY ingredient slots are malformed")
    target = payload["ingredient_to_replace"]
    if sum(any(alias == target for alias in group) for group in groups) != 1:
        raise PackageError("READY exact target must resolve to one nested slot")


def validate_record_shapes(rows: list[dict[str, Any]], *, generated: bool) -> None:
    seen: set[str] = set()
    for row in rows:
        if generated and row.get("status") == "PREPARATION_ERROR":
            if set(row) != {"case_ref", "reason", "status"}:
                raise PackageError("preparation-error shape is not strictly sanitized")
            identity = row.get("case_ref")
            reason = row.get("reason")
            if not isinstance(identity, str) or not identity or identity in seen:
                raise PackageError("preparation-error identity is invalid or duplicated")
            if not isinstance(reason, str) or reason not in ALLOWED_ERROR_REASONS:
                raise PackageError("preparation-error reason is not allowlisted")
            seen.add(identity)
            continue
        if generated:
            if set(row) != {"case_id", "case_ref", "payload", "source_fields", "status"} or row.get("status") != "READY":
                raise PackageError("generated READY record shape/status is invalid")
            identity = row.get("case_ref")
            if row.get("case_id") != identity:
                raise PackageError("READY case reference and candidate identity differ")
        else:
            if set(row) != {"case_id", "payload", "source_fields"}:
                raise PackageError("request candidate shape differs from the reviewed contract")
            identity = row.get("case_id")
        if not isinstance(identity, str) or not identity or identity in seen:
            raise PackageError("READY identity is invalid or duplicated")
        seen.add(identity)
        validate_source_payload(row.get("source_fields"), row.get("payload"))


def validate_partition_inventory(parts: Any, expected_count: int = 54) -> list[dict[str, Any]]:
    if not isinstance(parts, list) or len(parts) != expected_count:
        raise PackageError("T203 partition inventory mismatch")
    for index, part in enumerate(parts):
        if not isinstance(part, dict) or part.get("partition_index") != index:
            raise PackageError("T203 partition order mismatch")
    return parts


def validate_partition_hashes(part: dict[str, Any], companion_hash: str, candidate_hash: str) -> None:
    if companion_hash != part.get("case_records_sha256") or candidate_hash != part.get("request_candidates_sha256"):
        raise PackageError("partition input hash differs from T203 manifest")


def serialize_cohort(
    companion_partitions: list[list[dict[str, Any]]],
    candidate_partitions: list[list[dict[str, Any]]],
    parent_manifest: dict[str, Any],
    *,
    expected_partitions: int = 54,
    expected_total: int = EXPECTED_TOTAL,
    expected_ready: int = EXPECTED_READY,
    expected_errors: int = EXPECTED_ERRORS,
) -> tuple[bytes, bytes, dict[str, Any]]:
    if len(companion_partitions) != expected_partitions or len(candidate_partitions) != expected_partitions:
        raise PackageError("exactly 54 companion/candidate partitions are required")
    request_rows: list[dict[str, Any]] = []
    disposition_rows: list[dict[str, Any]] = []
    all_identities: list[str] = []
    errors: dict[str, int] = {}
    seen: set[str] = set()
    for companions, candidates in zip(companion_partitions, candidate_partitions):
        validate_record_shapes(companions, generated=True)
        validate_record_shapes(candidates, generated=False)
        candidate_by_id = {candidate["case_id"]: candidate for candidate in candidates}
        ready_ids = set()
        for row in companions:
            identity = row["case_ref"]
            if identity in seen:
                raise PackageError("cohort contains duplicate local case identity")
            seen.add(identity)
            all_identities.append(identity)
            if row["status"] == "PREPARATION_ERROR":
                errors[row["reason"]] = errors.get(row["reason"], 0) + 1
                disposition_rows.append({"case_ref": identity, "status": "PREPARATION_ERROR", "reason": row["reason"]})
                continue
            candidate = candidate_by_id.get(row["case_id"])
            if candidate is None or candidate["case_id"] in ready_ids:
                raise PackageError("READY record lacks exactly one matching request candidate")
            if candidate["source_fields"] != row["source_fields"] or candidate["payload"] != row["payload"]:
                raise PackageError("request candidate differs from its pinned READY record")
            ready_ids.add(candidate["case_id"])
            body = serializer.request_body(
                {"system_message": SYSTEM_MESSAGE, "max_output_tokens": 256},
                candidate["payload"],
            )
            body_json = canonical(body)
            # Parse the serialized request to assert local identity is absent.
            decoded = json.loads(body_json.decode("utf-8"))
            if "case_id" in decoded or "case_ref" in decoded or "case_ref" in body_json.decode("utf-8"):
                raise PackageError("local identity leaked into request body")
            request_rows.append(decoded)
            disposition_rows.append({
                "case_ref": identity,
                "status": "READY",
                "request_ordinal": len(request_rows) - 1,
            })
        if ready_ids != set(candidate_by_id):
            raise PackageError("request candidates contain missing or unmatched READY records")

    if len(all_identities) != expected_total or len(seen) != expected_total:
        raise PackageError("fixed denominator or unique identity count mismatch")
    ready_count = len(request_rows)
    error_count = sum(errors.values())
    if ready_count != expected_ready or error_count != expected_errors:
        raise PackageError("fixed READY/error counts mismatch")
    if errors != parent_manifest.get("preparation_error_reasons"):
        raise PackageError("preparation-error reason totals differ from parent manifest")
    identity_digest = hashlib.sha256(("\n".join(all_identities) + "\n").encode("utf-8")).hexdigest()
    if identity_digest != parent_manifest.get("case_refs_sha256"):
        raise PackageError("ordered case-reference digest differs from parent manifest")
    counts = {
        "denominator_cases": len(all_identities),
        "ready_cases": ready_count,
        "preparation_error_cases": error_count,
        "preparation_error_reasons": errors,
        "case_refs_sha256": identity_digest,
    }
    return jsonl_bytes(request_rows), jsonl_bytes(disposition_rows), counts


def load_pinned_inputs(manifest_path: Path) -> tuple[dict[str, Any], list[list[dict[str, Any]]], list[list[dict[str, Any]]], dict[str, str], dict[str, str]]:
    manifest_path = manifest_path.resolve()
    if sha256_file(manifest_path) != T203_MANIFEST_SHA256:
        raise PackageError("T203 parent manifest hash mismatch")
    parent = read_json(manifest_path)
    if (
        parent.get("protocol_sha256") != PROTOCOL_SHA256
        or parent.get("state") != "LOCAL_ONLY_C2_API_SOURCE_CANDIDATE_NOT_SELECTED_OR_UPLOAD_AUTHORIZED"
        or parent.get("cohort_count") != EXPECTED_TOTAL
        or parent.get("ready_count") != EXPECTED_READY
        or parent.get("preparation_error_count") != EXPECTED_ERRORS
        or parent.get("cohort_selected") is not False
        or parent.get("provider_called") is not False
        or parent.get("upload_authorized") is not False
    ):
        raise PackageError("T203 parent state/count contract mismatch")
    parts = validate_partition_inventory(parent.get("partitions"))
    companions: list[list[dict[str, Any]]] = []
    candidates: list[list[dict[str, Any]]] = []
    companion_hashes: dict[str, str] = {}
    candidate_hashes: dict[str, str] = {}
    base = manifest_path.parent
    for index, part in enumerate(parts):
        directory = base / "partitions" / f"test-{index:03d}"
        companion_path = directory / "case_records.jsonl"
        candidate_path = directory / "request_candidates.jsonl"
        for path in (companion_path, candidate_path):
            if not path.is_file():
                raise PackageError("required pinned partition file is missing")
        companion_hash = sha256_file(companion_path)
        candidate_hash = sha256_file(candidate_path)
        validate_partition_hashes(part, companion_hash, candidate_hash)
        c_rows = read_jsonl(companion_path)
        r_rows = read_jsonl(candidate_path)
        if len(c_rows) != part.get("case_count") or len(r_rows) != part.get("ready_count"):
            raise PackageError("partition record count differs from T203 manifest")
        if sum(1 for row in c_rows if row.get("status") == "READY") != part.get("ready_count"):
            raise PackageError("partition READY count differs from T203 manifest")
        if sum(1 for row in c_rows if row.get("status") == "PREPARATION_ERROR") != part.get("preparation_error_count"):
            raise PackageError("partition error count differs from T203 manifest")
        companions.append(c_rows)
        candidates.append(r_rows)
        companion_hashes[f"test-{index:03d}"] = companion_hash
        candidate_hashes[f"test-{index:03d}"] = candidate_hash
    return parent, companions, candidates, companion_hashes, candidate_hashes


def build(manifest_path: Path, output_dir: Path) -> dict[str, Any]:
    output_dir = output_dir.resolve()
    if output_dir.exists():
        raise PackageError("output path already exists; refusing overwrite")
    if not output_dir.parent.is_dir():
        raise PackageError("output parent directory is missing")
    parent, companions, candidates, companion_hashes, candidate_hashes = load_pinned_inputs(manifest_path)
    request_bytes, index_bytes, counts = serialize_cohort(companions, candidates, parent)
    serializer_path = Path(serializer.__file__).resolve()
    if sha256_file(serializer_path) != SERIALIZER_SHA256:
        raise PackageError("canonical serializer source hash mismatch")
    builder_hash = sha256_file(Path(__file__).resolve())
    temp_dir = Path(tempfile.mkdtemp(prefix=f".{output_dir.name}.pending-", dir=output_dir.parent))
    try:
        request_path = temp_dir / "request_bodies.jsonl"
        index_path = temp_dir / "dispositions.jsonl"
        request_path.write_bytes(request_bytes)
        index_path.write_bytes(index_bytes)
        for path in (request_path, index_path):
            # Windows does not permit fsync on a read-only handle. Open the
            # already-written file for read/write so the durability barrier
            # is portable without changing its bytes or package semantics.
            with path.open("r+b") as stream:
                os.fsync(stream.fileno())
        inventory = {
            path.name: {"bytes": path.stat().st_size, "sha256": sha256_file(path)}
            for path in (request_path, index_path)
        }
        package_manifest = {
            "schema": "C2_OFFLINE_REQUEST_BOUNDARY_PACKAGE_V1",
            "status": "LOCAL_ONLY_NOT_SELECTED_NOT_AUTHORIZED_FOR_DISPATCH",
            "protocol_sha256": PROTOCOL_SHA256,
            "t90_decision_sha256": T90_DECISION_SHA256,
            "parent_t203_manifest_sha256": T203_MANIFEST_SHA256,
            "serializer_source_sha256": SERIALIZER_SHA256,
            "builder_sha256": builder_hash,
            "request_candidate_partition_sha256": candidate_hashes,
            "companion_case_record_partition_sha256": companion_hashes,
            "counts": counts,
            "output_inventory": inventory,
            "cohort_selected": False,
            "dispatch_authorized": False,
            "provider_called": False,
            "model_input_tokens": None,
            "model_input_token_cost_usd": None,
        }
        manifest_path_out = temp_dir / "manifest.json"
        manifest_path_out.write_bytes(json.dumps(package_manifest, sort_keys=True, indent=2).encode("utf-8") + b"\n")
        with manifest_path_out.open("r+b") as stream:
            os.fsync(stream.fileno())
        os.rename(temp_dir, output_dir)
    except Exception:
        # Preserve the temporary directory for diagnosis; never retry here.
        raise
    return package_manifest


def validate_saved_package(manifest_path: Path, package_dir: Path) -> dict[str, Any]:
    package_dir = package_dir.resolve()
    package_manifest_path = package_dir / "manifest.json"
    if not package_manifest_path.is_file():
        raise PackageError("saved package manifest is missing")
    saved = read_json(package_manifest_path)
    expected_names = {"request_bodies.jsonl", "dispositions.jsonl"}
    if set(saved.get("output_inventory", {})) != expected_names:
        raise PackageError("saved package inventory differs from exact expected files")
    for name in expected_names:
        path = package_dir / name
        record = saved["output_inventory"][name]
        if not path.is_file() or path.stat().st_size != record.get("bytes") or sha256_file(path) != record.get("sha256"):
            raise PackageError("saved output inventory/hash mismatch")
    parent, companions, candidates, companion_hashes, candidate_hashes = load_pinned_inputs(manifest_path)
    expected_request, expected_index, counts = serialize_cohort(companions, candidates, parent)
    if (package_dir / "request_bodies.jsonl").read_bytes() != expected_request:
        raise PackageError("saved request body serialization is not deterministic")
    if (package_dir / "dispositions.jsonl").read_bytes() != expected_index:
        raise PackageError("saved disposition index differs from frozen records")
    serializer_path = Path(serializer.__file__).resolve()
    expected = {
        "schema": "C2_OFFLINE_REQUEST_BOUNDARY_PACKAGE_V1",
        "status": "LOCAL_ONLY_NOT_SELECTED_NOT_AUTHORIZED_FOR_DISPATCH",
        "protocol_sha256": PROTOCOL_SHA256,
        "t90_decision_sha256": T90_DECISION_SHA256,
        "parent_t203_manifest_sha256": T203_MANIFEST_SHA256,
        "serializer_source_sha256": SERIALIZER_SHA256,
        "builder_sha256": sha256_file(Path(__file__).resolve()),
        "request_candidate_partition_sha256": candidate_hashes,
        "companion_case_record_partition_sha256": companion_hashes,
        "counts": counts,
        "output_inventory": saved["output_inventory"],
        "cohort_selected": False,
        "dispatch_authorized": False,
        "provider_called": False,
        "model_input_tokens": None,
        "model_input_token_cost_usd": None,
    }
    if sha256_file(serializer_path) != SERIALIZER_SHA256 or saved != expected:
        raise PackageError("saved package manifest does not match exact current inputs/source/output")
    return {
        "status": "OFFLINE_PACKAGE_VALIDATED_LOCAL_ONLY",
        "denominator_cases": counts["denominator_cases"],
        "ready_bodies": counts["ready_cases"],
        "preparation_errors": counts["preparation_error_cases"],
        "preparation_error_reasons": counts["preparation_error_reasons"],
        "request_body_sha256": saved["output_inventory"]["request_bodies.jsonl"]["sha256"],
        "disposition_index_sha256": saved["output_inventory"]["dispositions.jsonl"]["sha256"],
        "manifest_sha256": sha256_file(package_manifest_path),
        "cohort_selected": False,
        "dispatch_authorized": False,
        "model_input_tokens": None,
        "model_input_token_cost_usd": None,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--build", action="store_true")
    mode.add_argument("--validate", action="store_true")
    parser.add_argument("--parent-manifest", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    try:
        result = build(args.parent_manifest, args.output_dir) if args.build else validate_saved_package(args.parent_manifest, args.output_dir)
    except (OSError, PackageError, serializer.PreflightError) as exc:
        parser.exit(1, f"T208 stopped: {exc}\n")
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
