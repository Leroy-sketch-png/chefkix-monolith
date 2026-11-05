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
