from __future__ import annotations

import unittest
from unittest import mock

from prepare_adjudication_handoff import pack_binding, prepare


def fixture():
    key = {"format": "c1-private-key-v1", "entries": []}
    packs, returns = {}, {}
    queue = []
    for reviewer in ("R1", "R2"):
        views, judgments = [], []
        for i in range(150):
            case_id = f"case-{i:03d}"
            view_id = f"{reviewer.lower()}-{i:03d}"
            candidates = []
            for j in range(6 if i < 117 else 5):
                candidate = f"candidate-{i:03d}-{j:02d}"
                candidates.append({"blind_candidate": candidate, "display_names": [f"Food {j}"]})
                key["entries"].append({"reviewer": reviewer, "case_id": case_id,
                                       "identity_id": f"identity-{i:03d}-{j:02d}",
                                       "view_id": view_id, "blind_candidate": candidate,
                                       "display_names": [f"Food {j}"], "recorded_gold": j == 5})
                label = "A" if j == 0 and reviewer == "R1" else ("C" if j == 0 else "N")
                judgments.append({"record_id": f"{reviewer}:{view_id}:{candidate}",
                                  "view_id": view_id, "blind_candidate": candidate, "label": label})
                if j == 0 and reviewer == "R1":
                    queue.append({"case_id": case_id, "identity_id": f"identity-{i:03d}-00",
                                  "rater_labels_differ": True})
            views.append({"view_id": view_id, "recipe_title": f"Recipe {i}",
                          "ingredients": ["water", "salt"], "target": "oil", "candidates": candidates})
        packs[reviewer] = {"reviewer": reviewer, "pack_id": f"{reviewer.lower()}" * 16, "views": views}
        returns[reviewer] = {"reviewer": reviewer, "pack_id": packs[reviewer]["pack_id"],
                             "pack_binding_sha256": pack_binding(packs[reviewer]),
                             "complete": True, "expected_judgments": 867, "judgments": judgments}
    return key, packs, returns, queue


class AdjudicationHandoffTests(unittest.TestCase):
    def prepare_fixture(self):
        key, packs, returns, queue = fixture()
        with mock.patch("prepare_adjudication_handoff.load") as load, \
             mock.patch("prepare_adjudication_handoff.digest", return_value="a" * 64):
            load.side_effect = lambda path: key if "key" in str(path) else (
                packs["R1"] if "r1-pack" in str(path) else
                packs["R2"] if "r2-pack" in str(path) else
                returns["R1"] if "r1-return" in str(path) else returns["R2"])
            with mock.patch("pathlib.Path.read_text", return_value="\n".join(
                    __import__("json").dumps(row) for row in queue)):
                return prepare(*[__import__("pathlib").Path(x) for x in
                                 ("key", "r1-pack", "r2-pack", "r1-return", "r2-return", "queue")])

    def test_exact_disagreement_packet_is_blinded_and_preserves_context(self):
        packet, sealed, receipt = self.prepare_fixture()
        self.assertEqual(len(packet["items"]), 150)
        self.assertEqual(receipt["adjudicated_values_created"], 0)
        first = packet["items"][0]
        self.assertEqual(first["unordered_raw_labels"], ["A", "C"])
        self.assertEqual(first["ingredients"], ["water", "salt"])
        self.assertNotIn("case_id", first)
        self.assertNotIn("identity_id", first)
        self.assertNotIn("recorded_gold", first)
        self.assertEqual(len(sealed["items"]), 150)
        self.assertEqual(sealed["items"][0]["raw_labels_by_rater"], {"R1": "A", "R2": "C"})

    def test_rejects_context_mismatch(self):
        key, packs, returns, queue = fixture()
        for view in packs["R2"]["views"]:
            view["recipe_title"] = "different"
        returns["R2"]["pack_binding_sha256"] = pack_binding(packs["R2"])
        with mock.patch("prepare_adjudication_handoff.load") as load, \
             mock.patch("prepare_adjudication_handoff.digest", return_value="a" * 64):
            load.side_effect = lambda path: key if "key" in str(path) else (
                packs["R1"] if "r1-pack" in str(path) else
                packs["R2"] if "r2-pack" in str(path) else
                returns["R1"] if "r1-return" in str(path) else returns["R2"])
            with mock.patch("pathlib.Path.read_text", return_value="\n".join(
                    __import__("json").dumps(row) for row in queue)):
                with self.assertRaisesRegex(ValueError, "context or candidate identity"):
                    prepare(*[__import__("pathlib").Path(x) for x in
                              ("key", "r1-pack", "r2-pack", "r1-return", "r2-return", "queue")])

    def test_rejects_missing_disagreement_identity(self):
        key, packs, returns, queue = fixture()
        queue[0]["identity_id"] = "missing"
        with mock.patch("prepare_adjudication_handoff.load") as load, \
             mock.patch("prepare_adjudication_handoff.digest", return_value="a" * 64):
            load.side_effect = lambda path: key if "key" in str(path) else (
                packs["R1"] if "r1-pack" in str(path) else
                packs["R2"] if "r2-pack" in str(path) else
                returns["R1"] if "r1-return" in str(path) else returns["R2"])
            with mock.patch("pathlib.Path.read_text", return_value="\n".join(
                    __import__("json").dumps(row) for row in queue)):
                with self.assertRaisesRegex(ValueError, "absent from one issued pack"):
                    prepare(*[__import__("pathlib").Path(x) for x in
                              ("key", "r1-pack", "r2-pack", "r1-return", "r2-return", "queue")])


if __name__ == "__main__":
    unittest.main()
