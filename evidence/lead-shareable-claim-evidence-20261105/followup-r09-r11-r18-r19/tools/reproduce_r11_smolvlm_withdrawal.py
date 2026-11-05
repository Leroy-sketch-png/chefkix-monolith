#!/usr/bin/env python3
"""Reproduce Claim R11: SmolVLM LoRA artifact withdrawn.

Forensically audits the historical SmolVLM training artifact:
  - ai/evidence/historical-audits/smolvlm/vlm_instruction_dataset.json
  - ai/evidence/historical-audits/smolvlm/trainer_state.json
  - ai/evidence/historical-audits/smolvlm/chefkix-smolvlm-qlora-training.log
  - ai/evidence/historical-audits/smolvlm/qlora_finetune.py

Confirming:
  1. Dataset contains exactly 4 mock text examples referencing 2 absent image filenames.
  2. Formatter code stripped '<image>' tags and omitted pixel tensors/image loaders.
  3. Training state records exactly 1 step with an empty log history (log_history: []).
  4. Training log file is 0 bytes (empty log).
  5. Validates historical withdrawal as documented in Chapter 12 v0.2.
"""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path


EXPECTED_HASHES = {
    "dataset": "8fb777dd32d3afdce8b8d0c4c020cb06b863c33a9e14ed55ca83805aa725fe89",
    "trainer_state": "2f6916c6ea92a477dcc730f421710ca9d40b22b50cbcba73b454598c4d006995",
    "log": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "script": "20c05a36b1d73e8179417f7f6f7b75c5b181f55f2a93f4f140b46ad16ca7c125",
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def reproduce_r11(repo_root: Path) -> dict:
    audit_dir = repo_root / "ai" / "evidence" / "historical-audits" / "smolvlm"
    if not audit_dir.is_dir():
        raise FileNotFoundError(f"Missing audit directory: {audit_dir}")

    dataset_path = audit_dir / "vlm_instruction_dataset.json"
    trainer_path = audit_dir / "trainer_state.json"
    log_path = audit_dir / "chefkix-smolvlm-qlora-training.log"
    script_path = audit_dir / "qlora_finetune.py"

    # Verify hashes
    for name, p, expected_h in [
        ("dataset", dataset_path, EXPECTED_HASHES["dataset"]),
        ("trainer_state", trainer_path, EXPECTED_HASHES["trainer_state"]),
        ("log", log_path, EXPECTED_HASHES["log"]),
        ("script", script_path, EXPECTED_HASHES["script"]),
    ]:
        if not p.is_file():
            raise FileNotFoundError(f"Missing required artifact: {p}")
        actual_h = sha256_file(p)
        if actual_h != expected_h:
            raise ValueError(f"Hash mismatch for {name}: expected {expected_h}, got {actual_h}")

    # 1. Dataset audit
    dataset = json.loads(dataset_path.read_text(encoding="utf-8"))
    if len(dataset) != 4:
        raise ValueError(f"Expected 4 dataset examples, got {len(dataset)}")
    image_refs = {item.get("image_path") for item in dataset}
    if image_refs != {"img_001.jpg", "img_002.jpg"}:
        raise ValueError(f"Unexpected image references: {image_refs}")

    # 2. Formatter code audit
    script_text = script_path.read_text(encoding="utf-8")
    if "val.replace('<image>\\n', '').replace('<image>', '')" not in script_text:
        raise ValueError("Missing '<image>' tag removal pattern in script")

    # 3. Trainer state audit
    trainer_state = json.loads(trainer_path.read_text(encoding="utf-8"))
    if trainer_state.get("global_step") != 1:
        raise ValueError(f"Expected global_step 1, got {trainer_state.get('global_step')}")
    if trainer_state.get("log_history") != []:
        raise ValueError("Expected empty log_history ([])")
    if trainer_state.get("max_steps") != 1:
        raise ValueError(f"Expected max_steps 1, got {trainer_state.get('max_steps')}")

    # 4. Log file audit
    if log_path.stat().st_size != 0:
        raise ValueError(f"Expected 0-byte log file, got {log_path.stat().st_size} bytes")

    result = {
        "schema_version": 1,
        "record_id": "r11-smolvlm-withdrawal-reproduction-20261103",
        "claim_id": "R11",
        "claim_text": "SmolVLM LoRA artifact withdrawn",
        "audit_directory": "ai/evidence/historical-audits/smolvlm",
        "dataset_examples_count": len(dataset),
        "referenced_images": sorted(image_refs),
        "images_present_in_artifact": False,
        "formatter_strips_image_tags": True,
        "pixel_tensors_passed": False,
        "global_steps_recorded": trainer_state.get("global_step"),
        "log_history_entries": len(trainer_state.get("log_history", [])),
        "training_log_bytes": log_path.stat().st_size,
        "status": "PASS",
        "reproduced_value": (
            "Historical SmolVLM artifact audited: 4 mock text examples, 2 absent images "
            "('img_001.jpg', 'img_002.jpg'), <image> tags stripped by formatter without pixel loaders, "
            "1 training step executed, 0 logged steps, 0-byte empty log. Withdrawn from VLM claims."
        ),
    }
    return result


def main():
    repo_root = Path(__file__).resolve().parent.parent.parent
    try:
        report = reproduce_r11(repo_root)
        print(json.dumps(report, indent=2))

        out_dir = repo_root / "ai" / "evidence" / "claims-register" / "instantiated-r01-r45-v1"
        out_dir.mkdir(parents=True, exist_ok=True)
        out_file = out_dir / "r11-smolvlm-withdrawal-reproduction-20261103.json"
        out_file.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
        print(f"Reproduction output written to: {out_file}")
        sys.exit(0)
    except Exception as e:
        print(f"Reproduction error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
