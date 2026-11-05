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
  tools/research/c2/ai/tools/test_preflight_c2_with_token_receipts.py
```

The expected run in the authoring checkout is 143 passed and 21 subtests
passed. Tests use synthetic fixtures and local files; they do not load private
case data or make network/provider calls.

## Explicitly not included / still missing for M-I-2

The shared checkout did not contain the following modules, and this subset does
not fabricate replacements or claim the gated path is operational:

- `c2_attempt_journal_v2.py`;
- `bridge_c2_offline_package_to_attempt_journal.py`;
- `validate_c2_openai_response.py`;
- the saved Batch-result-to-journal bridge and remote-state reconciliation
  integration needed to preserve the frozen one-attempt/recovery contract.

The transport/ingestion integration tests therefore cannot be collected from
this subset. M-I-2 remains partial; M-I/C2 remains blocked on the existing
protocol, permission, account/capacity, cost/reserve, exposure, release, and
attempt gates. The frozen denominator remains 10,747 = 10,499 READY + 248
preparation errors. Provider payload remains exactly the approved three
fields. No provider call, upload, paid work, private data, credentials, or
empirical C2 output is included or implied.

`SOURCE.json` records the copied source and bundled artifact hashes. Historical
source paths identify provenance; the portable fallback changes are called out
separately and do not modify Protocol v1 or its scientific decisions.
