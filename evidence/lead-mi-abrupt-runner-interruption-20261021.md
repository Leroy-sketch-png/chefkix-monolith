# M-I abrupt runner-interruption regression — bounded acceptance receipt

Date: 2026-10-21
Scope: T49 synthetic-only forced termination of the local research runner CLI, after child start and reservation. This records one fail-closed regression boundary; it does not close M-I.

## Reviewed research artifacts

All detailed source, task, test output, and independent review remain in the local research worktree because that repository has no configured remote. The scoped local commit is `153da37db43fca24b741065e3a7fe382645edcd3`:

`D:/code/chefkix/.git-worktrees/root-master-segment10-20261014`

| Artifact | SHA-256 |
| --- | --- |
| `ai/tools/test_research_job_runner.py` | `aa0b4210b5dcd78273fc42b098839baaedb268188f0a659f53e10a14b0c266b` |
| `ai/tools/RESEARCH_JOB_RUNNER.md` | `5228ede7e32bf3104e93e1e4bd4b87a2dcdef577cb1b608cea9100aec36c211f` |
| `ai/campaigns/iron-chef-outcome-push-20261021/tasks/T49-runner-abrupt-reservation-fail-closed.md` | `55455e16ac783d75373e391dee7d1ebd092f536e5f1a07469ae88d0234e0aa88` |
| `ai/campaigns/iron-chef-outcome-push-20261021/tasks/T49-independent-review.md` | `bc2bc838c368777e2b2b4b19f6326d9b1c6de5e3b7f4c772c19e1a592e3cf951` |
| `ai/evidence/member-backlog/mi-abrupt-runner-interruption-20261021/acceptance.md` | `680b1555a54ecabb2d3add28ee7ec53d9d7a3e61934699fa6c79b330652998f5` |

## Observed validation

- Focused abrupt-runner termination regression passed on native Windows and Ubuntu WSL.
- Full runner suite passed on both: Windows 53 tests (49 pass, four pre-existing symlink-privilege skips); WSL 53/53 pass.
- `py_compile` and scoped `git diff --check` passed. Production `research_job_runner.py` was not changed.
- Independent review is PASS narrowly (report SHA above) and lead acceptance is limited to this fixture and the stated environments.

The fixture verifies that after forced runner-process termination the ledger remains `RESERVED`, no terminal receipt is fabricated, one synthetic child invocation is recorded, and direct/CLI inspection remains read-only and reports `MANUAL_REVIEW_REQUIRED`; same-key execution is refused. The acceptance ceiling excludes machine/power loss, storage durability, concurrent hosts, stale-process detection, arbitrary interruption timing, sandboxing, and automated recovery. `UNKNOWN` remains do-not-retry. No real job, credentials, API, network, paid resource, scientific data, labels, protocol, inference, quality claim, or M-I status closure is established.

This receipt is a summary pointer, not an independent reproduction. It does not mirror the local research artifacts into the monolith.
