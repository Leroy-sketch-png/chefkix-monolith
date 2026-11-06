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
