import hashlib
import json
import unittest

from validate_adjudication_return import validate


def packet():
    return {"format": "c1-l36-adjudication-packet-v1",
            "specification_id": "iron-chef-l36-case-rule-v1",
            "items": [{"item_id": f"{i:024x}", "recipe_title": "Dish",
                       "ingredients": ["one"], "target": "one", "candidate_names": ["two"],
                       "unordered_raw_labels": ["A", "C"], "final_label": None, "rationale": None}
                      for i in range(150)]}


def return_file(p, count=150):
    return {"format": "c1-l36-adjudication-return-v1",
            "specification_id": p["specification_id"],
            "packet_sha256": hashlib.sha256(json.dumps(p).encode()).hexdigest(),
            "exported_at": "2026-11-07T00:00:00Z", "expected_items": 150,
            "complete": count == 150,
            "decisions": [{"item_id": p["items"][i]["item_id"], "final_label": "U", "rationale": ""}
                          for i in range(count)], "resume": {"index": 0}}


class AdjudicationReturnTests(unittest.TestCase):
    def test_complete_and_partial_returns_preserve_fixed_denominator(self):
        p = packet()
        p_raw = json.dumps(p).encode()
        complete = validate(p_raw, json.dumps(return_file(p)).encode())
        self.assertEqual((complete["expected_items"], complete["recorded_decisions"], complete["missing_decisions"]), (150, 150, 0))
        partial = validate(p_raw, json.dumps(return_file(p, 149)).encode())
        self.assertEqual((partial["expected_items"], partial["recorded_decisions"], partial["missing_decisions"]), (150, 149, 1))
        self.assertFalse(partial["complete"])

    def test_rejects_packet_hash_mismatch_and_duplicate_or_foreign_ids(self):
        p = packet()
        p_raw = json.dumps(p).encode()
        r = return_file(p)
        r["packet_sha256"] = "0" * 64
        with self.assertRaises(ValueError):
            validate(p_raw, json.dumps(r).encode())
        r = return_file(p)
        r["decisions"][1]["item_id"] = r["decisions"][0]["item_id"]
        with self.assertRaises(ValueError):
            validate(p_raw, json.dumps(r).encode())

    def test_rejects_invalid_labels_and_completion_flags(self):
        p = packet()
        p_raw = json.dumps(p).encode()
        r = return_file(p)
        r["decisions"][0]["final_label"] = "X"
        with self.assertRaises(ValueError):
            validate(p_raw, json.dumps(r).encode())
        r = return_file(p, 149)
        r["complete"] = True
        with self.assertRaises(ValueError):
            validate(p_raw, json.dumps(r).encode())


if __name__ == "__main__":
    unittest.main()
