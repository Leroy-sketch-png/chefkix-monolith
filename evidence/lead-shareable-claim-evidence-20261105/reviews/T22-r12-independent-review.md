# T22 — Independent review of the R12 FooDB inventory reproduction

**Decision: PASS NARROWLY — local presence-inventory arithmetic only.** This is independent evidence for the bounded count in R12, not a claim-register status change and not evidence for any C1–C5 outcome. The existing v7 row remains `UNVERIFIED` pending the separately authorized one-row application and its validation.

## Independent checks

Read-only review performed in research worktree `D:/code/chefkix/.git-worktrees/root-master-segment10-20261014`, HEAD `8529a8740c4d0c8c4f79799d4aa91b77ff08b185`; the nested AI service checkout was at `067520856b44597e59bb1e8464dd41ea4de624e0`. The ignored local profile artifact was read in place; it was not copied, uploaded, or redistributed. I did not run the T21 reproduction driver or its count function. Instead I parsed the pinned raw JSON bytes with a separate read-only Python expression, independently counted normalized food keys, distinct lowercased nonempty compound names, and all food-compound rows, then instantiated the pinned service `CompoundIndex` directly with bytecode writing disabled.

| Item | Independently observed SHA-256 / result |
|---|---|
| Local ignored profile `D:/code/chefkix/chefkix-ai-service/models/foodb_compounds.json` | `8490cd6adadc5bad0e59f5f4edd8d05020b941b987ca47e38ac0cc80d85a04ac` |
| Provenance sidecar `.../models/foodb_compounds.provenance.json` | `5117b791bd825b5c7c87f4f816342ff4bc65100b74502a8652157a989ed9d208` |
| Runtime source `.../src/compound_reasoning.py` | `e2bcad3cd81e196490aaa6bcc4f014721e9cb9e69734351520fabedef7463e3a` |
| T21 reproduction driver | `77bde878ec1f8636ff3760ea5ab269b02dd08c5a7121838d4bc54bded803d029` |
| T21 focused test file | `0466abaf35a90d8f0ab7fffb23188ca6bf4a9cc6321f437bd86e37d474f6e61b` |
| T21 JSON receipt | `14a3bf7f66a74e303f09d61d3329b0d308b077aa1916de33e5017bf74b6dbd2d` |
| T21 Markdown report | `31be0a43add7da0dcd2f68d03c531eefdeb261017a7aefaa602e9d5c4090e9a0` |
| Direct recount from profile bytes (31,902,304 bytes) | 971 food keys after lowercase/trim; 206 distinct lowercase compound names; 125,992 link rows; no empty/non-list food rows, malformed/unnamed link rows, or duplicate normalized food keys |
| Direct service load, Python 3.10.11 | `foodb_loaded=true`, `size=971`, `load_error=null`; runtime verified the sidecar’s `data_sha256` against the profile bytes |

The sidecar itself declares FooDB 1.0, the official JSON download URL, CC BY-NC 4.0 with commercial use requiring permission from FooDB authors, and presence-only semantics for the selected 255 Content source IDs. Its declared profile digest matches the independently rehashed profile. The runtime source checks that declaration, profile digest, required provenance hash fields, presence-only wording, row shape, and positive global Content support before loading. The pinned files and declared semantics therefore bind the reproduction; this check did not independently retrieve or validate the upstream archive.

The count calculation was independent of the T21 driver: on raw decoded JSON, `len({food.lower().strip()})` = 971; `len({row['name'].lower()})` = 206; `sum(len(rows) for rows in profile.values())` = 125,992. Those values match the T21 JSON receipt, sidecar fields, and live runtime food-key count. The runtime does not separately expose counts for unique compound names or links, so those two values are direct profile/sidecar arithmetic—not runtime measurements. Driver inspection shows that its `run()` hashes the driver at runtime, checks all three pinned files, recomputes these direct counts, then checks only runtime `size` against the food-name count. T21’s receipt and report accurately distinguish the runtime-exposed count from the two counts it cannot expose.

## R12 fit, evidence grade, and limits

SSoT §15.1’s R12 wording is “FooDB 971/206/125,992 service inventory” and its limiting condition is “Bounded presence facts.” The reproduced arithmetic fits that narrowly construed claim. The 206 is a count of distinct lowercased **names in this profile**, not an independently resolved count of unique FooDB compound IDs; the 125,992 is the number of profile rows/food-compound links, not distinct food-to-compound pairs proven against the upstream database. Avoid stronger labels such as “verified FooDB compounds” unless the canonical claim wording or a separate source-level audit establishes those meanings.

**Evidence grade: E2 for local reproducible arithmetic and runtime load/count; E1 for the provenance/license statement, which was inspected in the pinned sidecar but not independently checked against the upstream source or rights holder.** This does not authorize commercial use or establish legal clearance. It does not establish dataset completeness/correctness relative to FooDB, ingredient similarity, functional or food-form equivalence, flavor, nutrition, safety, recommendation quality, human outcomes, or C1–C5 acceptance. No labels, network, GPU, paid resource, human study, or external service were used.

## Alternative, falsifier, and decision-changing unknown

- **Strongest alternative interpretation:** the three counts are properties of a locally installed, sidecar-described derived presence table, not proof that the table is a complete/correct representation of FooDB or that its 206 names correspond one-to-one with authoritative compound identities.
- **Falsifier:** a fresh byte comparison/count from the same pinned artifact that disagrees on any count, a runtime failure to load the same pinned bytes, or a finding that the v7 R12 definition requires upstream identity/completeness rather than bounded local inventory.
- **Decision-changing unknown:** whether R12’s canonical definition intends distinct display names or authoritative compound IDs, and whether rights/provenance documentation independently supports the sidecar’s stated noncommercial terms. Resolve wording/ownership with the designated human; do not infer either from this reproduction.

## Acceptance artifacts and preserved boundary

For any subsequent R12-only application, retain this review, the exact T21 JSON receipt/report and their hashes above, the ignored profile/sidecar/runtime hashes above, the source-recorded command, and an application/validation receipt showing only the R12 row changed. Do not treat the successful reproduction as permission to modify other rows, the mismatch ledger, backlogs, strategy, or C1–C5 outcomes. This audit created only this review file; no claim register or source was edited.
