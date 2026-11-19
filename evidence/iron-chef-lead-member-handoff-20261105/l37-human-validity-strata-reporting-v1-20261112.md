# L37 / H2 strict-A@5 point-estimand and reporting rule v1

**Scope/status:** Lead-owned, post-adjudication reporting convention for the existing 150-case human strict-A@5 audit. It fixes descriptive point estimands and strata presentation only. Because it was adopted after outcomes were available, resulting H2 summaries are exploratory/descriptive, not preregistered or confirmatory. It sets no H2 support threshold and changes neither Protocol v1 nor the sample, case labels, arm roster, or H1 decision. This is **not** a complete Protocol-accepted result: the addendum does not select or estimate intervals and does not waive any applicable Protocol v1 uncertainty/reporting requirement. Those requirements remain unresolved for acceptance; no Wilson/bootstrap method or weighted-contrast interval is adopted here. Sampling uncertainty is **not estimated** by this rule.

## Frozen frame and denominators

- Protocol v1 SHA-256: `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.
- R14 frame-count receipt SHA-256: `aab2c1fa67fb9473f449856bd085bc90767bbb0d5939387593ec1393a0416ff0`; frame SHA-256: `e5d065228c20e70ead36c1eb92cb104fab9ceab83a0595230f852b74c4711905`.
- The 150 assigned cases occupy four fixed cells, written `(N_h, n_h, W_h=N_h/n_h)`: unseen-profiled `(176,50,3.52)`; unseen-unprofiled `(3,639,50,72.78)`; seen-profiled `(986,25,39.44)`; seen-unprofiled `(5,946,25,237.84)`. Frame total `N=10,747`; assigned sample `n=150`; unseen `N=3,815`; seen `N=6,932`; both-endpoint-profiled `N=1,162`; not-both-profiled `N=9,585`.
- The frozen current human-audit roster is the GISMo arm only. Do not require other planned C2 arms to make the first H2 table. A future arm comparison needs its own matched, version-bound decision and identical case IDs.
- “Profiled” means both endpoints have the required profile data under the frozen frame definition. It is not a treatment, causal exposure, or allergen-truth claim. H2 all-case validity does not require the H1 `H=0` map.
- A blinded extra gold item counts only if it is actually among the exact primary GISMo top five being adjudicated; do not use extra adjudication outside that frozen slate to change a case outcome.

## Case and cell flow

Preserve each assigned case and the exact primary GISMo top-five slate identity. Apply the adopted case rule `iron-chef-l36-case-rule-v1` to that verified slate: any final adjudicated `A` among its five items gives `V=1`; no `A` and five final `C/N` labels gives `V=0`; no `A` plus one or more `U` or unresolved/missing judgments on an otherwise valid verified slate gives `V=unknown`. Keep raw ratings separate from final adjudication.

Report the single frozen GISMo arm, one row for each of the four cells. Every row shows assigned `n_h`, verified valid slates, `V=1` count `v_h`, `V=0` count `z_h`, `V=unknown` count `u_h`, and operational-failure and input-integrity flags/counts. These flags may overlap the outcome flow; they are not extra mutually exclusive V categories. For a cell where every assigned slate and identity is verified, require `v_h + z_h + u_h = n_h`.

- A valid, verified slate with an absent or unresolved human judgment is genuine `V=unknown`.
- A malformed/duplicated/altered slate, broken source join, or unverifiable case/arm/top-five identity is an L36 input-integrity failure. Retain the case and assigned denominator, but mark the affected cell metric unavailable. Do not turn it into `V=0`, `V=unknown`, or exclude it. Other unaffected cells may still be reported; only pools/contrasts requiring the unavailable cell are unavailable.
- An operational failure/no-output is a separate execution-flow flag, not a human judgment and not a fourth V category. Do not impute a `V=0` or `V=unknown` label from it in this human-audit estimand. If it prevents verification of the frozen slate, mark the affected metric unavailable under the same integrity rule. Operational yield is outside this L37 point-estimand decision.

For each fully verified cell, report the sample point-estimate endpoints for the unresolved labels:

```text
valid@5 lower_h = v_h / n_h
valid@5 upper_h = (v_h + u_h) / n_h
```

These are **unknown-label point-estimate envelopes for the assigned sample**, not bounds on the true 10,747-case population parameter. Keep `V=1/0/unknown` flow and failure/integrity fields beside them. No sampling uncertainty is estimated here.

## Fixed-frame point summaries and contrasts

Let `H_g` be the two frozen cells for `g ∈ {unseen, seen}`. For either endpoint `e_h` (`lower_h` or `upper_h`), report natural-composition descriptive points:

```text
P_g^e = Σ(h∈H_g) N_h e_h / Σ(h∈H_g) N_h
```

Show the four underlying cell rows beside each summary. Also report a fixed-frame pooled point across all four cells:

```text
P_all^e = Σ(all four h) N_h e_h / 10,747
```

Use the frozen frame counts once; equivalently use `W_h=N_h/n_h` on cell numerator and denominator. Never use the unweighted 150-case average, apply both forms of weights, or silently renormalize around unknowns or unavailable cells. If a required cell fails integrity, the dependent pool/contrast is unavailable—not reweighted over remaining cells.

The equivalent global case-weighted endpoint formulas make the denominator explicit:

```text
P_all^lower = Σ_h W_h v_h / Σ_h W_h n_h
P_all^upper = Σ_h W_h (v_h + u_h) / Σ_h W_h n_h
Σ_h W_h n_h = 10,747
```

The denominator is the frame's design-weight total, not the number of human judgments.

Report within-profile novelty contrasts first, with positive meaning unseen minus seen:

```text
Δ_c^valid@5 ∈ [P_unseen,c^lower - P_seen,c^upper,
                P_unseen,c^upper - P_seen,c^lower]
```

This arithmetic range reflects unknown labels within the assigned-sample point estimates; it is not a confidence interval or a population partial-identification bound. Do not subtract lower from lower or upper from upper and call that the contrast range. Report the two profile strata separately. Then show natural-composition seen/unseen summaries using each group’s own frame mix; describe their difference, not a causal effect.

For the natural-composition point contrast, use the same endpoint arithmetic explicitly: `[P_unseen^lower - P_seen^upper, P_unseen^upper - P_seen^lower]`. It is still only an assigned-sample unknown-label envelope, not sampling uncertainty or a causal effect.

As a separately labeled composition sensitivity only, standardize both novelty groups to the same full-frame profile mix:

```text
q_profiled   = 1,162 / 10,747
q_unprofiled = 9,585 / 10,747
P_g,std^e    = q_profiled P_g,profiled^e
               + q_unprofiled P_g,unprofiled^e
Δ_std^valid@5 ∈ [P_unseen,std^lower - P_seen,std^upper,
                 P_unseen,std^upper - P_seen,std^lower]
```

Use exactly the same `q` values for seen and unseen. Label this “common-profile-mix sensitivity”; it is not a profile effect or causal adjustment, and does not replace the natural-composition primary description. Profile cells are descriptive strata, not routing features.

## Claim and operation boundary

This rule authorizes only the above post-outcome H2 strict-A@5 descriptive point tables and sample unknown-label envelopes. It does not choose or satisfy sampling-uncertainty reporting, establish population generalization, infer causality, establish H2 support/refutation, transfer H1’s `0.15` threshold, establish system superiority, or establish allergen safety. Do not redesign Hit@k/MRR or claim them from this human-validity audit. Protocol v1’s applicable interval/reporting requirements remain in force; resolve and review them before describing any result as fully accepted under that protocol. H1 remains a separate unseen-only conditional analysis under its adopted L36 rule and existing uncertainty hold.

M-I owns the calculation and must bind the actual frozen frame, assigned sample, exact primary-top-five key/slates, adjudication receipt, and verified bytes before computing. R14's receipt counts are frame metadata, not a fresh frame reproduction; matching aggregate counts do not establish case identity. LEAD accepts the later aggregate output only after those input bindings are verified. Do not expose raw case rows, IDs, recipe text, or labels in shared reports. This rule itself reads no adjudicated case values and creates no estimate, interval, score, milestone, or acceptance credit.
