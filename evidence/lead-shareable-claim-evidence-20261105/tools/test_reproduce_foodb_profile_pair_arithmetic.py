from __future__ import annotations

import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import reproduce_foodb_profile_pair_arithmetic as r13  # noqa: E402


class Analysis:
    def __init__(self, left, right):
        shared = left & right
        union = left | right
        self.shared_compound_names = sorted(shared)
        self.overlap_percentage = len(shared) / len(union) if union else 0.0


class FakeAnalyzer:
    def __init__(self, profiles, mutate=False):
        self.profiles = profiles
        self.mutate = mutate

    def analyze(self, left_name, right_name):
        left = {row["name"].lower() for row in self.profiles[left_name]}
        right = {row["name"].lower() for row in self.profiles[right_name]}
        result = Analysis(left, right)
        if self.mutate:
            result.overlap_percentage += 0.01
        return result


def test_pair_denominators_keep_diagonals_distinct():
    result = r13.pair_denominators(971)
    assert result == {
        "profile_count": 971,
        "inclusive_pairs_with_diagonal": 471_906,
        "diagonal_self_pairs": 971,
        "distinct_cross_profile_pairs": 470_935,
    }


def test_zero_union_oracle_is_defined_as_zero():
    assert r13.direct_oracle([], []) == (set(), 0.0)


def test_small_synthetic_pair_replay_matches_independent_set_oracle():
    profiles = {
        "a": [{"name": "one"}, {"name": "two"}],
        "b": [{"name": "two"}, {"name": "three"}],
        "c": [{"name": "four"}],
    }
    first = r13.run_reproduction(profiles, FakeAnalyzer(profiles), expected_profile_count=3, expected_pair_count=6)
    second = r13.run_reproduction(profiles, FakeAnalyzer(profiles), expected_profile_count=3, expected_pair_count=6)
    assert first == second
    assert first["diagonal_self_pairs"] == 3
    assert first["distinct_cross_profile_pairs"] == 3
    assert first["runtime_oracle_mismatches"] == 0
    assert first["result"] == "PASS_NARROW_ARITHMETIC_ONLY"


def test_mutated_runtime_result_is_rejected_as_pair_mismatch():
    profiles = {"a": [{"name": "one"}], "b": [{"name": "one"}, {"name": "two"}]}
    result = r13.run_reproduction(profiles, FakeAnalyzer(profiles, mutate=True), expected_profile_count=2, expected_pair_count=3)
    assert result["runtime_oracle_mismatches"] == 3
    assert result["result"] == "MISMATCH"
    assert result["first_mismatch_pairs"][0] == {"left": "a", "right": "a"}


def test_duplicate_profile_identities_after_runtime_key_normalization_fail():
    profiles = {" celery ": [{"name": "leaf"}], "CELERY": [{"name": "stalk"}]}
    with pytest.raises(ValueError, match="duplicate normalized profile identity"):
        r13.validate_profiles(profiles)


def test_normalized_order_preserves_original_profile_keys_for_runtime_lookup():
    profiles = {"Basil": [{"name": "leaf"}], " celeriac ": [{"name": "stalk"}]}
    assert r13.validate_profiles(profiles) == ["Basil", " celeriac "]


def test_invalid_or_duplicate_compound_metadata_fails_closed():
    with pytest.raises(ValueError, match="invalid compound metadata"):
        r13.validate_profiles({"celery": [{"id": 1}]})
    with pytest.raises(ValueError, match="duplicate compound names"):
        r13.validate_profiles({"celery": [{"name": "salt"}, {"name": "SALT"}]})


def test_runtime_loader_fails_closed_for_wrong_service_hash(tmp_path: Path):
    root = tmp_path / "service"
    source = root / "src" / "compound_reasoning.py"
    source.parent.mkdir(parents=True)
    source.write_text("# synthetic test module\n", encoding="utf-8")
    with pytest.raises(ValueError, match="hash does not match"):
        r13.load_runtime(root)


def test_input_hash_checker_refuses_missing_and_changed_bytes(tmp_path: Path):
    missing = tmp_path / "missing.json"
    with pytest.raises(ValueError, match="missing"):
        r13._require_hash(missing, "0" * 64, "test input")
    changed = tmp_path / "changed.json"
    changed.write_text("{}", encoding="utf-8")
    with pytest.raises(ValueError, match="SHA-256 mismatch"):
        r13._require_hash(changed, "0" * 64, "test input")


def test_atomic_evidence_writer_refuses_overwrite(tmp_path: Path):
    destination = tmp_path / "receipt.json"
    r13.atomic_write_new(destination, json.dumps({"synthetic": True}).encode("utf-8"))
    with pytest.raises(FileExistsError, match="refusing to overwrite"):
        r13.atomic_write_new(destination, b"replacement")
    assert json.loads(destination.read_text(encoding="utf-8")) == {"synthetic": True}


def test_runtime_pair_comparison_rejects_nonfinite_scores():
    class Bad:
        shared_compound_names = []
        overlap_percentage = float("nan")

    assert not r13.compare_pair("a", "b", [], [], Bad())
