"""Render an already-blinded, rater-specific C1 view pack as one offline HTML file."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

TOKEN = re.compile(r"[a-f0-9]{32,64}\Z")
VIEW_FIELDS = {"view_id", "recipe_title", "ingredients", "target", "candidates"}


def text(value: object, name: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"Missing text: {name}")
    return value


def validate_pack(pack: object) -> dict:
    if not isinstance(pack, dict) or set(pack) != {"pack_id", "reviewer", "views"}:
        raise ValueError("Pack must contain only pack_id, reviewer and views")
    if not isinstance(pack["pack_id"], str) or not TOKEN.fullmatch(pack["pack_id"]):
        raise ValueError("pack_id must be an opaque hexadecimal token")
    if pack["reviewer"] not in {"R1", "R2"}:
        raise ValueError("Unknown reviewer")
    if not isinstance(pack["views"], list) or len(pack["views"]) != 150:
        raise ValueError("A production reviewer pack requires exactly 150 frozen views")
    seen = set()
    for view in pack["views"]:
        if not isinstance(view, dict) or set(view) != VIEW_FIELDS:
            raise ValueError("Unexpected reviewer-view fields; private data must not be embedded")
        view_id = view["view_id"]
        if not isinstance(view_id, str) or not TOKEN.fullmatch(view_id) or view_id in seen:
            raise ValueError("View IDs must be unique opaque tokens")
        seen.add(view_id)
        text(view["recipe_title"], "recipe title")
        text(view["target"], "target ingredient")
        if not isinstance(view["ingredients"], list) or not view["ingredients"]:
            raise ValueError("Ingredients must be a nonempty string array")
        for ingredient in view["ingredients"]:
            text(ingredient, "ingredient")
        candidates = view["candidates"]
        if not isinstance(candidates, list) or not 5 <= len(candidates) <= 6:
            raise ValueError("GISMo-top-five plus gold requires 5 or 6 deduplicated candidates")
        candidate_ids = set()
        for candidate in candidates:
            if not isinstance(candidate, dict) or set(candidate) != {"blind_candidate", "display_names"}:
                raise ValueError("Unexpected candidate fields")
            token = candidate["blind_candidate"]
            if not isinstance(token, str) or not TOKEN.fullmatch(token) or token in candidate_ids:
                raise ValueError("Candidate IDs must be unique opaque tokens within a view")
            candidate_ids.add(token)
            if not isinstance(candidate["display_names"], list) or not candidate["display_names"]:
                raise ValueError("Candidate names must be a nonempty string array")
            for name in candidate["display_names"]:
                text(name, "candidate name")
    return pack


def render(pack: dict, destination: Path) -> str:
    validate_pack(pack)
    raw = json.dumps(pack, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    binding = hashlib.sha256(raw.encode("utf-8")).hexdigest()
    safe = raw.replace("&", "\\u0026").replace("<", "\\u003c").replace(">", "\\u003e")
    safe = safe.replace("\u2028", "\\u2028").replace("\u2029", "\\u2029")
    template = Path(__file__).with_name("reviewer-template.html").read_text(encoding="utf-8")
    if template.count("__PACK_JSON__") != 1 or template.count("__PACK_BINDING__") != 1:
        raise ValueError("Template substitution markers are invalid")
    document = template.replace("__PACK_BINDING__", binding).replace("__PACK_JSON__", safe)
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        raise FileExistsError("Refusing to overwrite an issued reviewer file")
    destination.write_text(document, encoding="utf-8", newline="\n")
    return binding


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    pack = json.loads(args.input.read_text(encoding="utf-8"))
    digest = render(pack, args.output)
    print(json.dumps({"output": str(args.output), "reviewer": pack["reviewer"],
                      "cases": len(pack["views"]), "candidates": sum(len(v["candidates"]) for v in pack["views"]),
                      "pack_binding_sha256": digest}))
