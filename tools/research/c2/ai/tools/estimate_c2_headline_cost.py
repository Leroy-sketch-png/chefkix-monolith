"""Produce a local-only, assumption-bounded cost forecast for frozen C2 package-v3.

This is not an exact token count, hard upper bound, account balance, authorization,
cohort selection, reservation, or dispatch path. It reads the already materialized
source-only request bodies, emits aggregate values only, and never contacts a provider.
Requires tiktoken==0.14.0 (o200k_base) for a reproducible local token proxy.
The encoding blob is hash-pinned and must already exist in an explicit cache;
the loader is guarded against network fallback on cache miss.
"""

from __future__ import annotations

import argparse
import contextlib
import hashlib
import importlib.util
import json
import math
import os
import subprocess
import sys
import tempfile
from decimal import Decimal, ROUND_CEILING
from pathlib import Path
from typing import Any, Callable

try:
    from . import build_senath_c2_offline_request_package as _request_package
    from .build_senath_c2_offline_request_package import PackageError, validate_saved_package
except ImportError:  # direct script execution from ai/tools/
    import build_senath_c2_offline_request_package as _request_package
    from build_senath_c2_offline_request_package import PackageError, validate_saved_package


PACKAGE_SCHEMA = "C2_OFFLINE_REQUEST_BOUNDARY_PACKAGE_V1"
T203_MANIFEST_SHA256 = "c84493e76240f041b4dccb01650be3f0aaa2033ad0b35b1ab9c88b135fa90fcb"
T208_VALIDATOR_SHA256 = "4d454678575fc81244deb4fe2043be3eda83fe47a9a5a65f4f66bf05a1a0dabf"
MODEL = "gpt-4o-2024-08-06"
T203_CASE_COUNT = 10_747
T203_READY_COUNT = 10_499
T203_ERROR_COUNT = 248
PACKAGE_SHA256 = "82973520e41a41160eb938ab9921fff64046bfe5cc133b6e05456dae7a8030d1"
PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
TOKENIZER_NAME = "o200k_base"
TOKENIZER_PACKAGE_VERSION = "0.14.0"
TOKENIZER_BLOB_URL = "https://openaipublic.blob.core.windows.net/encodings/o200k_base.tiktoken"
TOKENIZER_BLOB_SHA256 = "446a9538cb6c348e3516120d7c08b09f57c36495e2acfffe59a5bf8b0cfb1a2d"
INPUT_USD_PER_MILLION = Decimal("2.50")
OUTPUT_USD_PER_MILLION = Decimal("10.00")
OUTPUT_CAP_PER_REQUEST = 256
INPUT_UPLIFT_NUMERATOR = 3  # 50% scenario uplift over the local tokenizer proxy.
INPUT_UPLIFT_DENOMINATOR = 2
FRAMING_ALLOWANCE_PER_REQUEST = 256  # Explicit planning assumption, not a published bound.
RESERVE_NUMERATOR = 6  # 20% planning reserve over the rate-card estimate.
RESERVE_DENOMINATOR = 5
FULL_TEST_FRACTION_NUMERATOR = 4  # Protocol requires forecast <= 80% of residual budget.
FULL_TEST_FRACTION_DENOMINATOR = 5
FROZEN_SERIALIZER_COMMIT = "217358f495c96f5c3bd655e0150844e4427f2bb1"
FROZEN_SERIALIZER_PATH = "ai/tools/preflight_c2_openai_cost.py"
FROZEN_SERIALIZER_SHA256 = "4c5694f378d5b3ae6b1bda6e0733a25926dffaaa235e6ff2292f1b3d9385d829"


class ForecastError(ValueError):
    pass


@contextlib.contextmanager
def use_frozen_package_serializer():
    """Validate T208 with its exact pinned serializer, without changing the package."""
    repository_root = Path(__file__).resolve().parents[2]
    revision = f"{FROZEN_SERIALIZER_COMMIT}:{FROZEN_SERIALIZER_PATH}"
    try:
        result = subprocess.run(
            ["git", "show", revision],
            cwd=repository_root,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
            env={**os.environ, "GIT_NO_LAZY_FETCH": "1"},
        )
    except OSError as exc:
        raise ForecastError("could not read the frozen serializer from local Git history") from exc
    if result.returncode != 0:
        snapshot = Path(__file__).resolve().parents[2] / "_frozen" / "preflight_c2_openai_cost.py"
        try:
            source_bytes = snapshot.read_bytes()
        except OSError as exc:
            raise ForecastError("pinned frozen serializer is unavailable in Git history and bundle") from exc
    else:
        source_bytes = result.stdout
    if hashlib.sha256(source_bytes).hexdigest() != FROZEN_SERIALIZER_SHA256:
        raise ForecastError("pinned frozen serializer Git object failed its SHA-256 check")

    previous_serializer = _request_package.serializer
    with tempfile.TemporaryDirectory(prefix="chefkix-t208-frozen-serializer-") as temp_dir:
        module_path = Path(temp_dir) / "preflight_c2_openai_cost.py"
        module_path.write_bytes(source_bytes)
        spec = importlib.util.spec_from_file_location(
            "preflight_c2_openai_cost_t208_frozen", module_path
        )
        if spec is None or spec.loader is None:
            raise ForecastError("could not load the hash-verified frozen serializer")
        frozen_serializer = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(frozen_serializer)
        _request_package.serializer = frozen_serializer
        try:
            yield
        finally:
            _request_package.serializer = previous_serializer


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def ceil_decimal(value: Decimal) -> Decimal:
    return value.quantize(Decimal("0.01"), rounding=ROUND_CEILING)


def forecast_cost(
    *,
    tokenizer_proxy_tokens: int,
    request_count: int,
    output_cap_tokens: int,
    input_rate: Decimal = INPUT_USD_PER_MILLION,
    output_rate: Decimal = OUTPUT_USD_PER_MILLION,
) -> dict[str, Any]:
    if any(isinstance(value, bool) or value < 0 for value in (tokenizer_proxy_tokens, request_count, output_cap_tokens)):
        raise ForecastError("token and request counts must be nonnegative integers")
    if any(not isinstance(value, int) for value in (tokenizer_proxy_tokens, request_count, output_cap_tokens)):
        raise ForecastError("token and request counts must be integers")
    padded_tokens = math.ceil(tokenizer_proxy_tokens * INPUT_UPLIFT_NUMERATOR / INPUT_UPLIFT_DENOMINATOR)
    framing_tokens = request_count * FRAMING_ALLOWANCE_PER_REQUEST
    estimated_input_tokens = padded_tokens + framing_tokens
    output_ceiling = request_count * output_cap_tokens
    input_cost = Decimal(estimated_input_tokens) * input_rate / Decimal(1_000_000)
    output_cost = Decimal(output_ceiling) * output_rate / Decimal(1_000_000)
    # Use displayed upward-rounded components as the reserve base so the
    # relationship between every reported amount is arithmetically exact.
    displayed_input_cost = ceil_decimal(input_cost)
    displayed_output_cost = ceil_decimal(output_cost)
    subtotal = displayed_input_cost + displayed_output_cost
    with_reserve = subtotal * RESERVE_NUMERATOR / RESERVE_DENOMINATOR
    required_residual = with_reserve * FULL_TEST_FRACTION_DENOMINATOR / FULL_TEST_FRACTION_NUMERATOR
    return {
        "tokenizer_proxy_input_tokens": tokenizer_proxy_tokens,
        "input_uplift_fraction": "0.50",
        "framing_allowance_tokens_per_request": FRAMING_ALLOWANCE_PER_REQUEST,
        "framing_allowance_is_published_bound": False,
        "estimated_input_tokens_with_assumptions": estimated_input_tokens,
        "output_cap_tokens_per_request": output_cap_tokens,
        "output_tokens_at_cap": output_ceiling,
        "input_rate_usd_per_million": str(input_rate),
        "output_rate_usd_per_million": str(output_rate),
        "estimated_input_cost_usd": str(displayed_input_cost),
        "estimated_output_cost_usd": str(displayed_output_cost),
        "estimated_subtotal_usd": str(subtotal),
        "reserve_fraction": "0.20",
        "forecast_with_reserve_usd": str(ceil_decimal(with_reserve)),
        "scenario_residual_budget_at_protocol_80_percent_threshold_usd": str(ceil_decimal(required_residual)),
        "forecast_is_hard_upper_bound": False,
    }


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    with path.open("r", encoding="utf-8") as stream:
        for line_number, line in enumerate(stream, start=1):
            if not line.strip():
                raise ForecastError(f"blank request-body line at {line_number}")
            value = json.loads(line)
            if not isinstance(value, dict):
                raise ForecastError(f"request body at line {line_number} is not an object")
            records.append(value)
    return records


def build_report(
    package_manifest_path: Path,
    *,
    encode: Callable[[str], list[int]],
    tokenizer_version: str,
) -> dict[str, Any]:
    package_manifest_path = package_manifest_path.resolve()
    package_root = package_manifest_path.parent
    if sha256(package_manifest_path) != PACKAGE_SHA256:
        raise ForecastError("T208 package manifest hash does not match its frozen pin")
    package_manifest = json.loads(package_manifest_path.read_text(encoding="utf-8"))
    if (
        package_manifest.get("schema") != PACKAGE_SCHEMA
        or package_manifest.get("protocol_sha256") != PROTOCOL_SHA256
        or package_manifest.get("parent_t203_manifest_sha256") != T203_MANIFEST_SHA256
        or package_manifest.get("cohort_selected") is not False
        or package_manifest.get("dispatch_authorized") is not False
        or package_manifest.get("provider_called") is not False
    ):
        raise ForecastError("T208 manifest identity or local-only state differs from the pinned contract")
    parent_manifest_path = package_root.parents[3] / "ai" / "out" / "T203" / "full-test-source-only-v1" / "manifest.json"
    if not parent_manifest_path.is_file():
        raise ForecastError("pinned T203 parent package manifest is unavailable at the expected workspace-relative location")
    with use_frozen_package_serializer():
        package_check = validate_saved_package(parent_manifest_path, package_root)
    expected_counts = {
        "denominator_cases": T203_CASE_COUNT,
        "ready_bodies": T203_READY_COUNT,
        "preparation_errors": T203_ERROR_COUNT,
    }
    if package_manifest.get("builder_sha256") != T208_VALIDATOR_SHA256:
        raise ForecastError("T208 builder/validator source hash differs from its frozen implementation pin")
    if {key: package_check.get(key) for key in expected_counts} != expected_counts:
        raise ForecastError("T208 validator-returned counts differ from the frozen headline contract")
    manifest_counts = {
        "denominator_cases": package_manifest.get("counts", {}).get("denominator_cases"),
        "ready_bodies": package_manifest.get("counts", {}).get("ready_cases"),
        "preparation_errors": package_manifest.get("counts", {}).get("preparation_error_cases"),
    }
    if manifest_counts != expected_counts:
        raise ForecastError("T208 manifest counts differ from the frozen headline contract")
    inventory = package_manifest.get("output_inventory")
    if not isinstance(inventory, dict) or set(inventory) != {"request_bodies.jsonl", "dispositions.jsonl"}:
        raise ForecastError("T208 output inventory differs from the frozen package contract")
    request_ref = inventory["request_bodies.jsonl"]
    body_path = (package_root / "request_bodies.jsonl").resolve()
    body_path.relative_to(package_root.resolve())
    if not body_path.is_file() or sha256(body_path) != request_ref.get("sha256"):
        raise ForecastError("T208 request-body file is missing or its pinned hash changed")
    if body_path.stat().st_size != request_ref.get("bytes"):
        raise ForecastError("T208 request-body byte count differs from the pinned inventory")

    bodies = read_jsonl(body_path)
    if len(bodies) != package_check["ready_bodies"]:
        raise ForecastError("requestable-body count differs from frozen denominator disposition")
    token_counts: list[int] = []
    for index, body in enumerate(bodies, start=1):
        if body.get("model") != MODEL or body.get("temperature") != 0 or body.get("max_output_tokens") != OUTPUT_CAP_PER_REQUEST:
            raise ForecastError(f"request body {index} differs from the frozen model/runtime parameters")
        if body.get("store") is not False or "case_id" in body or "case_ref" in body:
            raise ForecastError(f"request body {index} crosses the local identity/storage boundary")
        encoded = json.dumps(body, ensure_ascii=False, separators=(",", ":"))
        token_counts.append(len(encode(encoded)))

    total_proxy = sum(token_counts)
    ordered_count_hash = hashlib.sha256(("\n".join(str(count) for count in token_counts) + "\n").encode("ascii")).hexdigest()
    estimated = forecast_cost(
        tokenizer_proxy_tokens=total_proxy,
        request_count=len(bodies),
        output_cap_tokens=OUTPUT_CAP_PER_REQUEST,
    )
    return {
        "status": "LOCAL_FORECAST_ONLY",
        "evidence_grade": "E1 local deterministic estimate over a hash-pinned candidate package",
        "protocol_sha256": PROTOCOL_SHA256,
        "model": MODEL,
        "package": {
            "manifest_sha256": PACKAGE_SHA256,
            "request_bodies_sha256": request_ref["sha256"],
            "request_bodies_bytes": request_ref["bytes"],
            "denominator_cases": T203_CASE_COUNT,
            "ready_requests": T203_READY_COUNT,
            "preparation_errors_retained_in_denominator": T203_ERROR_COUNT,
            "ready_body_ordered_token_count_sha256": ordered_count_hash,
        },
        "token_proxy": {
            "library": "tiktoken",
            "version": tokenizer_version,
            "encoding": TOKENIZER_NAME,
            "encoding_blob_sha256": TOKENIZER_BLOB_SHA256,
            "proxy_scope": "canonical serialized candidate request JSON; not server-computed model input tokens",
            "total_tokens": total_proxy,
            "minimum_per_request": min(token_counts, default=0),
            "median_per_request": sorted(token_counts)[len(token_counts) // 2] if token_counts else 0,
            "maximum_per_request": max(token_counts, default=0),
            "request_count": len(token_counts),
        },
        "cost_scenario": estimated,
        "decision": {
            "forecast_is_dispatch_gate": False,
            "cohort_selected": False,
            "dispatch_authorized": False,
            "account_capacity_observed": False,
            "billing_or_call_cap_verified": False,
            "transfer_rights_verified": False,
            "reservation_installed": False,
            "interpretation": "planning scenario only; the 50% tokenizer uplift and 256-token/request framing allowance are assumptions, not published bounds; reserve does not convert this into a hard maximum or prove affordability",
            "remaining_gates": [
                "fresh authorized account balance and itemized mission ledger",
                "verified model availability and provider billing/call-cap treatment",
                "transfer-rights gate for the frozen three-field payload",
                "lead selection under Protocol v1 after current evidence review",
                "separate reservation and dispatch authorization",
            ],
        },
        "source_validation": {
            "t208_package_validator": package_check["status"],
            "t203_parent_manifest_sha256": T203_MANIFEST_SHA256,
            "t208_validator_source_sha256": T208_VALIDATOR_SHA256,
            "serializer_source_sha256": FROZEN_SERIALIZER_SHA256,
            "serializer_source_commit": FROZEN_SERIALIZER_COMMIT,
        },
    }


def load_offline_encoder() -> tuple[Callable[[str], list[int]], str]:
    """Load the pinned encoding only from a verified explicit cache; never fetch."""
    cache_dir = os.environ.get("TIKTOKEN_CACHE_DIR")
    if not cache_dir:
        raise ForecastError("TIKTOKEN_CACHE_DIR must point to an explicit pre-staged offline cache")
    cache_path = Path(cache_dir).resolve() / hashlib.sha1(TOKENIZER_BLOB_URL.encode("utf-8")).hexdigest()
    if not cache_path.is_file() or sha256(cache_path) != TOKENIZER_BLOB_SHA256:
        raise ForecastError("pinned o200k_base encoding blob is absent or has the wrong SHA-256; refusing network fallback")

    import tiktoken
    import tiktoken.load

    if tiktoken.__version__ != TOKENIZER_PACKAGE_VERSION:
        raise ForecastError(
            f"tiktoken version must be {TOKENIZER_PACKAGE_VERSION}; found {tiktoken.__version__}"
        )

    original_read_file = tiktoken.load.read_file

    def refuse_network(url: str) -> bytes:
        raise ForecastError("tokenizer cache miss attempted a network read; offline guard refused it")

    tiktoken.load.read_file = refuse_network
    try:
        encoder = tiktoken.get_encoding(TOKENIZER_NAME)
    finally:
        tiktoken.load.read_file = original_read_file
    return lambda value: encoder.encode(value, disallowed_special=()), tiktoken.__version__


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--package-manifest", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        encode, tokenizer_version = load_offline_encoder()
        report = build_report(
            args.package_manifest,
            encode=encode,
            tokenizer_version=tokenizer_version,
        )
    except (ImportError, OSError, json.JSONDecodeError, ForecastError, PackageError, TypeError, KeyError, ValueError) as exc:
        report = {"status": "BLOCKED", "dispatch_authorized": False, "cohort_selected": False, "error": str(exc)}
    serialized = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        try:
            with args.output.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(serialized)
        except FileExistsError:
            sys.stderr.write(f"refusing to overwrite existing output: {args.output}\n")
            return 2
    else:
        sys.stdout.write(serialized)
    return 0 if report.get("status") == "LOCAL_FORECAST_ONLY" else 2


if __name__ == "__main__":
    raise SystemExit(main())
