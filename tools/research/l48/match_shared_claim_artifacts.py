"""Match register v20 artifact pins against the curated public Git blobs (independent of checkout line endings)."""

import csv
import hashlib
import io
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

from claims_adjudication_evaluator import CLAIMS_REGISTER_V20_SHA256

ROOT = Path(__file__).resolve().parents[3]
EVIDENCE = ROOT / "evidence"
REGISTER = (
    EVIDENCE / "iron-chef-lead-member-handoff-20261105"
    / "claims" / "claims-register-r01-r45-v20.csv"
)
HANDOFFS = (
    EVIDENCE / "lead-r05-split-counts-20261105",
    EVIDENCE / "lead-shareable-claim-evidence-20261105",
)
COLUMNS = (
    "claim_id", "recorded_status", "recorded_outcome",
    "artifact_status", "artifact_shared_path", "tool_status", "tool_shared_path",
)


def match_pin(path, pin, indexed_paths, line_ending_variants):
    if not path:
        return "NO_REFERENCE", ""
    if not pin:
        return "NO_PIN", ""
    digest = pin.lower()
    matches = indexed_paths.get(digest)
    if matches:
        return "EXACT_BYTES_SHARED", " | ".join(matches)
    matches = line_ending_variants.get(digest)
    if matches:
        return "MATCH_AFTER_EOL_NORMALIZATION", " | ".join(matches)
    return "NO_EXACT_COPY_IN_HANDOFFS", ""


def build_crosswalk():
    register_bytes = REGISTER.read_bytes()
    if hashlib.sha256(register_bytes).hexdigest() != CLAIMS_REGISTER_V20_SHA256:
        raise ValueError("Claims register v20 does not match its byte pin")
    rows = list(csv.DictReader(io.StringIO(register_bytes.decode("utf-8"))))
    if [row["ID"] for row in rows] != [f"R{i:02}" for i in range(1, 46)]:
        raise ValueError("Expected R01–R45 in register order")

    indexed_paths = defaultdict(list)
    line_ending_variants = defaultdict(list)
    directories = [folder.relative_to(ROOT).as_posix() for folder in HANDOFFS]
    tracked = subprocess.check_output(
        ["git", "ls-tree", "-rz", "--name-only", "HEAD", "--", *directories], cwd=ROOT,
    ).decode("utf-8").split(chr(0))
    for path in sorted(tracked):
        if Path(path).suffix in {".md", ".json", ".py"}:
            raw = subprocess.check_output(["git", "cat-file", "blob", f"HEAD:{path}"], cwd=ROOT)
            indexed_paths[hashlib.sha256(raw).hexdigest()].append(path)
            lf_bytes = raw.replace(b"\r\n", b"\n")
            for variant in (lf_bytes, lf_bytes.replace(b"\n", b"\r\n")):
                if variant != raw:
                    line_ending_variants[hashlib.sha256(variant).hexdigest()].append(path)

    yield COLUMNS
    for row in rows:
        artifact = match_pin(row["Artifact_path"], row["SHA256"], indexed_paths, line_ending_variants)
        tool = match_pin(row["Reproduction_path"], row["Reproduction_sha256"], indexed_paths, line_ending_variants)
        yield (row["ID"], row["Evidence_status"], row["Outcome"], *artifact, *tool)


if __name__ == "__main__":
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    csv.writer(sys.stdout, lineterminator="\n").writerows(build_crosswalk())
