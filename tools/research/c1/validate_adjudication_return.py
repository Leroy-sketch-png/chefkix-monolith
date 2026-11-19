"""Validate a private L36 adjudicator return against its frozen 150-item packet."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

from render_adjudication import LABELS, validate_packet

RETURN_FIELDS = {
    "format", "specification_id", "packet_sha256", "exported_at", "expected_items",
    "complete", "decisions", "resume",
}


def sha256(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def validate(packet_raw: bytes, return_raw: bytes) -> dict[str, Any]:
    packet = validate_packet(json.loads(packet_raw.decode("utf-8")))
    returned = json.loads(return_raw.decode("utf-8"))
    packet_hash = sha256(packet_raw)
    if (not isinstance(returned, dict) or set(returned) != RETURN_FIELDS or
            returned.get("format") != "c1-l36-adjudication-return-v1" or
            returned.get("specification_id") != packet["specification_id"] or
            returned.get("packet_sha256") != packet_hash or
            returned.get("expected_items") != 150 or
            not isinstance(returned.get("exported_at"), str) or
            not isinstance(returned.get("resume"), dict) or set(returned["resume"]) != {"index"} or
            not isinstance(returned["resume"].get("index"), int) or
            not 0 <= returned["resume"]["index"] < 150 or
            not isinstance(returned.get("decisions"), list)):
        raise ValueError("Return header does not match the frozen packet")
    allowed = {item["item_id"] for item in packet["items"]}
    decisions: dict[str, str] = {}
    for row in returned["decisions"]:
        if (not isinstance(row, dict) or set(row) != {"item_id", "final_label", "rationale"} or
                not isinstance(row.get("item_id"), str) or row["item_id"] not in allowed or row["item_id"] in decisions or
                not isinstance(row.get("final_label"), str) or row["final_label"] not in LABELS or
                not isinstance(row.get("rationale"), str) or len(row["rationale"]) > 10000):
            raise ValueError("Return contains an invalid, duplicate, or foreign decision")
        decisions[row["item_id"]] = row["final_label"]
    complete = len(decisions) == len(allowed)
    if returned.get("complete") is not complete:
        raise ValueError("Completion flag does not match the fixed packet denominator")
    return {
        "format": "c1-l36-adjudication-validation-v1",
        "packet_sha256": packet_hash,
        "return_sha256": sha256(return_raw),
        "expected_items": len(allowed),
        "recorded_decisions": len(decisions),
        "missing_decisions": len(allowed) - len(decisions),
        "complete": complete,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--packet", type=Path, required=True)
    parser.add_argument("--return-file", type=Path, required=True)
    args = parser.parse_args()
    receipt = validate(args.packet.read_bytes(), args.return_file.read_bytes())
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
