import unittest

from test_vietnamese_pilot_sampling import make_dummy_record
from tools.research.l40.vietnamese_pilot_review import ReviewCandidate, build_reviewer_payloads
from tools.research.l40.vietnamese_pilot_sampling import REGIONS, run_vietnamese_pilot_sampling


class ReviewPayloadTests(unittest.TestCase):
    pin = "ab" * 32
    secret = b"x" * 32

    def setUp(self):
        records = [
            make_dummy_record(
                "PRIVATE_CASE_" + region + str(index), "constraint", region, "rice",
                "PRIVATE_CLUSTER_" + region + str(index),
            )
            for region, count in zip(REGIONS, (14, 13, 13))
            for index in range(count)
        ]
        self.sample = run_vietnamese_pilot_sampling(records, self.pin)
        self.slates = {
            row.case_id: [ReviewCandidate(f"PRIVATE_CANDIDATE_{i}", (f"tên {i}",)) for i in range(5)]
            for row in records
        }

    def build(self, slates):
        return build_reviewer_payloads(self.sample, "constraint", slates, self.secret)

    def test_payload_separation_and_input_order_invariance(self):
        packs, key = self.build(self.slates)
        reversed_slates = {case_id: list(reversed(slate)) for case_id, slate in reversed(self.slates.items())}
        self.assertEqual((packs, key), self.build(reversed_slates))
        self.assertEqual(len(key["entries"]), 400)
        self.assertEqual(len(packs["R1"]["views"]), 40)
        self.assertEqual(packs["R1"]["label_codes"], ["A", "C", "N", "U"])
        self.assertTrue({view["view_id"] for view in packs["R1"]["views"]}.isdisjoint(
            view["view_id"] for view in packs["R2"]["views"]
        ))
        self.assertNotIn("PRIVATE_", str(packs))
        self.assertNotIn("english_gloss", str(packs))
        self.assertNotIn("allergen_source_relation", str(packs))

    def test_empty_slate_retained_but_missing_case_rejected(self):
        case_id = next(iter(self.slates))
        self.slates[case_id] = []
        packs, key = self.build(self.slates)
        for pack in packs.values():
            self.assertEqual(len(pack["views"]), 40)
            self.assertEqual(sum(not view["candidates"] for view in pack["views"]), 1)
        self.assertEqual(sum(row["case_id"] == case_id for row in key["entries"]), 2)
        del self.slates[case_id]
        with self.assertRaisesRegex(ValueError, "Slate keys"):
            self.build(self.slates)

    def test_only_optional_extra_gold_can_be_a_sixth_displayed_item(self):
        slate = next(iter(self.slates.values()))
        slate.append(ReviewCandidate("PRIVATE_GOLD", ("tên nguồn",)))
        with self.assertRaisesRegex(ValueError, "optional gold"):
            self.build(self.slates)
        slate[-1] = ReviewCandidate("PRIVATE_GOLD", ("tên nguồn",), extra_recorded_gold=True)
        packs, key = self.build(self.slates)
        self.assertNotIn("PRIVATE_GOLD", str(packs))
        self.assertNotIn("extra_recorded_gold", str(packs))
        self.assertTrue(any(row.get("identity_id") == "PRIVATE_GOLD" for row in key["entries"]))

    def test_rejects_shortfall_and_duplicate_candidate_identity(self):
        shortfall = run_vietnamese_pilot_sampling([], self.pin)
        with self.assertRaisesRegex(ValueError, "fixed allocation"):
            build_reviewer_payloads(shortfall, "constraint", {}, self.secret)
        slate = next(iter(self.slates.values()))
        slate.append(slate[0])
        with self.assertRaisesRegex(ValueError, "deduplicated"):
            self.build(self.slates)
