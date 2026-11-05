# L42 — S47 human-evaluation count reconciliation

Date: 2026-11-05
Disposition: bounded primary-text reconciliation; no novelty or acceptance claim.

## Official source inspected

Li and Zaki, “Food Knowledge Representation Learning with Adversarial Substitution,” AACL-IJCNLP 2022, official ACL Anthology PDF: <https://aclanthology.org/2022.aacl-main.50.pdf>, human-evaluation section and Table 3.

## Finding

The methods prose states that the study covers 150 food substitutions. It then describes randomly selecting 50 recipe substitutions, 50 ingredient substitutions, and 100 generalized ingredient substitutions. Those component counts total 200 if they are disjoint. The paper does not specify in the screened passage whether the sets overlap, so it does not establish that there were 200 unique items or explain the 150-versus-200 difference. The unique-case and annotation denominator remain unresolved; preserve the prose total and each category count without choosing one as verified.

The same section reports ten annotators, five-point ratings, and Fleiss' kappa of 0.61. This confirms relevant context-aware human-evaluation prior art. Its reported appropriateness/grammar/semantic outcomes are not the proposed fixed shared-pool, blinded per-candidate A/C/N/U audit or the frozen conditional single-gold rescue estimand.

## Limits and action

This does not establish a typo, category overlap, the exact number of distinct cases, author intent, independent replication, or global novelty. No author contact, dataset acquisition, participant records, scorecard change, C1 acceptance, milestone, or protocol change occurred. The precise discrepancy and its limit are integrated into the local SSoT and remain part of the open L42 full-funnel/lineage work. Protocol v1 SHA-256 remains `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.
