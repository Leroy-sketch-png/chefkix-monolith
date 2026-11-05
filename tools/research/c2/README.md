# C2 offline-preparation subset

This is a portable, source-only subset of the existing C2 preparation tools,
published here so collaborators can inspect and run the same offline checks.
It is **not** the complete M-I-2 request/transport/journal/ingestion toolchain,
and it is not a release or dispatch authorization.

## What is included

- T208 frozen three-field request serializer and fail-closed local package checks.
- Local cost forecast and input-token receipt preparation/validation.
- A JSONL Batch transport *candidate builder*. It writes files only; it does
  not upload, contact an API, or authorize execution.
- The existing T316 default-off, phase-oriented transport driver and its
  synthetic tests, plus the existing T314 saved-result-to-journal adapter and
  tests. The driver release pin remains unset.
- Attempt-resume utilities and synthetic regression tests for the included
  preparation components.
- The frozen Protocol v1 bytes used by the included pin test.

The portable package carries the exact frozen serializer source at
`_frozen/preflight_c2_openai_cost.py` (SHA-256
`4c5694f378d5b3ae6b1bda6e0733a25926dffaaa235e6ff2292f1b3d9385d829`). The
request-package builder prefers the pinned upstream Git object and uses this
snapshot only when that object is absent; both paths require the same digest.
The cost forecast follows the same Git-first, hash-verified fallback rule.
Protocol v1 is preserved byte-for-byte at
`research/protocols/IRON-CHEF-PROTOCOL-v1.md` (SHA-256
`b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`).

## Versioned saved-result replacement (offline only)

`ai/tools/c2_saved_batch_reconcile_v2.py` is a newly authored, explicitly
versioned offline replacement for the missing historical T222/T249/T246
integration path. It does **not** claim those original source pins, alter the
protocol, send requests, authorize dispatch, or grant empirical C2 credit.
It validates already-saved package/result artifacts against the frozen
10,747-case denominator, keeps all 248 preparation errors visible, converts
each of the 10,499 saved outcomes into a sanitized terminal journal
disposition, and binds resume to one exact saved bundle. A durable `STARTED`
receipt permits only that same bundle to resume; a changed bundle or uncertain
journal write stops without retry. The caller must pass every saved input path
so the ingestion receipt cannot alias and overwrite evidence.

The replacement depends only on the existing pinned T219 journal helper and
T253 package validator and is callable as an explicit offline phase of the
existing `senath_c2_batch_transport.py` workflow. Select
`--phase offline-saved-results-v2`; without `--apply-saved-results` it only
validates, and with that explicit flag it appends to the existing local
journal. This phase bypasses only the missing T222/T249/T246 historical
contracts; every original phase still follows the original pin-loading path
and fails closed when those exact sources are unavailable. It never sets the
remote release pin or constructs an SDK client.

The bundle receipt is automatically created beside the journal using the
validated bundle digest in its filename. It is exclusive-create and can be
resumed only for the same saved files. The focused synthetic suite exercises
the existing driver's opt-in phase end-to-end:

```powershell
$env:PYTHONDONTWRITEBYTECODE = '1'
python -m pytest -q -p no:cacheprovider `
  tools/research/c2/ai/tools/test_c2_saved_batch_reconcile_v2.py
```

These tests establish local contract behavior only. The new module is wired
only to the explicit offline phase, not remote phases; it does not replace the
absent historical modules or establish account, rights, cost, exposure,
release, real provider output, or empirical acceptance gates. The three historical missing
hashes remain listed unchanged in `SOURCE.json`.

During the next offline-ledger integration, a stale Protocol v1 digest in the
v2 source constant was found: its prior synthetic fixtures agreed with the
stale value but not the frozen file/T253 contract. The constant and source
manifest are corrected to the frozen SHA-256 above, and a regression now
compares the reconciler, driver and T253 protocol pins. Protocol bytes and
scientific decisions are unchanged.

## Versioned offline attempt-ledger preparation (local only)

`ai/tools/c2_offline_attempt_ledger_v1.py` adds an explicit
`offline-attempt-ledger-v1` preparation/validation phase to the existing T316
driver. It binds T253's verified local Batch candidate package to T208's
complete disposition order and T219's pinned case schema, producing a
10,747-row case inventory and an **empty** attempt journal. The initial states
are 10,499 `NOT_STARTED` and 248 retained `PREPARATION_ERROR` cases. It does
not create intent/submission events, select a cohort, contact a provider,
dispatch, or change any historical T222/T249/T246 source pin. This is a new
versioned preparation contract, not recovered historical code and not
Protocol v1 execution readiness or empirical C2 acceptance.

Prepare and then validate into a new local directory separate from all input
directories (replace the example paths with the operator's actual paths):

```powershell
python tools/research/c2/ai/tools/senath_c2_batch_transport.py `
  --root . --package-dir <validated-T253-package> `
  --phase offline-attempt-ledger-v1 --ledger-action prepare `
  --ledger-dir <new-local-ledger-directory>
python tools/research/c2/ai/tools/senath_c2_batch_transport.py `
  --root . --package-dir <same-validated-T253-package> `
  --phase offline-attempt-ledger-v1 --ledger-action validate `
  --ledger-dir <same-local-ledger-directory>
```

The validator re-derives expected outputs from pinned inputs, verifies the
ledger's exact file inventory and hashes, and refuses symlink/overlapping
input-output paths or changed bytes. Windows CRLF checkouts are hashed and
loaded as normalized LF source, matching the pinned Git blob without modifying
the checkout. These tests establish local synthetic behavior only.

## Run the included offline tests

From the repository root, with Python 3.10+ and pytest installed:

```powershell
$env:PYTHONDONTWRITEBYTECODE = '1'
python -m pytest -q -p no:cacheprovider `
  tools/research/c2/ai/tools/test_build_senath_c2_offline_request_package.py `
  tools/research/c2/ai/tools/test_preflight_c2_openai_cost.py `
  tools/research/c2/ai/tools/test_estimate_c2_headline_cost.py `
  tools/research/c2/ai/tools/test_build_senath_c2_batch_transport_candidate.py `
  tools/research/c2/ai/tools/test_c2_attempt_resume.py `
  tools/research/c2/ai/tools/test_build_c2_input_token_count_package.py `
  tools/research/c2/ai/tools/test_validate_c2_input_token_receipts.py `
  tools/research/c2/ai/tools/test_preflight_c2_with_token_receipts.py `
  tools/research/c2/ai/tools/test_c2_saved_batch_reconcile_v2.py `
  tools/research/c2/ai/tools/test_c2_offline_attempt_ledger_v1.py
```

The expected run in the authoring checkout including the versioned replacement
is 161 passed and 21 subtests
passed. Tests use synthetic fixtures and local files; they do not load private
case data or make network/provider calls.

## Exact pinned integration gap for M-I-2

The existing T314/T316 source is now shared with its original pins. Its pinned
dependency chain still cannot run in this checkout because three exact source
files are unavailable; the tracked-history scan found no matching source blob.
Do not substitute newly authored lookalikes or change these digests:

- T222 package/journal bridge `ai/tools/bridge_c2_offline_package_to_attempt_journal.py`, SHA-256 `993f4718b138ebcfcf8a66d0a66b4a509d6b0bbd8a3af6d4b3ea3e20e7d58ac4`.
- T249 v2 journal `ai/tools/c2_attempt_journal_v2.py`, SHA-256 `5f894e1b2d50120781ef7aaad9bcfeb0fa70e976787586bca00367f83686b2ae`.
- T246 response validator `ai/tools/validate_c2_openai_response.py`, SHA-256 `342dfc1f547bebb539267648c6b7dca6fd2f749d1f4e18e0b57e089d73f34656`.

Observed historical integration boundary: the original pin-dependent T316
suite has **43 setup failures** because its source-pinned T222 module is
absent; test bodies do not run. The new explicit offline-v2 phase has its own
synthetic end-to-end coverage, but does not repair that historical suite.
T314 test collection stops before any tests because the T249 journal file is
absent; the adapter also requires the pinned T246 parser. Restore those exact
historical files (or a verified Git bundle containing them) before running the
original integration suites. M-I-2 remains partial; M-I/C2 remains
blocked on the source pins and the independent Protocol v1 permission,
account/capacity, cost/reserve, exposure, release, and attempt gates. The
frozen denominator remains 10,747 = 10,499 READY + 248 preparation errors.
Provider payload remains exactly the approved three fields. No provider call,
upload, paid work, private data, credentials, or empirical C2 output is
included or implied.

`SOURCE.json` records the copied source and bundled artifact hashes. Historical
source paths identify provenance; the portable fallback changes are called out
separately and do not modify Protocol v1 or its scientific decisions.
