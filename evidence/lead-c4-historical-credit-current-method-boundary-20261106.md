# C4 historical G1/G2 credit vs current L43 method readiness

**Disposition:** Keep the two historically recorded campaign milestones earned, narrowly. They do not establish implementation readiness for the current L43 selection, any judge-vs-human acceptance, or completion/acceptance of contribution C4. No score or Protocol v1 status changes.

## Reconciled evidence

- The current campaign ledger records C4-G1 and C4-G2 as earned in Continuation 368, and its current fixed measure is 19/25. Continuation 368 binds G1 to the historical `iron-chef-c4-judge-contract-v1` contract (SHA-256 `23d9667406850ef40ee951baa137f4d01a13dd8bfd16a8ca6f19cbe9ea52c19d`) and G2 to the historical `c4_llm_judge_runner.py` plus its 12-test suite and five-artifact deterministic build receipt. Treat these as dated, bounded implementation receipts—not as evidence that the later-selected judge method is implemented or scientifically accepted.
- The standalone `tasks/ACADEMIC_ACCEPTANCE_MAP.md` named by older mission text is absent from the current research checkout. The retained T01 audit says C4 acceptance requires the C4 strategy/protocol gates, disclosure of judge rejection, and separately authorized/consented human study evidence; synthetic judge tests do not constitute human evidence. The campaign's current stable outcome map likewise says C4 is not accepted until its human/judge and usefulness/comprehension gates are met. This note does not reconstruct missing verbatim milestone wording.
- L43's current shared decision selects `gemini-2.5-flash` and strategy §7.1's pool-level A/C/N/U prompt `c4-judge-pool-prompt-s7.1-v1`. It explicitly states the old single-candidate runner is incompatible: the prompt exposes order/position; the old report hardcodes leniency/self-preference metrics; and forward/reverse agreement is not agreement with human labels. L43 therefore labels G1/G2 as historical bounded contract/runner evidence only, not proof of current family/prompt compatibility or actual judge acceptance.
- The Member's current L43 handoff correctly assigns updating the existing contract/runner path (not a parallel framework), with opaque candidate IDs, requested and returned model identity, both-order raw outputs and fixed failures. Current judge usability/C4 acceptance/results still require paired human final adjudications and frozen Protocol v1 gates; no provider call or spend is authorized by that handoff.

## Ledger interpretation

| Question | Reconciled answer |
| --- | --- |
| Were historical G1/G2 receipts deleted or revoked? | No. Retain the ledger's 2/5 milestone credit as historically recorded by Continuation 368. |
| Does that credit mean the selected L43 method is ready? | No. Current L43 explicitly identifies the incompatibility and gives M-I the existing-path correction. |
| Is C4 accepted? | No. C4 remains 2/5 campaign milestones and is not a fully accepted contribution; paired human adjudications, Protocol gates and the other C4 acceptance conditions remain outstanding. |
| Did this review alter the scientific method or authorize execution? | No. Protocol v1 remains SHA-256 `b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`; no call, spend, test, label access, data transfer or job was performed or authorized here. |

## Next acceptance boundary

M-I should update the existing path to implement the exact L43 selected pool-level prompt and serialization, including opaque IDs, hidden order/position, model identity checks, deterministic reverse-order pass and raw response/failure preservation. Source-free implementation checks establish only implementation behavior. Before test scoring, freeze exact prompt/model/serialization and exposure; judge acceptance requires paired human final labels and the frozen Protocol v1 agreement/leniency/instability/invalid-output gates. Do not treat bootstrap agreement between two judge passes as human agreement, and do not silently count a second judge family as selected.

## Provenance and limits

- Current shared LEAD decision: [`lead-l43-judge-family-prompt-decision-20261106.md`](lead-l43-judge-family-prompt-decision-20261106.md).
- Current shared MEMBER handoff: `BACKLOG_MEMBER.md`, “M-I L43 C4 judge handoff (2026-11-06)”.
- Historical ledger receipt: campaign MISSION Continuation 368 and its cited contract/runner/test/build receipts.
- The exact deleted/absent standalone acceptance-map wording was not recovered. The conclusion relies on the retained campaign summary, Continuation 368, T01 audit and current L43 decision; it does not claim verbatim recovery of the missing map.
