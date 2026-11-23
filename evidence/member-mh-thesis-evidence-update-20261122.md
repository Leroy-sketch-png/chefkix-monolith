# M-H thesis evidence update for Lead review

Baseline: monolith master 1c1cb90. This is a Member draft for Lead review. It uses the shared aggregate receipts and preserves the existing criterion, cohort, and uncertainty method.

## Reusable L36 evidence table

| Report item | Value from the shared receipts | Scope |
| --- | --- | --- |
| Fixed frame audit sample | 150 cases, 867 paired candidate judgments | Frozen GISMo-primary audit, recorded gold shown blind where present |
| Exact four-category agreement | 717/867 (82.7%) | Candidate judgment agreement in the issued packs |
| Nominal Cohen’s κ | 0.746567, case-cluster percentile interval 0.700901–0.786907 | Already reported by Chapter 6. Lead accepted C1-G4 narrowly on 2026-11-23; no broader C1 or H1 conclusion follows |
| Integrated case outcomes | 117 strict-A successes, 28 definite failures, 5 unknown | Full 150-case adjudicated cohort |
| Unseen H=0 membership | 49/50 profiled and 48/50 unprofiled, 97 total | Assigned unseen cohort retained at 100; three observed top-five hits |
| Weighted H=0 denominator and conditional bounds | 3,665.92; 0.711494–0.752160 | Descriptive partial-identification bounds from the existing authorized computation |
| Mission accounting | 21/25 milestones (84%); C1 4/5; C4 2/5 | Lead accepted C1-G4 narrowly on 2026-11-23; primary H1 remains open. The Chapter 6 review note in BACKLOG_LEAD still says G4 is open; reconcile that wording before amending Lead-owned thesis text |

Sources: [Chapter 6](iron-chef-lead-member-handoff-20261105/chapters/06-human-validity-audit.md), [conditional result and provenance](l36-conditional-h0-descriptive-point-20261122.md), and [Lead status](../BACKLOG_LEAD.md#current-evidence-and-recovery). No participant rows or private receipts are required to reuse this table.

## Chapter 6 figure and table for Lead review

- [Figure: strict-A validity among recorded-reference misses](member-l36-thesis-exhibit-20261122/l36-h0-outcomes.svg)
- [Table: H=0 case outcomes and descriptive bounds](member-l36-thesis-exhibit-20261122/l36-h0-outcomes.csv)

**Proposed caption.** The two unseen strata assigned 100 cases. The recorded reference appeared in the primary GISMo top five for three cases, leaving 97 observed H=0 cases. Panel A gives the final strict-A outcomes by stratum. Panel B shows the weighted descriptive partial-identification bounds of 71.1494% to 75.2160%. The lower endpoint counts observed strict-A successes, and the upper endpoint additionally treats the unknown cases as potentially strict-A. The weighted eligible denominator is 3,665.92. Lead accepted C1-G4 narrowly on 2026-11-23. The design-matched uncertainty interval and primary H1 decision remain open.

Build from the repository root with Python and Matplotlib 3.10.8.

```powershell
py -3 tools/research/l45/build_l36_exhibit.py ./out/l36-thesis-exhibit
```

The generator reads only the shared [L36 aggregate receipt](l36-conditional-h0-descriptive-point-20261122.md) and verifies its SHA-256 after converting line endings to LF. It checks stratum membership and weighted arithmetic before writing the SVG and CSV. The SVG includes a text description. A clean Git snapshot reproduced both files byte for byte. The CSV SHA-256 is `ab033c47ccdef406f6d76336241814680f213fe6827fce2fc26681ad5044f250` and the SVG SHA-256 is `ea48d255b8fec72a4f788fb4884400930e5920289f4603562596cffdd05d7bb8`.

## Offline manuscript review copy

The [L45 builder](../tools/research/l45/README.md#offline-manuscript-review-copy) assembled all 14 authored chapters from monolith `fb24de9513f1b358b612e221fd33a84a9c823b4b`. All 14 input hashes matched the Lead manifest. The build preserves the chapter prose and dated progress statements. It adds navigation, working links to exact shared copies, and the L36 aggregate figure as a separate review appendix.

The browser checks found 154 rendered equations, 10 tables, and 47 in-document chapter links. Chromium displayed the review copy at 1360 and 390 CSS px with no horizontal page overflow, broken in-document anchors, or external requests. The reference appendix lists 54 unavailable citation occurrences covering 48 distinct chapter and destination pairs. The citation labels and original destinations remain available for review.

Two runs from the pinned Git revision emitted the same 434,875-byte HTML file. Its SHA-256 is `a0a524864b6d973d4ff44dec517f6904d83449416a599ae3c3f187b10258e974`. This is a Member review build. Lead manuscript reconciliation, review of unavailable citations, and publication decisions remain open.

### Exact citation follow-up

The review build from monolith `79f967f6b5c2ee57574d9bef49081a47b1451503` restores eight links using [the reference pins](../tools/research/l45/shared_reference_pins.json). Six came from the v20 claims register and exact-byte Member crosswalk (R05, R09, R11, R19, R41, R42). Two came from the [Lead M-A representation receipt](lead-ma-graph-representation-boundary-20261021.json): the current-source build check and the rebuilt local runtime. These links recover published evidence at its recorded scope.

The two similarly named M-A receipts for the initial loaded-artifact route and earlier local runtime snapshot remain unresolved. Their published Git bytes differ from the original pins, and the default branch history contained no match at those paths. The appendix now lists 46 unresolved citation occurrences across 40 chapter and destination pairs. Chromium checked both review widths. A changed-pin check left the affected citation unlinked.

Two builds from the same source revision produced identical 435,134-byte HTML. SHA-256 `40edc672d5aafffa6c10c88cce57dbb23497df3f7de30e6eb4ac6a4359334b57`.

### Lead chapter refresh (2026-11-23)

The offline manuscript was rebuilt from Lead revision `b52e5b646ce174604aa57ee95a005bd61c91904c`, including the refreshed Chapters 13 and 14. The builder verified all 14 chapter hashes against the handoff manifest. The review copy has 154 rendered equations, 10 tables, 49 chapter links, eight restored exact citation links, and 44 unresolved citation occurrences across 38 chapter and destination pairs.

Two builds produced identical 441,650-byte HTML with SHA-256 `a12dcf593e10202621a9731c06a83982f51b674d5a9dbf723584be2ce2129da8`. The local output is `output/reports/iron-chef-manuscript-review-b52e5b6.html`. The earlier Chromium layout checks apply to the earlier build. Q20 revalidation and third-person reproduction remain open.

## Suggested manuscript edits

| Location | Proposed correction |
| --- | --- |
| Chapter 13 evidence-state header and §13.2 | Replace the stale current-status 19/25 and C1 2/5 figures with 21/25 and C1 4/5, reflecting the 2026-11-23 narrow parent acceptance. Replace the unexecuted-audit sentence with the completed frozen audit, integrated adjudication, and scoped C1-G3 disposition in Chapter 6. Keep the 200-case diagnostic separate. |
| Chapter 13 change conditions and §13.2 evidence map | Record that the adopted operational addendum resolved case-level V/U handling and the conditional descriptive bounds are available. Record the narrow C1-G4 acceptance per the 2026-11-23 parent decision; keep the held primary joint-cluster interval/decision open for H1. Do not broaden C1 or H1 claims. |
| Chapter 14 evidence-state header and current accounting | Use 21/25 and C1 4/5 for current accounting; date 19/25 and 20/25 as historical. Preserve the distinction between campaign milestones, individual claim status, and current acceptance for the study deployment. |
| Chapter 14 C2 transport wording | Label the canonical-first projection as historical packaging. The current shared Leader disposition names T83's nested alias-group representation as controlling for the frozen run. Preserve the 10,747-case denominator and distinguish prepared payloads from provider results. |
| Chapters 13/14 product wording and L46 defense | Cite the older graph result at its recorded scope. Describe the present staging integration gate separately: mount the pinned bundle, verify authenticated serving metadata and Explorer hash, then observe the timed rehearsal. Keep T395 open until those observations are made. |

Suggested §13.2 replacement for review:

> The frozen audit received both independent reviewer returns and the complete adjudication return. The integrated output preserved 150 cases and 867 candidate judgments, and Lead accepted C1-G3 for the collected and adjudicated cohort. Under the adopted strict-A and explicit-unknown rule, the weighted conditional H=0 descriptive bounds were 0.711494–0.752160. Their width reflects unknown case outcomes. Lead accepted C1-G4 narrowly on 2026-11-23. The design-matched uncertainty analysis and primary H1 decision remain open.

## Review and clean checkout

- Run the [L45 checker](../tools/research/l45/README.md) against the current shared manifest to verify chapter integrity and the recorded text boundaries.
- Use the [M-G byte crosswalk](member-mg-shared-artifact-crosswalk-20261122.csv) to locate published evidence before opening source-owner requests. A match to the wording snapshot provides provenance only.
- Q20 still needs the exact Segment-10/11 revalidation receipt and any referenced authorized artifacts. Real-result reproduction and a witnessed rehearsal each require their own receipts.
