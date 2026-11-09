# L47 Venue, Submission, and Sustainable Reviewing Policy Evaluator

Implements and verifies the Lead-owned venue choice, submission deadlines,
and Sustainable Reviewing policy requirements specified in Lead Backlog L47
and `evidence/lead-l47-arr-calendar-20261105.md`.

## Evaluated Constraints

1. **Official Calendar Status (as of 2026-11-05):**
   - ARR October 2026 submission deadline (October 12) -> **EXPIRED**.
   - EACL 2027 final submission (August 3) and commitment (October 11) -> **EXPIRED**.
   - NAACL 2027 final submission (October 12) -> **EXPIRED**.
   - COLING 2027 final submission (October 12) -> **EXPIRED**.
   - ACL 2027 listed only as "January, 2027" -> **PROVISIONAL** candidate only.

2. **Sustainable Reviewing Policy Requirements:**
   - Author OpenReview profile and ORCID verification.
   - Designated qualified service-contributor assignment.
   - 48-hour post-deadline service form window.
   - Policy explicitly marked "Work in progress" -> fail-closed disposition.

3. **Lead Boundary:**
   - No premature submission, author commitment, or venue lock.
   - L47 status strictly preserved as `OPEN`.
   - Zero contribution score or C1–C5 acceptance credit implied.

## Verification

Run the test suite:
```bash
python -m pytest tools/research/l47/ -v
```
