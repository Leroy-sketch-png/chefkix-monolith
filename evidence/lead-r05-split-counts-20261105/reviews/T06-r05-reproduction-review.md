# T06 — Independent R05 reproduction review

**State:** DONE — independent review passed narrowly for R05 fixed-file split counts; no status application was delegated.

## Outcome

Independently review the R05 split-count reproduction package for row-level semantic fit to SSoT §15.1 and exact byte/provenance bindings. Return pass/fail with concrete reasons. Do not edit the claims register, mismatch ledger, mission, strategy, or backlogs.

## Inputs

- `academic_vision_and_strategy.md` §15.1 R05 wording and source note.
- `ai/evidence/claims-register/instantiated-r01-r45-v1/claims-register-r01-r45-v4.csv` current R05 row and hash.
- `ai/evidence/claims-register/r05-split-counts-reproduction-20261021.md` and its JSON, both in this research worktree.
- `ai/tools/reproduce_lead_split_counts.py` in this research worktree; compare its bytes to `D:/code/chefkix/ai/tools/reproduce_lead_split_counts.py` only if necessary.
- Root-checkout source pins and actual tool stdout summarized in the evidence report. Do not inspect CSV cell values or labels; hashes and report metadata suffice.

## Write scope

Write exactly one review report: `ai/campaigns/iron-chef-outcome-push-20261021/tasks/T06-r05-reproduction-review.md`. No other paths. Do not apply evidence or change status. The root lead owns any subsequent application after reviewing your report.

## Acceptance criteria

1. Confirm whether the three input digests and frozen manifest digest are internally consistent with the reproduced JSON.
2. Confirm tool behavior is a true fixed-file row count and checks all source hashes before counting; identify any important weakness.
3. Compare the literal R05 claim text to the result and state its evidence ceiling.
4. Confirm no R04, performance, generalization, paper-equivalence, rights, or broader backlog acceptance follows.
5. Report verdict, source hashes inspected, limitations, and any counterevidence. Do not grant confidence from the lead's assertions alone.

---

# Independent review result — 2026-10-21

**Verdict: PASS, narrowly, for the literal R05 fixed-file split-count claim.** This is independent local reproduction evidence for the three hash-pinned files—not an application of evidence or a register status change. R05 may be considered eligible for lead review as `VERIFIED` only for its literal claim, after the lead decides whether to apply the evidence to a new register derivative. Current v4 R05 remains `UNVERIFIED`.

## Binding and reproduction checks

I checked `academic_vision_and_strategy.md` §15.1 at L579: “49,044/10,729/10,747 split counts,” with the limit “Local/source lead; actual files required.” The reported count tuple directly matches that wording.

The reproduction JSON SHA-256 is `e85a635a762095201eb106237098b159249b0ef7ee16160ad4b3b8034a77875e`; its manifest pin is `134e92d635ab00120b94becf1cac86fc63e067e571d720b07d63d24c9d5b3eea`. The root manifest’s declared split digests exactly match the output JSON and the actual local file-byte hashes. Re-executing the read-only `build_report` function against the root checkout emitted the same manifest and tuple:

- train: 49,044; SHA-256 `65d143c3bacd21555e5f16abdddd961585fce7dcbfdabfcbad0c5ccf8d60557c`
- validation: 10,729; SHA-256 `002588cb1d7db2d1644cb16079542df641c0ace4807490be380e150973796a0b`
- test: 10,747; SHA-256 `7d239b405ef71a4dea3513afe2a22b11b9e0e15256428b40896a31cae0ddd2a6`

The reproduction tool SHA-256 is `23a92e0bb6b2d7453e5b2f208690bf61da2fc13c788183463d65c10fcccba3d5` in both this research worktree and the root checkout. Source inspection confirms it resolves inputs under the selected repository root, rejects path escapes and symlinked splits, requires exactly the three split keys, hashes each entire file and compares the digest to the manifest before parsing/counting CSV data records via `csv.DictReader`, then checks the expected count. I did not inspect or report cell values or labels.

## Evidence ceiling and counterevidence

The reproduction is against the exact local bytes pinned by the v26 manifest, not a claim that the manifest or data are canonical upstream releases. The tool does not itself pin the manifest to an expected SHA before reading it; it reports the manifest digest and enforces the manifest’s per-split hashes. Here, I independently checked the manifest digest against the reported pin and rehashed the actual local split files, which closes that specific gap for this review. The tool’s data-row count treats each parsed CSV record as a row; it does not establish semantic validity, protocol equivalence, or downstream suitability.

No conclusion follows for R04 identity reconstruction; GISMo/paper equivalence; model or benchmark performance; generalization; rights; or broader M-B/M-G/L48 acceptance. The current v4 R05 row is still `UNVERIFIED`, and historical statuses are not inherited. Confidence ceiling: strong for the narrow local byte/count reproduction (E2), none for the excluded claims. Reversal evidence would be a newly identified mismatch in the pinned manifest/source bytes or an independently established defect in the CSV record-count contract.
