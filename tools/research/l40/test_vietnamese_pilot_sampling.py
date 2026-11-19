"""
Unit tests for L40 Vietnamese Pilot Deterministic Sampling Frame Validator and Allocator.

Verifies:
1. Seed derivation bit-invariance and hex validation.
2. Case rank determinism and tie-breaking.
3. EligibleCaseRecord schema, diacritics, and NFC validation.
4. Exact cohort targets: Culinary=60 (20/20/20), Constraint=40 (14/13/13).
5. Cross-cohort recipe cluster deduplication.
6. Least-filled region x family cell round-robin allocation.
7. Named family floors (>= 5 in culinary, >= 3 in constraint) and shortfall reporting.
8. Fallback families (other, unknown) never filling named family floors.
"""

import unittest
import unicodedata

from tools.research.l40.vietnamese_pilot_sampling import (
    derive_sampling_seed,
    compute_case_rank,
    EligibleCaseRecord,
    allocate_cohort,
    run_vietnamese_pilot_sampling,
    CULINARY_TARGETS,
    CONSTRAINT_TARGETS,
    NAMED_DISH_FAMILIES,
    FALLBACK_DISH_FAMILIES,
    REGIONS,
)


def make_dummy_record(
    case_id: str,
    cohort_id: str,
    region: str,
    dish_family: str,
    cluster_id: str,
    recipe_id: str = "rec_1",
    title: str = "Phở bò truyền thống",
    target_slot: str = "thịt bò tái",
    context: str = "Nấu nước dùng phở bò với gừng nướng, quế hồi",
    constraint: str = "không thịt đỏ",
    allergen_relation: str = "WHO/IUIS Bovine albumin",
) -> EligibleCaseRecord:
    """Helper to create a valid EligibleCaseRecord for testing."""
    return EligibleCaseRecord(
        case_id=case_id,
        source_recipe_id=recipe_id,
        recipe_cluster_id=cluster_id,
        target_slot_id="slot_0",
        cohort_id=cohort_id,
        region=region,
        dish_family=dish_family,
        vietnamese_title=unicodedata.normalize("NFC", title),
        vietnamese_ingredient_list=["thịt bò", "bánh phở", "hành lá", "nước dùng"],
        vietnamese_target_slot=unicodedata.normalize("NFC", target_slot),
        vietnamese_context=unicodedata.normalize("NFC", context),
        english_gloss={"title": "Traditional beef pho", "target": "rare beef"},
        rights_record_id="RR-VN-2026-001",
        rights_scope="research_blind_evaluation",
        declared_constraint=constraint if cohort_id == "constraint" else None,
        allergen_source_relation=allergen_relation if cohort_id == "constraint" else None,
    )


class SeedDerivationTests(unittest.TestCase):
    def test_seed_derivation_bit_exact(self):
        dummy_hash = "a" * 64
        seed1 = derive_sampling_seed(dummy_hash)
        seed2 = derive_sampling_seed(dummy_hash)
        self.assertEqual(seed1, seed2)
        self.assertIsInstance(seed1, int)
        self.assertGreater(seed1, 0)

    def test_seed_derivation_invalid_hex(self):
        with self.assertRaises(ValueError):
            derive_sampling_seed("not_64_chars")

        with self.assertRaises(ValueError):
            derive_sampling_seed("g" * 64)  # non-hex chars


class CaseRankTests(unittest.TestCase):
    def test_rank_determinism(self):
        seed = 123456789012345
        rank1 = compute_case_rank(seed, "VN_CASE_001")
        rank2 = compute_case_rank(seed, "VN_CASE_001")
        self.assertEqual(rank1, rank2)

    def test_rank_differentiation(self):
        seed = 123456789012345
        rank1 = compute_case_rank(seed, "VN_CASE_001")
        rank2 = compute_case_rank(seed, "VN_CASE_002")
        self.assertNotEqual(rank1[0], rank2[0])


class RecordValidationTests(unittest.TestCase):
    def test_valid_culinary_record(self):
        rec = make_dummy_record("C01", "culinary", "north", "noodle_soup", "CL01")
        rec.validate()

    def test_valid_constraint_record(self):
        rec = make_dummy_record("C02", "constraint", "south", "rice", "CL02")
        rec.validate()

    def test_invalid_region_rejected(self):
        rec = make_dummy_record("C03", "culinary", "mars", "noodle_soup", "CL03")
        with self.assertRaises(ValueError):
            rec.validate()

    def test_invalid_family_rejected(self):
        rec = make_dummy_record("C04", "culinary", "central", "pizza", "CL04")
        with self.assertRaises(ValueError):
            rec.validate()

    def test_constraint_without_constraint_details_rejected(self):
        rec = EligibleCaseRecord(
            case_id="C05",
            source_recipe_id="R1",
            recipe_cluster_id="CL1",
            target_slot_id="S1",
            cohort_id="constraint",
            region="north",
            dish_family="noodle_soup",
            vietnamese_title="Bún chả",
            vietnamese_ingredient_list=["thịt heo", "bún"],
            vietnamese_target_slot="thịt heo",
            vietnamese_context="Nướng thịt trên than hoa",
            english_gloss={"title": "Bun cha"},
            rights_record_id="RR-01",
            rights_scope="eval",
            declared_constraint=None,  # Missing!
            allergen_source_relation=None,
        )
        with self.assertRaises(ValueError):
            rec.validate()

    def test_non_nfc_normalization_rejected(self):
        # Create decomposed unicode (NFD) string
        nfd_title = unicodedata.normalize("NFD", "Phở bò")
        rec = EligibleCaseRecord(
            case_id="C06",
            source_recipe_id="R1",
            recipe_cluster_id="CL1",
            target_slot_id="S1",
            cohort_id="culinary",
            region="north",
            dish_family="noodle_soup",
            vietnamese_title=nfd_title,  # NFD, not NFC!
            vietnamese_ingredient_list=["bò"],
            vietnamese_target_slot="bò",
            vietnamese_context="Nấu nước dùng",
            english_gloss={"title": "Pho"},
            rights_record_id="RR-01",
            rights_scope="eval",
        )
        with self.assertRaises(ValueError):
            rec.validate()


class AllocationTargetTests(unittest.TestCase):
    def setUp(self):
        self.seed = 20261106

    def test_full_allocation_success(self):
        # Build sufficient eligible records:
        # Culinary needs: North=20, Central=20, South=20 across 7 families
        records = []
        rec_idx = 0

        # Culinary pool: 30 per region (10 in each region x family), enough for all targets
        for reg in REGIONS:
            for fam in NAMED_DISH_FAMILIES:
                for k in range(5):
                    rec_idx += 1
                    records.append(
                        make_dummy_record(
                            case_id=f"CUL_{rec_idx}",
                            cohort_id="culinary",
                            region=reg,
                            dish_family=fam,
                            cluster_id=f"CLUSTER_CUL_{rec_idx}",
                        )
                    )

        # Constraint pool: 20 per region across families
        for reg in REGIONS:
            for fam in NAMED_DISH_FAMILIES:
                for k in range(4):
                    rec_idx += 1
                    records.append(
                        make_dummy_record(
                            case_id=f"CON_{rec_idx}",
                            cohort_id="constraint",
                            region=reg,
                            dish_family=fam,
                            cluster_id=f"CLUSTER_CON_{rec_idx}",
                        )
                    )

        result = run_vietnamese_pilot_sampling(records, "0123456789abcdef" * 4)

        self.assertTrue(result.is_success)
        self.assertEqual(result.status, "SUCCESS")
        self.assertEqual(result.total_allocated, 100)
        self.assertTrue(result.cluster_deduplication_passed)

        # Verify Culinary targets
        cul = result.culinary
        self.assertEqual(cul.allocated_count, 60)
        self.assertEqual(cul.regional_counts["north"], 20)
        self.assertEqual(cul.regional_counts["central"], 20)
        self.assertEqual(cul.regional_counts["south"], 20)
        for fam in NAMED_DISH_FAMILIES:
            self.assertGreaterEqual(cul.family_counts[fam], 5)

        # Verify Constraint targets
        con = result.constraint
        self.assertEqual(con.allocated_count, 40)
        self.assertEqual(con.regional_counts["north"], 14)
        self.assertEqual(con.regional_counts["central"], 13)
        self.assertEqual(con.regional_counts["south"], 13)
        for fam in NAMED_DISH_FAMILIES:
            self.assertGreaterEqual(con.family_counts[fam], 3)

    def test_cluster_deduplication_across_cohorts(self):
        # If a cluster is used in culinary, constraint cannot use it
        records = []
        rec_idx = 0
        # Common cluster between culinary and constraint
        shared_cluster = "SHARED_CLUSTER_123"

        records.append(
            make_dummy_record(
                case_id="CUL_SHARED",
                cohort_id="culinary",
                region="north",
                dish_family="noodle_soup",
                cluster_id=shared_cluster,
            )
        )
        records.append(
            make_dummy_record(
                case_id="CON_SHARED",
                cohort_id="constraint",
                region="north",
                dish_family="noodle_soup",
                cluster_id=shared_cluster,
            )
        )

        # Fill rest of culinary
        for reg in REGIONS:
            for fam in NAMED_DISH_FAMILIES:
                for k in range(5):
                    rec_idx += 1
                    records.append(
                        make_dummy_record(
                            case_id=f"CUL_{rec_idx}",
                            cohort_id="culinary",
                            region=reg,
                            dish_family=fam,
                            cluster_id=f"CL_CUL_{rec_idx}",
                        )
                    )

        # Fill rest of constraint
        for reg in REGIONS:
            for fam in NAMED_DISH_FAMILIES:
                for k in range(4):
                    rec_idx += 1
                    records.append(
                        make_dummy_record(
                            case_id=f"CON_{rec_idx}",
                            cohort_id="constraint",
                            region=reg,
                            dish_family=fam,
                            cluster_id=f"CL_CON_{rec_idx}",
                        )
                    )

        result = run_vietnamese_pilot_sampling(records, "fedcba9876543210" * 4)
        self.assertTrue(result.cluster_deduplication_passed)

        # Ensure shared cluster was only allocated once
        cul_clusters = {c.recipe_cluster_id for c in result.culinary.allocated_cases}
        con_clusters = {c.recipe_cluster_id for c in result.constraint.allocated_cases}
        self.assertTrue(cul_clusters.isdisjoint(con_clusters))

    def test_regional_shortfall_stops_cohort(self):
        # Provide only 5 records for North in culinary (target is 20)
        records = []
        rec_idx = 0
        for k in range(5):
            rec_idx += 1
            records.append(
                make_dummy_record(
                    case_id=f"CUL_N_{rec_idx}",
                    cohort_id="culinary",
                    region="north",
                    dish_family="rice",
                    cluster_id=f"CL_{rec_idx}",
                )
            )

        alloc = allocate_cohort(records, "culinary", self.seed)
        self.assertFalse(alloc.is_success)
        self.assertEqual(alloc.status, "SHORTFALL")
        self.assertLess(alloc.allocated_count, 60)
        self.assertTrue(any("Regional shortfall in north" in r for r in alloc.shortfall_reasons))

    def test_named_family_shortfall_reported(self):
        # Provide plenty of noodle_soup and rice, but 0 sweet_soup
        records = []
        rec_idx = 0
        for reg in REGIONS:
            for fam in ["noodle_soup", "rice"]:
                for k in range(15):
                    rec_idx += 1
                    records.append(
                        make_dummy_record(
                            case_id=f"CUL_{rec_idx}",
                            cohort_id="culinary",
                            region=reg,
                            dish_family=fam,
                            cluster_id=f"CL_{rec_idx}",
                        )
                    )

        alloc = allocate_cohort(records, "culinary", self.seed)
        self.assertEqual(alloc.family_counts["sweet_soup"], 0)
        self.assertIn("sweet_soup", alloc.family_shortfalls)
        self.assertEqual(alloc.family_shortfalls["sweet_soup"], 5)
        self.assertTrue(any("Named family floor shortfall for 'sweet_soup'" in r for r in alloc.shortfall_reasons))

    def test_fallback_families_never_fill_named_floors(self):
        # Populate with 'other' and 'unknown' dish families
        records = []
        rec_idx = 0
        for reg in REGIONS:
            for fam in FALLBACK_DISH_FAMILIES:
                for k in range(15):
                    rec_idx += 1
                    records.append(
                        make_dummy_record(
                            case_id=f"CUL_{rec_idx}",
                            cohort_id="culinary",
                            region=reg,
                            dish_family=fam,
                            cluster_id=f"CL_{rec_idx}",
                        )
                    )

        alloc = allocate_cohort(records, "culinary", self.seed)
        # Even if 60 cases were allocated across 'other'/'unknown',
        # all 7 named dish families must have shortfalls of 5
        for fam in NAMED_DISH_FAMILIES:
            self.assertEqual(alloc.family_shortfalls[fam], 5)


if __name__ == "__main__":
    unittest.main()
