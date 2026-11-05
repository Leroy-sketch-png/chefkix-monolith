#!/usr/bin/env python3
"""Contract, rubric, and gate definitions for C4 LLM Judge under Section 7.

This module formalizes:
1. Pinned System and Evaluation Prompts with Section 6 A/C/N/U rubric.
2. Pinned JSON Input/Output schemas.
3. Candidate-order reversal and stability protocol.
4. Section 7 validation gates: Cohen's kappa >= 0.60 (lower 95% >= 0.50),
   instability <= 10%, position trend <= 5pp, leniency delta <= 10pp,
   self-preference delta <= 10pp, invalid-output rate <= 2%.
5. Strict fail-closed boundary: judge-acceptable != human-valid or safe.
"""
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from typing import Any, Final

CONTRACT_VERSION: Final[str] = "iron-chef-c4-judge-contract-v1"
PROTOCOL_SHA256: Final[str] = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"

# Permitted A/C/N/U labels from Section 6/7
PERMITTED_LABELS: Final[frozenset[str]] = frozenset(("A", "C", "N", "U"))

# Section 7 Validation Gates
GATE_KAPPA_MIN: Final[float] = 0.60
GATE_KAPPA_LOWER95_MIN: Final[float] = 0.50
GATE_LENIENCY_DELTA_MAX: Final[float] = 0.10       # 10 percentage points
GATE_SELF_PREFERENCE_MAX: Final[float] = 0.10      # 10 percentage points
GATE_POSITION_TREND_MAX: Final[float] = 0.05       # 5 percentage points
GATE_INSTABILITY_MAX: Final[float] = 0.10          # 10% unstable items
GATE_INVALID_OUTPUT_MAX: Final[float] = 0.02       # 2% invalid outputs
MAX_RETRIES: Final[int] = 1                        # Exactly 1 logged retry allowed
MAX_REASON_WORDS: Final[int] = 20                  # Reasons <= 20 words, not scores

# Pinned System Prompt
SYSTEM_PROMPT: Final[str] = """You are a rigorous culinary substitution judge for the IRON CHEF research protocol.
Your task is to evaluate whether a candidate substitute can validly replace a target ingredient in a specific recipe.

You must follow the frozen A/C/N/U rubric:
- A: Works with normal quantity logic; recognizably same dish, minor taste/texture change.
- C: Works only with quantity, technique, or companion-ingredient adjustment; still recognizably same dish.
- N: Breaks culinary function or dish identity, is nonsensical, identical to target, or too vague to act on.
- U: Cannot judge with supplied evidence; unknown information, NOT a negative.

Constraints:
1. Judge within the recipe context (dish title, ingredients, and optional context).
2. Judge reasons MUST be concise: 20 words or fewer.
3. Do NOT provide numerical scores or probabilities.
4. Output MUST be valid JSON adhering strictly to the schema."""

# Pinned User Prompt Template
USER_PROMPT_TEMPLATE: Final[str] = """Recipe Title: {recipe_title}
Ingredients: {ingredients_text}
Target Ingredient to Replace: {target}
Candidate Substitute: {candidate}
Context Notes: {context_notes}

Respond strictly in JSON with keys "label" (one of "A", "C", "N", "U") and "reason" (at most 20 words explaining the culinary rationale)."""

INPUT_SCHEMA: Final[dict[str, Any]] = {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "C4JudgeCaseInput",
    "type": "object",
    "properties": {
        "case_id": {"type": "string"},
        "recipe_title": {"type": "string"},
        "ingredients": {"type": "array", "items": {"type": "string"}},
        "target": {"type": "string"},
        "candidate": {"type": "string"},
        "context_notes": {"type": "string"},
        "presentation_order": {"type": "string", "enum": ["forward", "reversed"]},
    },
    "required": ["case_id", "recipe_title", "ingredients", "target", "candidate"],
    "additionalProperties": True,
}

OUTPUT_SCHEMA: Final[dict[str, Any]] = {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "title": "C4JudgeOutput",
    "type": "object",
    "properties": {
        "label": {"type": "string", "enum": ["A", "C", "N", "U"]},
        "reason": {"type": "string", "maxLength": 200},
    },
    "required": ["label", "reason"],
    "additionalProperties": False,
}

FAIL_CLOSED_BOUNDARIES: Final[dict[str, Any]] = {
    "human_h1_audit_replaced": False,
    "judge_acceptable_equals_human_valid": False,
    "clinical_safety_guarantee": False,
    "confirmatory_claim_allowed_without_human_audit": False,
    "synthetic_labels_in_production": False,
}


def get_contract_hash() -> str:
    """Return deterministic SHA-256 digest of pinned contract text and schemas."""
    payload = {
        "contract_version": CONTRACT_VERSION,
        "protocol_sha256": PROTOCOL_SHA256,
        "system_prompt": SYSTEM_PROMPT,
        "user_prompt_template": USER_PROMPT_TEMPLATE,
        "input_schema": INPUT_SCHEMA,
        "output_schema": OUTPUT_SCHEMA,
        "gates": {
            "kappa_min": GATE_KAPPA_MIN,
            "kappa_lower95_min": GATE_KAPPA_LOWER95_MIN,
            "leniency_delta_max": GATE_LENIENCY_DELTA_MAX,
            "self_preference_max": GATE_SELF_PREFERENCE_MAX,
            "position_trend_max": GATE_POSITION_TREND_MAX,
            "instability_max": GATE_INSTABILITY_MAX,
            "invalid_output_max": GATE_INVALID_OUTPUT_MAX,
            "max_retries": MAX_RETRIES,
            "max_reason_words": MAX_REASON_WORDS,
        },
        "boundaries": FAIL_CLOSED_BOUNDARIES,
    }
    dumped = json.dumps(payload, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(dumped.encode("utf-8")).hexdigest()


@dataclass(frozen=True)
class JudgeDeclaration:
    contract_version: str
    contract_hash: str
    protocol_sha256: str
    gates: dict[str, float | int]
    boundaries: dict[str, bool]

    def to_dict(self) -> dict[str, Any]:
        return {
            "contract_version": self.contract_version,
            "contract_hash": self.contract_hash,
            "protocol_sha256": self.protocol_sha256,
            "gates": self.gates,
            "boundaries": self.boundaries,
        }


def make_declaration() -> JudgeDeclaration:
    return JudgeDeclaration(
        contract_version=CONTRACT_VERSION,
        contract_hash=get_contract_hash(),
        protocol_sha256=PROTOCOL_SHA256,
        gates={
            "kappa_min": GATE_KAPPA_MIN,
            "kappa_lower95_min": GATE_KAPPA_LOWER95_MIN,
            "leniency_delta_max": GATE_LENIENCY_DELTA_MAX,
            "self_preference_max": GATE_SELF_PREFERENCE_MAX,
            "position_trend_max": GATE_POSITION_TREND_MAX,
            "instability_max": GATE_INSTABILITY_MAX,
            "invalid_output_max": GATE_INVALID_OUTPUT_MAX,
            "max_retries": MAX_RETRIES,
            "max_reason_words": MAX_REASON_WORDS,
        },
        boundaries=dict(FAIL_CLOSED_BOUNDARIES),
    )


if __name__ == "__main__":
    decl = make_declaration()
    print(f"Contract: {decl.contract_version}")
    print(f"Contract Hash: {decl.contract_hash}")
    print("Gates:")
    for k, v in decl.gates.items():
        print(f"  {k}: {v}")
    print("Boundaries:")
    for k, v in decl.boundaries.items():
        print(f"  {k}: {v}")
