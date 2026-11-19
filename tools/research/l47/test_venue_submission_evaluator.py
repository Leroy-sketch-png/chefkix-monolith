"""
Tests for L47 Venue and Submission Evaluator.

Verifies:
1. Exact status of ARR calendar deadlines as of 2026-11-05.
2. Expired venues: Oct 2026, EACL 2027, NAACL 2027, COLING 2027.
3. Provisional status for ACL 2027 (January 2027 unannounced).
4. Sustainable Reviewing policy requirements.
5. Strict fail-closed submission denial and OPEN lead_status.
"""

from __future__ import annotations

import unittest

from venue_submission_evaluator import (
    OFFICIAL_VENUES,
    SustainableReviewingReadiness,
    evaluate_venue_calendar,
)


class VenueCalendarTests(unittest.TestCase):

    def test_official_venue_count(self):
        self.assertEqual(len(OFFICIAL_VENUES), 5)

    def test_expired_venues_list(self):
        res = evaluate_venue_calendar()
        expected_expired = ["ARR_OCT_2026", "EACL_2027", "NAACL_2027", "COLING_2027"]
        self.assertEqual(res.expired_venues, expected_expired)

    def test_acl_2027_is_provisional(self):
        res = evaluate_venue_calendar()
        self.assertEqual(res.provisional_venues, ["ACL_2027"])

    def test_lead_status_always_open(self):
        res = evaluate_venue_calendar()
        self.assertEqual(res.lead_status, "OPEN")

    def test_submission_never_permitted_by_default(self):
        res = evaluate_venue_calendar()
        self.assertFalse(res.submission_permitted)
        self.assertFalse(res.readiness_satisfied)
        self.assertTrue(len(res.unmet_readiness_conditions) > 0)

    def test_submission_denied_even_with_satisfied_readiness(self):
        # Even if local author profiles and service contributors are verified,
        # ACL 2027 dates are unannounced and remaining venues are expired.
        readiness = SustainableReviewingReadiness(
            author_openreview_profile_verified=True,
            author_orcid_verified=True,
            service_contributor_assigned=True,
            service_form_48h_window_monitored=True,
            submission_cap_observed=True,
            policy_wip_disclaimer_acknowledged=True,
        )
        res = evaluate_venue_calendar(readiness=readiness)
        self.assertTrue(res.readiness_satisfied)
        self.assertEqual(res.unmet_readiness_conditions, [])
        self.assertFalse(res.submission_permitted)  # Still false because ACL 2027 has no exact deadline
        self.assertEqual(res.lead_status, "OPEN")

    def test_unmet_readiness_details(self):
        readiness = SustainableReviewingReadiness(
            author_openreview_profile_verified=False,
            author_orcid_verified=True,
            service_contributor_assigned=False,
            service_form_48h_window_monitored=True,
            submission_cap_observed=True,
            policy_wip_disclaimer_acknowledged=True,
        )
        res = evaluate_venue_calendar(readiness=readiness)
        self.assertFalse(res.readiness_satisfied)
        self.assertIn("author_openreview_profile_unverified", res.unmet_readiness_conditions)
        self.assertIn("service_contributor_unassigned", res.unmet_readiness_conditions)


if __name__ == "__main__":
    unittest.main()
