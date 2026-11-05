#!/usr/bin/env python3
"""Compose the genuine blocked C2 preflight with offline count-receipt checks.

This adapter does not obtain or authenticate receipts, estimate cost, select a
cohort, reserve budget, or authorize dispatch. It requires the genuine local
preflight to remain blocked and keeps its readiness result unchanged.
"""

from __future__ import annotations

import argparse
import json
import sys
import tempfile
from datetime import datetime
from pathlib import Path
from typing import Any

if __package__:
    from .preflight_c2_openai_cost import PreflightError, preflight
    from .validate_c2_input_token_receipts import ReceiptError, validate_receipts
else:  # direct script execution from ai/tools/
    from preflight_c2_openai_cost import PreflightError, preflight
    from validate_c2_input_token_receipts import ReceiptError, validate_receipts


def compose_preflight_and_receipts(
    manifest_path: Path,
    receipt_path: Path,
    *,
    now: datetime | None = None,
) -> dict[str, Any]:
    """Return preflight plus consistency evidence bound to its own request hashes."""
    report = preflight(manifest_path, now=now)
    if report.get("status") != "BLOCKED":
        raise PreflightError(
            "token-count consistency requires the existing blocked preflight; "
            "a different preflight state is not accepted"
        )
    if report.get("dispatch_authorized") is not False or report.get("cohort_selected") is not False:
        raise PreflightError("preflight unexpectedly selected a cohort or authorized dispatch")
    if (
        report.get("selected_cohort", "absent") is not None
        or report.get("conditional_scenario_if_receipts_are_true", "absent") is not None
        or report.get("reservation_installed", False) is not False
        or report.get("model_input_tokens") is not None
        or report.get("model_input_token_cost_usd") is not None
        or any(
            isinstance(report.get(field), dict)
            and (
                report[field].get("model_input_tokens") is not None
                or report[field].get("model_input_token_cost_usd") is not None
            )
            for field in ("full_test_request_size", "fallback_2000_request_size")
        )
    ):
        raise PreflightError("receipt composition requires selection, reservation, and token costs to remain unresolved")

    # T354 accepts a report path. Generate it from this call's actual preflight,
    # rather than allowing a caller to substitute a different hash map.
    with tempfile.TemporaryDirectory(prefix="c2-preflight-receipts-") as temp_dir:
        report_path = Path(temp_dir) / "preflight-report.json"
        report_path.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
            newline="\n",
        )
        receipt_check = validate_receipts(report_path, receipt_path)

    return {
        "status": "BLOCKED",
        "dispatch_authorized": False,
        "cohort_selected": False,
        "selected_cohort": None,
        "preflight": report,
        "input_token_receipt_consistency": receipt_check,
        "interpretation_boundary": (
            "preflight remains authoritative for readiness; receipt consistency only "
            "matches caller-supplied counts to hashes generated from the pinned local "
            "manifest and does not authenticate captures, transport, billing/call-cap "
            "treatment, rights, account capacity, or dispatch"
        ),
        "required_next_action": (
            "independently resolve endpoint billing/call-cap, transfer rights, account "
            "capacity and authorization; do not select, reserve, or dispatch from this report"
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--input-token-receipts", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    try:
        result = compose_preflight_and_receipts(args.manifest, args.input_token_receipts)
        code = 0
    except (OSError, json.JSONDecodeError, PreflightError, ReceiptError, TypeError, KeyError, AttributeError) as exc:
        result = {
            "status": "BLOCKED",
            "dispatch_authorized": False,
            "cohort_selected": False,
            "error": str(exc),
        }
        code = 2
    serialized = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        try:
            with args.output.open("x", encoding="utf-8", newline="\n") as stream:
                stream.write(serialized)
        except FileExistsError:
            sys.stderr.write(f"refusing to overwrite existing output: {args.output}\n")
            return 2
    else:
        sys.stdout.write(serialized)
    if code == 0 and result.get("input_token_receipt_consistency", {}).get("status") != "LOCAL_CONSISTENCY_CHECK_ONLY_UNAUTHENTICATED_INPUTS":
        return 2
    return code


if __name__ == "__main__":
    raise SystemExit(main())
