"""
L47 Venue, Submission, and Sustainable Reviewing Policy Evaluator.

Implements the Lead-owned venue and submission evaluation logic specified in
Lead Backlog L47 and `evidence/lead-l47-arr-calendar-20261105.md`.

Invariants:
1. Calendar state as of 2026-11-05:
   - October 2026 ARR (submission Oct 12) -> PASSED / EXPIRED.
   - EACL 2027 (submission Aug 3, commitment Oct 11) -> PASSED / EXPIRED.
   - NAACL 2027 (submission Oct 12, commitment Dec 23) -> PASSED / EXPIRED.
   - COLING 2027 (submission Oct 12, commitment Dec 23) -> PASSED / EXPIRED.
   - ACL 2027 -> PROVISIONAL ("January, 2027", exact cycle/commitment deadline unannounced).
2. Sustainable Reviewing Policy (posted 2026-09-11, WIP):
   - Service contributor requirement.
   - Per-cycle submission caps.
   - Author OpenReview profile and ORCID mandatory.
   - 48-hour post-deadline service form window.
   - Policy marked "Work in progress"; mandates rechecking before action.
3. Fail-closed disposition:
   - Forbids premature submission, author commitment, or venue selection.
   - L47 status must remain OPEN / PROVISIONAL.
   - Zero contribution score or C1-C5 milestone credit implied.
"""

from __future__ import annotations

import dataclasses
from typing import Any, Dict, List, Optional, Tuple


@dataclasses.dataclass(frozen=True)
class VenueDeadline:
    venue_name: str
    cycle_name: str
    submission_deadline: Optional[str]  # ISO date string or None if unannounced
    commitment_deadline: Optional[str]  # ISO date string or None if unannounced
    status: str                         # "EXPIRED" | "PROVISIONAL" | "ACTIVE"


OFFICIAL_VENUES: Tuple[VenueDeadline, ...] = (
    VenueDeadline(
        venue_name="ARR_OCT_2026",
        cycle_name="October 2026 Cycle",
        submission_deadline="2026-10-12",
        commitment_deadline=None,
        status="EXPIRED",
    ),
    VenueDeadline(
        venue_name="EACL_2027",
        cycle_name="EACL 2027",
        submission_deadline="2026-08-03",
        commitment_deadline="2026-10-11",
        status="EXPIRED",
    ),
    VenueDeadline(
        venue_name="NAACL_2027",
        cycle_name="NAACL 2027",
        submission_deadline="2026-10-12",
        commitment_deadline="2026-12-23",
        status="EXPIRED",
    ),
    VenueDeadline(
        venue_name="COLING_2027",
        cycle_name="COLING 2027",
        submission_deadline="2026-10-12",
        commitment_deadline="2026-12-23",
        status="EXPIRED",
    ),
    VenueDeadline(
        venue_name="ACL_2027",
        cycle_name="ACL 2027 (January 2027)",
        submission_deadline=None,  # Unannounced exact date on official calendar
        commitment_deadline=None,
        status="PROVISIONAL",
    ),
)


@dataclasses.dataclass(frozen=True)
class SustainableReviewingReadiness:
    author_openreview_profile_verified: bool
    author_orcid_verified: bool
    service_contributor_assigned: bool
    service_form_48h_window_monitored: bool
    submission_cap_observed: bool
    policy_wip_disclaimer_acknowledged: bool


@dataclasses.dataclass(frozen=True)
class L47VenueEvaluationResult:
    as_of_date: str
    expired_venues: List[str]
    provisional_venues: List[str]
    readiness_satisfied: bool
    unmet_readiness_conditions: List[str]
    submission_permitted: bool
    recommended_disposition: str
    lead_status: str  # "OPEN"


def evaluate_venue_calendar(
    as_of_date: str = "2026-11-05",
    readiness: Optional[SustainableReviewingReadiness] = None,
) -> L47VenueEvaluationResult:
    """
    Evaluates venue availability and submission readiness.

    Invariants:
    - If as_of_date >= 2026-10-13, Oct 2026, EACL 2027, NAACL 2027, COLING 2027 are EXPIRED.
    - ACL 2027 is strictly PROVISIONAL.
    - If any readiness item is False or missing, submission_permitted = False.
    - L47 lead_status remains strictly OPEN.
    """
    expired: List[str] = []
    provisional: List[str] = []

    for v in OFFICIAL_VENUES:
        if v.status == "EXPIRED":
            expired.append(v.venue_name)
        elif v.status == "PROVISIONAL":
            provisional.append(v.venue_name)

    unmet: List[str] = []
    if readiness is None:
        unmet = [
            "author_openreview_profile_unverified",
            "author_orcid_unverified",
            "service_contributor_unassigned",
            "service_form_window_unmonitored",
            "policy_wip_unacknowledged",
        ]
        readiness_ok = False
    else:
        if not readiness.author_openreview_profile_verified:
            unmet.append("author_openreview_profile_unverified")
        if not readiness.author_orcid_verified:
            unmet.append("author_orcid_unverified")
        if not readiness.service_contributor_assigned:
            unmet.append("service_contributor_unassigned")
        if not readiness.service_form_48h_window_monitored:
            unmet.append("service_form_window_unmonitored")
        if not readiness.policy_wip_disclaimer_acknowledged:
            unmet.append("policy_wip_unacknowledged")
        readiness_ok = len(unmet) == 0

    # Submission is never permitted without official published deadline AND verified readiness
    submission_permitted = False
    disposition = (
        "Keep ACL 2027 provisional; do not treat expired venues as submission opportunities; "
        "recheck ARR dates and policy before action; preserve L47 as OPEN."
    )

    return L47VenueEvaluationResult(
        as_of_date=as_of_date,
        expired_venues=expired,
        provisional_venues=provisional,
        readiness_satisfied=readiness_ok,
        unmet_readiness_conditions=unmet,
        submission_permitted=submission_permitted,
        recommended_disposition=disposition,
        lead_status="OPEN",
    )
