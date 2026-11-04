# Kaggle existing-job and output audit

**Observed:** 2026-11-04. **Owner:** Member. **Scope:** read-only inspection/retrieval of already-existing Kaggle resources after the user explicitly authorized Kaggle-related operations. Kaggle CLI 2.2.5 was authenticated as `tnvtrung`; no credential value was read or recorded. No kernel was submitted/re-run and no dataset was uploaded or updated in this audit.

## Existing Recipe1MSubs kernels

Kaggle reports both `tnvtrung/recipe1msubs-train-only-ranker-t4` and `tnvtrung/recipe1msubs-ranker-t4-smoke-test` as `KernelWorkerStatus.COMPLETE`. Account search finds only those two Recipe1MSubs kernels; no owned GISMo preprocessing kernel or Recipe1M dataset was found.

The current private dataset `tnvtrung/chkix` is Kaggle dataset ID `12858487`, reports `isPrivate: true` and license `unknown`, and contains exactly these current-version files:

| Remote file | Bytes | Created on Kaggle |
| --- | ---: | --- |
| `train_comments_subs.pkl` | 3,914,545 | 2026-10-26 03:44 UTC |
| `val_comments_subs.pkl` | 910,636 | 2026-10-26 03:44 UTC |
| `test_comments_subs.pkl` | 919,937 | 2026-10-26 03:44 UTC |

The train file was downloaded from that private dataset to local-only `C:\Users\LENOVO\ResearchData\kaggle-source-identity-audit-20261104-f573f7415aaa4c5a9a7bb8bc47c4145d\train_comments_subs.pkl`. Its SHA-256 `b4d58e6c6c6747671e531812393397acf75c02d2a9ce00c2edcc7794bf567c81` matches the existing local train file exactly. No val/test file was opened in this identity check.

Kaggle source was pulled to local-only `C:\Users\LENOVO\ResearchData\kaggle-kernel-source-audit-20261104-6097f53cb0cb4d4d8b1f1600e07967e6`. The downloaded script's raw SHA-256 is `286091b86953ecd12c1fca14b4b9ec44205d59b1ea8df81dfe506f742d1c4638`; its LF-normalized SHA-256 is `fa8c0b4292225586b90d6a64015eca58367ebbe4afe4cbe4db64bb586059a0db`, matching the local kernel source after line-ending normalization. The script resolves/opens `train_comments_subs.pkl` only and contains no val/test file references. The dataset nevertheless exposed all three split files to the Kaggle workspace; keep test status `EXPOSED` and do not treat exposure as permission for confirmatory use.

## Remote output reconciliation

The latest train kernel output was fetched read-only to `C:\Users\LENOVO\ResearchData\kaggle-remote-output-audit-20261104-060c41ddb28b4e73a2da54d590495250`. The remote v5 manifest SHA-256 `6a7a1fd9d8ac87a35ad756e725f57b4ced3cf41936cda828d6752887fb43036f`, `ranker.pt` SHA-256 `cd002cf84b282a7d7c5c5210a0354547f53b7f28fe96d0442d29e10f8cd0dd09`, and `training_loss.svg` SHA-256 `043fd36515a68b7f39e89a140079e747b02fbc571df9090f5ba8f3604172b981` all match the existing local v5 files byte-for-byte. The manifest binds the 49,044-row train pickle SHA above and states `validation_or_test_files_opened: false`.

The remote synthetic smoke kernel's model SHA-256 `c90b9f7269f86ac1d9f8f9a5690d7beb8ef5549f6f87f368f9db64ca743c77a3` matches local. Its remote manifest (`e20a9195a12e3bba3cd8f598b1d5f2ce7b97c4c5bd730e085f17d46cbecee20e`) differs from the local smoke manifest (`85d192ce3814ae9d89ce10e52ba6862694e8a62262e7fe36771d1847502bc84a`): remote says `source_dataset: synthetic_fixture`, while the local copy says `Recipe1MSubs`; the local source fields still say `synthetic_fixture_only`, 0 bytes, and six rows. Treat this smoke as synthetic only; do not use its local label as evidence of real-data training.

## Backlog meaning and remaining gate

This verifies that historical Kaggle training completed and that the remote train-only output matches its local copy. It is development/training evidence only. It is not the C2 paid Batch output, a C3 export, M-D matched-arm output, a test claim, or Lead acceptance. The remote dataset contains only the three Recipe1MSubs pickle files; it does not contain the missing GISMo preprocessed split caches or the 971-profile FooDB reference.

The private Kaggle dataset license field remains `unknown`. The user later provided a separate authorization for Recipe1M-related processing/transfer on private Kaggle only; that authorization is recorded in [`member-user-recipe1m-private-kaggle-authorization-20261104.md`](member-user-recipe1m-private-kaggle-authorization-20261104.md). It covers this operation scope as user/Human authorization, without changing third-party license metadata or authorizing an external API, public release, redistribution, or product use. No new upload was made as part of the audit.

After that authorization, read-only metadata inspection independently confirmed `isPrivate: true` for `tnvtrung/chkix` (dataset ID `12858487`). Public Kaggle searches for `preprocessed_flavorgraph_substitutions_fixed_3` and “GISMo Recipe1M cache” found no dataset; the exact-title kernel search returned no result. This confirms that the missing historical GISMo cache was not available through those public Kaggle searches. The local replay already reproduces the documented 1,044/230/220 positional length gaps, so uploading the same metadata to repeat that replay would not resolve M-B; no redundant upload or job was started.
