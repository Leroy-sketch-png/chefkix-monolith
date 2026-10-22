# L45 v26 rendered-review gate — 2026-10-21

## Observation

The campaign's pending visual step is a local rendered inspection of `ai/demos/iron-chef-midterm-v26-exhibit.html` at desktop and narrow widths. The existing rendered review remains open; this turn did not produce or inspect screenshots.

The in-app browser rejected the exact `file:///D:/code/chefkix/.git-worktrees/root-master-segment10-20261014/ai/demos/iron-chef-midterm-v26-exhibit.html` request under its URL security policy. The refusal permits only HTTP(S) and explicitly prohibits attempting the same outcome by workaround, indirect execution, raw CDP, browser commands, or alternate browser surfaces. No alternate rendering route was attempted.

## Offline checks completed

- `py -3 -m unittest ai/tools/test_midterm_evidence_exhibit.py -v`: 15/15 passed. These are source/content and structural assertions; they do not constitute rendered, accessibility, physical-device, or human review.
- Exhibit SHA-256: `47d9aa1d7f5e1f3f704d43ea32af5773ef2cf94aa906d94aff232457e11c15ff`.
- Speaker-notes SHA-256: `6497f21775bc2938a0f7193f14b4cccf49cc6ee3363d72aa517aa157e2fca1e6`.
- Test-file SHA-256: `7305909c6262871ff2661db9549b60a87fc85b4c910ff59ddae07d59b4ded5e2`.

## Boundary and next step

Do not mark mobile/desktop visual review passed based on these tests. Resume only with a user-provided rendered capture or another explicitly permitted route that does not bypass the browser policy. The timed human rehearsal remains `NOT RUN`; no audience, comprehension, product-value, safety, model-quality, or thesis-acceptance claim follows.
