# C1 Standalone Reviewer Handouts

English instructions requested by HUMAN on 2026-11-06 after identifying two reviewers.

## Deliverables

- `output/pdf/reviewer-quick-guide.pdf`: one-page instructions to give each reviewer.
- `output/pdf/organizer-handoff-checklist.pdf`: one-page coordinator checklist, separate from reviewer materials.
- `handouts.json`: exact authored content for the PDFs.

These are Lead-owned measurement instructions, not a replacement app, populated audit pack, human-validation result, or new protocol. The first audit roster follows monolith `8432b52`: primary GISMo top-five plus recorded gold, on the unchanged 150-case frozen sample. The gold is never identified to reviewers. There are at most six unique candidates per case before deduplication; actual workload requires the real slate.

The rubric follows Protocol v1 section 2 and the existing C1 Chapter 4/6 handbook semantics. Ordinary quantity logic is compatible with A; a meaningful quantity/technique/companion change is C. U is unknown, not negative. No reliability threshold, H1 hypothesis, gold identity, model rank, or adjudication outcome is shown in the reviewer guide.

## Current Collection State — 2026-11-07

Both complete independent returns were analyzed privately under frozen Protocol v1 after HUMAN reported that institutional ethics/consent coverage includes this activity (not independently documentary-verified). The fixed 150-case/867-pair cohort yielded four-category agreement 717/867 (82.7%; Wilson 95% CI 80.0–85.1%) and unweighted nominal Cohen’s κ=0.747 (2,000-draw case-cluster percentile-bootstrap 95% CI 0.701–0.787; seed `20261021`); both frozen PR10 thresholds are met. No adjudication or primary H1 estimate was produced, and no C1 milestone/acceptance is claimed by this handoff. Raw returns, case-level disagreement queue, sealed mapping, and detailed receipts remain owner-private. Publicly shared information is limited to these aggregate statistics and the method/code reference. Private result receipt is local only; the frozen protocol was not changed.

The private execution helper is `tools/research/c1/compute_agreement.py` (SHA-256 `2337b8ed283a8cca3eb4fb3031a96f9850a0ea3fb1a27d757c90d56a2117c738`); its source-free math/cluster tests are `tools/research/c1/test_compute_agreement.py` (4/4 pass; SHA-256 `84a378b8e4d4ccb7286abb7aaef360bec13b92f1912302164a45acac01f26632`). No private artifact is packaged in this repository.

Method qualification: Protocol v1 requires case-cluster bootstrap bounds but does not state a quantile convention. The private receipt explicitly binds the percentile implementation before the corrected run; the lower-bound threshold is therefore reported as met under that implementation pending canonical review, not as a protocol change.

## Historical Collection State — before agreement analysis

The paragraph below is retained as the intake-time snapshot; its “Current state” wording records what was true before the subsequent analysis and is not the present status.

Current state (2026-11-07): HUMAN returned both independent ratings. The owner-private intake receipt records complete issued-pack-bound returns for 150 cases and 867 candidate entries per rater; raw files remain unchanged in the private archive. No agreement analysis or adjudication has been performed, and no scientific acceptance or milestone is claimed. The frozen declaration still records `BLOCKED_PENDING_ETHICS_CLEARANCE`; the applicable institutional determination/consent status must be established before the protocol's agreement evaluation. The primary rescue estimate also remains blocked on the already-recorded prospective raw-versus-adjudicated `V`/unresolved-`U` interpretation. Next owner action: establish the ethics/consent status, then perform only the frozen private agreement analysis and preserve any adjudication separately. Return files, raw labels, case data, and sealed mapping stay outside Git; no return-level hashes or values are published here.

Existing M-B packaging acceptance: two standalone personal packs; correct frozen sample and roster; blinded case/candidate content; no sealed key or cross-reviewer leakage; automatic stable-identifier preservation; A/C/N/U and optional notes; progress/unanswered visibility; save/resume; complete export; exact per-reviewer candidate counts and pack manifests. Reuse the existing audit path. Do not create a new research framework, change sampling, generate replacement model outputs, or expose the combined 300-view file to both reviewers.

User-facing timing is conditional arithmetic only. Observed pace on the first five real cases can inform scheduling, not alter the frozen rubric, sample, candidate policy, or completed labels. Adjudication is separate from original independent judgments. No third-person qualification requirement is invented here.

Only safe authored handouts are published. Actual source rows, private identities, consent files, sealed mappings and human judgments do not belong in this public repository.
