# T54 production-app runtime handoff — 2026-11-05

## Shared evidence

- AI-service repository: `Leroy-sketch-png/chefkix-ai-service`, default branch `main`, commit `ca0577115fed853c4c9179390e56000ff2bc3484`.
- Regression source: `tests/test_graph_preflight_app_composition.py`, SHA-256 `46eb9bbb13df224fecf4f5506c9e9f1b108dde5f18a8609cdd2bf91cee1a3299`.
- Reproduction command: `python -m pytest -o addopts='' -q tests/test_graph_preflight_app_composition.py tests/test_allergen_safety_unit.py::test_pantry_api_withholds_unknown_allergy_suggestion_and_disclaims_safety tests/test_allergen_safety_unit.py::test_pantry_api_labels_known_nonoverlap_as_bounded_policy_only`.
- Recorded result: **10 passed**, 4 existing Starlette/Pydantic warnings. The exact missing-calibration response is asserted through FastAPI `TestClient` against the production ASGI application. Fixtures are synthetic; the model call is mocked. This is local in-process HTTP evidence, not a deployed/live network result.

## What this proves—and no more

1. Invalid service/research credentials are refused before server-owned provider/calibration state is used.
2. The default-off research route with no calibration returns `no_guarantee`, missing binding, null manifest and `tHat`, zero calibration cases, `missing_artifact`, explicit abstention reasons, no ranker/filter provenance, and no candidates.
3. A generated unknown-allergy suggestion is withheld under the bounded policy; the response does not claim medical safety.
4. Unsupported legacy ratio/confidence values are serialized as null rather than echoed as evidence.

The route remains default-off. These tests establish no calibration result, recommendation, guarantee, clinical safety, deployed behavior, or T54/C2/C3/C5 acceptance.

## Exact unresolved input and ownership

The current owner is **LEAD** for locating/recovering the source-owned method-v2 producer and its source/custodian; that custodian is not identified in the checked campaign state. **MEMBER/M-I** owns implementation only after the real producer package is handed off.

Required source-backed package: (a) producer/schema/version and deterministic recipe-grouped development/calibration assignment plus identity join; (b) one record per assigned calibration case/cell containing the method-defined rank or an allowed explicit failure, retaining the fixed denominator; (c) byte-verifiable manifest/file inventory, sizes and SHA-256, cohort/cell, alpha/M and split/score definitions; and (d) independently observed, matching role bindings for graph/pool, hard-filter policy, ranker, scorer, score contract, label contract, assignment/identity index and calibration scores. The graph and calibration must bind to the same manifest. A loader must derive these bindings from verified bytes—not accept caller-declared digests as evidence. Until supplied and validated, the existing project-owned bundle serializer remains `missing/no_guarantee`; do not enable the route or claim calibration acceptance.

No private rows, labels, IDs, credentials or outputs are present in this handoff.
