from __future__ import annotations

import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parent))
import reproduce_foodb_service_inventory as r12  # noqa: E402


def test_count_inventory_counts_unique_named_dimensions_and_links():
    profiles = {
        "Celery leaf": [{"name": "Luteolin"}, {"name": "Apigenin"}],
        "Basil": [{"name": "luteolin"}, {"name": "Eugenol"}],
    }
    assert r12.count_inventory(profiles) == {
        "indexed_food_name_count": 2,
        "resolved_named_compound_count": 3,
        "indexed_food_compound_links": 4,
    }


@pytest.mark.parametrize("profiles", [None, {}, [], {"": [{"name": "a"}]}, {"celery": []}])
def test_malformed_or_empty_profiles_fail_closed(profiles):
    with pytest.raises(ValueError):
        r12.count_inventory(profiles)


def test_duplicate_normalized_food_identity_fails_closed():
    with pytest.raises(ValueError, match="duplicate normalized food identity"):
        r12.count_inventory({"Celery": [{"name": "a"}], " celery ": [{"name": "b"}]})


def test_duplicate_compound_identity_within_food_fails_closed():
    with pytest.raises(ValueError, match="duplicate normalized compound names"):
        r12.count_inventory({"celery": [{"name": "Luteolin"}, {"name": "luteolin"}]})


def test_compound_metadata_must_be_named():
    with pytest.raises(ValueError, match="empty or non-string compound name"):
        r12.count_inventory({"celery": [{"name": " "}]})


def test_wrong_bytes_are_rejected_before_parsing(tmp_path: Path):
    path = tmp_path / "altered.json"
    path.write_text("{}", encoding="utf-8")
    with pytest.raises(ValueError, match="SHA-256 mismatch"):
        r12.require_hash(path, "0" * 64, "profile artifact")


def test_atomic_writer_refuses_to_overwrite_existing_receipt(tmp_path: Path):
    target = tmp_path / "receipt.json"
    r12.atomic_write_new(target, b'{"first":true}\n')
    with pytest.raises(FileExistsError, match="refusing to overwrite"):
        r12.atomic_write_new(target, b'{"second":true}\n')
    assert json.loads(target.read_text(encoding="utf-8")) == {"first": True}


def test_expected_counts_are_fixed_inventory_not_a_progress_score():
    assert r12.EXPECTED_COUNTS == {
        "indexed_food_name_count": 971,
        "resolved_named_compound_count": 206,
        "indexed_food_compound_links": 125_992,
    }
