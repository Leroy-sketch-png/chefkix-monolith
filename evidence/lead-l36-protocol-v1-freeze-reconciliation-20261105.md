# L36 — Protocol v1 freeze reconciliation

Date: 2026-11-05
Scope: reconcile the Lead L36 audit-method status with the byte-frozen protocol and its canonical instrument chapters. This is a bounded document/source reconciliation, not a human audit, statistical result, protocol amendment, or milestone claim.

## Frozen facts verified

- Protocol: `research/protocols/IRON-CHEF-PROTOCOL-v1.md`, SHA-256 `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.
- Primary estimand: conditional rescue `P(V=1 | H=0)` in the unseen GISMo-identity population, primary GISMo arm, strict valid@5. Joint rescue is secondary. Support requires the stratified-design 95% lower bound `>= 0.15`; refute only when the upper bound is `< 0.15`; otherwise inconclusive.
- Freeze-A sample: 150 cases, allocation 50/50/25/25 across unseen-profiled, unseen-unprofiled, seen-profiled, seen-unprofiled; `k=5`; frame weights `W_h=N_h/n_h`; stratified ratio estimator; 2,000 case-cluster bootstrap draws, seed `20261021`; undersupplied strata are refused. The recorded frame is 10,747, of which 3,815 are unseen by GISMo identity.
- Gold/labels: blind the recorded gold like every candidate; preserve raw A/C/N/U immutably and adjudication separately/versioned; if both raters mark gold N, report “reference disputed” separately and retain benchmark Hit unchanged. `U` is unknown, not a negative.
- Identity: pinned GISMo vocabulary exact match is primary; raw strings retained; Senath normalization/reviewed aliases are named sensitivity analyses.
- Human audit remains unperformed. No independent labels, adjudication outcomes, reliability statistic, H1 result, C1 acceptance, or mission score follows from a frozen/prepared method.

## Remaining protocol-text ambiguity

The protocol and Chapter 6 define strict `A` and a separate adjudicated field, but do not explicitly state whether case-level `V` is computed from raw-rater labels or the adjudicated label. They also do not specify how a case with no `A` but at least one adjudicated `U` enters the primary binary `V` calculation. The distinction affects the estimand's operational mapping and cannot be resolved by source inspection or synthetic fixtures. No protocol bytes were changed. A single targeted HUMAN method clarification has been requested; scoring remains held until answered.

## Source pointers and action

- `research/protocols/IRON-CHEF-PROTOCOL-v1.md` §2.
- `research/chapters/04-instrument-and-identity-contract.md` §§4.4, 4.6.
- `research/chapters/06-human-validity-audit.md` §§6.3–6.8.
- Research-workspace `academic_vision_and_strategy.md` §§2, 3, 6, 16, 24 was reconciled to identify the frozen decisions and the narrow remaining ambiguity. That workspace has no configured Git remote; this note and the shared backlog row are the fetchable collaboration record.

No source rows, participant data, labels, APIs, GPUs, network data access, spending, or protocol mutation were involved. No L36 milestone or C1 contribution acceptance is claimed.

## Lead recommendation for the remaining operational mapping (2026-11-05)

The HUMAN has not adopted an answer in the current decision record. Do not treat broad authorization as a scientific-method choice. The estimand stays `P(V=1 | H=0)` for the primary GISMo arm's frozen top-5 on the unseen-identity stratum; the options below concern only how case-level `V` is observed. No scoring is authorized until one choice is explicitly adopted and the prospective specification is versioned. Protocol v1 stays byte-frozen.

**Recommended: adjudicated strict-A with fixed-denominator partial identification.** For each H=0 case, `V=1` if any of the five primary-arm candidates has a final, separately stored adjudicated `A`; `V=0` only when all five are resolved as `C` or `N`; if no candidate is `A` and at least one is adjudicated `U`, preserve the case as `V=unknown`. Keep every sampled H=0 case in the weighted denominator. Report a lower estimate assigning unknown case outcomes to 0 and an upper estimate assigning them to 1, then apply the frozen case-cluster procedure to both endpoints. Support only if the lower 95% confidence bound for the lower endpoint is at least 0.15; refute only if the upper 95% confidence bound for the upper endpoint is below 0.15; otherwise report inconclusive. This keeps adjudication as the final validity classification, preserves raw ratings for reliability, honors `U` as unknown, and avoids silently dropping difficult cases. The likely cost is a wider interval or an inconclusive result when `U` is common. Endpoint bootstrap mechanics require prospective freeze; they are not claimed as already specified in v1.

**Alternative 2: raw-rater union, with unknown bounds.** Define a candidate as raw-accepted if either independent rater records `A`; case `V=1` if any primary-arm top-5 candidate is raw-accepted; `V=0` only when both raters rate all five `C` or `N`; otherwise, when no `A` and any rating is `U`, keep `V` unknown and use the same bounds. This avoids adjudication discretion in the primary but estimates “at least one rater accepts,” not the adjudicated construct; it is leniency-sensitive and would make adjudication a sensitivity analysis.

**Alternative 3: adjudicated complete-case point estimate.** Use adjudicated strict `A` with the same case rule, but omit no-`A` cases containing any `U` from the binary estimate. This is simpler but changes the target to `P(V=1 | H=0, determinately classified)`, changes the denominator, and may select on case difficulty or evidence sufficiency. It is not directly comparable to the frozen full-denominator estimand and should not be the primary recommendation.

Counting `U` as `V=0` is not offered as a valid primary alternative: it directly treats unknown as negative. It may be shown only as the lower endpoint of the recommended bound, not as an observed label.

**Decision requested once:** choose Alternative 1 (recommended), 2, or 3 for a prospective version; or state a different rule. No option changes prior labels/results, raw rater records, gold/Hit@k, the 150-case sample, weights, population, or protocol v1 bytes. PR13 ethics/consent and actual rater qualifications remain separate gates.
