# IRON CHEF v4: LEAD BACKLOG
> **AUTHORITY:** `academic_vision_and_strategy.md` (canonical SSoT) is the single source of truth. This backlog is derived from §3 (C1–C5), §17 (Work Packages), §18 (Schedule), §19 (Risks), and §16 (Protocol Resolutions). When they conflict, the strategy file wins.
> **MIGRATION:** Updated from v3 on 2026-10-12 (LEAD v113). v3 "four trained models" and "nine contributions" structure retired. See Appendix Y of SSoT for superseded v3 text.

---

## Current Evidence Boundary (from SSoT §1)

| Area | Verified Fact | Tier |
|------|---------------|------|
| GISMo reproduction | 10,747 test: Hit@1 20.694%, Hit@3 36.010%, Hit@10 54.164%, MRR 0.31636 | E3 |
| Unseen-pair collapse | GISMo unseen Hit@1 1.206%, MRR 0.04308; Lookup overall 17.996%, unseen 0.000% | E3 |
| Corrected SAG | Mean Hit@1 0.1171% vs semantic 0.3964%, naive fusion 0.3333% | E3 NEGATIVE |
| True-context dual encoder | 16.1223% overall Hit@1 vs GISMo 20.6941%; unseen MRR 0.016601 vs 0.043084 | E3 NEGATIVE |
| CLIP projection | R@1 34.10% → 35.42%, missed +2pp gate | E2 NEGATIVE |
| Flavor pairing | 2,231,142 RecipeNLG rows, p=0.238095, not supported | VERIFIED NEGATIVE |
| FooDB service | 971 foods, 206 dimensions, 125,992 links; 471,906 profile pairs, 0 mismatches | VERIFIED |
| FooDB reach | Both endpoints: 1,162/10,747 (10.81%); unique endpoints 260/2,650 (9.81%) | MEASURED |
| OFF concordance | 400/408 declared groups (98.04%, Wilson 96.18–99.00%); trace 22/40 (55%) | PARTIAL E2 |
| OFF partition | 23 identities cover 760,877/767,780 tag occurrences (99.10%) | E3 |
| Allergen protocol | 208 cases, 13 groups, direct/trace/multi/cross-contact; Mistral raw 208/208 | PARTIAL E2 |
| Mistral Stage 3 | Verified through 374/561; Segment 7 launched, RUNNING toward 444 | PARTIAL E3 |
| Novelty reviews | v8/v9: broad claims rejected; exhaustive re-attack open | PARTIAL |

**No accepted IRON CHEF model exists.** No allergen comparison result (GPT-4o/Gemini unrun, no adjudication). No human usefulness evidence. Stage 3 incomplete.

---

## The Five Contributions (C1–C5 from SSoT §3)

| ID | Contribution | Floor | Target | Stretch |
|----|--------------|-------|--------|---------|
| **C1** | **Instrument**: strata × allergen cases × valid@k audit | 208 allergen cases + Recipe1MSubs strata + 150-case 2-rater audit | + Vietnamese pilot (~100 cases, 2 native raters) | Public release with datasheet |
| **C2** | **Measurement campaign** across arms | GISMo + lookup + one API LLM | + Mistral Stage 3 + second API LLM; intervals everywhere | Contamination-aware 2×2 (public/private × Western/Vietnamese) |
| **C3** | **Guardrail**: hard allergen filter + per-cell conformal sets + abstention | Frozen protocol, honest run, cells reported | Informative sets in ≥1 cell; vacuous cells shown | Fewer allergen violations than raw arms with intervals |
| **C4** | **Human evidence** | 2-reviewer pass on audit pilot, κ reported | LLM judge calibrated to humans (κ ≥ 0.6) | Usefulness study N ≥ 20 (ethics permitting) |
| **C5** | **Product proof** | Real-data Explorer + evidence endpoint + visible abstention | Live side-by-side demo vs raw LLM on same inputs | Reusable component |

**Bounded negatives (no further spend):** SAG, dual encoder, flavor pairing, CLIP projection, VLM artifact, behavioral overlay, voice copilot. One thesis chapter: *What did not work, and what each failure taught about the evaluation.*

**L13 re-scoped:** from "train new GNN" → "shift-aware selector over frozen scorers" (SSoT §3.5).

---

## Acceptance Gates per Contribution (SSoT §3.2)

| ID | Gate (all binary, checkable) |
|----|------------------------------|
| C1 | Instrument schema validated; strata files regenerate bit-for-bit from official splits; audit pilot labels for ≥150 cases exist with two independent raters and adjudication log; κ reported with case-level interval |
| C2 | Every arm evaluated on identical case list, identical normalization, identical scoring script; each number in a table has an interval; arms with unmatched-output rate >5% flagged |
| C3 | Protocol frozen and hashed before test access; calibration only on validation; per-cell status table (ok / vacuous / no_guarantee); cost-of-trust figure from frozen run |
| C4 | Rater agreement computed per §6.8; judge-vs-human agreement per §7.5; if κ < 0.6 twice, fallback applied and stated |
| C5 | Explorer loads from hash-pinned graph artifact; demo runs from clean checkout without network except API arms; abstention state reachable and tested |

---

## Work Packages (from SSoT §17.1 — Lead Items)

| ID | Deliverable / Acceptance | Dependencies | Effort | Fallback |
|----|--------------------------|--------------|--------|----------|
| **L13′** | Re-scope to shift-aware selector over frozen scorers; edited item text + link to §8 | None | 1h | None |
| **L36** | 150-case audit: all independent labels/adjudication/cluster intervals/H1 table | Raters (L41), pools (M-B), PR01–03 resolved | 30–40h | Shrink to 100 cases |
| **L37** | Deterministic strata and interval-reported metrics; optional endpoint-frequency split | Identity/counts (M-B) | 10h | Drop frequency sub-split |
| **L38** | Frozen calibration, all-cell status, diagnostics, cost-of-trust figure | Audit/judge/arms, PR04–08 resolved | 25h | Report only ok cells; label rest |
| **L39** | Matched adjudicated allergen outputs; expansion to n≥299 or honestly bounded interval | API keys, reviewers (L41), rights | 30h | Report interval at n=208 |
| **L40** | Frozen ~100-case native-reviewed Vietnamese pilot | Authors/raters/rights (L41), U8, U9 | 30h | Qualitative appendix only |
| **L41** | Written ethics outcome; named raters; handbook + T0/T1 results | Human dependencies | 12h | Fallbacks per §14.8 |
| **L42** | Hashed novelty funnel; full-text/citation chase; independent adjudication | Sources, reviewer | 15h | Narrow claims |
| **L43** | Frozen judge prompt + evaluated gates, or explicit unusable outcome | Leak-free human comparison (L36) | 12h | Humans only on smaller set |
| **L44** | Validated Stage 3; 200-case timing; matched ranked outputs for val/test subsamples | Quota, adapter, arm construction | 12h + waiting | Stage-2 adapter arm |
| **L45** | 14 evidence-bound thesis chapters (§21) | All results/claims | 80+h | Cut by cut line (§3.6) |
| **L46** | Source-bound defense answers; timed human rehearsal; zero forbidden claims | Thesis (L45) | 15h | None |
| **L47** | ARR paper draft per §23 | Evidence + venue check | 40h | Thesis only |
| **L48** | Integrated strategy + completed claims register | This migration completes strategy | 10h | None |

**Critical dependency path (SSoT §17.4):**
```
L41 (raters/ethics) → L36 (audit) → L43 (judge) → L38 (calibration) → L45 (writing) → L46 (defense)
         ↑                              ↑
    M-B (pools)                   M-D (arm outputs) ← L44 (inference)
```
**L41 → L36 → L43 → L38 is the critical path.** Everything on it waits on human availability, not compute.

---

## Schedule & Capacity (from SSoT §18)

**Today:** 2026-10-12. Defense assumed on/after Dec 8 (U3). All dates are proposals `[P]`.

| Week | Dates | Lead Focus | Member Focus | Gates / Freezes |
|------|-------|------------|--------------|-----------------|
| **W0** | Oct 8–11 | Answer U1–U3, U7, U11 (hours, advisor, defense, quota, ethics). Back up SSoT. Start asking raters. | Start M-A, M-F | **Decision: compression level** |
| **W1** | Oct 12–18 | L48 first pass. L42 query runs. Read Rita §3 + Fatemi data (U4). Check val scores + unseen share (U5). Draft rater handbook. | M-A recompute; M-B strata/identity; M-D GISMo/lookup outputs; M-F inventory | **Oct 15: two raters named. Ethics answer by Oct 12** |
| **W2** | Oct 19–25 | Obtain API keys; run API LLM #1 on 150 audit cases. Rater T0/T1. Draft allergen expansion. Write Ch2/Ch3 drafts. | Pool builder fixture test → real run. M-I runner fixture test. | **Oct 22: FREEZE A** (audit protocol, Δmin, arms, normalization). **Oct 23: Vietnamese raters/authors named, licenses checked** |
| **W3** | Oct 26–Nov 1 | Audit labeling. Judge prompt dry run. 200-case timing (by Oct 29). Allergen cases frozen/hashed. | M-C Explorer build; M-G register paths/hashes | **Oct 29: allergen reviewers named; timing done. Nov 1: profiles complete** |
| **W4** | Nov 2–8 | Adjudicate audit; κ with cluster CI; first H1 table. Judge calibration (L43). Allergen arms (GPT-4o, Gemini). Stage 3 check. | M-C Explorer T-03–T-09; M-E demo skeleton | **Nov 6: FREEZE B** (conformal protocol, §8.9) before any test coverage. Advisor interim. |
| **W5** | Nov 9–15 | Judge-label val pools; per-cell calibration (rank arms); diagnostics §8.12; test-subsample eval. Vietnamese freeze. H4 prep if ethics. | M-E recorded Act 3 from frozen files | **Nov 10: usefulness participants recruited (or dropped)** |
| **W6** | Nov 16–22 | Mistral inference + calibration (or Stage-2 adapter). Allergen adjudication. Vietnamese labeling. Cost-of-trust. | Explorer polish T-10–T-12 | **Nov 20: EXPERIMENT FREEZE.** No new runs. Analysis/writing only. |
| **W7** | Nov 23–29 | Red-team claims (§24.2). Rebuild defense answers (§22). Chapters Ch5–Ch9. | M-H reproducibility kit; clean-machine reproduction | **Nov 27: register script exits 0; zero forbidden claims** |
| **W8** | Nov 30–Dec 6 | Write Ch1, Ch4, Ch11, Ch13, Ch14. Defense dry run 1. | Demo hardening | **Dec 1: ARR service-contributor decision** |
| **W9** | Dec 7–13 | **Dec 8: thesis freeze.** Start ARR draft from thesis artifacts. | Support | Thesis frozen |
| **W10–12** | Dec 14–Jan 3 | ARR v1; internal review. ARR v2; limitations/datasheet. Final read, hashes. | Figures/tables regenerate. | Service-contributor registration ready |
| **W13** | **Jan 4, 2027** | **ARR submission (AoE)** | | Registration form open until 48h post-deadline |

**Capacity arithmetic (SSoT §18.1):** 225 MUST + 32 SHOULD + 70 COULD = 327 lead hours. 8 weeks at 30h/week = 240h (short 87h). At 40h/week = 320h (fits). **Run arithmetic with real hours on Oct 12 (U1), pick compression level before starting.**

**Compression protocol (SSoT §18.4):** Level 0 (full), Level 1 (drop COULD except 20-case Vietnamese appendix), Level 2 (GISMo/lookup only calibration, no judge, n=208 allergen, minimal Explorer), Level 3 (collapse map + 100-case audit + allergen interval + bounded negatives + claims register). Gates never weakened.

---

## Risks & Pre-mortem (from SSoT §19)

| ID | Risk | Likelihood | Impact | Early Warning | Mitigation |
|----|------|------------|--------|---------------|------------|
| K1 | Raters not found | M | H | <2 named by Oct 15 | Ask earlier; shrink to 100; label exploratory |
| K2 | Rater agreement too low (κ < 0.5 twice) | M | H | T1 failure | Rewrite handbook; negative anchors; shrink to audited subset |
| K3 | H1 inconclusive (CI straddles Δmin) | M | M | CI straddles 15pp | Extend to 300 only if time; report inconclusive |
| K4 | Judge fails gates (§7.5) | M | M | Gates fail | Humans-only on smaller calibration; state power loss |
| K5 | All cells vacuous | M | M | Early GISMo diagnostics | Report quantified limit; policy = abstain-by-default |
| K6 | Calibration/test not exchangeable | M | H | KS diagnostic on gold rank | Merge cells or weighted conformal; state limit |
| K7 | Validation scores missing | M | H | M-B check fails | Regenerate GISMo val (cheap); LLM arms on subsample |
| K8 | Stage 3 late/fails | M | M | No 327 by Nov 1 | **OBSOLETE — 327 recorded verified.** Stage-2 adapter hedge exists |
| K9 | Inference too slow | M | M | 200-case timing | Stratified subsample (§13.3) |
| K10 | API keys/budget unavailable | M | H | No key by Oct 22 | Use free/open-weight LLM; pin version; say so |
| K11 | Ethics blocks usefulness study | M | L | No answer by Oct 12 | Drop H4 |
| K12 | Allergen n too small to certify | H | M | Arithmetic fixed | Expand to ≥299 or report interval |
| K13 | Claim breaks under review | M | H | Red-team finds forbidden | §24.2 audit; claims register |
| K14 | Novelty duplicate found | L–M | H | L42 search | Re-scope C1 to Vietnamese+allergen+shift combo |
| K15 | Advisor says scope mismatch | L–M | H | U2 reply | Re-scope before more spend |
| K16 | No qualified ARR contributor | M | M | None by Dec 1 | Thesis only or later cycle |
| K17 | Licenses block release | M | L–M | M-F, §10.2 | Release code/aggregates only |
| K18 | Scope creep | H | H | >MUST tier in progress | Cut from bottom, weekly |
| K19 | Overnight job overspend | L | M | Receipt > cap | Per-job caps; refuse-to-start |
| K20 | Teammate unavailable | M | M | Tasks slip 2 weeks | Reassign M-A, M-B to Lead; compress |
| K21 | R0 numbers leak into thesis | M | H | Figure without claim ID | Register rule (§15.2) |
| K22 | Non-thesis workload | H | H | Weekly hours below plan | Compression level (§18.4) |

**Pre-mortem triggers (SSoT §19.3 — reopen architecture only on these):**
- H1 lower Wilson bound ≥ Δmin → lead with benchmark validity
- H1 upper bound < Δmin → lead with collapse map + abstention
- H1 straddles Δmin → report inconclusive; extend only if time
- κ < 0.6 twice → rewrite handbook once; shrink to audited subset
- All cells vacuous → reframe C3 as quantified limit
- Novelty duplicate → re-scope C1
- Advisor mismatch → re-scope before spend
- Exchangeability diagnostic fails → weighted conformal or merge; state limit
- No ARR contributor by Dec 1 → thesis only

---

## Protocol Resolutions (from SSoT §16 — must resolve before affected work)

| Flag | Conflict | Resolution Required |
|------|----------|---------------------|
| PR01 | §6.1 conditional rescue vs §6.9 joint rescue; > vs ≥ Δmin | Choose estimand, denominator, operator prospectively |
| PR02 | Gold forced A yet both-rater N = label noise | Specify raw/adjudicated/gold policy; implications for V≥H |
| PR03 | Equal allocation vs population inference; top-4 vs top-5 | Resolve allocation, weighting, stratum minima, consistent k |
| PR04 | Cell uses gold substitute (unavailable at deployment) | Separate descriptive oracle strata from deployable partition |
| PR05 | Judge chosen on test audit; claim freeze before test | Establish dev/audit/cal/eval separation; disclose exposure |
| PR06 | Filter/rerank/truncation/empty-pool mismatch; infinity handling | Define returned-set success, filter ordering, guarantee population |
| PR07 | KS called exchangeability check; test-driven merging | Diagnostics cannot prove assumption; freeze variants or label exploratory |
| PR08 | Coverage grid .50–.95 vs α={.05,.1,.2,.3,.5} | Freeze single grid and status semantics |
| PR09 | Lossy normalization vs v23 collision findings | Validate identity/food-form behavior before freeze |
| PR10 | Different κ point/lower-bound gates; Wilson vs exact | State each gate's population/statistic/interval method |
| PR11 | Train-only graph vs "no val/test pair" despite seen overlap | Resolve row provenance leakage test without changing splits |
| PR12 | Endpoint example seen=true/count=0/unseen simultaneously | Replace only via validated contract design |
| PR13 | Annotation pending ethics vs clearance-before-study | Obtain institutional authorization; prep continues |
| PR14 | Seed placeholders marked VERIFIED; checker accepts dummy | Implement real artifact validation separately |
| PR15 | Synthetic tests omit bounded-M/infinite/missing-cell | Independent validation before reuse |

---

## Operating Rhythm (from SSoT §24)

- **Daily (10 min):** Morning review of overnight jobs (§13.8)
- **Monday (30 min):** Claims register diff; overnight receipts; plan week against cut line
- **Wednesday (15 min):** Check gates due; chase people
- **Friday (30 min):** Update status block (generated); write one thesis paragraph from week's evidence; decide cuts
- **Biweekly advisor update (5 lines):** Evidence IDs, gates moved, plan changes/trigger, precise request, next action

**Freeze A (Oct 22):** Audit protocol, Δmin, strict/lenient, arms, blinding, normalization, primary arm/stratum, κ bar, timestamp/hash.

**Freeze B (Nov 6):** Deployable cells, n floor, M, α grid, labeling function/selection, filter/rank/set semantics, infinity/missing, informativeness, diagnostics/sensitivities, evaluation access. **Neither freeze completed here — resolve §16 first.**

**Forbidden claims (§24.2 — red-team every sentence):** No "zero/<1% violations" without n≥299; no "safe/safer/clinical"; no "human-valid coverage" from judge; no "first/novel" before 4.6+second reviewer; no Vietnamese guarantee; no "measured contamination"; no R0 figures without reconstruction; no cross-task rankings; no "four trained models/production-ready AI"; no "zero cost/local-only" without runtime proof; no user outcome claims; no untraceable numbers; no "calibrated" without cell/label/assumption; no "hypothesis confirmed" when interval includes null.

---

## Evidence Integrity Rules

- Never mark COMPLETE because code was written — require executable evidence (checkpoint, hash-bound predictions, scored evaluation).
- Never compare cancelled run or paper number to locally executed result.
- Never report metric without dataset, split, protocol version, seed count.
- Negative results (SAG, dual encoder, CLIP, flavor, VLM, behavioral, voice) are thesis contributions — report honestly.
- Do NOT cite: 22.04% Senath (unverified journal abstract); 0.2222 HGAT (synthetic); 0.00% allergen (self-labeled, withdrawn).
- Every number in thesis carries a claim ID from register (§15). Unverified = say so.

---

## Appendix: Mapping from v3 Backlog (for reference only)

| v3 Epic | v4 Treatment |
|---------|--------------|
| EPIC 3/3b/3c (M1 GNN/SAG/dual encoder) | Bounded negatives chapter; L13′ = shift-aware selector |
| EPIC 10 (M2 Mistral Stage 1–3) | L44 (Stage 3 completion + inference); Stage-2 adapter hedge |
| EPIC 11 (M3 VLM) | Bounded negative; no further spend |
| EPIC 12 (M4 CLIP) | Bounded negative; no further spend |
| EPIC 5/5b (Allergen) | L39 (matched arms + expansion + adjudication) |
| EPIC 4 (Compound explanation) | C4/H4 (usefulness study if ethics) |
| EPIC 9 (Thesis 14 chapters) | L45 (14 v4 chapters per §21) |
| L29 (Photo chain) | Bounded negative; §12 product proof replaces |
| L31 (AI runtime cert) | Separate infra work; not thesis contribution |
| L35 (Defense pack) | L46 (rebuilt answers + rehearsal) |