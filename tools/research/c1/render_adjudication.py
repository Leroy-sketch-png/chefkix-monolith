"""Render the frozen L36 disagreement packet as a standalone offline form."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
from pathlib import Path
from typing import Any

LABELS = {"A", "C", "N", "U"}
ITEM_ID = re.compile(r"[a-f0-9]{24}\Z")
ITEM_FIELDS = {
    "item_id", "recipe_title", "ingredients", "target", "candidate_names",
    "unordered_raw_labels", "final_label", "rationale",
}


def digest_bytes(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def validate_packet(packet: Any) -> dict[str, Any]:
    if (not isinstance(packet, dict) or set(packet) != {"format", "specification_id", "items"}
            or packet.get("format") != "c1-l36-adjudication-packet-v1"
            or packet.get("specification_id") != "iron-chef-l36-case-rule-v1"):
        raise ValueError("Unexpected adjudication packet contract")
    items = packet["items"]
    if not isinstance(items, list) or len(items) != 150:
        raise ValueError("The frozen adjudication packet must contain exactly 150 items")
    seen: set[str] = set()
    for item in items:
        if not isinstance(item, dict) or set(item) != ITEM_FIELDS:
            raise ValueError("Unexpected item fields; private identifiers must not enter the visible packet")
        token = item.get("item_id")
        if not isinstance(token, str) or not ITEM_ID.fullmatch(token) or token in seen:
            raise ValueError("Item IDs must be unique opaque tokens")
        seen.add(token)
        for name in ("recipe_title", "target"):
            if not isinstance(item.get(name), str) or not item[name].strip():
                raise ValueError(f"Missing {name}")
        ingredients = item.get("ingredients")
        names = item.get("candidate_names")
        if not isinstance(ingredients, list) or not ingredients or any(not isinstance(x, str) or not x.strip() for x in ingredients):
            raise ValueError("Invalid ingredient context")
        if not isinstance(names, list) or not names or any(not isinstance(x, str) or not x.strip() for x in names):
            raise ValueError("Invalid candidate display names")
        labels = item.get("unordered_raw_labels")
        if (not isinstance(labels, list) or len(labels) != 2 or
                any(not isinstance(x, str) or x not in LABELS for x in labels) or
                labels[0] == labels[1] or labels != sorted(labels)):
            raise ValueError("Each item must retain exactly two distinct conflicting labels")
        if item.get("final_label") is not None or item.get("rationale") is not None:
            raise ValueError("The frozen packet must not contain an adjudicated value")
    return packet


def render(packet_path: Path, output_path: Path) -> tuple[str, str]:
    raw = packet_path.read_bytes()
    packet = validate_packet(json.loads(raw.decode("utf-8")))
    packet_sha = digest_bytes(raw)
    embedded = json.dumps(packet, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    embedded = embedded.replace("&", "\\u0026").replace("<", "\\u003c").replace(">", "\\u003e")
    embedded = embedded.replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
    template_path = Path(__file__).with_name("adjudication-template.html")
    template = template_path.read_text(encoding="utf-8")
    if template.count("__PACK_JSON__") != 1 or template.count("__PACK_SHA256__") != 1:
        raise ValueError("Invalid adjudication template markers")
    document = template.replace("__PACK_SHA256__", packet_sha).replace("__PACK_JSON__", embedded)
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("x", encoding="utf-8", newline="\n") as stream:
        stream.write(document)
        stream.flush()
        os.fsync(stream.fileno())
    return packet_sha, digest_bytes(document.encode("utf-8"))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    packet_sha, html_sha = render(args.input, args.output)
    print(json.dumps({"packet_sha256": packet_sha, "html_sha256": html_sha,
                      "items": 150, "network_access": False}, sort_keys=True))


if __name__ == "__main__":
    main()
