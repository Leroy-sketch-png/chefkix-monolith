# L43 C4 judge family and prompt decision

**Decision ID:** `iron-chef-l43-gemini-pool-prompt-v1`
**Date / authority:** 2026-11-06; bounded LEAD method decision under the HUMAN's existing mission authority.
**State:** Family and prospective prompt selected for the first C4 judge; implementation and human-calibration acceptance remain open. No Protocol v1 amendment, model call, spend, output, C4 milestone, or empirical claim.

## Pinned basis

- Frozen Protocol v1: SHA-256 `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`; §3.1 permits development/calibration and method choice only on assigned validation portions and requires family/version/prompt freeze before test scoring. §3.3 requires a judge family different from every judged arm and gives the human-comparison acceptance gates. §4 fixes the relevant candidate-producing arms and the exact authorized model/resource envelope.
- Operative strategy mirror: SHA-256 `31d601b85789856baa1be4676f42c13d8793050d4563ad904b092099d1a7a6e1`; §7.1's pool-level prompt is a reference template, explicitly not frozen in that source. This decision now selects it prospectively for the first judge; the source mirror itself remains unchanged.
- Existing C4 contract/runner reference files: SHA-256 `a823003838685d47ae851364faee25d8fe0c23cb6c31d4a96cdad3363dcd11de` and `c28d4d8e17923024fcf6e7f9ddf7a471f69543dbd78170314a8b8255f5f9c6bf`, respectively. C4-G1/G2 remain their historical, bounded source-contract and runner evidence; they do not prove this selected model-family compatibility or actual judge acceptance.

## Bounded selection

1. **First judge family/model:** Google Gemini, exact intended model `gemini-2.5-flash`, temperature `0`. This is already named in the frozen Protocol's authorized Google model envelope. It is distinct from the frozen GPT-4o and Mistral candidate-generating arms; GISMo and lookup are non-LLM arms. Record both requested and provider-returned model identity on each response. A mismatch, fallback, or unknown provider identity is a method failure, not an interchangeable success. Do not include a Gemini-family generator in a C4 slate judged by this Gemini judge; if such an arm enters scope, it needs a different predeclared judge family or a separately reviewed design.
2. **First judge prompt:** select the pool-level candidate-order-blind prompt in operative strategy §7.1, exact source pin above, as `c4-judge-pool-prompt-s7.1-v1`. Use its A/C/N/U rubric and JSON judgments keyed to opaque candidate IDs. For each frozen case slate, submit all candidates together, independently judge each, and do not rank them. Opaque IDs must not reveal arm, rank, source, gold, or model family. The order-reversal pass contains exactly the same case and IDs in reverse order; do not tell the judge which pass it is or disclose presentation positions. Bind the final prompt bytes, serialization, response schema, model/version, and generation parameters in the existing execution manifest before any test scoring.
3. **Prompt-change boundary:** the selected prompt may be developed only on the prospectively assigned development portion and calibrated on its assigned calibration portion under Protocol §3.1, with exposure recorded. After any allowed development revision, freeze the exact final bytes before test scoring. Any change after test access creates a new exposed exploratory version and cannot be described as held-out. The test cohort is never used to select model family, prompt, thresholds, or candidate-order treatment.
4. **Acceptance / rejection:** apply Protocol v1 §3.3 against paired human final-adjudicated labels: Cohen's κ point estimate at least `0.60`, lower confidence bound at least `0.50`, leniency delta no more than 10 percentage points per cell, order instability no more than 10%, and invalid output no more than 2%. Failed/invalid/missing judge outputs remain in their fixed denominator under the existing attempt rule. A judge that misses any frozen gate is rejected for the scoped use; thresholds are not relaxed. The strategy's position-trend and self-preference proposals may be reported as diagnostics, but are not substituted for or silently added to the frozen Protocol gates by this decision.

## Compatibility finding and implementation handoff

The existing C4 reference does **not** settle this choice. Its prompt contract identifies no model family/version or temperature. Its runner issues one prompt per candidate and inserts presentation-order and position text into model-visible context, unlike the selected §7.1 pool prompt. More importantly, its current report hardcodes leniency/self-preference deltas, and without human references computes kappa between forward/reverse judge outputs; that is internal consistency, not judge-versus-human agreement. These are source-review observations, not a new test result. Do not rerun its existing mock suite or treat C4-G1/G2 as human validation.

**M-I owns** updating the existing C4 contract/runner/manifest path to implement this single selected pool-level method (no parallel framework), verify actual returned model identity, preserve opaque-ID coverage and both-order outputs, compute the frozen gates from real adjudications, and retain failures/denominators. It may test implementation behavior using already-approved independent fixtures, but no source labels are to be fabricated. Before any test scoring, require the exact prompt/model/serialization freeze and exposure receipt. Human adjudication, agreement estimates, subgroup validity, and C4 acceptance require their actual authorized inputs and independent methods review. No API call or spend is authorized by this decision.

## Operational Gate Decision — 2026-11-08

**Authority and scope:** LEAD adopts these prospective calculation semantics for
the selected pool judge before any judge outputs exist. Protocol v1's numeric
gates, models, exposure rules and attempt limits are unchanged. This is an
implementer-facing decision, not implementation, empirical acceptance, or the
independent methods review required for inferential claims.

### Units, Reference and Primary Pass

- Freeze case IDs, candidate identities, arm memberships and descriptive cells
  before scoring. A unit is one distinct candidate identity within one case, not
  each occurrence in an arm's top-k. Preserve all arm memberships separately.
- Human references are final, separately adjudicated A/C/N/U labels. Do not
  substitute recorded gold, one cook, a majority algorithm or reverse-pass
  predictions. An absent final reference is unavailable, not N or U.
- The forward pass is the prespecified primary human-comparison pass. Report
  the reverse pass separately; never choose the better pass, average labels or
  select a pass after seeing agreement. This does not alter the paired common
  slate or the requirement to retain both orders.
- U is a valid nominal response category, not a parser failure and not negative
  validity. Missing, malformed, duplicate-ID or foreign-ID output is a failure.
  Reject an invalid outer pool response as a request failure; do not salvage a
  convenient subset of its candidate results.

### Agreement and Leniency

For judge-versus-human agreement use unweighted nominal Cohen's kappa over the
four A/C/N/U categories on valid, identity-matched primary-pass/reference pairs.
This is distinct from any separately reported strict A-versus-C/N diagnostic;
never silently drop U or collapse it into N. Report assigned, reference-present,
judge-valid and paired counts, plus U counts for each rater. The paired statistic
is conditional on observed pairs and does not hide failures from other gates.

For n paired units, let p_o be exact-label agreement and let p_e be the sum of
the products of the two marginal category proportions. Kappa is
`(p_o - p_e) / (1 - p_e)`. With no pairs or a zero denominator it is unavailable,
not zero or perfect agreement. Resample whole cases, retaining every paired
candidate in a sampled case, for 2,000 draws using seed `20261021`; use the
order-statistic percentile convention: sort the B valid draws and select
zero-based indices `floor(0.025 * B)` and `floor(0.975 * B) - 1`.
Record requested/valid/degenerate draws. Fewer than 20 valid draws means the
interval and lower-bound gate are unavailable. Do not change conventions to
cross a gate. The required point/lower-bound cutoffs remain 0.60/0.50.

Within each frozen descriptive cell c, use exactly the same paired units for
both A rates. Define `delta_c = mean(judge == A) - mean(human == A)` and apply
the existing 10-percentage-point limit to `abs(delta_c)`. C is not counted as A.
Report the signed delta and both counts as well as its absolute value. A cell
with no paired references is unavailable, not a zero-delta pass. Evaluate every
prespecified nonempty cell; never merge or omit a failing cell after scoring.
Arm-specific summaries retain deduplicated candidate memberships and do not
change the primary per-cell gate.

### Order Stability and Invalid Output

Let D be the fixed number of assigned case-candidate units for the two-order
comparison. Let F be the number with two valid but different labels, and let M
be the number lacking either valid label. Report `F / (D - M)` as observed
conditional label instability when its denominator is positive, and report
the full-assignment bounds `[F / D, (F + M) / D]`. A fully missing comparison is
not stable. Claim the 10% instability gate passes only when the conservative
upper bound is at most 0.10; an empty assignment is unavailable.

The invalid-output gate uses the fixed planned initial pool requests, including
both orders: denominator `2 * number_of_assigned_cases`. Count malformed or
incomplete pools, transport failures and missing initial responses as failed
requests. The 2% gate is their proportion of that fixed denominator. Retain
first-attempt and terminal dispositions separately if an existing authorized
retry occurs; retry success does not erase an initial failure. Also report all
actual attempts and failures for resource accounting. This decision authorizes
no retry, provider call or spend and does not replace an applicable attempt cap.

### M-I Finish Condition

Implement these calculations in the existing C4 path, with actual data supplied
later. Emit each gate as PASS, FAIL or UNAVAILABLE with its numerator,
denominator, frozen cell/pass identity and input hashes. Any FAIL rejects the
judge for that scope; any UNAVAILABLE prevents a complete acceptance claim.
Report missingness and conditional-statistic limitations even when all numeric
gates pass. Preserve historical G1/G2 evidence; it does not implement this
decision. Synthetic fixtures may verify arithmetic and refusal behavior only.
No present human references, judge results, pipeline readiness or C4 credit are
created by this handoff.

## Development Candidate Baseline Assets

The exact pool-level prompt text is available as shared, machine-usable UTF-8 text assets:

- System: [`lead-l43-c4-prompt-s7.1-v1-system.txt`](lead-l43-c4-prompt-s7.1-v1-system.txt), SHA-256 `96e24ae1f629d9c5f4a58457ae0c148253be2571ca55eb8d4560b1f1281f5db7`.
- User template: [`lead-l43-c4-prompt-s7.1-v1-user-template.txt`](lead-l43-c4-prompt-s7.1-v1-user-template.txt), SHA-256 `713997acc2715d327fda254d3a34a4904999f5806c43b3e990e8dfb98782539b`.

These are **development candidate baselines**, not final prompt bytes, not human-validated, and not scoring authorization. They are transcribed into usable text from the operative strategy §7.1 template under the L43 family/pool-blinding decision. Do not run them on test. Any prompt development/revision uses only assigned development/calibration portions with exposure recorded; exact final bytes/model/parameters must be frozen before test scoring, after the required human comparison.

### Placeholder and opaque-ID contract

- `{title}` and `{target}` are JSON string literals; `{ingredient_list}` is a JSON array of the recipe's ingredient strings; `{numbered_candidates}` is a JSON array shaped exactly as `{"id":"<opaque_id>","candidate":"<candidate_text>"}`. Candidate strings and user fields are JSON-escaped. The legacy placeholder name `numbered_candidates` does not authorize numeric IDs, ranking, or order information.
- Each opaque ID is unique within its case pool and reveals or correlates with none of arm, rank, source, gold, model family, or semantic candidate identity. It remains identical for that candidate across forward/reverse presentations, both judges in the paired diagnostic, and raw-output reconciliation. Only the local mapping binds IDs to candidate provenance; no provenance fields enter the prompt.
- Forward and reverse requests contain the same case and exact candidate records/IDs; only array order changes. Do not expose pass name, position, rank, or ordering rationale. The pool composition for each judge comparison is governed by the preceding common-slate decision; do not rebuild a different pool per judge.
- `U` is a valid explicit unknown judgment. Missing/extra/duplicate IDs, malformed JSON, invalid labels, or missing/overlong reasons are failures retained in the fixed denominator; never coerce failures to `U` or silently omit a case.

### Existing response binding and owner acceptance

The per-candidate value binds to the existing `C4JudgeOutput` in `evidence/iron-chef-lead-member-handoff-20261105/c4_llm_judge_contract.py`, SHA-256 `a823003838685d47ae851364faee25d8fe0c23cb6c31d4a96cdad3363dcd11de`: exactly `label` (`A|C|N|U`) and `reason` (string). The outer `judgments` map is this Lead-selected pool serialization for the development baseline, keyed by opaque IDs to meet L43's ID-keyed requirement; it is not the strategy's illustrative array response shape. The historical single-candidate `OUTPUT_SCHEMA` does not describe the outer map, and the current runner does not validate it. This baseline adds no parallel schema, serializer, or runner and claims no code integration. M-I's source-free implementation acceptance in the existing path is to consume these exact files, render only these placeholders, enforce exact ID coverage and one typed nested result per ID, enforce the 20-word reason rule, and preserve raw responses/failures for both orders. Synthetic checks prove only implementation behavior. Final prompt freeze, judge-vs-human agreement, and C4 acceptance remain distinct gates; no source labels, human comparison, runtime, provider calls, or spend are supplied here.

## Initial second-family state (historical)

The operative strategy describes **two** independent judge families as a proposal. The initial 2026-11-06 decision selected only the first and left the second open. The following prospective addendum resolves that design question narrowly; it does not claim full two-family coverage of every arm or close L43/C4.

The decision establishes compatibility by specification only. It does not establish account availability, quota, actual model serving identity, prompt performance, human validity, clinical safety, study completion, or contribution credit.

## Addendum — prospective second-family common-slate diagnostic

**Decision ID:** `iron-chef-l43-second-judge-common-slate-v1`

**Date / authority:** 2026-11-06; bounded Lead design within the existing Protocol v1 family and resource envelope.

**State:** Prospective specification only. No prompt bytes beyond the selected §7.1 template are frozen, no job/call/spend is authorized here, and the current account/cost/capacity receipt remains a dispatch gate.

### Decision

Select OpenAI `gpt-4o-2024-08-06`, temperature `0`, as the second judge family for a separately reported **matched common-slate robustness diagnostic**. Keep the already selected Gemini `gemini-2.5-flash` as the sole primary judge of the complete L43 C4 candidate slate. Do not average judge outputs, replace the primary judge, use the secondary comparison to select/tune a prompt on test, or claim two-family coverage of the full slate.

The common-slate diagnostic uses exactly the same preselected audit case IDs and candidate IDs for both judges. Its admissible candidate pool contains GISMo and lookup outputs, plus the retained-best Mistral adapter only if it passes the existing DR-04 eligibility checks. Exclude **all GPT-4o-generated candidates from both judges' diagnostic pools**; GPT-4o is the second judge's own family. This is not a self-family exemption: every candidate in the paired diagnostic is judged by both Gemini and GPT-4o, and each judge family differs from every generator represented in that diagnostic pool. Preserve the full frozen candidate-arm obligation separately: Gemini continues to judge GPT-4o-generated candidates in the full-slate primary analysis, while the GPT-4o family does not judge those candidates. The pool membership, cases, IDs, serialization and order must be byte-identical between the two diagnostic judges; the reverse-order pass reverses that same pool without exposing pass/order/position to the judge.

After the exact §7.1-derived prompt bytes are prospectively frozen, submit one pool per judge in forward and reverse order using byte-identical prompt, opaque IDs, temperature and response schema. Compare each judge separately against the same paired human final-adjudicated labels, then report their difference as a secondary diagnostic. Apply Protocol v1 §3.3's judge-versus-adjudicated gates separately; failures and missing outputs remain in the fixed denominator. This diagnostic does not replace the primary Gemini results or meet a stronger interpretation of the strategy's proposed two-family/full-slate ambition.

### Feasibility and boundaries

- This uses only the already authorized OpenAI and Google model settings; it introduces no provider, model setting, candidate arm or protocol rule. Frozen Protocol v1 remains unchanged. The candidate slate is the one already named by L43/Protocol v1, not an expansion to every arm mentioned in broader strategy proposals.
- For `n` cases in the paired audit subset, the common-slate diagnostic requires `2n` calls per judge (forward and reverse, one case/pool per call), in addition to Gemini's full-slate primary calls. At the strategy's 150-case audit planning scale, the diagnostic alone is 300 Google plus 300 OpenAI calls; actual input/output tokens and costs are unknown until the exact pools exist. Before dispatch, verify remaining quota, spend/reservation and worst-case cost under Protocol §6. If the exact frozen subset cannot fit, do not silently shrink/reselect it; stop before calls and record the bounded capacity result.
- Protocol §3.1's assigned validation development/calibration and exposure rules still apply. Freeze both exact model identities, final prompt bytes, pool/ID serialization, parameters and order treatment before any test scoring. Protocol §3.3's audit-set human-agreement gates remain mandatory for each judge. No human labels, provider calls, account availability, quota, prompt performance or empirical agreement is established by this design decision.
- The minimum execution input still missing is a rights/ethics-authorized paired human final-adjudication set over the exact common candidate units, plus the hash-bound eligible candidate-pool/input package and observed account/cost capacity. This is an execution prerequisite, not a reason to ask for another Lead method choice.

### Strongest alternative and claim ceiling

The strongest alternative is to keep Gemini as the only judge and avoid a second-family diagnostic. That is simpler and preserves full-slate coverage under one eligible family, but leaves the strategy's proposed independent-family robustness question untested. Conversely, judging the entire slate with GPT-4o would violate Protocol §3.3 on GPT-4o-generated candidates; comparing two judges on different pools would confound judge-family with candidate-pool composition. The common-slate design avoids those errors, at the cost of extra calls and a narrower secondary estimand. It supports only a paired diagnostic on the common non-self-family subset—not full-slate two-family robustness, human validity, C4 acceptance, safety, or a new empirical claim. If the intended C4 criterion requires two judges independently covering every arm, this within-envelope design is insufficient; the precise missing capability is a second authorized judge family distinct from all generator families, not more mock tests or a different interpretation of self-family rows.
