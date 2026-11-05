from __future__ import annotations

import json
import unittest

import build_senath_c2_batch_transport_candidate as batch


def synthetic_body(model: str = "synthetic-model") -> dict:
    return {
        "model": model,
        "input": [{"role": "user", "content": [{"type": "input_text", "text": "synthetic source fields only"}]}],
        "temperature": 0,
        "max_output_tokens": 4,
        "store": False,
        "text": {"format": {"type": "json_schema", "name": "synthetic", "schema": {"type": "object"}}},
    }


def synthetic_inputs():
    refs = ["local-case-alpha", "local-case-beta", "local-case-error"]
    dispositions = [
        {"case_ref": refs[0], "status": "READY", "request_ordinal": 0},
        {"case_ref": refs[1], "status": "READY", "request_ordinal": 1},
        {"case_ref": refs[2], "status": "PREPARATION_ERROR", "reason": "TARGET_SLOT_MATCH_MULTIPLE"},
    ]
    bodies = [synthetic_body(), synthetic_body()]
    raw = [batch.canonical_json(body) for body in bodies]
    return bodies, raw, dispositions, refs


class BatchTransportCandidateTests(unittest.TestCase):
    def serialize_fixture(self, tokens=None, *, bodies=None, raw=None, dispositions=None):
        if bodies is None:
            bodies, raw, dispositions, _ = synthetic_inputs()
        if tokens is None:
            tokens = iter(["c2_" + "A" * 43, "c2_" + "B" * 43])
        return batch.serialize_batch(
            bodies, raw, dispositions, lambda: next(tokens),
            expected_denominator=3, expected_ready=2, expected_errors=1,
        )

    def test_exact_batch_line_envelope_preserves_frozen_bodies_and_local_mapping(self):
        bodies, raw, dispositions, refs = synthetic_inputs()
        request_bytes, map_bytes = self.serialize_fixture(bodies=bodies, raw=raw, dispositions=dispositions)
        request_lines = [json.loads(line) for line in request_bytes.decode().splitlines()]
        map_lines = [json.loads(line) for line in map_bytes.decode().splitlines()]
        self.assertEqual(len(request_lines), 2)
        self.assertTrue(all(set(row) == {"custom_id", "method", "url", "body"} for row in request_lines))
        self.assertEqual([row["method"] for row in request_lines], ["POST", "POST"])
        self.assertEqual([row["url"] for row in request_lines], ["/v1/responses", "/v1/responses"])
        self.assertEqual([row["body"] for row in request_lines], bodies)
        self.assertEqual([row["case_ref"] for row in map_lines], refs[:2])
        self.assertTrue(all(batch.TOKEN_RE.fullmatch(row["custom_id"]) for row in request_lines))
        self.assertEqual([row["custom_id"] for row in request_lines], [row["custom_id"] for row in map_lines])
        for ref in refs:
            self.assertNotIn(ref.encode(), request_bytes)
        self.assertNotIn(refs[2].encode(), map_bytes)

    def test_preparation_error_remains_in_denominator_but_has_no_request(self):
        bodies, raw, dispositions, refs = synthetic_inputs()
        request_bytes, map_bytes = self.serialize_fixture(bodies=bodies, raw=raw, dispositions=dispositions)
        self.assertEqual(len(request_bytes.splitlines()), 2)
        self.assertEqual(len(map_bytes.splitlines()), 2)
        self.assertEqual(len(dispositions), 3)
        self.assertEqual(sum(row["status"] == "PREPARATION_ERROR" for row in dispositions), 1)
        self.assertNotIn(refs[2].encode(), request_bytes)

    def test_fixed_mapping_makes_serialization_repeatable(self):
        first, first_map = self.serialize_fixture()
        second, second_map = self.serialize_fixture()
        self.assertEqual(first, second)
        self.assertEqual(first_map, second_map)

    def test_fresh_random_tokens_are_opaque_and_not_reused(self):
        values = [batch.opaque_custom_id() for _ in range(4)]
        self.assertEqual(len(set(values)), len(values))
        self.assertTrue(all(batch.TOKEN_RE.fullmatch(value) for value in values))

    def test_duplicate_case_identity_is_rejected(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        dispositions[1]["case_ref"] = dispositions[0]["case_ref"]
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(bodies=bodies, raw=raw, dispositions=dispositions)

    def test_missing_extra_or_duplicate_ordinal_is_rejected(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(bodies=bodies[:1], raw=raw[:1], dispositions=dispositions)
        _, _, missing, _ = synthetic_inputs()
        missing[1]["request_ordinal"] = 2
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(bodies=bodies, raw=raw, dispositions=missing)
        _, _, duplicate, _ = synthetic_inputs()
        duplicate[1]["request_ordinal"] = 0
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(bodies=bodies, raw=raw, dispositions=duplicate)

    def test_error_disposition_is_strictly_allowlisted(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        dispositions[2]["case_id"] = "forbidden"
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(bodies=bodies, raw=raw, dispositions=dispositions)

    def test_bad_or_reused_custom_id_is_rejected(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(iter(["case-id-encoding", "other-token"]), bodies=bodies, raw=raw, dispositions=dispositions)
        token = "c2_" + "A" * 43
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(iter([token, token]), bodies=bodies, raw=raw, dispositions=dispositions)

    def test_case_reference_collision_with_opaque_token_is_rejected(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        token = "c2_" + "A" * 43
        dispositions[2]["case_ref"] = token
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(iter([token, "c2_" + "C" * 43]), bodies=bodies, raw=raw, dispositions=dispositions)

    def test_reference_substring_in_provider_metadata_is_rejected(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        dispositions[2]["case_ref"] = "AAAA"
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(iter(["c2_" + "A" * 43, "c2_" + "C" * 43]), bodies=bodies, raw=raw, dispositions=dispositions)

    def test_many_reference_check_scans_metadata_once_not_body_per_reference(self):
        class CountedText(str):
            scanned = 0

            def __iter__(self):
                for char in super().__iter__():
                    type(self).scanned += 1
                    yield char

        refs = [f"opaque-local-ref-{index:05d}" for index in range(4_000)]
        metadata = CountedText("c2_" + "Z" * 43 + "\nPOST\n/v1/responses")
        CountedText.scanned = 0
        self.assertFalse(batch.contains_any_reference(metadata, refs))
        self.assertEqual(CountedText.scanned, len(metadata))

    def test_metadata_scan_detects_reference_inside_endpoint_metadata(self):
        lines = [{"custom_id": "c2_" + "A" * 43, "method": "POST", "url": "/v1/responses", "body": synthetic_body()}]
        with self.assertRaises(batch.CandidateError):
            batch.validate_provider_metadata(lines, ["/v1/response"])

    def test_noncanonical_or_altered_body_is_rejected(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        raw[0] = b'{ "altered": true }'
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(bodies=bodies, raw=raw, dispositions=dispositions)

    def test_mixed_models_are_rejected_for_one_batch_file(self):
        bodies, raw, dispositions, _ = synthetic_inputs()
        bodies[1] = synthetic_body("different-model")
        raw[1] = batch.canonical_json(bodies[1])
        with self.assertRaises(batch.CandidateError):
            self.serialize_fixture(bodies=bodies, raw=raw, dispositions=dispositions)


if __name__ == "__main__":
    unittest.main()
