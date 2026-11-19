# Standalone C1 Reviewer Renderer

This renders an already-blinded, rater-specific pack as one offline HTML file. It does not recover source data, generate candidates, alter the sample, select arms, or expose the private mapping key.

```powershell
python tools/research/c1/render_reviewer.py --input PRIVATE_R1_DATASET.json --output PRIVATE_R1_REVIEW.html
```

The input contains only `pack_id` (opaque 32-64 hexadecimal characters), `reviewer` (`R1`/`R2`), and `views`. Exactly 150 views use the existing public pack fields: `view_id`, `recipe_title`, `ingredients`, `target`, `candidates`. Candidates contain only `blind_candidate` and `display_names`. There must be five or six unique opaque candidates per case for this first audit roster; missing model output is not padded or silently removed. A failed input is a coordinator problem, not an invented reviewer task.

The renderer rejects unexpected/private fields, non-opaque IDs, missing context, duplicate IDs, altered case counts and output overwrites. A content binding separates browser state and returned answers across packs. Script-safe embedded JSON and text-only DOM rendering preserve recipe content without executing it. The file makes no network requests.

Underscores in ingredient/target/candidate names become spaces for display only; the embedded original strings, opaque IDs and return binding are unchanged. Slash-separated source naming variants remain visible for all candidates under the same identity-based presentation rule, not a gold-only alias cue. The guide says to record U with a note when those variants have incompatible meanings in the case; no semantic identity repair is implied.

Reviewers can answer, add optional notes, navigate, save/resume through browser storage, download progress/final answers, and restore their own downloaded file. Browser-storage failures produce a download/restore fallback, not a silent save claim. No accounts, server or dependencies are needed to use the issued HTML in a normal desktop browser. Mobile layout is checked; local-file execution in phone attachment previewers is not established.

The downloaded return includes original four-field judgment records under `judgments`, optional notes separately, completion state and pack binding. Private ingestion must validate those records against the original issued pack; a browser export is not scientific acceptance. Do not send synthetic QA fixtures to reviewers or commit populated files, screenshots of real recipes, sealed mappings, or answers to public Git.

```powershell
python -m unittest discover -s tools/research/c1 -p test_render_reviewer.py -v
```

The software tests are source-free. They do not establish human validity, actual 150-case pack readiness, identities, consent, model performance, or a research milestone.

## C1 Adjudication Integrator Pipeline (`c1_adjudication_integrator.py`)

Integrates the private L36 adjudicator return (`c1-adjudication-return.json`) with the 717 agreed candidate judgments via the sealed opaque identity key (`c1-l36-adjudication-key-v1`), constructs the unified 867-judgment matrix, classifies all 150 cases under `iron-chef-l36-case-rule-v1`, and computes partial-identification point bounds on the unseen strata ($N=3815, n=100$).

- Unanswered adjudications default to `U` under the L36 rule.
- Case-level rule: at least one `A` $\implies V=1$; all `C` or `N` $\implies V=0$; contains `U` without `A` $\implies V=\text{unknown}$.
- Operational failures retained in denominator as $V=0$.
- Fixed denominator preserved verbatim; never complete-case analysis.
- Generates canonical SHA-256 sealed integration receipt (`c1-l36-adjudication-integration-receipt-v1`).
- `extract_agreed_map_from_returns()` parses raw reviewer returns (R1, R2) and `c1-private-key-v1` to cleanly isolate the 717 agreed pairs.
- Full audit mode runs 2,000-draw recipe cluster bootstrap and evaluates H1 decision rule (`SUPPORT_H1`, `REFUTE_H1`, `INCONCLUSIVE`, `JOINT_CLUSTER_HOLD`).

```powershell
python tools/research/c1/c1_adjudication_integrator.py \
  --sealed-key c1-l36-adjudication-key.json \
  --adjudication-return c1-adjudication-return.json \
  --case-manifest case_manifest.json \
  --agreed-map agreed_map.json \
  --output integration-receipt.json
```


