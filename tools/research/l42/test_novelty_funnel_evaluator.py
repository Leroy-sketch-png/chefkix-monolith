"""
Unit tests for L42: §25.4 Novelty Evidence Funnel & Defensible Contribution Evaluator.

Verifies:
1. Canonical prior art source taxonomy integrity and SHA-256 bindings.
2. Literature deduplication by DOI and normalized title.
3. Broad-claim refutation matrix: fail-closed rejection of unsubstantiated priority claims.
4. Candidate differentiator evaluation under the 5-attribute thesis design.
5. S43/S46, S47, and Morales-Garzon lineage discrepancy verifications.
6. Full funnel report generation, research question preservation, and Claims Register v20 alignment.
"""

import pytest

from tools.research.l42.novelty_funnel_evaluator import (
    BroadClaimType,
    DispositionStatus,
    SourceRecord,
    CLAIMS_REGISTER_V20_SHA256,
    PINNED_S43_PDF_SHA256,
    PINNED_S46_PDF_SHA256,
    PINNED_S57_PDF_SHA256,
    SURVIVING_RESEARCH_QUESTION,
    deduplicate_literature_records,
    evaluate_broad_claim,
    get_canonical_prior_art_sources,
    normalize_title,
    run_novelty_funnel_evaluation,
    verify_morales_garzon_lineage,
    verify_s43_s46_count_discrepancy,
    verify_s47_count_discrepancy,
)


class TestCanonicalTaxonomy:
    def test_canonical_source_count_and_keys(self):
        sources = get_canonical_prior_art_sources()
        assert len(sources) >= 12
        source_ids = {s.source_id for s in sources}
        expected_ids = {"S43", "S46", "S47", "S48", "S49", "S55", "S56", "S57", "S58", "S60", "S61", "S62"}
        assert expected_ids.issubset(source_ids)

    def test_pinned_hashes_match(self):
        sources = {s.source_id: s for s in get_canonical_prior_art_sources()}
        assert sources["S43"].pdf_sha256 == PINNED_S43_PDF_SHA256
        assert sources["S46"].pdf_sha256 == PINNED_S46_PDF_SHA256
        assert sources["S57"].pdf_sha256 == PINNED_S57_PDF_SHA256


class TestDeduplication:
    def test_normalize_title(self):
        assert normalize_title("AdaptaFood: An Interactive Tool!") == "adaptafood an interactive tool"
        assert normalize_title("  Recipe-Context   Substitution...  ") == "recipecontext substitution"

    def test_deduplicate_by_doi(self):
        rec1 = SourceRecord(
            source_id="T01",
            citation_key="Rec1",
            title="Title A",
            authors=("Author 1",),
            year=2020,
            venue="Venue 1",
            doi="10.1000/182",
        )
        rec2 = SourceRecord(
            source_id="T02",
            citation_key="Rec2",
            title="Different Title",
            authors=("Author 2",),
            year=2021,
            venue="Venue 2",
            doi="10.1000/182",  # Same DOI
        )
        deduped, dup_count = deduplicate_literature_records([rec1, rec2])
        assert len(deduped) == 1
        assert dup_count == 1
        assert deduped[0].source_id == "T01"

    def test_deduplicate_by_normalized_title(self):
        rec1 = SourceRecord(
            source_id="T01",
            citation_key="Rec1",
            title="Exploiting Food Embeddings",
            authors=("Author 1",),
            year=2020,
            venue="Venue 1",
        )
        rec2 = SourceRecord(
            source_id="T02",
            citation_key="Rec2",
            title="exploiting food embeddings!!!",
            authors=("Author 2",),
            year=2021,
            venue="Venue 2",
        )
        deduped, dup_count = deduplicate_literature_records([rec1, rec2])
        assert len(deduped) == 1
        assert dup_count == 1


class TestBroadClaimEvaluation:
    @pytest.fixture
    def canonical_sources(self):
        return get_canonical_prior_art_sources()

    def test_refute_first_substitution_system(self, canonical_sources):
        eval_res = evaluate_broad_claim(BroadClaimType.FIRST_SUBSTITUTION_SYSTEM, canonical_sources)
        assert eval_res.is_refuted is True
        assert eval_res.disposition == DispositionStatus.CONTRADICTED_DO_NOT_CLAIM
        assert "S55" in eval_res.refuting_sources
        assert "S56" in eval_res.refuting_sources

    def test_refute_first_human_evaluated_substitution(self, canonical_sources):
        eval_res = evaluate_broad_claim(BroadClaimType.FIRST_HUMAN_EVALUATED_SUBSTITUTION, canonical_sources)
        assert eval_res.is_refuted is True
        assert eval_res.disposition == DispositionStatus.CONTRADICTED_DO_NOT_CLAIM
        assert "S46" in eval_res.refuting_sources
        assert "S47" in eval_res.refuting_sources

    def test_refute_first_context_aware_ranking(self, canonical_sources):
        eval_res = evaluate_broad_claim(BroadClaimType.FIRST_CONTEXT_AWARE_RANKING, canonical_sources)
        assert eval_res.is_refuted is True
        assert eval_res.disposition == DispositionStatus.CONTRADICTED_DO_NOT_CLAIM
        assert "S43" in eval_res.refuting_sources
        assert "S47" in eval_res.refuting_sources

    def test_refute_first_food_swap_acceptance(self, canonical_sources):
        eval_res = evaluate_broad_claim(BroadClaimType.FIRST_FOOD_SWAP_ACCEPTANCE, canonical_sources)
        assert eval_res.is_refuted is True
        assert eval_res.disposition == DispositionStatus.CONTRADICTED_DO_NOT_CLAIM
        assert "S48" in eval_res.refuting_sources

    def test_candidate_differentiator_only_novelty_unestablished(self, canonical_sources):
        eval_res = evaluate_broad_claim(BroadClaimType.FIRST_SHARED_POOL_BLINDED_ACNU_AUDIT, canonical_sources)
        assert eval_res.is_refuted is False
        assert eval_res.disposition == DispositionStatus.CANDIDATE_DIFFERENTIATOR_ONLY
        assert len(eval_res.refuting_sources) == 0
        assert "UNESTABLISHED" in eval_res.evidence_summary

    def test_hypothetical_full_anticipation_refutes_design(self):
        anticipating_source = SourceRecord(
            source_id="S999",
            citation_key="Prior2019",
            title="Anticipating Study",
            authors=("Someone",),
            year=2019,
            venue="Conf",
            has_shared_blinded_pool=True,
            has_acnu_labels=True,
            has_single_gold_rescue=True,
            has_unseen_identity_strata=True,
            has_separate_allergen_endpoint=True,
        )
        eval_res = evaluate_broad_claim(
            BroadClaimType.FIRST_SHARED_POOL_BLINDED_ACNU_AUDIT,
            [anticipating_source],
        )
        assert eval_res.is_refuted is True
        assert eval_res.disposition == DispositionStatus.CONTRADICTED_DO_NOT_CLAIM
        assert "S999" in eval_res.refuting_sources


class TestDiscrepancyVerifiers:
    @pytest.fixture
    def source_map(self):
        return {s.source_id: s for s in get_canonical_prior_art_sources()}

    def test_verify_s43_s46_count(self, source_map):
        res = verify_s43_s46_count_discrepancy(source_map["S43"], source_map["S46"])
        assert res["s43_pdf_sha256_match"] is True
        assert res["s46_pdf_sha256_match"] is True
        assert res["ground_truth_pair_count"] == 708
        assert res["prediction_export_row_count"] == 13056
        assert res["discrepancy_resolved_for_caveat"] is True
        assert res["editorial_cause_proven"] is False

    def test_verify_s47_count(self, source_map):
        res = verify_s47_count_discrepancy(source_map["S47"])
        assert res["reported_items"] == 150
        assert res["sum_of_disjoint_categories"] == 200
        assert res["status"] == "UNRESOLVED_CATEGORY_OVERLAP"

    def test_verify_morales_garzon_lineage(self, source_map):
        res = verify_morales_garzon_lineage(
            source_map["S57"], source_map["S58"], source_map["S60"]
        )
        assert res["s57_pdf_sha256_match"] is True
        assert res["s60_lfs_hash_pinned"] is True
        assert res["corpus_arithmetic_reconciled"] is True
        assert res["corpus_total_records"] == 267071
        assert res["s58_three_corpus_sum"] == 256392
        assert res["bbc_food_records"] == 10679
        assert res["lineage_rule"] == "ONE_LINKED_LINEAGE_NOT_INDEPENDENT_REPLICATIONS"
        assert res["participant_overlap_status"] == "UNKNOWN"
        assert res["case_overlap_status"] == "UNKNOWN"


class TestMasterFunnelRunner:
    def test_full_report_execution(self):
        report = run_novelty_funnel_evaluation()
        assert report.total_retrieved >= 12
        assert report.deduplicated_count == report.total_retrieved  # Canonical list is already deduplicated
        assert report.screened_title_abstract >= 12
        assert report.fulltext_reviewed >= 9

        # Broad claims verified refuted
        assert report.broad_claim_evaluations[BroadClaimType.FIRST_SUBSTITUTION_SYSTEM].is_refuted is True
        assert report.broad_claim_evaluations[BroadClaimType.FIRST_HUMAN_EVALUATED_SUBSTITUTION].is_refuted is True
        assert report.broad_claim_evaluations[BroadClaimType.FIRST_CONTEXT_AWARE_RANKING].is_refuted is True
        assert report.broad_claim_evaluations[BroadClaimType.FIRST_FOOD_SWAP_ACCEPTANCE].is_refuted is True
        assert report.broad_claim_evaluations[BroadClaimType.FIRST_SHARED_POOL_BLINDED_ACNU_AUDIT].is_refuted is False

        # SSoT boundaries preserved
        assert report.surviving_research_question == SURVIVING_RESEARCH_QUESTION
        assert report.r28_status == "UNVERIFIED"
        assert report.r29_status == "UNVERIFIED"
        assert report.claims_register_v20_sha256 == CLAIMS_REGISTER_V20_SHA256

        # Lineage discrepancies present
        assert "S43_S46_count" in report.lineage_discrepancies
        assert "S47_count" in report.lineage_discrepancies
        assert "MoralesGarzon_lineage" in report.lineage_discrepancies
