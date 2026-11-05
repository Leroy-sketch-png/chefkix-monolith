# T132 — Independent review of the proposed R42 `PARTIAL`

**Disposition: PASS, narrowly, for the proposed future v10 `UNVERIFIED` → v11 `PARTIAL` transition.** This is an independent review of the candidate under the exact T58 policy bytes and the exact cited local article PDF. It is not lead acceptance, a register edit, an experiment reproduction, or a model-quality finding. T131's earlier blocked attempt is preserved unchanged; it stopped before reading the changed T58 source or PDF and is not evidence for this disposition.

## Pin verification performed before claim assessment

| Input | SHA-256 | Verification/use |
|---|---|---|
| T129 candidate `tasks/T129-t58-rows-32-45-candidate.md` | `9f73ad5d494eeede9e29ba54b442f3db7ad51d2a87ef3e6a8d024aa1092b1761` | Exact match; reviewed proposed R42 boundary |
| Restored T58 policy `tasks/T58-register-v11-reconciliation.md` | `93ec5eff82ba9c86bc1f746d73db29bdf32adde3ad9301273493ea28f3b60823` | Exact match to candidate's cited acceptance source |
| Publisher PDF `D:/code/chefkix/ai/out/iron-chef-lead-v205/senath-2025-nlpj-100177.pdf` | `cc0506debfaee0832d17c7e28b760ecc91a2887d4915b2bce0c19062af410976` | Exact match; read locally after all required hashes matched |
| Current register v10 `ai/evidence/claims-register/instantiated-r01-r45-v1/claims-register-r01-r45-v10.csv` | `34e3a76c0d5dafdb8c4a6510d520f9228b303d43ac744b767b8ac747cd70dea4` | Exact current row/context checked read-only |
| Historical reviewed register v185 `D:/code/chefkix/ai/out/iron-chef-lead-v232/claims_register_R01_R45_reviewed_v185.csv` | `8eac99ef54e0752b6cf2ed6e7938553a47e4d3aa18e4c7c8cd94cd0373a53a62` | Exact historical row/context checked read-only |
| T131 blocked report `tasks/T131-independent-r42-partial-status-review-report.md` | `b9253ff176ce87bfd141494279c90dc065c994035289f9d7c7d6ca9256568079` | Hash recorded to preserve attempt lineage; context only, no claim assessment borrowed |
| Current wording/provenance snapshot | `2adebe9b039e26eaa9609b60fde5c40b5c9f2bfb1f3ec1e7ffff0d077925be6a` | v10's R42 wording-source pointer; not treated as empirical support |
| Current academic strategy | `89117fccc411c29357b4872e1f0154e819ff04621a589a734587e715ae506ee4` | S02 attribution context; not treated as experimental evidence |

The three exact frozen evidence pins required by T132 (T129, restored T58, publisher PDF), plus the current/historical register and review-context pins listed above, were independently recomputed from the named bytes before examining article content. The historical register shows R42 `PARTIAL / NA`; v10 is `UNVERIFIED / NA`. The claim text is unchanged: “Senath journal abstract22.04% and weaker Hit@3/10.” The v10 pointer is only a wording/provenance snapshot, so by itself it cannot support a status change. The direct PDF is the distinct primary article record named by v185 and T129.

## Independent article check and claim fit

I inspected only the local, hash-matched nine-page PDF, using local text extraction; no source lookup, network call, test-label/data access, model run, or external service was used.

- The article is titled *Large language models for ingredient substitution in food recipes using supervised fine-tuning and direct preference optimization*; its first page identifies DOI `10.1016/j.nlp.2025.100177` and the Natural Language Processing Journal 12 (2025), article 100177. The abstract on PDF page 1 and its continuation on page 2 state that the authors' best SFT+DPO system reports Hit@1 of 22.04 and that its Hit@3/Hit@10 results fall below GISMO.
- Table 9 on PDF page 8 directly gives GISMO `20.56 / 35.89 / 54.27 / 31.51` and “Ours” `22.04 / 33.66 / 40.96 / 28.83` for Hit@1, Hit@3, Hit@10, and MRR, respectively. Thus the cited numbers and direction of the Hit@3/10 comparison fit the narrow, explicitly attributed claim.
- The methods/results text on PDF page 6 says prompt selection used a subset of 500 recipes from the test set and context selection used a test sample of 1,000. The dataset section on PDF page 4 reports 49,044/10,729/10,747 train/validation/test substitution samples and says this split is the same as GISMO. The article does not resolve whether the selection subsets overlap the final full-test evaluation or how any reuse was handled. This is material test-informed-selection caveat, not proof of a specific amount of leakage.
- The paper describes the result as its own report; this review did not reconstruct its execution, inspect source data/code, validate metric implementation, independently confirm the PDF's repository provenance, or test any model. I do not read the article's results as evidence about the project's Senath model or as a clean confirmatory held-out estimate.

## Status judgment under T58

T58 requires a new derivative to preserve the conservative v10 boundary unless row-level evidence is directly reviewed, requires claim fit and explicit evidence ceiling/limitations, and rejects automatic inheritance from v185. This independent review finds that the *attributed publication-report* portion is directly evidenced by the matching full-text article and Table 9. Because R42 is phrased as a journal-abstract report rather than as a claim that this project reproduced or independently validated the score, `PARTIAL` is supportable as a bounded record of what the authors reported. It is not supportable as `VERIFIED` evidence of performance, an uncontaminated test result, reproducibility, generalization, novelty, safety, or usefulness.

### Strongest alternative and disconfirming evidence

The strongest alternative is to keep `UNVERIFIED` because the article's test-informed prompt/context selection makes the reported comparison unsuitable as independent held-out evidence, and a `PARTIAL` label could be misread as validating the score rather than documenting attribution. The counterpoint is that the exact claim itself names the journal abstract, and the pinned article directly contains the 22.04 value and weaker Hit@3/10 comparison. Retaining `PARTIAL` is justified only if the v11 row's evidence notes make that attribution and evidence ceiling unmistakable and preserve the test-informed selection caveat. Do not remove or weaken the caveat while applying the status.

### Falsifier / reversal condition

Revert this candidate disposition if a source-identity check shows the pinned PDF is not the cited article/version, if the relevant abstract/Table 9 values do not correspond to the claimed SFT+DPO-vs-GISMO comparison, or if the claim/register's `PARTIAL` semantics require independently validated experimental results rather than an explicitly attributed publication report. Conversely, resolving the subset overlap alone would not establish independent reproduction; that would require source/code/data and a separately valid evaluation.

## Review conclusion and boundary

**PASS narrowly:** T129's proposed R42 `PARTIAL` is supportable for the attributed published-report fact, with test-informed selection and non-reproduction limitations preserved. v10 remains unchanged; T132 does not apply the status. Lead acceptance and a new versioned register are still required. No finding follows for novelty, our model, exact reproducibility, clean held-out performance, safety, calibration, or any other claim row/C1–C5 floor.

Only this T132 report was written. T131's blocked-attempt record, both registers, T58, strategy, mission and all other files were left unchanged. No test labels, row data, models/weights, external sources, tests, paid resources, or network services were accessed.
