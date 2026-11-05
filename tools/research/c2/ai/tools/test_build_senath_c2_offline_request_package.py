"""Synthetic contract tests for the offline C2 request-boundary builder."""

from __future__ import annotations

import hashlib
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_senath_c2_offline_request_package as package


def fixture():
    ready_ref = "opaque-ready-ref"
    error_ref = "opaque-error-ref"
    source = {
        "recipe_title": "synthetic soup",
        "ingredients": [["green onion", "scallion"], ["salt"]],
        "target_ingredient": "green onion",
    }
    payload = {
        "recipe_title": source["recipe_title"],
        "ingredient_list": source["ingredients"],
        "ingredient_to_replace": source["target_ingredient"],
    }
    candidate = {"case_id": ready_ref, "source_fields": source, "payload": payload}
    companions = [{
        "case_id": ready_ref, "case_ref": ready_ref, "payload": payload,
        "source_fields": source, "status": "READY",
    }, {
        "case_ref": error_ref, "reason": "TARGET_SLOT_MATCH_MULTIPLE",
        "status": "PREPARATION_ERROR",
    }]
    refs_sha = hashlib.sha256((ready_ref + "\n" + error_ref + "\n").encode()).hexdigest()
    parent = {
        "preparation_error_reasons": {"TARGET_SLOT_MATCH_MULTIPLE": 1},
        "case_refs_sha256": refs_sha,
    }
    return companions, [candidate], parent


class OfflineRequestBoundaryTests(unittest.TestCase):
    def test_loaded_serializer_matches_the_manifest_source_pin(self):
        self.assertEqual(
            package.sha256_file(Path(package.serializer.__file__)), package.SERIALIZER_SHA256
        )

    def test_builder_imports_exact_serializer_from_local_git_history(self):
        with tempfile.TemporaryDirectory() as temp_name:
            repository = Path(temp_name)
            subprocess.run(["git", "init", "--quiet"], cwd=repository, check=True)
            subprocess.run(["git", "config", "user.name", "Synthetic Test"], cwd=repository, check=True)
            subprocess.run(["git", "config", "user.email", "test@example.invalid"], cwd=repository, check=True)
            source = repository / "ai" / "tools" / "serializer.py"
            source.parent.mkdir(parents=True)
            source.write_text("VALUE = 'frozen'\n", encoding="utf-8")
            subprocess.run(["git", "add", "ai/tools/serializer.py"], cwd=repository, check=True)
            subprocess.run(["git", "commit", "--quiet", "-m", "fixture"], cwd=repository, check=True)
            revision = subprocess.run(
                ["git", "rev-parse", "HEAD"], cwd=repository, check=True, capture_output=True, text=True
            ).stdout.strip()
            committed_source = subprocess.run(
                ["git", "show", f"{revision}:ai/tools/serializer.py"],
                cwd=repository,
                check=True,
                capture_output=True,
            ).stdout
            expected = hashlib.sha256(committed_source).hexdigest()

            module, temporary = package.load_verified_git_module(
                repository, revision, "ai/tools/serializer.py", expected, "synthetic_pinned_serializer"
            )
            try:
                self.assertEqual(module.VALUE, "frozen")
                self.assertEqual(hashlib.sha256(Path(module.__file__).read_bytes()).hexdigest(), expected)
                with self.assertRaises(package.PackageError):
                    package.load_verified_git_module(
                        repository, revision, "ai/tools/serializer.py", "0" * 64,
                        "synthetic_bad_pin_serializer",
                    )
            finally:
                temporary.cleanup()

    def test_builder_uses_hash_pinned_snapshot_when_monolith_has_no_source_history(self):
        snapshot = Path(package.__file__).resolve().parents[2] / "_frozen" / "preflight_c2_openai_cost.py"
        module, temporary = package.load_verified_git_module(
            Path(package.__file__).resolve().parents[2],
            "missing-upstream-history",
            package.SERIALIZER_GIT_PATH,
            package.SERIALIZER_SHA256,
            "portable_pinned_serializer",
            fallback_source=snapshot,
        )
        try:
            self.assertEqual(package.sha256_file(Path(module.__file__)), package.SERIALIZER_SHA256)
            self.assertTrue(hasattr(module, "main"))
        finally:
            temporary.cleanup()

    def test_builder_rejects_modified_portable_serializer_snapshot(self):
        with tempfile.TemporaryDirectory() as temp_name:
            snapshot = Path(temp_name) / "serializer.py"
            snapshot.write_text("VALUE = 'changed'\n", encoding="utf-8")
            with self.assertRaises(package.PackageError):
                package.load_verified_git_module(
                    Path(temp_name),
                    "missing-upstream-history",
                    package.SERIALIZER_GIT_PATH,
                    package.SERIALIZER_SHA256,
                    "tampered_portable_serializer",
                    fallback_source=snapshot,
                )

    def serialize(self, companions=None, candidates=None, parent=None):
        default_companions, default_candidates, default_parent = fixture()
        return package.serialize_cohort(
            [default_companions if companions is None else companions],
            [default_candidates if candidates is None else candidates],
            default_parent if parent is None else parent,
            expected_partitions=1, expected_total=2, expected_ready=1, expected_errors=1,
        )

    def test_body_allowlist_excludes_local_identifiers_and_errors(self):
        request_bytes, index_bytes, counts = self.serialize()
        self.assertEqual(counts["denominator_cases"], 2)
        self.assertEqual(counts["ready_cases"], 1)
        self.assertEqual(counts["preparation_error_cases"], 1)
        self.assertEqual(len(request_bytes.splitlines()), 1)
        self.assertEqual(len(index_bytes.splitlines()), 2)
        for sentinel in (b"opaque-ready-ref", b"opaque-error-ref", b"case_id", b"case_ref"):
            self.assertNotIn(sentinel, request_bytes)
        body = package.json.loads(request_bytes.decode()) if hasattr(package, "json") else None
        self.assertIsNotNone(body)
        self.assertEqual(set(body), {"model", "input", "temperature", "max_output_tokens", "store", "text"})
        user_content = body["input"][1]["content"]
        import json
        self.assertEqual(set(json.loads(user_content)), package.PAYLOAD_KEYS)
        self.assertIn(b"TARGET_SLOT_MATCH_MULTIPLE", index_bytes)

    def test_serialization_is_deterministic(self):
        first = self.serialize()
        second = self.serialize()
        self.assertEqual(first, second)

    def test_duplicate_companion_identity_rejected(self):
        companions, candidates, parent = fixture()
        companions[1]["case_ref"] = companions[0]["case_ref"]
        with self.assertRaises(package.PackageError):
            self.serialize(companions, candidates, parent)

    def test_missing_ready_candidate_rejected(self):
        with self.assertRaises(package.PackageError):
            self.serialize(candidates=[], parent=fixture()[2])

    def test_unknown_error_reason_rejected(self):
        companions, candidates, parent = fixture()
        companions[1]["reason"] = "UNKNOWN_REASON"
        with self.assertRaises(package.PackageError):
            self.serialize(companions, candidates, parent)

    def test_error_payload_or_source_is_rejected(self):
        companions, candidates, parent = fixture()
        companions[1]["payload"] = {"forbidden": "synthetic"}
        with self.assertRaises(package.PackageError):
            self.serialize(companions, candidates, parent)

    def test_ready_candidate_payload_mismatch_rejected(self):
        companions, candidates, parent = fixture()
        candidates[0]["payload"]["recipe_title"] = "altered"
        with self.assertRaises(package.PackageError):
            self.serialize(companions, candidates, parent)

    def test_parent_reason_or_ordered_identity_hash_mismatch_rejected(self):
        companions, candidates, parent = fixture()
        parent["preparation_error_reasons"] = {}
        with self.assertRaises(package.PackageError):
            self.serialize(companions, candidates, parent)
        companions, candidates, parent = fixture()
        parent["case_refs_sha256"] = "0" * 64
        with self.assertRaises(package.PackageError):
            self.serialize(companions, candidates, parent)

    def test_partition_inventory_and_hash_mismatch_rejected(self):
        with self.assertRaises(package.PackageError):
            package.validate_partition_inventory([{"partition_index": 0}], expected_count=2)
        with self.assertRaises(package.PackageError):
            package.validate_partition_inventory([{"partition_index": 1}], expected_count=1)
        part = {"case_records_sha256": "a" * 64, "request_candidates_sha256": "b" * 64}
        with self.assertRaises(package.PackageError):
            package.validate_partition_hashes(part, "0" * 64, "b" * 64)
        with self.assertRaises(package.PackageError):
            package.validate_partition_hashes(part, "a" * 64, "0" * 64)


if __name__ == "__main__":
    unittest.main()
