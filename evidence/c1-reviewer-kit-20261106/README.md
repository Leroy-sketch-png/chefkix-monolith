# C1 Standalone Reviewer Handouts

English instructions requested by HUMAN on 2026-11-06 after identifying two reviewers.

## Deliverables

- `output/pdf/reviewer-quick-guide.pdf`: one-page instructions to give each reviewer.
- `output/pdf/organizer-handoff-checklist.pdf`: one-page coordinator checklist, separate from reviewer materials.
- `handouts.json`: exact authored content for the PDFs.

These are Lead-owned measurement instructions, not a replacement app, populated audit pack, human-validation result, or new protocol. The first audit roster follows monolith `8432b52`: primary GISMo top-five plus recorded gold, on the unchanged 150-case frozen sample. The gold is never identified to reviewers. There are at most six unique candidates per case before deduplication; actual workload requires the real slate.

The rubric follows Protocol v1 section 2 and the existing C1 Chapter 4/6 handbook semantics. Ordinary quantity logic is compatible with A; a meaningful quantity/technique/companion change is C. U is unknown, not negative. No reliability threshold, H1 hypothesis, gold identity, model rank, or adjudication outcome is shown in the reviewer guide.

## Current L36 Status — 2026-11-12

The fixed 150-case/867-pair agreement analysis and separate 150-unit chef adjudication are already complete; their raw returns, packet and sealed map remain owner-private. Final candidate-level labels across the selected disagreement units are A=71, C=46, N=32, U=1. The bounded lead interpretation of their optional rationales is in [`l36-disagreement-interpretation-v1-20261112.md`](l36-disagreement-interpretation-v1-20261112.md). It reports the observed label-pair counts and contextual qualitative reading without exposing case details or turning rationale themes into new labels. It identifies one displayed-identity ambiguity and a separate multi-name bundle where rationales condition some judgments on a specific form; do not propagate a bundle-level decision to every underlying name. No adjudication value, raw rating, sample, candidate identity, or protocol was changed.

This is descriptive interpretation of selected disagreements only. It does not establish culinary truth, safety, H1, population performance, reliability, C1 acceptance, or milestone credit. Exact source identity remains a separate question. **M-B's next bounded action:** inspect the existing private source/alias linkage for the affected displayed units and return provenance/ambiguities to LEAD; do not edit the frozen packet, raw returns or labels. LEAD decides whether verified identity evidence changes interpretation. The 58 both-N recorded-gold judgments remain a separate PR02 diagnostic; this disagreement-only packet does not independently adjudicate those N/N gold judgments. Cases may overlap through other disputed candidates; no disjoint-case claim is made.

## Historical Collection State — 2026-11-07 (superseded)

The snapshot below records the state at that date only. Its “no adjudicated labels/no response” statements are superseded by the current L36 status above; retain them as chronology, not instructions.

Both complete independent returns were analyzed privately under frozen Protocol v1 after HUMAN reported that institutional ethics/consent coverage includes this activity (not independently documentary-verified). The fixed 150-case/867-pair cohort yielded four-category agreement 717/867 (82.7%; Wilson 95% CI 80.0–85.1%) and unweighted nominal Cohen’s κ=0.746567 (2,000-draw case-cluster percentile-bootstrap 95% CI 0.700901–0.786907; seed `20261021`). Both PR10 cutoffs pass under the predeclared order-statistic interval; the lower-bound margin over 0.60 is 0.100901. A same-input linear-interpolation sensitivity changed the lower bound by only −0.00000742; both methods pass, and the original predeclared interval remains primary. The candidate-level Wilson interval is descriptive, not cluster-adjusted; κ uncertainty resamples cases as clusters. Protocol v1 does not prescribe a κ quantile convention; the L36 addendum's linear interpolation applies only to H1 endpoint bounds, so it is not a conflicting κ rule. No protocol edit was made.

The private comparison also yielded 150 candidate-level disagreements across the fixed cohort and 58 cases where both raters marked recorded gold `N` (reported separately as reference-disputed under PR02). No adjudicated labels or H1 estimate exist yet. A context-bearing packet containing exactly the 150 disagreement units is prepared in the owner-private return directory, with a separate sealed identity map; it is not public and has no adjudicator responses. Each visible unit includes recipe context, candidate display identity, the two unordered raw labels, and blank final-label/rationale fields. It omits case/identity IDs, rater identities, arm, rank and gold flags. The raw returns remain unchanged. No C1 milestone or acceptance is claimed.

The private execution helper is `tools/research/c1/compute_agreement.py` (SHA-256 `2ee0146d4db61db36876fc8dcee566ca1fe2e12359d5f92b5b6442d0c4ca0ef7`); its source-free math/cluster tests are `tools/research/c1/test_compute_agreement.py` (5/5 pass; SHA-256 `03ae30eee1b83f068290ab471df1326edce88763627173c2cc2e8ef3f60d3368`). The complete focused C1 suite passes 16/16. No private artifact is packaged in this repository.

The new source-free handoff builder is `tools/research/c1/prepare_adjudication_handoff.py`; its independent synthetic regression suite is `tools/research/c1/test_prepare_adjudication_handoff.py` (3/3 pass). It validates the fixed disagreement cohort against both issued packs and returns, checks case context and candidate identity across raters, separates visible context from a sealed identity map, and writes create-only outputs. `render_adjudication.py` plus `adjudication-template.html` reuse the standalone offline reviewer pattern for the frozen packet; `test_render_adjudication.py` covers denominator/schema/privacy rejection, escaping, binding and overwrite prevention. `validate_adjudication_return.py` and its tests bind returns to the frozen packet and preserve missing items within the fixed 150 denominator. The complete focused C1 suite passes 22/22. `smoke_adjudication_ui.js` executed the private local form with sanitized assertions for four choices, progress, navigation, export, restore and tamper rejection; no case content was printed. The current HTML and sealed map remain only in the protected private directory; no populated artifact is tracked or pushed.

HUMAN-confirmed adjudication method: show recipe/candidate context and the two conflicting labels in unordered form, without rater identity, model arm, rank, or recorded-gold marker; record a final A/C/N/U value, using U when unresolved, with optional short rationale. Preserve independent labels unchanged and store adjudication separately. The protected packet and standalone HTML are ready in the local private directory; no email, upload, or new online destination is required. Only the HUMAN's choice of adjudicator remains before use. The offline export is bound to the packet hash; missing/withdrawn answers remain unknown under the adopted L36 case rule and are not removed from the denominator. The 58 both-N gold cases remain a separate PR02 reference-dispute diagnostic, not a reason to rewrite Hit.

To inspect the source-free form or validate a returned file, use `tools/research/c1/render_adjudication.py` and `tools/research/c1/validate_adjudication_return.py`. The private local-use instructions and current file hash live beside the HTML and are not copied into Git. `node tools/research/c1/smoke_adjudication_ui.js <private-review.html>` exercises the UI in memory and prints only a pass/fail summary.

## Historical Collection State — before agreement analysis

The paragraph below is retained as the intake-time snapshot; its “Current state” wording records what was true before the subsequent analysis and is not the present status.

Historical intake snapshot (2026-11-07, superseded): HUMAN had returned both independent ratings; the owner-private intake receipt recorded complete issued-pack-bound returns for 150 cases and 867 candidate entries per rater. At that earlier checkpoint, agreement analysis and adjudication had not been performed, the frozen intake declaration still read `BLOCKED_PENDING_ETHICS_CLEARANCE`, and the primary `V/U` mapping was unresolved. Subsequent HUMAN-reported ethics/consent confirmation, agreement analysis, and the adopted L36 case-rule addendum supersede those statements for current operations. The historical declaration and raw files remain preserved; the later report does not independently verify institutional documents. Return files, raw labels, context, and sealed mapping stay outside Git.

Existing M-B packaging acceptance: two standalone personal packs; correct frozen sample and roster; blinded case/candidate content; no sealed key or cross-reviewer leakage; automatic stable-identifier preservation; A/C/N/U and optional notes; progress/unanswered visibility; save/resume; complete export; exact per-reviewer candidate counts and pack manifests. Reuse the existing audit path. Do not create a new research framework, change sampling, generate replacement model outputs, or expose the combined 300-view file to both reviewers.

User-facing timing is conditional arithmetic only. Observed pace on the first five real cases can inform scheduling, not alter the frozen rubric, sample, candidate policy, or completed labels. Adjudication is separate from original independent judgments. No third-person qualification requirement is invented here.

Only safe authored handouts are published. Actual source rows, private identities, consent files, sealed mappings and human judgments do not belong in this public repository.

## Current C1-G4 independent-output review handoff — 2026-11-23

The original guide and organizer checklist document the two-rater collection; they are not a current G4 review report. The fixed 25-cell ledger credits C1-G3 narrowly and leaves C1-G4 OPEN. This handoff lets an independent reviewer begin a **read-only integrity and interpretation review** of the already-produced, source-bound output. It does not reopen collection, change Protocol v1, accept G4, or resolve the separate H1 uncertainty-method hold.

### Frozen review anchors and safe artifact pins

| Role | SHA-256 | Boundary |
|---|---|---|
| Frozen Protocol v1 | `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12` | Immutable method authority. |
| Sample manifest | `8d5d375629968f519d334aaed8c29bc806c2504f7df8b9abe10a2fba1b568c8d` | Frozen cohort binding. |
| Sample JSONL file bytes | `e195126b5e0abf87e125d22b3c75b799ffe970a75e07b302ecece3c44455e60d` | File-byte SHA; distinct from the canonical sample digest recorded by receipts. |
| Sample canonical digest (receipt field) | `c352c0fa1d6f891ca3dfb0d20df92e68383aa921ac8072ba5fed8308ac7c5dd3` | Canonical selected-case digest, not the JSONL file-byte SHA. |
| Candidate manifest | `b6375014e420e02960193a50ad85595d84f0d92e4b031fa60e502dabf311e207` | Selection/source manifest. |
| Candidate-source file | `6c72dc4a89b0a4587db37757cbca7a9204d133b581eca56d659589b3a525620c` | Pinned input bytes; not an output-quality claim. |
| Bound-return intake receipt | `fa69f77447dbd07a64fed4324eb167b5223ab99c4b4027109668341a179d873d` | Records two role-separated complete returns bound to their issued packs. |
| R1 / R2 original returns | `50888330644beffe34430d4d9c161fd255c656788e657a6d36c8eaf1e8463dd8` / `2ea1a8749e20db07814e502384825c133bf14b8e3c894f9d6302eb13ab44d861` | Exact private return-file pins; no row contents are published here. |
| Raw-agreement result receipt | `653abfaa5e63cdcd02e3c82e53b15c588b0689b8f549b490ba9d8fef296259cb` | Existing fixed-cohort aggregate receipt. |
| Chef/adjudication return | `5d7e323831b5314813f0635f3e4f1d9b91afac3724802f0a4b9f49dff0a8e4f8` | Separate adjudication output. |
| Adjudication integration receipt | `00eb1db336e76e315cb3b0d279635723cfd86709390ae32069b6fdc5e917edb6` | Binds integrated fixed-cohort output; does not release H1. |
| v5 retained caller / source-free tests | `9405ece272b70d438a5ee470f7211fdc1b7dd9afe784b460b2e8a4580aaecaf1` / `562e68608da43d1c607520bab47ba83f7069cd81341b929c86605e63179b23dc` | Exact calculation path and synthetic regression suite. |
| v5 pre-access / metadata preflight | `11c84ed8b2149206b7cfd7fa9b300d0cc61df79bc9d544521e74e3fc87daf57c` / `e014158f785083b35c83689bb48cc26cd401514536d1d6ac150cbd655d1d7c26` | Bind 22 input-file and five dependency-code roles; preflight did not parse outcome rows or call a scorer. |
| H=0 membership / point result files | `af0b31eb83e24a16f1b0441c5c8f73d1867ffe29b58540f4ea7ae62f7dbe8feb` / `9683bd13bde23f82480899622ceaac62ca08ee419c3af01c5d3325a1bd043a82` | Private output-file SHA-256 values. |
| Point-receipt content digest | `85f7aa1f3fd2ae4573f01595110e0920d196cd31131b9ca1ba047b61c18ba676` | Canonical payload digest, not the point-file SHA. |
| Safe aggregate interpretation | [`L36 conditional H=0 result`](../l36-conditional-h0-descriptive-point-20261122.md) | Published aggregate, denominator, limits, and digest-domain distinctions. |

The complete local role-to-file map, including protected inputs and reviewer-specific locators, remains in the existing owner-private package and is not copied into Git. A protected companion note identifies those exact paths and hashes. Do not infer private locations from this shared handoff.

### Independent reviewer task and required return

Using the exact frozen pins above and the protected read-only role map, return a concise `PASS`, `REVISE`, or `BLOCKED` report. Check: (1) file-byte pins versus canonical receipt digests are kept distinct; (2) protocol, sample, candidate selection, issued reviewer-role packs, original returns, adjudication, and final descriptive output form the recorded provenance chain; (3) fixed denominators and assigned-case flow are preserved; (4) the retained v5 caller and preflight agree with the output's declared scope; and (5) the interpretation does not turn descriptive bounds into an interval, H1 decision, safety claim, overall accuracy, or usefulness claim. Cite exact mismatches by role and pin, without embedding case-level material. Do not rewrite source files, replace frozen artifacts, or create another outcome calculation.

### Bounded independent source/output review — 2026-11-23

An independent in-session reviewer who did not author the v5 caller returned **PASS for the source/output integrity slice only**. This was a bounded technical review, not a human study reviewer or domain expert. The reviewed caller and receipt chain are internally consistent: the caller's 22 input and five code roles match its pre-access and metadata-preflight pins; result-validation, run-state, membership sidecar, and point-result links match; and the file-byte versus canonical digest domains remain distinct. The reviewer found no pin mismatch. The role-bound intake, join-integrity, and display-check receipts support artifact-level separation and return binding. The review used metadata and source only; it did not access case rows, raw ratings, sealed-map contents, source records, or run a calculation.

This is not a separately hashed reviewer report or C1-G4 award. Parent review accepted only this bounded integrity slice; the remaining evidence and acceptance status above remain open. The existing artifacts do not identify people by R1/R2 role or document credentials; HUMAN's later role/process account is attributed reporting, not independent identity verification. Apply only the actual canonical G4 criterion; do not infer formal credentials or create a witness/video requirement. C1-G4 remains OPEN, and H1 remains `JOINT_CLUSTER_HOLD`.

### Evidence ceiling and still-unverified human facts

HUMAN reported on 2026-11-23 that R1, R2, and the chef/adjudicator were three different people; R1/R2 had cooking experience and culinary knowledge; their first-round ratings were completed independently before either saw the other's answers; and the chef/adjudicator was separate. This is attributed human reporting, not independently verified identity or credential evidence. The role-specific pack files, return-binding/intake receipts, and read-only pack checks independently support **artifact-level** R1/R2 separation and return-to-pack binding, but do not identify the people or document the stated experience. No evidence reviewed here establishes whether the SSoT §6 practice/handbook sequence was executed; no separate identity-to-role document was found in the reviewed artifact inventory. Do not convert this into a demand for formal credential certificates, a witness/video, or another rating round. No names, contact details, consent documents, raw labels, case IDs, sealed mapping, or private rows are included in this shared handoff.

### Independent method review and proposed C1-G4 disposition — 2026-11-23

The reported role arrangement is consistent with Protocol v1 §2's two independent raters and separate adjudication, and the bound pack/return/integration receipts establish the artifact chain. The same non-author technical reviewer assessed method/acceptance scope, not merely the prior pin audit. **Parent decision (2026-11-23): ACCEPT C1-G4 NARROWLY.** Protocol v1 §2's agreement gates are met by the aggregate output (κ=0.746567; case-cluster lower bound=0.700901, 2,000 valid draws), with separate adjudication and source/output integrity checks. HUMAN's role/process report is attributed rather than independently verified identity or credential evidence.

The reviewer found SSoT §6's T0 practice/handbook, repeat, and weekly-drift steps to be prospective controls, not explicit hard G4 gates in Protocol v1 §2 or the fixed acceptance map. Their execution remains unproven and limits training/drift claims; it does not alone fail narrow G4. No separate 20-case T1 checkpoint is evidenced, so the staged process must not be claimed. This review does not validate every individual label or establish safety, generalization, usefulness, or a primary H1 decision. No H1 interval is required for this narrow G4 disposition. The fixed ledger is **21/25** after parent acceptance; C1 is **4/5**, while fully accepted contributions remain **3/5**. H1 remains `JOINT_CLUSTER_HOLD`.

This review is independent verification of existing evidence, not a new rater round, adjudication, bootstrap/interval, or H1 analysis. The parent accepted C1-G4 narrowly; no further G4 review or activity is implied. H1 remains `JOINT_CLUSTER_HOLD` under unchanged Protocol v1.
