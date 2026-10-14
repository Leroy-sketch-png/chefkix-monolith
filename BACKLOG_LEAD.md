# IRON CHEF v4: LEAD BACKLOG
> **AUTHORITY:** `academic_vision_and_strategy.md` (canonical SSoT) is the single source of truth. This backlog is derived from §3 (C1–C5), §17 (Work Packages), §18 (Schedule), §19 (Risks), and §16 (Protocol Resolutions). When they conflict, the strategy file wins.
> **MIGRATION:** Updated from v3 on 2026-10-12 (LEAD v113). v3 "four trained models" and "nine contributions" structure retired. See Appendix Y of SSoT for superseded v3 text.

## LEAD v208 — Identify changed local app listeners for M-A (2026-10-14)

- **Fresh read-only host evidence (2026-10-14 07:54 UTC):** port 3000's listener (PID 6036) is Node running Next's `start-server.js` from `D:\code\chefkix\chefkix-fe`; its `/` route returns HTTP 301. Port 8080's listener (PID 22960) is Java; its parent process chain includes the local Maven wrapper and `D:\code\chefkix\chefkix-monolith`. `GET /actuator/health` returns Tomcat HTTP 404 (`text/html`). Port 8000 has no TCP listener in `netstat`; `/health` gives no HTTP response. Only process identity/path and status/content-type were recorded; no response bodies, environment, credentials, or logs were read.
- **Route/source cross-check:** static source maps monolith `KnowledgeGraphController` to `GET /knowledge/ingredients/{name}` under the `/api/v1` context and `KnowledgeGraphService` to Mongo repositories. The read-only `GET /api/v1/knowledge/ingredients/salt` reached that controller (HTTP 200; application envelope `success=false`, `statusCode=404`, `data=null`), i.e. the queried ingredient was absent. This confirms an active monolith knowledge route, not a loaded thesis HGAT/substitution graph. AI-service source separately exposes `GET /health`, but port 8000 had no listener. The default-vs-persisted model-path discrepancy is detailed in v209.
- **M-A disposition:** the changed 3000/8080 listeners are now tied to the local frontend and monolith process chains, respectively, rather than generic `node`/`java` labels. The monolith knowledge route is distinct from the AI-service HGAT graph; no graph count or producer→artifact→loaded-process lineage has been established. No AI-service HGAT route was queried, no service was started, and no response body or credential was exposed.
- **Next:** when the AI service is already available under an authorized runtime, check its exact `/health` and model/graph readiness route, then trace registry → weight/subgraph files → loaded process. Keep the AI-service graph unavailable at this observation and do not promote artifact counts to runtime truth.

## LEAD v209 — Trace static graph-loader deployment path for M-A (2026-10-14)

- **Trigger finding:** `src/ml_loader.py` documents `hgat_weights.pt` as the graph-rag state dict, then loads the registry-selected path as a state dict and derives sibling `substitution_subgraph.pt`. The checked-in default in `src/ml_registry.py` instead points `graph-rag` to `local:models/food_graph.pt`; persisted `models/registry.json` points to `hgat_weights.pt`. These configurations differ.
- **Blast-radius inspection:** the AI-service Dockerfile copies only `src/` and `data/`, not `models/`; Compose builds that image and mounts only the feedback-data volume, not the model directory. Thus a fresh container does not receive the checked-in registry/artifacts through the declared build/mount path and `_load()` falls back to code defaults. The default HGAT entry is also `health_status=unknown` with no health-check timestamp, while `get_serving_model()` fails closed on that readiness condition (and missing local artifacts); this prevents it from being selected rather than proving a load failure. The workspace installer places `hgat_weights.pt` and `substitution_subgraph.pt` in the host model directory and updates the host registry when present, but does not alter the Docker build/mount contract. The host model inventory is metadata-only; no weight/subgraph bytes were read or copied.
- **Systemic sweep:** a workspace-wide reference search found no checked-in call site for `ml_loader.load_hgat_model()`; `load_graph()` is called only from that loader method. Copilot imports the singleton but currently invokes `load_vlm()`. The separate monolith `/api/v1/knowledge/...` route is Mongo-backed and cannot stand in for HGAT serving. This is a static deployment/integration gap and latent path mismatch, not a verified runtime exception: the AI service is not running on this host, and the loader is not invoked by the inspected route path.
- **M-A disposition / acceptance:** current evidence establishes an image/registry/artifact packaging inconsistency and no production HGAT call path in the inspected source, not an active loaded graph or model result. Product-owner decision is needed on whether HGAT serving remains in the active architecture or is dormant; then align registry default, artifact manifest/placement, image or mount contract, and an explicit serving call path. Only after that approved repair, run a fresh isolated container and verify `/health`, readiness, exact loaded artifact hashes, and an inference request; do not promote local files or old scores to runtime evidence. M-A remains open; no code or model artifact was changed.

## LEAD v206 — Flag paper-to-filter-code mismatch for L42 (2026-10-14)

- **Historical filter-code snapshot found:** Bitbucket commit `a348acbce72e5b70d196a99a9491643f8b3ffaf4` added `substitution_filtering.ipynb` (retrieval body 5,136 bytes; SHA-256 `7b9ed1a7ef6197459791b4df2b2eadba80d45e15f59813cf046d26175f413a6a`). Static inspection only: its one code cell selects `data[20000:30000]` (10,000 rows) from `gpt3.5_fine_tune_train_val.jsonl`; asks `gpt-3.5-turbo` for Yes/Maybe/No; sets temperature `0`, `max_tokens=5`, batches 100, and writes `substitution_results_3.json`. Source: [pinned filter notebook](https://bitbucket.org/iAnalytica/ingredient-substitution/src/a348acbce72e5b70d196a99a9491643f8b3ffaf4/ingredient_substitution/generativeAI/filtering_comments_data/openai/gpt-3.5turbo/substitution_filtering.ipynb); [commit](https://bitbucket.org/iAnalytica/ingredient-substitution/commits/a348acbce72e5b70d196a99a9491643f8b3ffaf4).
- **History cross-check:** Bitbucket's 27-commit API history dates `a348acb...` to 2024-02-02 and `d60c1c...` to 2024-02-09. The same 5,136-byte filter notebook/hash is still present at `d60c1c...`; that commit adds the 70,520-line input, merged result/ID JSON, and `substitution_results_3.json` through `_7.json`. The earlier `a348...` tree contains `_1.json` and `_2.json` (listed sizes about 10.7 MB and 10.9 MB); its README describes a generic `substitution_results.json` output, not the notebook's `_3.json` filename. The later `f295174...` “Models and data added” commit (2024-11-07) is the current `master` tip and its tree no longer lists the filter notebook/path. This chronology shows historical filter-code plus filtered artifacts, but does not map the five paper-reported runs to the seven numbered result files or establish that the February notebook produced the November paper's reported experiment.
- **Difference from paper protocol:** Rita et al. describe assessing all 70,520 substitutions, five filtering runs, temperature 0.5, and 10 output tokens; the inspected notebook code instead shows a 10k slice at temperature 0 / 5 tokens. That is a date/version/method correspondence gap—not evidence the authors used the older notebook for the published table, that no later/private script existed, or that results are invalid. The full 2024-11-12 access-date tree has not been reconstructed from a historical ref.
- **L42 decision effect:** raise artifact-to-table mapping and configuration provenance to a concrete challenge. Keep the paper's Hit@1 values attributed and unreproduced; do not substitute the 10k notebook's behavior for the described 70,520-item method. No local dataset or outputs were downloaded, no code/API was executed, and no reproduction or protocol ruling occurred. Exact novelty remains `UNKNOWN`.
- **Next / acceptance:** using metadata and paper text only, map the seven numbered results and merged artifacts to Appendix C/D and the paper's five-run protocol; check whether a later filter-code/config revision exists between the February notebook and November repository tip. Do not retrieve result bodies or datasets for this mapping. If public metadata cannot resolve the mapping, record the precise author question and proceed with independent work; do not let this provenance gap idle unrelated backlog lanes. Continue source-specific rights review; no model/data reuse.

## LEAD v207 — Reconcile stale Rita-method wording in canonical SSoT (2026-10-14)

- **Internal-doc contradiction:** `academic_vision_and_strategy.md` §4 table says Rita et al.'s “refinement definition is unread,” while its bibliography maps [C:S03] to the same paper and the current dated SCOPE L42 entry records a full 15-page arXiv PDF screen (SHA-256 `9dbe664a6a9d9b131cd4587751397a92d1acc06dee2974e995d74e50a6c65601`) describing the Recipe1MSubs split, Correct/Potential/Incorrect filtering, and original vs filtered results. LEAD v202 also records those paper-described methods. The claim that the paper's refinement method is unread is therefore stale; this is a documentation consistency finding, not new experimental evidence.
- **Required correction boundary:** in the next authorized canonical-strategy edit, change the §4 row to say that the paper's high-level filter and evaluation populations have been screened, while exact mapping from the February Bitbucket notebook/configuration and numbered/merged artifacts to the paper's five-run tables remains unresolved (see LEAD v206). Keep all scores author-reported/unreproduced, preserve the original 10,747 vs filtered 6,721 test-population distinction, and do not label the code discrepancy a result-validity defect. Update [C:S03] provenance only with the already recorded source/method evidence and its hash; do not invent a new code-release citation or assert the data were used with permission.
- **Authority/acceptance:** this backlog note does not edit or supersede the canonical file. On the next canonical SSoT maintenance pass, harmonize §4, Appendix C:S03, and the dated L42 evidence without erasing v4.2 provenance. Until then, the SSoT row remains the authority and its stale wording is a flagged correction request; no novelty claim or score/status changes.

## LEAD v205 — Map the pinned LLM-substitution repo without executing it (2026-10-14)

- **Version-pinned static inventory:** inspected only names/metadata and selected text at Bitbucket commit `f29517446dff50a5b1fbfba685c1e781d7f8c50b` (no clone, execution, data/model download, or reuse). The tree lists original/filtered train, validation, and test JSONL plus five-run prediction files for the GPT-3.5 and DaVinci arms; TinyLlama checkpoint/prediction/data paths; and category/name-clustering notebooks. Fingerprints of the inspected public text artifacts: root README `08df6a3849fb33abe93e1521b389c77e4035f794eff6bbc06adfddc442651041`; GPT-3.5 fine-tuning/inference notebook `b6906afaf374d5678fed894a9331d925d4cd98be165a14a8d5a75fe610171a60`; DaVinci fine-tuning/inference notebook `4d383413c8beaacaae8571f5548bbc8b7014fdfd094fa72eddb4fe8fb647e5b2`; ingredient-name analysis notebook `a2b789140577c53b76423acf8ad339477b5ea5a513247bd7681ddb2ff58a8dc6`; category notebook `397bc39c0953af028b1c4d8910b4604be096cee2cd1bf198b3f0ae9d8f3ca506`. These are retrieval hashes, not locally archived source bytes. Pinned sources: [repository commit](https://bitbucket.org/iAnalytica/ingredient-substitution/commits/f29517446dff50a5b1fbfba685c1e781d7f8c50b), [GPT-3.5 notebook](https://bitbucket.org/iAnalytica/ingredient-substitution/src/f29517446dff50a5b1fbfba685c1e781d7f8c50b/GPT-3.5%20Fine-Tuning/fine-tuning_inference.ipynb), [DaVinci notebook](https://bitbucket.org/iAnalytica/ingredient-substitution/src/f29517446dff50a5b1fbfba685c1e781d7f8c50b/DaVinci%20Fine-Tuning/fine-tuning_inference.ipynb).
- **Observed workflow boundary:** the visible GPT-3.5 notebook uploads train and validation JSONL for fine-tuning and separately calls the model on `test_data.jsonl`; this matches a training/inference outline, not an independently verified reproduction or the paper's complete filtering workflow. The current tree inventory has no separately named substitution-filter generation script; the notebook text screened here does not establish where/how that filter was produced. The data/results are present in the tree, but their exact mapping to every paper table, the preprint-vs-journal repository rename, and historical accessed revision remain unresolved. No metric is adopted and no artifact is run.
- **Next / acceptance:** inspect only the paper-linked notebooks/readmes and commit history needed to bind the filter, split construction, predictions and paper tables; record a file-by-file mapping with hashes if permitted. Keep raw dataset/prediction analysis and reuse behind source-specific rights review, and do not run paid API calls or third-party code. L42 and exact novelty remain open/`UNKNOWN`.

## LEAD v204 — Pin the LLM substitution paper's current artifact trail (2026-10-14)

- **Repository identity discrepancy:** the versioned arXiv v1 PDF names `iAnalytica/recipe-generator`; the journal version's data-availability statement names `iAnalytica/ingredient-substitution` and says it was accessed 2024-11-12. Bitbucket's public API currently returns 404 for `recipe-generator`. It identifies the public `ingredient-substitution` repository's `master` at `f29517446dff50a5b1fbfba685c1e781d7f8c50b`, commit date 2024-11-07, message “Models and data added.” The current tree inventory contains model-arm folders, train/validation/test JSONL files, prediction outputs, notebooks, category data, and a README. Sources: [journal article](https://www.mdpi.com/2504-4990/6/4/131), [published repository](https://bitbucket.org/iAnalytica/ingredient-substitution), [observed commit](https://bitbucket.org/iAnalytica/ingredient-substitution/commits/f29517446dff50a5b1fbfba685c1e781d7f8c50b), [arXiv v1 PDF](https://arxiv.org/pdf/2409.08792v1).
- **What this does and does not establish:** a paper-linked artifact exists now, and the currently visible commit predates the paper's stated access date by five days. That temporal proximity is consistent with the statement, but does not prove it was the exact accessed tree, that these files produced every reported table, or that the preprint's differently named repository was the same repository before a rename. A present-day 404 is not evidence of historical nonexistence. No repository was cloned, executed, or redistributed; no dataset, output, model weight, or code bytes were inspected or locally archived; no reuse or rights approval is inferred. The API result is a version locator, not an experimental reproduction.
- **L42 disposition:** use `f295174...` as the next version-pinned paper-to-artifact candidate, while tracking the arXiv/journal URL difference. Compare published split, filtering/judge flow, output files, and model/prediction correspondence with the paper; preserve uncertainty wherever the artifact cannot bind an output to a table. Exact novelty remains `UNKNOWN`; no author-reported score becomes local evidence.

## LEAD v203 — Resolve full-text access for the 2025 substitution review (2026-10-14)

- **Access state changed from v201:** the PMC web page itself presented an access challenge and MDPI returned HTTP 429, but the versioned Europe PMC full-text XML endpoint succeeded for PMCID `PMC12651232`. Retrieved body: 148,204 bytes; SHA-256 `b2bb5dc6e849212c5c09744576cead4b3ceb84c98fdf2ee232f426f389d92484`; URL: [Europe PMC full-text XML](https://www.ebi.ac.uk/europepmc/webservices/rest/PMC12651232/fullTextXML). The hash is a retrieval fingerprint; the XML was not archived in the repository.
- **Screen result:** the article's own methods section confirms a targeted Scopus/Web of Science/Google Scholar search and labels the approach conceptual/narrative rather than systematic or quantitative. The full text lays out sensory, functional, nutritional, cultural/regulatory, environmental and AI-method domains, and cites concrete prior substitution systems including Shirai et al.'s FoodKG substitution work and recipe-embedding work. A lexical scan of the retrieved text found no exact occurrences of `conformal`, `abstain`, `risk-coverage`, `Recipe1MSubs`, `unseen pair`, or `selective prediction`; this is only a search aid, not evidence that those ideas are absent semantically.
- **Disposition:** replace v201's conditional “if still inaccessible, proceed from primary papers” with the current state: review full text is now accessible and hash-fingerprinted through Europe PMC, but no claim-level exhaustive coding or independent adjudication has been completed. The review remains one secondary source, not primary evidence or an exhaustive novelty search. L42 stays open; exact novelty remains `UNKNOWN`.
- **Next:** use the review's reference trail to discover and reconcile primary papers against the frozen task matrix. Where a source is already screened locally (including Shirai et al.), reuse that evidence and avoid duplicate work; track only new artifact/protocol questions. No SSoT or claims-register status changed.

## LEAD v202 — Add direct LLM/Recipe1MSubs prior art to L42 (2026-10-14)

- **Direct comparator found by citation chase:** Rita et al., “Optimizing Ingredient Substitution Using Large Language Models to Enhance Phytochemical Content in Recipes,” *Machine Learning and Knowledge Extraction* 6(4):2738–2752 (2024), DOI `10.3390/make6040131`, arXiv `2409.08792v1`. The arXiv v1 PDF was retrieved from the versioned arXiv endpoint (2,379,827 bytes; SHA-256 `9dbe664a6a9d9b131cd4587751397a92d1acc06dee2974e995d74e50a6c65601`; snapshot was not retained locally). Sources: [versioned arXiv full text](https://arxiv.org/pdf/2409.08792v1), [arXiv record](https://arxiv.org/abs/2409.08792), [journal article](https://www.mdpi.com/2504-4990/6/4/131).
- **What the paper reports (author-reported, not reproduced):** Recipe1MSubs’ fixed 49,044/10,729/10,747 train/validation/test partition; recipe-context prompts; fine-tuned GPT-3.5-Turbo-1106, DaVinci-002, and TinyLlama-1.1B, compared with GISMo using Hit@1. On the original dataset the paper reports GPT-3.5 at `38.03 ± 0.28%` vs GISMo `34.53 ± 0.10%`; on its separately filtered dataset it reports `54.46 ± 0.29%` vs `40.24 ± 0.36%`. The filtering calls GPT-3.5-Turbo to classify substitutions across all partitions as Correct/Potential/Incorrect and retains Correct examples; the resulting test set has 6,721 examples rather than the original 10,747. The paper also reports phytochemical enrichment of 1,951 pairings / 1,639 recipes and explicitly cautions that disease relevance is preclinical, not clinical evidence.
- **Material scope effect:** This is direct prior art for LLM fine-tuning and recipe-context ingredient-substitute ranking on the same named benchmark, plus model-assisted substitution-validity filtering. Do not frame “LLM-based Recipe1MSubs substitution,” contextual LLM ranking, or model-assisted curation as an unoccupied space. The paper does **not**, on the inspected text, establish the frozen task’s repeated-vs-unseen-pair strata, selective risk/coverage, calibrated abstention, evidence-missingness strata, or provenance-bearing safety decision. The filtered-set score changes both the evaluation population and its judge-derived inclusion rule; it is not a same-test-set comparison with the original score, and neither result is an independently reproduced local metric. Exact novelty remains `UNKNOWN`; L42 stays open pending the full hashed funnel and independent adjudication.
- **Next / acceptance:** Pin and inspect the paper-linked Bitbucket artifact at the cited version (or record the access failure), then align its candidate identity, split overlap, judge/filter flow, prompt/task, and evaluation denominator against the frozen ChefKix protocol. Carry the paper as an adjacent/direct comparator in the claim matrix; do not claim it is a duplicate or dismiss it as irrelevant. No SSoT, protocol, scores, or claims-register status changed here.

## LEAD v201 — Add 2025 ingredient-substitution review to L42 scope map (2026-10-14)

- **New dated lead:** Oz & Oz, “Artificial Intelligence-Enabled Ingredient Substitution in Food Systems: A Review and Conceptual Framework for Sensory, Functional, Nutritional, and Cultural Optimization,” *Foods* 14(22):3919 (2025-11-17), DOI `10.3390/foods14223919`, PMID `41300077`. PubMed metadata and abstract describe a four-domain framework (flavor/aroma, functional properties, nutritional profiling, and constraint-based filtering) and explicitly classify the review method as narrative/domain-focused, not systematic or quantitative. It is now a concrete L42 full-text/citation-chase target, not just a broad-topic lead. Sources: [PubMed record and abstract](https://pubmed.ncbi.nlm.nih.gov/41300077/), [publisher DOI](https://doi.org/10.3390/foods14223919), [PMC record](https://pmc.ncbi.nlm.nih.gov/articles/PMC12651232/).
- **Screen boundary:** PubMed abstract/metadata was readable; PMC full text returned an access challenge and the publisher page returned HTTP 429 in this pass. Therefore no full-text coding, exact methods/results extraction, reference chasing, local source hash, or independent adjudication is claimed. Do not treat the abstract’s three deployment gaps as an exhaustive novelty search or as evidence of an absent feature in the exact frozen task.
- **Claim effect:** Add this as adjacent prior art relevant to broad AI-enabled substitution, multi-domain quality, constraint filtering, explainability, and deployment integration. Its abstract alone does not resolve pair identity/unseen-pair generalization, evidence-missingness strata, selective prediction or abstention, allergen evidence provenance, calibrated guarantees, Vietnamese context, or ChefKix’s frozen estimand. Exact novelty remains `UNKNOWN`; L42 remains open. This review and Kim et al.’s 2024 arXiv survey are distinct review records, not independent experimental replications; do not count either as primary evidence for model performance.
- **Next / acceptance:** obtain a permitted, version-identifiable full text for this review; screen its cited primary substitution systems against the frozen task matrix; record source/version and claim-level coverage in the hashed L42 funnel; then obtain independent adjudication. If full text remains inaccessible, preserve that as a bounded access limitation and proceed from independently located primary papers rather than inferring coverage from the abstract. No SSoT claim, protocol, result, or thesis-completion percentage changes here.

## LEAD v200 — Recheck M-A runtime boundary after host listeners changed (2026-10-14)

- **Fresh read-only observation (2026-10-14 07:23 UTC):** Docker Engine 28.5.1 is reachable. The listed containers include healthy infrastructure dependencies, but no ChefKix AI-service or monolith container; Keycloak is exited. Ports 3000 and 8080 now have generic `node` and `java` listeners, respectively, superseding v193's older no-listener observation for those ports only. The 8080 listener returns Tomcat 404 at `/actuator/health`; neither listener has been tied to a ChefKix service. Port 8000 has no listener.
- **M-A disposition:** this is a changed host observation, not proof that the Java process is ChefKix, that an AI graph endpoint is loaded, or that any R0 graph/count is available. No graph endpoint was queried and no service was started. Keep the active runtime graph `UNKNOWN`; do not reuse the port listener or infrastructure state as graph evidence.
- **Next:** identify the two host processes and their owning project/read-only health routes, then locate an authorized AI-service graph path and trace producer → exact artifact → loaded process. If that path remains unavailable, record the bounded unavailable state without starting services or promoting artifact counts to runtime truth.

## LEAD v199 — Resolve exact-model availability before matched-arm collection (2026-10-14)

- **L22 / R21–R22 dependency sharpened from current official documentation:** OpenAI lists the frozen `gpt-4o-2024-08-06` snapshot for API shutdown on 2027-01-14; Google says Gemini 2.5 API access is limited to users who actively used those models previously, while `gemini-2.5-flash` remains served until further notice; Mistral's current model comparison still lists the frozen `mistral-small-2603` ID. Sources: [OpenAI deprecations](https://developers.openai.com/api/docs/deprecations/), [Google Gemini 2.5 Flash](https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash), and [Mistral model comparison](https://docs.mistral.ai/getting-started/models/compare?models=mistral-small-4-0-26-03) (checked 2026-10-14).
- **Execution consequence:** keep the exact frozen model IDs; do not substitute a current model under the existing protocol. Before scheduling paid calls, verify account-specific endpoint eligibility for each arm, set the authorized spend cap, and reserve the matched run inside the GPT-4o shutdown window. If an exact endpoint is inaccessible, stop that arm and request an explicitly versioned protocol decision; do not silently revise the experimental treatment. No provider credentials were read and no API calls were made.
- **Evidence ceiling:** public documentation establishes published model lifecycle/access conditions, not this project's account entitlement, quota, price at execution, or successful authenticated invocation. The Mistral page's model listing likewise is not proof of account-level access. R21/R22 remain open until the required raw arms are run once and validated; R23 human review and R24 protocol decisions remain separate gates. This is a scheduling/risk update, not a completion percentage or scoreboard.
- **Next:** obtain the three account-specific eligibility checks and explicit spend authorization; then run the two missing arms separately under the frozen protocol, preserving raw outputs/errors without retries. Escalate any blocked or retired exact model for a human-approved, separately versioned protocol decision before collection.

## LEAD v198 — Verify matched-arm execution and merge path is prepared (2026-10-14)

- **L22 / R21–R22 preparation advanced:** static inspection of the frozen runner confirms explicit provider contracts for `gpt-4o-2024-08-06` (`OPENAI_API_KEY`) and `gemini-2.5-flash` (`GOOGLE_API_KEY`), alongside the already collected `mistral-small-2603` arm. The runner checks selected-arm credentials before entering provider calls, and the separate multi-arm merger requires complete 208-cell arms, exact protocol/rubric pins, one requested model per arm, unique response IDs across arms, and an unadjudicated claim boundary.
- **Offline regression evidence:** `python -m pytest ai/tools/test_merge_allergen_benchmark_v2_arms.py -q` passes 4 tests plus 5 subtests. The suite uses a `FakeRunner` and synthetic in-memory arms; it confirms merge/mutation rejection behavior only and did not contact any provider. Runner SHA-256 `78dedb87a1a6f14e31144ea08cf904589bda76d9a812f61dd5274f1f1a28c774`; merger SHA-256 `67ddad0d741bf77fc950c01067b571aba9a3def5c139d34c2c6fce0d91f9694f`; test SHA-256 `0e9d861b2abd29a7a8d86a79629c9872c3071770d39d133d7ee017a4f7f03289`.
- **Boundary:** this is implementation/test readiness, not execution of GPT-4o or Gemini, evidence that credentials are presently configured, cost authorization, result validity, comparative scoring, safety, or clinical evidence. R21/R22 remain open until real raw arms are separately run once under the frozen protocol and validated; R23 review/adjudication stays separate. No credentials were read and no network request was made.
- **Next:** when the keys and spend cap are explicitly available, run each missing arm in its own immutable state/output, preserve raw errors without retries, validate both, then merge with the existing Mistral arm. Keep human review and adjudication gated; no protocol or product code changed in this backlog-only update.

## LEAD v197 — Bind R19/R20 evidence into the local claims derivative (2026-10-14)

- Applied separate hash-bound evidence records to R19 and R20 in a new v197 derivative of the 45-row register. The fail-closed validator confirms 11 `VERIFIED`, 6 `PARTIAL`, and 28 `UNVERIFIED` rows, with all local references resolving. R19 and R20 are deliberately `PARTIAL`: protocol/raw records plus their validators support bounded artifact claims, not independent protocol approval or external provider attestation.
- Derivative: `ai/out/iron-chef-lead-v197/claims-register-R01-R45-v197.csv`, SHA-256 `44689a1898dcbc09a7c40d3fa9c53ee2007540d5601eda8448c138213ea0cd9b`. Evidence records: R19 `923e5df18f5a7d9ea890c5735fbeebe1da0a23fa0a9e2188c84d972f93d8578d`; R20 `17fc393e79f67c15311d2c2dd068dcfc5a7a8bb2ae8f0da97be19d9ad5fbd7e8`. Both apply operations and structural validation passed; focused evidence-application/register tests pass 11/11.
- **Boundary:** register validation confirms schema/path/hash consistency only; it does not independently reproduce the claims. The counts are derivative row statuses, not a thesis-completion percentage or scoreboard. R21/R22 still require their actual matched arms; R23 requires two independent reviews and disagreement adjudication; R24's expansion/interval decision remains open. No API call or human review was made.
- **Next:** carry the R21/R22 credential/resource gate and R23 reviewer/authorization gate as dependencies; do not start paid calls or unblind the key without the required approval and both completed reviews. No canonical SSoT, experiment protocol, or product code changed.

## LEAD v196 — Revalidate frozen allergen protocol and blind-pack readiness (2026-10-14)

- **L22 / R19 and review preparation advanced, not closed:** a fresh run of `python ai/tools/validate_allergen_benchmark_v2.py` reconstructed the hash-pinned v2 protocol and rubric, 208 unique case families across all 13 declared groups, task/schema constraints, review requirements, and checkpoint controls; 12/12 protocol, rubric, runner, and corruption gates passed. A fresh `python ai/tools/validate_allergen_v2_blind_review_pack.py` check confirms two separate 208-item packs, independent ordering, identity-hidden content, and a key sealed until both reviews finish; all 6/6 pack mutation checks passed.
- **Exact evidence:** frozen protocol SHA-256 `0e97c829cccfd6b43bf1dd974db9ae54dd9b561d037ebbca18eb4fbd2bc14fc9`; rubric `517c85f87462fae8cc9b3a2e0efefa61b8257ebab77398827c36d185175520a0`. Protocol validation report `ai/out/iron-chef-lead-v39/VALIDATION.json` SHA-256 `11eda985d1b070198555635dcba7de2ef2dab811f953fd3d688d2e11ecc1fce8`; blind-pack report `ai/out/iron-chef-lead-v39/REVIEW_PACK_VALIDATION.json` SHA-256 `083c13d9e6deb5d965a8bdfa575c12ee136b3842047b8c86a1750fbc5540528d`. Reviewer-pack and sealed-key hashes are `2e26a1cbfe9e7fd68ee16fe4535b1407a687cdb8afa94e253eaf2625bc5053b3`, `68fa426d0fcdc73fc9827b44a06771fa54b7abc415a99e399fdbdb0d9324ea33`, and `b60b465962c286bc46e78f0a8d26b80324d2d60769b4e403fc6a58bfe70c0e83` respectively.
- **Boundary:** reports explicitly say `PROTOCOL_READY_REVIEW_PENDING` and `MISTRAL_REVIEW_PACK_READY_UNREVIEWED`. No reviewer has supplied labels here; no agreement/adjudication, allergen safety, model comparison, or clinical result is inferred. R19 is supported only for this frozen protocol's structural contract; R23 remains open. The existing 208-case Mistral run remains raw-only as recorded in v195; no external calls or data collection occurred.
- **Next:** keep the identity key sealed until two independent reviews are complete, then adjudicate disagreements under the frozen rubric. Human authorization, availability, and rights remain prerequisites; protocol readiness is not a substitute. No protocol or code changes were made.

## LEAD v195 — Revalidate the frozen Mistral raw arm (2026-10-14)

- **L22 / R20 advanced, not closed:** a fresh read-only run of `python ai/tools/validate_allergen_benchmark_v2_raw.py` reconstructed the existing matched Mistral arm: all 208 frozen query cells have schema-valid raw responses, one recorded attempt per cell, 208 unique provider response IDs, and requested/returned model `mistral-small-2603`. All eight semantic/envelope mutation checks were rejected. The validator wrote the same structured summary to its ignored local output; no provider call, model call, review, score, or new experiment was performed.
- **Exact inputs and result:** protocol SHA-256 `0e97c829cccfd6b43bf1dd974db9ae54dd9b561d037ebbca18eb4fbd2bc14fc9`; rubric SHA-256 `517c85f87462fae8cc9b3a2e0efefa61b8257ebab77398827c36d185175520a0`; raw-response file SHA-256 `af2ac54862cd54959b4534abdf9e53a904da451f5cc57923752f034d63b07404`. Validator SHA-256 `07ece3c60e864428deaeb6fe6309a231ae212248561ce6dd1dd7b118076583e1`; runner SHA-256 `78dedb87a1a6f14e31144ea08cf904589bda76d9a812f61dd5274f1f1a28c774`. Revalidation report: `ai/out/iron-chef-lead-v39/RAW_VALIDATION.json`, SHA-256 `c6a3830e302b332719e2351818ed051fdfdab8b63bd7d36e8eee1e14ade83648`.
- **Evidence boundary:** this confirms raw collection integrity only. It does not establish candidate safety, an allergen score, comparative performance, clinical validity, or product readiness. The two blind packs and sealed key are not completed reviews; independent reviewer labels and disagreement adjudication remain absent. GPT-4o/Gemini matched arms remain unrun because their credentials are unavailable. L22 stays `PARTIAL`; R19/R20 are now only `PARTIAL` as recorded in v197; R21–R24 remain open. Do not compute rates or promote the raw arm to a winner.
- **Next:** continue the bounded R21–R24 mapping from existing artifacts; separately obtain required human/protocol/resource decisions before any collection or paid call. No SSoT, experiment design, code, claims register, or product behavior changed in this backlog-only update.

## LEAD v194 — Reproduce the bounded OFF taxonomy partition (2026-10-14)

- **L48 / R18 advanced:** independently rebuilt the partition from the hash-pinned Sep 8 OFF snapshot manifest and preserved official allergen taxonomy bytes last-modified Sep 30. Result: 3,377 distinct tags / 767,780 occurrences: 23 exact positive identities cover 760,877; the official `en:none` sentinel covers 564; 17 unique same-language aliases cover 32 and remain review-only; 3,336 unrecognized tags cover 6,307.
- The validator independently reconstructed report/queue bytes and rejected 8/8 semantic mutations; focused taxonomy tests pass 6/6. R18 is now `VERIFIED` only for this precise snapshot/taxonomy partition. The updated 45-row derivative validates at 11 `VERIFIED`, 4 `PARTIAL`, 30 `UNVERIFIED` (SHA-256 `a46bfe7499135945996cb0c993faff66c53ef97e268e09fd323425d623de7c7b`). Evidence record SHA-256 `d71d326dd0c2a8853348f15605b66aa814e1655c4ed8836bb3784ec0feda790f`; report SHA-256 `a5745b992f3ac9c9aa21708fb4d0322dc2ecf0fe585919d2f1c8e4f34bfe5c1a`; validation output SHA-256 `a5f97d1caacec550178ab2dd94c03aa7fd3847e7626893398e1f19798185cdb7`.
- **Evidence boundary:** the taxonomy post-dates the frozen snapshot, so do not describe this as taxonomy state at ingestion time. Aliases are suggestions, unrecognized values are not negatives, and `en:none` is not a positive allergen. No independent adjudication, clinical sensitivity/specificity, production safety, or user outcomes are established. This closes one bounded register row only; L20/L39 and L48 remain open, with no thesis-completion percentage.
- **Next:** continue the register’s R21–R24 source/protocol mapping separately from this partition. Human adjudication, paid model arms, and any safety claims remain behind their ethics, reviewer, rights, budget, and protocol gates.

## LEAD v193 — Refresh M-A local runtime availability observation (2026-10-14)

- Read-only host check at `2026-10-14T06:54:07Z`: Docker is reachable. `docker ps -a` showed infrastructure dependencies only; no ChefKix AI-service or monolith container was present. Keycloak was `Exited (1)` and MongoDB was `unhealthy`. The host had no process/listener on the checked application ports 8000, 8080, 8081, 3000, or 5000.
- **Bounded result:** this supersedes the 2026-10-12 observation that the Docker Desktop engine pipe was unavailable, only for this host/time. The local API/runtime path was not running in this check, so no graph was loaded and no runtime graph count was obtained. It does not establish absence or state of a separately deployed service. No service was started, no logs or credentials were read, and no health state was repaired.
- **M-A disposition:** local runtime availability is now `NOT OBSERVED / unavailable on checked host at observation time`, not a reconciled graph-truth result. M-A and R39/R40 remain open; keep artifact inventories and runtime claims separate. Next meaningful check requires an authorized healthy AI-service/runtime path, then trace exact producer→artifact→loaded process before recording graph counts.

## LEAD v192 — Separate M-A artifact graphs from runtime truth (2026-10-14)

- **Bounded local evidence now exists:** the source-only seed interpretation yields 79 typed vertices/59 directed statement arcs under its explicit unique-alias rule; the serialized HGAT subgraph has 195,600 feature rows and 7,247 directed arcs; and the AI-service sample graph has a separate 37-arc inventory. Reports are hash-pinned in SCOPE's M-A audit; the static graph-truth/HGAT inspector suite passed 10 tests (including 2 subtests) on this read-only check.
- **These are not interchangeable graphs:** seed-source interpretation, checkpoint bytes, sample artifact, registry metadata, and a loaded/deployed runtime are distinct provenance states. Producer linkage and live runtime graph remain `UNKNOWN`; the 79-node construction is not a claim about persisted target nodes. Historical Round-0 graph-size/degree claims are therefore not reconciled, and M-A remains **OPEN**. No runtime was started, no credentials were inspected, and no statistic was promoted to product truth.
- **R39/R40 and L48 consequence:** carry the evidence as bounded local-artifact findings only. Do not silently replace SSoT's historical numbers or use registry counts as a runtime count. R39/R40 require their exact source/artifact/protocol mappings and should remain `UNVERIFIED` until that evidence is independently reconstructed and recorded in the claims register.
- **Next action:** trace one exact producer→artifact→runtime path for each claimed graph; if the relevant runtime cannot be accessed under the approved environment, record the bounded unavailable observation and leave the claim unresolved. Then reconcile each Round-0 denominator, node-identity rule, directionality, and degree statistic against that exact path. This advances evidence mapping, not a top-level completion checkbox or thesis percentage.

Evidence pointers: root `SCOPE.md` M-A graph-truth audit; seed inventory `ai/out/iron-chef-lead-m-a-20261012/GRAPH_TRUTH.csv` SHA-256 `5e2ee30381a1cf1f79368fc9b069349acf743cde94ca9e132b39d64e4e1a4b65`; source-only seed interpretation `ai/out/iron-chef-lead-m-a-seed-interpretation-20261012/GRAPH_TRUTH.csv` SHA-256 `8d82de9fb60ec536d03e9e902b42a137f22daeafbccf45721e99ce13637304a6`; HGAT artifact report `ai/out/iron-chef-lead-m-a-hgat-artifact-20261012/HGAT_SUBGRAPH_ARTIFACT_TRUTH.json` SHA-256 `e9f8abf6cbee3d03526127d34afdaee18f584896b72a9a505c81c99fa552b66e`; combined inventory and manifest hashes are recorded at SCOPE's M-A section. Root evidence remains outside this commit.

## LEAD v191 — Reconcile verified L48 and Stage-3 boundaries (2026-10-14)

- **L48 / claims register:** The latest validated 45-row derivative is 10 `VERIFIED`, 4 `PARTIAL`, and 31 `UNVERIFIED` (register SHA-256 `c4a7350f1ccfb425da801e8a34b4a5112e2545ffd6c494e4eb4f8043f41e00bb`). The fail-closed validator confirmed all local paths and hashes; structural validation is not independent reproduction. R13 is bounded to profile-pair arithmetic, and R14 to frozen-file endpoint-profile presence. Their evidence ceilings remain narrow; L48 is **OPEN** and these row counts are not a thesis-completion percentage. R14 report SHA-256 `f5ae21dee9bab8d2125b0baea11c10550855344ac081d45b13b58335cc79525f`; its reproducible detail and non-adopted v4.2 comparison are in SCOPE/SESSION v171.
- **Stage 3 / L44:** Segment 9 was transactionally validated at exact step 561 with terminal state `STAGE3_SEGMENT_9_COMPLETE`, all 12 checkpoint hashes/inventory, recursively pinned Segment-8→1 and Stage-2 ancestry, frozen selections/source, and exactly the three frozen evaluations at steps 187/374/561. Segment-9 manifest SHA-256 `46f5b0f403f9840e07854ca4c1ce179e49d136b65cc5f319c56f3b2e626eb969`; validation JSON SHA-256 `07922fb02457319e2db85b94dc255671a980a1ba21c89287a821e0e80655fd7d`. This closes that artifact boundary only; it does not establish inference, scoring, model quality, or overall Stage-3 completion. The separate recorded Segment-10 observation in v185 is historical and must not be presented as a fresh status.
- **Critical path remains unchanged:** L41 facts and authorization (ethics classification, raters/adjudicator, real capacity, advisor/defense dates, authorized API/GPU caps) are still human dependencies; their completion is not inferred from elapsed dates or compute progress. Prepare independent work and evidence-bound writing in parallel, but do not start human collection, paid calls, or protocol-dependent experiments before applicable gates. Then preserve L41→L36→L43→L38→L45→L46; midterm/demo work supports communication and does not replace this path.
- **Immediate next actions:** (1) record dated L41 responses/receipts and actual capacity, (2) advance L48 by mapping the next unresolved claims to exact artifacts and independent checks, and (3) re-estimate the remaining schedule from confirmed capacity and open gates. Keep unresolved work explicitly open; no aggregate thesis percentage is authorized by the row counts.

Evidence pointers: root `SESSION.md` v169/v170/v171, `SCOPE.md` L48 R13/R14, `ai/out/iron-chef-lead-v99/secondary_stage3_segment9_validation.json`, and the v171 claims-register derivative. Generated evidence remains outside this backlog commit.

## LEAD v190 — Record date-bounded CARE artifact discrepancy for L42 (2026-10-14)

- **Paper-side statement:** PubMed Central's article record says the complete CARE v2.0 codebase, prompt templates, and CAREBench-150 were publicly available at `SerinaRica/CARE`, accessed 2026-02-12. The paper reports 400k Recipe1M+ and CAREBench-150 evaluation. Keep both as the authors' stated provenance and reported results, not independently verified experiment evidence. Source: [article data-availability record](https://pmc.ncbi.nlm.nih.gov/articles/PMC13206443/).
- **Repository-side dated evidence:** The visible `main` history has the core-code commit `186276c` dated 2026-02-02; the next visible commit `f217ab5` is dated 2026-04-04 and changes README branding only. Thus, **inference**: the linked `main` snapshot on the paper's stated 2026-02-12 access date appears to be `186276c`. That tree's experiment plan names a 17.5k Chinese recipe source and an intended 1,000+ enrichment; its tracked `test/test_queries.json` contains ten examples, not an identifiable 150-query CAREBench artifact. The checked-in README also says large recipe/nutrition files must be separately supplied. A 2026-10-14 read-only ref check found only `refs/heads/main` and no tags; targeted searches surfaced no separate release or CAREBench-150 mirror. This is an unsuccessful discovery, not proof that no external artifact exists. Sources: [2026-02-02 commit patch](https://github.com/SerinaRica/CARE/commit/186276c.patch), [2026-04-04 branding commit patch](https://github.com/SerinaRica/CARE/commit/f217ab50a53a6854861c77e7539f94a4146fb34a.patch), [experiment plan](https://github.com/SerinaRica/CARE/blob/186276c/docs/experiment_plan.md), [tracked query examples](https://github.com/SerinaRica/CARE/blob/186276c/test/test_queries.json), [README](https://github.com/SerinaRica/CARE/blob/f217ab50a53a6854861c77e7539f94a4146fb34a/README.md).
- **Meaning and limit:** This is a material, date-bounded code/data/version mapping gap between the article's availability statement and the identifiable artifacts in the linked main-branch tree. It does **not** prove the authors' dataset/results are false or unreproducible: a separately hosted release, unindexed artifact, omitted/private branch, later upload, or mistaken access date remains possible. Do not characterize CAREBench-150 as absent from every public channel without checking those alternatives.
- **L42 action/acceptance:** Seek the paper-linked release/tag and its commit/tree for the stated access date; map the CAREBench-150 150-item artifact, Recipe1M+ processing, prompt templates, and reported metric scripts to exact hashes. If unresolved, ask the authors through an approved channel and carry the unresolved correspondence into the claims register plus independent adjudication. Until then, preserve the article as adjacent prior art, do not equate the ChefMind harness with CARE v2.0, and keep exact novelty `UNKNOWN`.

## LEAD v189 — Separate CARE paper metrics from public repo harness (2026-10-14)

- At the pinned repository history's preceding `186276c` “Init: ChefMind core logic (excluding large data files)” commit, the tree includes `docs/experiment_plan.md` and `test/{test_runner,metrics,baselines}.py`. The experiment plan names a 17.5k Chinese recipe corpus, a proposed query set, and a baseline/ablation plan; those are repo-plan statements, not verified dataset or completed-experiment evidence. The test harness comments explicitly treat ambiguity resolution as a retrieval proxy, and `nutrient_error_rate()` returns `0.0` as a placeholder; the baseline module labels FoodKG ontology expansion as simulated and describes CookGPT as simulated. Sources: [commit/tree](https://github.com/SerinaRica/CARE/commit/186276c), [experiment plan](https://github.com/SerinaRica/CARE/blob/186276c/docs/experiment_plan.md), [test runner](https://github.com/SerinaRica/CARE/blob/186276c/test/test_runner.py), [metrics](https://github.com/SerinaRica/CARE/blob/186276c/test/metrics.py), [baselines](https://github.com/SerinaRica/CARE/blob/186276c/test/baselines.py).
- **Interpretation:** This is evidence that the public repository contains a proposed ChefMind evaluation harness with placeholders/simulations; it is not evidence that the article used this harness or that CAREBench-150 results are invalid. Do not reproduce, compare, or quote repo-plan targets as completed findings. Ask L42's paper-to-code question at the artifact level: exact commit/data/query split/result-generation procedure used for the article, and whether the linked repo is a later/different artifact.
- **Next action / acceptance:** Preserve paper metrics as author-reported and repo statements as commit-pinned code/plans. L42 advances only when the article's experimental artifact is mapped or the unresolved mapping is explicitly carried into the claims register and independently adjudicated; exact novelty stays `UNKNOWN` meanwhile.

## LEAD v188 — Pin CARE repository snapshot for follow-up (2026-10-14)

- **Immutable observation point:** `git ls-remote` resolved the repository's `main` ref to commit `f217ab50a53a6854861c77e7539f94a4146fb34a`. The commit page shows this commit changed only README branding from “ChefMind” to “CARE”; inspected source paths at that same commit are [`pipeline/chefmind.py`](https://github.com/SerinaRica/CARE/blob/f217ab50a53a6854861c77e7539f94a4146fb34a/pipeline/chefmind.py), [`rag/retriever.py`](https://github.com/SerinaRica/CARE/blob/f217ab50a53a6854861c77e7539f94a4146fb34a/rag/retriever.py), and [`llm/generator.py`](https://github.com/SerinaRica/CARE/blob/f217ab50a53a6854861c77e7539f94a4146fb34a/llm/generator.py). This anchors v187's code observations to one immutable tree; it is not a local archival or SHA-256 inventory.
- **Interpretation:** A README-only rename from ChefMind to CARE makes repository/article identity and experiment-to-code correspondence a concrete provenance check, while remaining insufficient to infer intent, validity, or a paper/repository mismatch. No code was cloned, executed, or treated as licensed for reuse. Continue L42 by resolving the paper's exact code/data/evaluation artifact version, then hash/archive only the relevant permitted artifacts and independently review before changing claim language.
- **Boundary:** The commit-page record confirms the README-only rename, not the publication date of the code, the article authors' experimental environment, or that this repository is the sole implementation. CARE remains adjacent prior art; novelty remains `UNKNOWN` and L42 remains open.

## LEAD v186 — Add CARE as a bounded L42 prior-art comparator (2026-10-14)

- The publisher's article record/full-text index and PubMed record identify Fu et al., “Nutrient-Aware Personalized Meal Recommendation Using Structured Food Knowledge and Constraint Verification,” *Foods* 15(10):1647 (9 May 2026), DOI `10.3390/foods15101647`, PMID `42195851`. CARE describes a lightweight 4R recipe-recommendation pipeline: fuzzy-query constraint extraction, food-knowledge-graph retrieval, recipe reranking with hard dietary filters, and optional agentic verification. The article reports author-produced CAREBench-150/Recipe1M+ results, including semantic Recall@5 and constraint-satisfaction rates; none of those numbers were independently reproduced here. Sources: [publisher article](https://www.mdpi.com/2304-8158/15/10/1647), [PubMed record](https://pubmed.ncbi.nlm.nih.gov/42195851/).
- **Positioning effect:** This is materially adjacent prior art against broad claims that knowledge-grounded food recommendation, hard dietary constraints, or an agentic checking stage are themselves new. The stated task is fuzzy recipe retrieval/recommendation; it does not, on the evidence reviewed here, establish the exact frozen ChefKix task of ingredient-substitute selection on Recipe1MSubs, train-relative unseen-pair evaluation, evidence-missingness strata, or calibrated selective abstention. Treat the contribution boundary as narrower, not as proof of novelty.
- **Evidence ceiling:** The publisher page was rate-limited on direct open; the indexed article text and PubMed metadata/abstract were inspected. No local full-text snapshot/hash, code execution, independent protocol review, or second-reviewer adjudication was produced. This is a bounded discovery update, not completion of L42's hashed funnel or novelty decision. Keep exact novelty `UNKNOWN`; do not promote the authors' reported metrics to local evidence.

## LEAD v187 — Flag CARE paper-to-repository version reconciliation for L42 (2026-10-14)

- **Observed discrepancy, not a paper verdict:** The article identifies a public implementation at [SerinaRica/CARE](https://github.com/SerinaRica/CARE). A read-only screen of the repository's current `main` README and selected raw files describes a USDA-backed recipe assistant with keyword/rule intent extraction, in-memory graph/tag plus lexical recipe retrieval, and an optional OpenAI-compatible generator/simulation fallback. In the inspected `pipeline/chefmind.py` entry path, `RecipeGenerator` is initialized but `run()` returns retrieved recipe cards without invoking it; the inspected retriever uses lexical scoring and a small hard-coded Chinese substitute map. These observations do **not** establish what code/version produced the paper's CAREBench-150 results, whether relevant code exists outside the inspected files, or whether the default branch changed after publication. Sources: [repository README](https://github.com/SerinaRica/CARE/blob/main/README.md), [pipeline](https://github.com/SerinaRica/CARE/blob/main/pipeline/chefmind.py), [retriever](https://github.com/SerinaRica/CARE/blob/main/rag/retriever.py), [generator](https://github.com/SerinaRica/CARE/blob/main/llm/generator.py), and [paper](https://www.mdpi.com/2304-8158/15/10/1647).
- **Required reconciliation:** Before coding CARE as directly comparable evidence, pin and archive the repository commit and inspect the complete tree/history, dataset/evaluation scripts, and paper-to-code mapping (including whether the authors identify a separate experiment branch or artifact). Record exact commit/source hashes and code availability/license evidence; request clarification from authors only if the public artifacts cannot resolve the mapping. Keep article-reported findings attributed and separate from repository behavior. Do not execute external code or reuse dataset/code pending provenance, safety, and rights review.
- **Claim effect and stop rule:** This raises a reproducibility/provenance question and strengthens the need for source-version identity in L42; it does not erase the paper's reported adjacent contribution or establish a mismatch, invalid result, direct duplicate, or novelty. L42 remains open and exact novelty remains `UNKNOWN` until the complete hashed funnel and independent adjudication are done. No conclusion is upgraded by this repository screen alone.

## LEAD v185 — Recenter execution on the thesis critical path (2026-10-14)

- **Priority clarification:** The midterm exhibit is a bounded communication event, not the thesis end state. Keep Q124 and its rehearsal work scoped as supporting evidence; do not let exhibit polish displace the scientific, writing, and defense path. This is a sequencing clarification, not a change to the canonical strategy or its evidence gates.
- **Schedule is not confirmed:** SSoT §18 is explicitly proposed/adaptive. On 2026-10-14 the source calendar is in W1 (Oct 12–18); its Oct 12 ethics-inquiry target has elapsed, its Oct 15 two-rater target is tomorrow, and proposed Freeze A is Oct 22. Completion/status of those human actions is **UNKNOWN** here. Do not infer success, failure, or permission for a post-result protocol change from the dates. Rebaseline only after recording actual responses and constraints.
- **Immediate critical-path dependency — L41:** Obtain and record (with owner, response/date, and evidence) the institution's written ethics classification/authorization; named audit raters and adjudicator; actual weekly lead/member capacity; advisor and defense-calendar facts; and authorized API/GPU caps. Prepare the handbook and logistics in parallel, but no human data collection or paid call begins without its applicable ethics, consent, protocol, and budget gates. See [SSoT §14](../academic_vision_and_strategy.md#s14), [§16](../academic_vision_and_strategy.md#s16), and [§17](../academic_vision_and_strategy.md#s17).
- **Protect the dependency order:** L41 → L36 → L43 → L38 → L45 → L46 remains the critical path. L36 still requires independent raters, frozen pools, and PR01–03 resolution; L43 requires the leak-free human comparison; L38 requires the audit/judge/arms and PR04–08 resolutions. Compute availability alone does not unlock these gates.
- **Advance independent lanes in parallel:** Continue only the remaining deterministic identity/pool/graph work (M-A/M-B), provenance-bound novelty review (L42), source-specific rights inventory (M-F), and evidence-register reconstruction (L48). Start evidence-bound thesis sections from already verified results and bounded negatives (L45); keep result-dependent claims explicitly open. L37 is already recorded complete and must not be reopened or counted again without a new, separately justified analysis.
- **Training remains a parallel, bounded lane:** The latest entry here records Segment 10 `RUNNING` at `2026-10-14T05:50:49.607985Z`, with no artifact transfer and no Segment-11 launch. This is a timestamped prior observation, not a fresh status check or checkpoint validation. Continue the existing gate; do not use training status/loss as a substitute for L41/L36/L43/L38 evidence.
- **Capacity warning:** SSoT §18's 327 lead-hour figure is a source estimate across MUST/SHOULD/COULD, not verified remaining effort; reviewer time is excluded and actual capacity is unknown. Do not publish a completion percentage or silently compress scientific gates. Re-estimate open work against confirmed capacity when the L41 facts arrive; retain the ideal scope and escalate resource/schedule needs first.
- **Next review evidence:** Replace UNKNOWN only with dated institutional/reviewer/capacity responses and link their receipts. Then issue a dependency-based schedule update that preserves the frozen protocol requirements and states any human-approved scope decision separately. No experiment or work package is marked complete by this entry.

## LEAD v184 — Separate the exhibit's screen-only scroll cue (2026-10-14)

- Root `master` now hides the narrow-screen table-scroll instruction in print; the focused regression test preserves screen/print behavior. Fifteen exhibit/script tests pass and Agent OS passes 124/124.
- This source check does not certify rendered print/mobile layout or listener comprehension. Q124 remains incomplete.
- Latest gated Segment-10 poll: RUNNING at `2026-10-14T05:50:49.607985Z`; zero artifact transfers / Segment-11 launches.

## LEAD v183 — Harden offline exhibit print layout (2026-10-14)

- Root `master` now declares letter-page geometry and print-specific table rules that avoid the interactive horizontal-scroll clipping path, with compact cells and a two-column walkthrough. A focused structural test protects the print contract; fifteen exhibit/script tests and Agent OS 124/124 pass.
- This does not substitute for rendered print/mobile inspection or a real timed listener rehearsal. Q124 remains incomplete.
- Latest guarded Segment-10 poll: RUNNING at `2026-10-14T05:45:43.916748Z`; zero artifact transfers and zero Segment-11 launches.

## LEAD v182 — Cross-check spoken values against the exhibit (2026-10-14)

- Root `master` has a cross-surface test requiring spoken hit counts/rates/denominators and calibration/selective metrics to appear in the display. Fourteen focused exhibit/script tests pass.
- This is consistency evidence only. Mobile visual acceptance and human rehearsal remain open; Q124 is incomplete.
- Latest guarded Segment-10 observation remains RUNNING at `2026-10-14T05:34:37.924109Z`; no artifact transfer or Segment-11 launch.

## LEAD v181 — Prepare a privacy-limited rehearsal record (2026-10-14)

- Root `master` speaker notes include a blank record for a real timed rehearsal: viewport, elapsed time, three exact anonymous listener answers, criteria, confusion/clipping, and outcome. It is explicitly NOT RUN and excludes identifying details. Thirteen focused tests pass.
- The template is not audience evidence. Mobile review and rehearsal remain open; Q124 is incomplete.
- Latest guarded Segment-10 observation: RUNNING at `2026-10-14T05:34:37.924109Z`; 0 artifact transfers / 0 Segment-11 launches.

## LEAD v180 — Bind narration to exact denominators (2026-10-14)

- Root `master` speaker script now states numerator/case counts with each overall, repeated-pair, and unseen-pair hit rate. Twelve focused tests bind those values and other spoken metrics to the exhibit's frozen result.
- This closes narration consistency only; mobile visual acceptance and human rehearsal remain outstanding, so Q124 is incomplete.
- Latest guarded Segment-10 poll: RUNNING at `2026-10-14T05:30:06.764162Z`; 0 artifact transfers / 0 Segment-11 launches.

## LEAD v179 — Make audience rehearsal acceptance measurable (2026-10-14)

- Root `master` speaker notes define three neutral listener prompts, pass criteria for correctly restating the frozen rejection and its limits, exact answer/time capture, and a NOT PASSED response that changes only explanation/layout. Eleven focused checks pass.
- No audience result is claimed. Q124 remains incomplete pending mobile visual review and an actual timed rehearsal.
- Latest guarded Segment-10 observation: RUNNING at `2026-10-14T05:30:06.764162Z`; 0 artifact transfers / 0 Segment-11 launches.

## LEAD v178 — Clarify offline presenter handoff (2026-10-14)

- Root `master` speaker notes now include a no-build/no-install/no-network preflight and ordered navigation cues. Ten exhibit/script tests pass. The notes do not claim that mobile rendering or audience comprehension was reviewed.
- Q124 remains incomplete pending mobile visual acceptance and timed human rehearsal.
- Latest guarded Segment-10 observation: RUNNING at `2026-10-14T05:26:45.794952Z`; 0 transfers / 0 Segment-11 launch attempts.

## LEAD v177 — Add evidence-bounded committee challenge answers (2026-10-14)

- Root `master` speaker notes now anticipate five questions on calibration versus selective rejection, the frozen threshold, benchmark-only cluster intervals, unsupported safety/usefulness claims, and future evidence needed to reopen a decision. Future independent evaluation is explicitly a proposed evidence bar, not an approved experiment. Ten focused tests pass.
- No rehearsal or listener result is claimed; mobile visual acceptance and timed audience rehearsal remain open. Q124 is not complete.
- Latest guarded Segment-10 poll at `2026-10-14T05:23:44.423544Z`: RUNNING; 0 artifact transfers / 0 Segment-11 launch attempts. Five-minute watcher remains active.

## LEAD v176 — Draft the midterm evidence narration (2026-10-14)

- Root `master` now includes a timed three-minute speaker-notes draft and checklist at `ai/demos/iron-chef-midterm-v26-speaker-notes.md`. Nine focused tests bind its timing, decision, metrics, fit boundary, and limits to the exhibit. No audience rehearsal or listener-comprehension result is claimed.
- Q124 remains incomplete: rendered mobile acceptance and timed human rehearsal are still open. The exhibit presents a fixed-benchmark rejection, not a product demo or quality claim.
- Latest guarded Segment-10 status: RUNNING at `2026-10-14T05:16:07.160877Z`; 0 artifact transfers / 0 Segment-11 launch attempts. The five-minute monitor remains active.

## LEAD v175 — Rinse narrow-layout overflow in the midterm exhibit (2026-10-14)

- The saved 390px capture showed horizontal clipping outside the intentionally wide table. Root `master` now constrains intrinsic grid-child sizing and makes the strata table's horizontal scroll region labeled and keyboard-focusable, with visible focus and a mobile scroll cue. Eight structure/accessibility/provenance tests pass; this is not rendered mobile acceptance.
- The authorized browser path refused the local file URL. Mobile visual acceptance and timed audience rehearsal remain open; Q124 remains incomplete. Root `master`'s follow-up commit is local-only because that repository has no remote. This backlog update is pushed directly to `origin/master`.
- Latest guarded secondary-account poll: Segment 10 RUNNING at `2026-10-14T05:10:38.181852Z`; 0 artifact transfers, 0 successor launches. Continue the existing five-minute monitor; no checkpoint or Segment-11 launch claim.

## LEAD v174 — Midterm evidence exhibit and continued training observation (2026-10-14)

- Prepared `ai/demos/iron-chef-midterm-v26-exhibit.html` in the root default-branch worktree: a self-contained offline research exhibit of the frozen v26 negative result. It leads with `REJECT` and 0/10,747 retained cases, separates calibration improvements from failed selective-reliability gates, and shows seven hash-bound fixed-benchmark strata with recipe-cluster intervals.
- Evidence checks: independent validation report is PASS with exact reconstruction and 4/4 corruptions rejected; fresh supplement generation is exactly equal to its saved report; all seven displayed rows match; 5 generator tests and 7 exhibit tests pass. Validation/evaluation/supplement SHA-256: `45492d30acdf46ef26fb443140a8bec2adf1c01e12e8e1049112a27029f217d5` / `293f0c9952d98d389bb3b7d23ee4f1f04b6ad675914e361de9102806f5539ea8` / `351230465cbf51a41378f42fd1c12e21ea936ebfa2bd616c31d76572460c9283`.
- This is partial work toward SSoT §12, not a live product demo, population guarantee, or model-quality claim. No raw prediction rows/checkpoint were copied. Desktop capture is legible; mobile visual acceptance and timed rehearsal remain open after the sanctioned local-file browser view was blocked. Root `master` has no configured remote; the exhibit/docs are committed locally there, while this monolith record is pushed directly to `origin/master`.
- Separately vaulted Segment-10 poll at `2026-10-14T05:06:03.543451Z` remained RUNNING; the guarded Segment-11 handoff saw 0 transfers and 0 launch attempts. Monitor cadence is five minutes to reduce post-completion idle time without changing the validation gate. No checkpoint validation or Segment-11 launch was claimed.

## LEAD v173 — Prepare gated Segment-11 training handoff (2026-10-14)

- Human direction explicitly requests continued Kaggle training while GPU time is available. Segment 10 remains the active remote job; it has not crossed exact step-631 artifact validation.
- A Segment-11 builder and advancement gate are prepared. They permit one isolated Segment-10 download and exact validation only after remote `COMPLETE`; a fresh official owner-kernel inventory must then prove Segment 11 absent before one launch. Status, transport, validation, inventory or acknowledgement failure blocks without retry.
- Proposed/authorized next bounded interval: resume 631→701 using the exact Segment-10 manifest, the same private bridge, and Segment 10 as sole predecessor. Preserve the already frozen evaluations at 187/374/561 and add no evaluation at 701; extra optimizer steps are not quality evidence.
- Focused tests: 11 Segment-11 builder/advancement tests and 10 Segment-10 builder/ingester tests pass. Segment 11 is prepared, not launched; Segment-10 checkpoint and overall Stage-3 completion remain unverified. No later segment is automatically authorized by this handoff.

## LEAD v172 — Segment 10 bounded continuation launched (2026-10-14)

- Segment 9 was transactionally verified at exact step 561 (`STAGE3_SEGMENT_9_COMPLETE`): 12 checkpoint files/hashes, recursive pinned ancestry, frozen source/data selections, unchanged evaluation history at 187/374/561, and eight-file retained-best continuity. Segment-9 manifest SHA-256: `46f5b0f403f9840e07854ca4c1ce179e49d136b65cc5f319c56f3b2e626eb969`; retained-best adapter hash: `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814`.
- Segment 10 package passed validation and was launched once through the official Kaggle CLI using the separately vaulted secondary account; version 1 was positively acknowledged. It pins the exact Segment-9 manifest, uses the same private bridge dataset, and has the historical checkpoint kernel as its sole predecessor. Script SHA-256 `0011a97db5e15fd2f27fbe972ff500bffdfa4c211a2c0e75b5bc10f49712b65c`; metadata SHA-256 `2ce9c0a7c7fef83551007e3c890cd0db7aa8e185a68bc278b467e8ff3ae390c7`.
- This is a bounded resume from step 561 to 631, continuing the established 70-step interval. Preserve exactly the frozen step-187/374/561 evaluations; do not evaluate again at 631 because the test split is repeatedly exposed. Latest authenticated status poll at `2026-10-14T04:22:10Z` was `RUNNING`, with zero artifact transports. Evidence: root [`secondary_stage3_segment10_launch.json`](../ai/out/iron-chef-lead-v99/secondary_stage3_segment10_launch.json); monitor state `secondary_stage3_segment10_ingestion_state.json`.
- Segment 10's launch is not checkpoint validation, inference, scoring, model quality, or overall Stage-3 completion. No later segment is authorized by this record. Do not infer progress from loss or status alone.

## Segment 8 status revalidation — 2026-10-13

- Fresh follow-up at `2026-10-13T06:34:10.055538Z`: secondary-wrapper status again returned `COMPLETE`; reuse of the canonical output again passed local exact-step-514 validation with zero transport attempts. Manifest remains `7857ddce5ff6efe5e61175fa63172a0c9c42a6f458212c742104a3ff4f4de189`; pinned ancestry, source/data-selection checks, 12 checkpoint files and eight retained-best files pass. Evaluation records remain only steps 187 (`0.19342221319675446`) and 374 (`0.20272061228752136`); retained-best adapter hash remains `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814`. No output download occurred. New receipt is in the root workspace at `ai/out/iron-chef-lead-v99/action-receipts/20261013T063410.055538Z-segment8_status_revalidated.json`.
- No inference, scoring, model-quality, or overall Stage-3 completion claim follows. Segment 9 remains unprepared and unauthorized; await human direction.

- At `2026-10-13T05:59:11.437826Z`, the separately vaulted `tnvtrung` wrapper invoked the official Kaggle CLI and returned Segment 8 version 1 `COMPLETE`. The already downloaded canonical output was revalidated locally at exact step 514, `STAGE3_SEGMENT_8_COMPLETE`, with the 12-file checkpoint inventory, eight-file retained-best inventory and manifest SHA-256 `7857ddce5ff6efe5e61175fa63172a0c9c42a6f458212c742104a3ff4f4de189`; no new output download was attempted.
- The only evaluation records remain the unchanged step-187 and step-374 evaluations. This observation establishes status and artifact/provenance continuity only—not inference, scoring, model quality or overall Stage-3 completion. Segment 9 remains unprepared and unauthorized pending human direction.
- Durable receipt: [`segment8_status_revalidated.json`](../ai/out/iron-chef-lead-v99/action-receipts/20261013T055911.437826Z-segment8_status_revalidated.json). Canonical details: [`SESSION.md`](../SESSION.md) and [`academic_vision_and_strategy.md`](../academic_vision_and_strategy.md).

## L42 prior-art lead — 2026-10-13

- Higher-priority adjacent lead: Gim & Ko's NutriHeal SSRN abstract describes graph-derived, validation-locked confidence gates for selective nutrition-recipe verification under incomplete nutrient evidence; the associated Zenodo record describes dish–goal data and leakage-exclusion fields. A linked 2026 KCI article by the same authors describes uncertain-item handling in Top-N recipe serving. Abstract/catalog metadata only: SSRN PDF delivery returned 403 and KCI timed out; author-reported metrics are not reproduced. This materially narrows broad “selective recipe recommendation under missing evidence is absent” claims but does not establish ingredient-substitution selection, unseen-pair evaluation, or provenance-bearing substitute abstention. Treat SSRN/KCI as one author lineage, not independent replication; L42 and exact novelty remain open. See [SCOPE L42](../SCOPE.md), SSoT Appendix C/S37: https://papers.ssrn.com/sol3/papers.cfm?abstract_id=6928681 ; https://zenodo.org/records/20149489 ; https://doi.org/10.13088/jiis.2026.32.1.351.
- Deeper ordinary OpenReview search-index excerpts from the same anonymous ICLR 2026 SpheriQ submission describe four generic recommendation/reasoning datasets, 80/10/10 splits, validation-grid tuning, five fixed seeds, ECE/Brier/AURC, risk–coverage, selective NDCG@10-versus-coverage, and an MIND confidence-threshold abstention case study. These are indexed author-submitted claims, not reproduced findings or a full-text screen.
- This is direct-adjacent prior art for broad generic selective-recommendation/risk–coverage absence claims, not the frozen ingredient-substitution task. It does not establish conformal/finitesample guarantees, unseen-pair substitution evaluation, evidence-missingness strata, or provenance-bearing substitute abstention. Direct PDF/forum access remains challenge-blocked; no bytes/hash, authors, current decision, complete protocol review, or independent adjudication. L42 remains open; exact novelty remains UNKNOWN. See [SCOPE L42](../SCOPE.md) and canonical Appendix C/S36.

## LEAD v115 — Segment 8 artifact boundary verified (2026-10-13)

- One official secondary-account Kaggle output transaction validated Segment 8 at exact step 514 and terminal state `STAGE3_SEGMENT_8_COMPLETE`: the exact 12-file checkpoint inventory/hashes, pinned Segment-7→1 and Stage-2 ancestry, frozen source and selection identities, manifest integrity, and retained-best continuity all passed.
- Segment-8 manifest SHA-256: `7857ddce5ff6efe5e61175fa63172a0c9c42a6f458212c742104a3ff4f4de189`; normalized launched-source SHA-256: `92c25e0daa6886e75ffd0360e10a543fa0a6467fc6a7b0cb71c5dc5a611bd176`. Manifest and trainer state contain only the unchanged step-187 (`0.19342221319675446`) and step-374 (`0.20272061228752136`) evaluations; step 514 adds no evaluation. The eight-file retained-best inventory matches Segment 7, including adapter model SHA-256 `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814` and best loss `0.19342221319675446`.
- Root evidence: `ai/out/iron-chef-lead-v99/secondary_stage3_segment8_advancement.json`; verified output: `ai/out/iron-chef-lead-v99/segment-8-output/`. Artifact/provenance validation does not establish inference, scoring, model quality, or overall Stage-3 completion. Segment 9 is not prepared or launched; its identity/package contract awaits human direction.

## LEAD v114 — Segment 7 verified; Segment 8 launched (2026-10-12)

- **L44 progress boundary:** one official secondary-account Kaggle transaction downloaded and validated Segment 7 at exact step 444, terminal state `STAGE3_SEGMENT_7_COMPLETE`, exact 12-file checkpoint inventory/hashes, recursively pinned Segment-6→1 and Stage-2 ancestry, frozen source/data selections, and manifest integrity. Segment-7 manifest SHA-256: `d3406733ca38209df9d25b7dd1883cc20a2659611573588f7c32d27690d15481`.
- The manifest preserves exactly the unchanged step-187 (`0.19342221319675446`) and step-374 (`0.20272061228752136`) evaluation entries. The eight-file retained-best adapter and all hashes remain identical to Segment 6; model SHA-256 is `d7da622deff97e1a2c58e627321cb7bd868e554adf93ffbfe9bcf2f4cfa2b814`.
- Only after those gates passed, Segment 8 was built for `tnvtrung` with the exact Segment-7 manifest pin, the same private bridge dataset, and Segment 7 as sole kernel predecessor. Package validation passed; one official-CLI launch was positively acknowledged as version 1. Script SHA-256 `2c97fce2424b992cd76a65c1c40de283847a805ff896145b23d14d782d825af7`; metadata SHA-256 `375a941232b12fa343a4e89eb34ac32c9f27884ac62b41ee39c8d56d8c840f88`. Latest secondary status observed `RUNNING` between `2026-10-12T11:36:38Z` and `11:36:40Z` toward step 514; no Segment-8 output has been validated.
- Evidence: root [`secondary_stage3_segment7_advancement.json`](../ai/out/iron-chef-lead-v99/secondary_stage3_segment7_advancement.json), transactional-ingestion and launch receipts under `ai/out/iron-chef-lead-v99/action-receipts/`. Stage-3 completion, inference, scoring, and model quality remain unproven; loss/evaluation/status are not quality evidence.

## Verified execution update — 2026-10-12

- **L13′ COMPLETE — scope edit only.** The former new-GNN backlog wording is replaced by a shift-aware selector over frozen scorers, with its method and guarantee boundary in SSoT [§8](../academic_vision_and_strategy.md#s8). This does **not** close scientific work package L13: no accepted model is established.
- **L37 COMPLETE — deterministic strata and benchmark-conditional top-1 intervals.** The frozen v26 evaluator was independently reconstructed (`result_validation_report.json` PASS); the supplemental 2,000-replicate recipe-cluster bootstrap reports Hit@1 and 95% percentile intervals for overall, repeated/unseen, and four evidence-missingness strata. Test denominator is 10,747; repeated/unseen is 6,866/3,881. This is conditional uncertainty for the fixed benchmark, not a population/exchangeability guarantee. Optional endpoint-frequency split was not run. Evidence: [`l37_cluster_intervals.json`](../ai/out/iron-chef-lead-v102/l37_cluster_intervals.json), SHA-256 `351230465cbf51a41378f42fd1c12e21ea936ebfa2bd616c31d76572460c9283`, deterministically reproduced byte-for-byte; generator SHA-256 `c26eed23b3c11c9bd989c938310150511e518a8ea3f57d4a08fd8610c5464ccb`; frozen evaluation SHA-256 `293f0c9952d98d389bb3b7d23ee4f1f04b6ad675914e361de9102806f5539ea8`; independent reconstruction report SHA-256 `45492d30acdf46ef26fb443140a8bec2adf1c01e12e8e1049112a27029f217d5`.
- **L42 advanced, not closed.** Official CiST program evidence, the hash-recorded full arXiv:2409.08792 text, a citation-chased ingredient-substitution survey plus Shirai et al. DIISH full text, full-text screens of KDD 2025 LPShift and the NeurIPS 2023 link-prediction evaluation paper, and an author-repository screen of conformal LLM recipe-rating work now bound direct versus adjacent prior art more precisely. The HUMMUS rating-interval lead challenges broad “first conformal prediction for LLM recipe recommendation” language, but is not ingredient-substitution selection, unseen-pair Hit@1, or selective abstention; paper acceptance/full text remain unverified. The graph papers support benchmark-rigor concerns, not ingredient or calibrated-selective-prediction evidence; the exact calibrated-selective candidate remains `UNKNOWN`. The survey’s generic query and 28-work snowball are not a complete search, its omission of separately known Senath work demonstrates a coverage limit, and independent adjudication remains absent. IEEE full text for the role-aware paper, complete hashed query/citation funnel, formal coding, and independent rubric adjudication remain missing. See [SCOPE follow-up](../SCOPE.md#l42-role-aware-followup); no novelty claim is accepted.
- A 2026 *Electronics* paper reports allergen-aware multi-objective **recipe ranking** with a substitution bonus (Wang & Wang, DOI `10.3390/electronics15122628`). Its official 25-page publisher PDF is now screened (SHA-256 `98D9550151509D61B28E8A07807361653BD4C449A2CAC15A647BE33B67D83413`): the displayed algorithm ranks recipes and does not specify ingredient-substitute generation/selection or a validated substitute-evidence procedure. Authors report N=20 home cooks from one recipe source, retroactive replay for external baselines, and no clinical validation; reported metrics were not reproduced. This is direct-adjacent ranking/scoring prior art, not the exact selective ingredient-substitution claim. See root SCOPE L42; formal coding/adjudication and L42 remain open.
- The publisher HTML returned HTTP 429, but the official static PDF was retrieved and screened; acquisition hash and sampled-page visual review are recorded in root SCOPE. The author-affiliated Yamaguchi University publication list identifies an earlier IMCOM 2026 version (Wang, Ding & Wang, pp. 1–6); Crossref now identifies DOI `10.1109/imcom69009.2026.11360864` with matching bibliographic metadata. OpenAlex indexes an abstract describing an allergen-safety score involving ingredient substitution, nutrition/NIMA scores, and weighted recipe ranking; it is abstract/catalog evidence only, not a full-text screen. Its record reports closed access/no repository full text, not proof no copy exists. IEEE Xplore returned HTTP 202 with an empty body, so conference full text remains unreviewed. Treat journal/conference versions as one linked prior-work line, not independent reviews.
- **Segment 7 status boundary:** the separately vaulted secondary Kaggle wrapper observed `RUNNING` at `2026-10-12T10:50:21Z`. The earlier default-CLI HTTP 403 is credential-path-specific, not evidence that this secondary path is inaccessible. This is status-only: no Segment-7 output download/validation or step-444 verification, and no Segment-8 build/launch.

## LEAD v107 execution update — secondary ingestion transactionalized (2026-10-08)

Secondary Stage-3 ingestion is now one fail-closed operation. It performs no transport for `QUEUED`/`RUNNING`, receipts terminal blockers, permits exactly one isolated attempt after `COMPLETE`, preserves failed bytes and hashes, refuses invalid canonical evidence, revalidates valid canonical evidence, and promotes only after the full segmented validator passes.

Nine focused tests pass and the combined ingestion/successor regression is 16/16, including post-promotion receipt-failure truth. A live authenticated Segment-5 probe returned `RUNNING`, action `NONE`, zero transport, and zero receipts. This checks off ingestion-boundary hardening only; exact step 327, later evaluation, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v106 execution update — secondary successor construction hardened (2026-10-08)

Secondary Stage-3 successor construction is now transactional and evidence-derived. It recursively validates predecessor artifacts and exact manifest identity, binds prior local source to the runtime manifest, rechecks frozen Stage-2/boundary/evaluation/exact-resume contracts, stages in isolation, validates full script and Kaggle metadata, and promotes only after every gate passes. It refuses existing-package overwrite.

Seven focused tests reject validator identity, runtime source, upstream pin, kernel predecessor, overwrite, and compile-failure/partial-publication paths. The live Segment-5 package independently revalidates exact launched script/metadata hashes and remains `RUNNING` toward step 327. This checks off successor-construction hardening only; remote step 327, later evaluation, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v105 execution update — secondary Stage 3 step 257 verified (2026-10-07)

Secondary Segment 4 is checked off as transactionally verified at exact step 257/561. Terminal state is `STAGE3_SEGMENT_4_COMPLETE`; manifest SHA-256 is `aa9e6f98...0fd9`; all 12 required checkpoint files/hashes reconstruct with exact Segment-3/Segment-2/Segment-1 and Stage-2 ancestry plus frozen data selections. The sole evaluation remains the frozen step-187 record and all eight retained-best files are byte-identical to Segment 3; no evaluation or best adapter was substituted.

Only after those gates passed, Segment 5 was derived with exact step-257 pin `aa9e6f98...0fd9`, the same private bridge dataset, and Segment 4 as its sole kernel predecessor. Prelaunch lookup proved absence; Kaggle acknowledged version 1; pulled source/dependencies reconstruct; authenticated state is `RUNNING` toward step 327. Step 327, the step-374 evaluation, remaining Stage-3 segments, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v104 execution update — secondary Stage 3 step 187 verified (2026-10-07)

New explicit human direction authorized one fresh isolated Segment-3 transfer after v103 preserved the failed bytes. The new transport validated before promotion: terminal `STAGE3_SEGMENT_3_COMPLETE`, exact step 187/561, manifest `dd974c74...efef`, all 12 checkpoint files/hashes, exact Segment-2 and Stage-2 ancestry, frozen selections, the first frozen evaluation, and all eight retained-best files. The prior partial directory remains preserved and rejected.

Segment 4 pins exact manifest `dd974c74...efef`, retains the private verified-input dataset, and names Segment 3 as its sole kernel predecessor. Complete owner inventory proved absence; official Kaggle CLI acknowledged version 1; pulled remote source and dependencies reconstruct; authenticated state is `RUNNING` toward step 257. Eval loss `0.19342221319675446` is telemetry, not model-quality evidence. Step 257, remaining Stage-3 segments, final adapter, inference, scoring, and quality remain unchecked.

---

## LEAD v103 blocked update — Segment 3 incomplete transport (2026-10-07)

Secondary Segment 3 reports remote `COMPLETE`, but official-CLI transport failed with `IncompleteRead(199249616 bytes read, 169892568 more expected)`. The preserved isolated directory contains 11 partial files and 373,160,325 bytes: eight apparent retained-adapter files, only three checkpoint files, a zero-byte checkpoint adapter, and no segment manifest. Canonical evidence was not created or overwritten.

No retry was made, no Segment-3 validation claim was emitted, and Segment 4 was neither built nor launched. Stage-3 remains verified only through exact step 140/561. Step 187 evaluation, retained-best integrity, Segment-3 ancestry reconstruction, remaining Stage-3 execution, inference, scoring, and model quality remain blocked pending a separately authorized complete transport attempt.

---

## LEAD v102 execution update — secondary Stage 3 step 140 verified (2026-10-06)

Secondary Segment 2 is checked off as transactionally verified at exact step 140/561. Terminal state is `STAGE3_SEGMENT_2_COMPLETE`; manifest SHA-256 is `2324c21e...03bd`; all 12 required checkpoint files and hashes reconstruct with exact Segment-1 manifest `ed1e3db...d991d`, Stage-2 manifest `435f02e7...6b72`, frozen base revision, data hashes, and selection hashes. Pulled remote source matches the launched source after LF normalization.

Only after those gates passed, secondary Segment 3 was built with exact step-140 pin `2324c21e...03bd`, the same private bridge dataset, and Segment 2 as its sole kernel predecessor. Complete owner inventory proved absence; the official current Kaggle CLI positively acknowledged version 1; pulled remote source and dependency metadata reconstruct; authenticated state is `RUNNING` toward the first evaluation boundary at step 187. Step 187, retained-best adapter integrity, remaining Stage-3 segments, inference, scoring, and model quality remain unchecked.

---

## LEAD v101 execution update — OFF taxonomy risk partition (2026-10-02)

The complete OFF snapshot's 3,377-tag union is now partitioned against hash-pinned official taxonomy bytes. Twenty-three exact positive identities cover 760,877/767,780 occurrences (`99.100914%`); official `en:none` is isolated at 564 occurrences; 17 same-language alias candidates cover 32 and remain review-only; 3,336 unrecognized values dominate identity count but cover only 6,307 occurrences (`0.821459%`). The queue is occurrence-ranked and binds snapshot manifest `5caa20cf...1dde`, derived snapshot `807fe58e...9c68`, and taxonomy `6fb38bfb...d153f`.

Every tag and occurrence reconstructs; 8/8 semantic substitutions reject and six focused tests pass. A v100 precursor remains rejected for incomplete Unicode-ligature normalization; v101 repairs it. This checks off deterministic risk partitioning only. Current-taxonomy timing differs from the frozen snapshot, and alias acceptance, qualified adjudication, verified negatives, clinical validity, production serving, legal review, and user outcomes remain unchecked.

---

## LEAD v99 execution update — secondary Stage 3 step 70 verified (2026-10-02)

Secondary Segment 1 is checked off as transactionally verified at exact step 70/561. Terminal state is `STAGE3_SEGMENT_1_COMPLETE`; manifest SHA-256 is `ed1e3db4...d991d`; all 12 required checkpoint files and hashes reconstruct with exact Stage-2 ancestry, frozen base revision, data hashes, and selection hashes. Pulled remote source is byte-identical to launched SHA-256 `d583e92f...a3af`; runtime source identity matches its LF-normalized form.

Only after those gates passed, secondary Segment 2 was pinned to exact manifest `ed1e3db4...d991d`, bound to the same private bridge dataset and Segment 1 as its sole kernel predecessor, and launched once. Complete owner inventory proved absence; Kaggle accepted version 1 with zero invalid sources and authenticated state is `QUEUED` toward step 140. Step 140, remaining Stage-3 segments, retained-best adapter integrity, inference, scoring, and model quality remain unchecked.

---

## LEAD v98 execution update — secondary Kaggle capacity activated (2026-10-01)

The new `tnvtrung` bearer credential is durably isolated from the preserved `phanphutho` legacy key under user-only ACLs and uses official Kaggle CLI 2.2.4. Because `tnvtrung` cannot read private `phanphutho` kernels, a private 11-file bridge dataset was created and remotely inventoried against exact Stage-2 manifest `435f02e7...6b72`, all eight adapter files, and frozen train/validation hashes.

Secondary Segment 1 preserves exact script SHA-256 `d583e92f...a3af`; only owner/input metadata changed. Complete owner inventory proved it absent before Kaggle accepted version 1 with zero invalid sources. Authenticated state is `RUNNING`. This checks off the governed Segment-1 launch only; exact step 70, checkpoint integrity, Stage-3 completion, inference, scoring, and model quality remain unchecked.

---

## LEAD v97 execution update — Stage 3 launch boundary hardened (2026-10-01)

The segmented Stage-3 executable now pins base script SHA-256 `88d79461...c8c6` across builder, validator, and ingester. Only the exact weekly-GPU-quota response is preserved as a retryable non-launch; malformed acknowledgements, authentication/transport failures, and other runtime errors propagate fail closed. Completed segments must reconstruct the exact expected 12-file checkpoint inventory in addition to every hash and exact-resume state.

All nine packages rebuild byte-identically, static validation remains `PASS`, 8/8 semantic attacks reject, and eight focused tests pass. Authenticated Segment 1 remains `NOT_CREATED`; no second push occurred. Stage 3, inference, scoring, and model-quality boxes remain unchecked.

---

## LEAD v96 execution update — segmented Stage 3 ready; GPU quota gate (2026-10-01)

The canceled 561-step monolith is replaced by nine exact-resume packages at `70, 140, 187, 257, 327, 374, 444, 514, 561`. The longest span is 70 steps (about 8.2 hours at the observed rate), while evaluations stay exactly at 187/374/561. Every successor binds the exact predecessor manifest and reconstructs checkpoint, optimizer, scheduler, RNG, trainer, data-selection, Stage-2, and retained-best state. Nine packages compile; 8/8 semantic attacks reject and five focused tests pass.

Kaggle rejected the authenticated Segment-1 push with `Maximum weekly GPU quota of 30.00 hours reached.` Segment 1 remains `NOT_CREATED`; this is not a failed training run or a launch. Recovery is package-ready but quota-blocked, and inference remains blocked.

---

## LEAD v95 execution update — Stage 3 canceled and inference blocked (2026-10-01)

Post-DPO SFT version 1 ended `CANCEL_ACKNOWLEDGED` with no failure message and an empty remote file inventory. The preserved log SHA-256 `a30a54f...105dc` reaches optimizer step 101/561 at `11:49:56`; the last periodic record is epoch `0.53`, loss `0.1519`. At roughly 421 seconds per step, the monolithic 561-step/three-epoch package cannot fit Kaggle's execution window and canceled before the first epoch evaluation/save boundary. No manifest, checkpoint, or Stage-3 adapter exists.

No retry was issued and inference remained cryptographically unmaterialized, so it did not launch. Stage-3 completion stays unchecked. Recovery requires a materially segmented package chain with exact optimizer/scheduler/RNG resume and retained-best semantics; the partial log is failure evidence, not model quality.

---

## LEAD v94 execution update — inference successor made fail-closed (2026-09-30)

Inference can no longer launch against a merely plausible Stage-3 artifact. Its package starts with an unmaterialized identity sentinel and refuses runtime execution. After Stage 3 completes, the transactional ingester must validate exact Stage-2 ancestry, frozen dataset/settings, and every adapter hash, atomically materialize the exact Stage-3 manifest SHA-256, rebuild the downstream manifest, and rerun package validation before one launch. Static gates pass and 17 focused tests cover identity/settings substitution, transactional promotion, remote-state refusal, and launch acknowledgement.

Stage 3 remains running. This clears successor-provenance preparation only; completed post-DPO SFT, inference output, matched scoring, and model quality remain unchecked.

---

## LEAD v93 execution update — exact post-DPO handoff and Stage 3 running (2026-09-30)

The downstream package cluster no longer references retired DPO Segment 6. Builder, validator, Stage-3 runtime, and transactional ingester now bind exact verified step-261 manifest SHA-256 `435f02e7461992a53d1e4e4882ec7d8bb3da5a3b19d571a2164f382e050f6b72`, Segment 9, its Segment-8 dependency, and the verified v69 terminal evidence. Static validation passes seven scientific checks and 7/7 adversarial mutations; 15 focused tests pass. Kaggle accepted post-DPO SFT version 1 with zero invalid sources and authenticated state `QUEUED`.

This checks off the retained-adapter handoff and Stage-3 launch only. Stage-3 completion, adapter integrity, 10,747 frozen predictions, matched scoring, paper reproduction, and M2 model quality remain open.

---

## LEAD v92 execution update — segmented DPO complete and verified (2026-09-30)

Final Segment 9 version 3 completed and transactionally validates exact step 261/261 with 12 checkpoint files, exact Segment-8 predecessor pin `54ea62d6...c645`, final manifest `9c63d9ed...159e`, terminal state `STAGE2_COMPLETE`, and final evaluation loss `1.1670465704938238e-09`. All nine packages now reconstruct end-to-end; canceled long intervals remain preserved and rejected. This checks off segmented DPO execution only. Near-zero loss remains shortcut-confound evidence, not quality proof. Post-DPO SFT, frozen predictions, matched scoring, paper reproduction, safety, rights, and product value remain open.

---

## LEAD v91 execution update — DPO step 240 verified; final Segment 9 running (2026-09-29)

New Segment 8 version 1 completed and transactionally validates exact step 240/261 with 12 checkpoint files, exact Segment-7 predecessor pin `258a5ded...5635`, and manifest `54ea62d6...c645`. Step 240 is transport-only; the final evaluation remains at 261. Final Segment 9 was rebuilt with the exact step-240 pin and Kaggle accepted materially changed version 3 with zero invalid sources, queued toward step 261. Only 240/261 steps are verified; do not check off DPO or begin post-DPO SFT until exact step-261 artifacts, final evaluation, and adapter integrity transactionally validate.

---

## LEAD v90 execution update — shortened final recovery running (2026-09-29)

The canceled 43-step final interval is now split into `218→240→261`, adding only a transport checkpoint while preserving all optimizer steps and evaluations at 87/174/261. Verified packages 1-7 have zero content diff. The nine-package build validates `PASS`, rejects 12/12 semantic corruptions, and passes 13 focused tests. New Segment 8 version 1 was launched once with exact step-218 manifest pin `258a5ded...5635`, zero invalid sources, and authenticated state `RUNNING` toward step 240. Only 218/261 steps are verified; do not check off Segment 8 or launch the final package until exact step-240 artifacts transactionally validate.

---

## LEAD v89 execution update — final Segment 8 canceled (2026-09-29)

Final Segment 8 version 2 ended `CANCEL_ACKNOWLEDGED` with no failure message or remote files. The failed state is preserved and no retry was issued. Its 43-step 218→261 interval repeats the long-duration failure shape of earlier canceled packages, while 22/21/22-step recoveries completed. Only 218/261 steps remain verified. Exact step 261, final evaluation, final adapter, completed DPO, and model quality are absent; post-DPO SFT remains gated. Require a materially shorter validated final recovery boundary before any retry.

---

## LEAD v88 execution update — DPO step 218 verified; final segment running (2026-09-28)

Segment 7 version 1 completed and transactionally validates exact step 218/261 with 12 checkpoint files, exact Segment-6 predecessor pin `ebee8452...309e`, and manifest `258a5ded...5635`. Step 218 is transport-only; evaluations remain frozen at 87/174/261. The fully-pinned validator mutation was corrected to remain genuinely adversarial; 12/12 semantic corruptions reject and 13 focused tests pass. The final Segment 8 package was rebuilt with the exact Segment-7 pin and Kaggle accepted materially changed version 2 with zero invalid sources, queued toward step 261. Only 218/261 steps are verified; do not check off final DPO or begin post-DPO SFT until exact step-261 artifacts and evaluation transactionally validate.

---

## LEAD v87 execution update — DPO step 196 verified; Segment 7 running (2026-09-23)

Shortened Segment 6 version 2 completed and transactionally validates at exact step 196/261 with 12 checkpoint files, exact Segment-5 predecessor pin `7bbf682c...9340d`, and manifest `ebee8452...309e`. The step-196 checkpoint is transport-only; evaluations remain frozen at 87/174/261, and the last boundary loss is not model-quality evidence. Segment 7 version 1 was launched once with the exact Segment-6 manifest pin, zero invalid sources, and authenticated state `RUNNING` toward step 218. Only 196/261 steps are verified; do not check off Segment 7 or launch the final package until exact step-218 artifacts transactionally validate.

---

## LEAD v86 execution update — shortened Segment 6 recovery running (2026-09-22)

DPO Segment 6 version 1 remains preserved as canceled without artifacts. The retry changes transport boundaries only: the chain is now eight packages at `44, 87, 131, 153, 174, 196, 218, 261`, preserving all optimizer steps and evaluations at 87/174/261. Verified packages 1-5 have zero content diff. The package passes validation, rejects 12/12 semantic corruptions, and passes 13 focused tests. Kaggle accepted Segment 6 version 2 with exact step-174 manifest pin `7bbf682c...9340d`, zero invalid sources, and authenticated state `RUNNING` toward step 196. Only 174/261 steps are verified; do not check off Segment 6 or launch Segment 7 until exact step-196 artifacts transactionally validate.

---

## LEAD v85 execution update — Segment 6 canceled; shorter recovery required (2026-09-22)

DPO Segment 5 version 1 remains transactionally verified at exact step 174/261 with 12 checkpoint files, manifest `7bbf682c...9340d`, exact Segment-4 predecessor pin, and boundary evaluation loss `1.4209885490501506e-09`; near-zero loss remains shortcut-confound evidence, not quality proof. Segment 6 version 1 ended `CANCEL_ACKNOWLEDGED` with no failure message or remote files. No retry was issued. Its 44-step interval repeats the duration shape that canceled the earlier long segment; a materially shorter transport boundary is required before retry. The deterministic multi-arm allergen bridge remains ready; Gemini's configured variable is empty and OpenAI is absent, so neither result is claimed.

---

## Current Evidence Boundary (from SSoT §1)

| Area | Verified Fact | Tier |
|------|---------------|------|
| GISMo reproduction | 10,747 test: Hit@1 20.694%, Hit@3 36.010%, Hit@10 54.164%, MRR 0.31636 | E3 |
| Unseen-pair collapse | GISMo unseen Hit@1 1.206%, MRR 0.04308; Lookup overall 17.996%, unseen 0.000% | E3 |
| Corrected SAG | Mean Hit@1 0.1171% vs semantic 0.3964%, naive fusion 0.3333% | E3 NEGATIVE |
| True-context dual encoder | 16.1223% overall Hit@1 vs GISMo 20.6941%; unseen MRR 0.016601 vs 0.043084 | E3 NEGATIVE |
| CLIP projection | R@1 34.10% → 35.42%, missed +2pp gate | E2 NEGATIVE |
| Flavor pairing | 2,231,142 RecipeNLG rows, p=0.238095, not supported | VERIFIED NEGATIVE |
| FooDB service | 971 foods, 206 dimensions, 125,992 links; 471,906 profile pairs, 0 mismatches | VERIFIED |
| FooDB reach | Both endpoints: 1,162/10,747 (10.81%); unique endpoints 260/2,650 (9.81%) | MEASURED |
| OFF concordance | 400/408 declared groups (98.04%, Wilson 96.18–99.00%); trace 22/40 (55%) | PARTIAL E2 |
| OFF partition | 23 identities cover 760,877/767,780 tag occurrences (99.10%) | E3 |
| Allergen protocol | 208 cases, 13 groups, direct/trace/multi/cross-contact; Mistral raw 208/208 | PARTIAL E2 |
| Mistral Stage 3 | Verified through 374/561; Segment 7 launched, RUNNING toward 444 | PARTIAL E3 |
| Novelty reviews | v8/v9: broad claims rejected; exhaustive re-attack open | PARTIAL |

**No accepted IRON CHEF model exists.** No allergen comparison result (GPT-4o/Gemini unrun, no adjudication). No human usefulness evidence. Stage 3 incomplete.

---

## The Five Contributions (C1–C5 from SSoT §3)

| ID | Contribution | Floor | Target | Stretch |
|----|--------------|-------|--------|---------|
| **C1** | **Instrument**: strata × allergen cases × valid@k audit | 208 allergen cases + Recipe1MSubs strata + 150-case 2-rater audit | + Vietnamese pilot (~100 cases, 2 native raters) | Public release with datasheet |
| **C2** | **Measurement campaign** across arms | GISMo + lookup + one API LLM | + Mistral Stage 3 + second API LLM; intervals everywhere | Contamination-aware 2×2 (public/private × Western/Vietnamese) |
| **C3** | **Guardrail**: hard allergen filter + per-cell conformal sets + abstention | Frozen protocol, honest run, cells reported | Informative sets in ≥1 cell; vacuous cells shown | Fewer allergen violations than raw arms with intervals |
| **C4** | **Human evidence** | 2-reviewer pass on audit pilot, κ (kappa) reported with cluster-level interval | LLM judge calibrated to humans (κ ≥ 0.6; kappa ≥ 0.6) | Usefulness study N ≥ 20 (N >= 20; ethics permitting) |
| **C5** | **Product proof** | Real-data Explorer + evidence endpoint + visible abstention | Live side-by-side demo vs raw LLM on same inputs | Reusable component |

**Bounded negatives (no further spend):** SAG, dual encoder, flavor pairing, CLIP projection, VLM artifact, behavioral overlay, voice copilot. One thesis chapter: *What did not work, and what each failure taught about the evaluation.*

**L13 re-scoped:** from "train new GNN" → "shift-aware selector over frozen scorers" (SSoT §3.5).

---

## Acceptance Gates per Contribution (SSoT §3.2)

| ID | Gate (all binary, checkable) |
|----|------------------------------|
| C1 | Instrument schema validated; strata files regenerate bit-for-bit from official splits; audit pilot labels for ≥150 cases exist with two independent raters and adjudication log; κ reported with case-level interval |
| C2 | Every arm evaluated on identical case list, identical normalization, identical scoring script; each number in a table has an interval; arms with unmatched-output rate >5% flagged |
| C3 | Protocol frozen and hashed before test access; calibration only on validation; per-cell status table (ok / vacuous / no_guarantee); cost-of-trust figure from frozen run |
| C4 | Rater agreement computed per §6.8 with cluster-level interval; judge-vs-human agreement per §7.5; if κ/kappa < 0.6 twice (kappa < 0.6 twice), fallback applied and stated |
| C5 | Explorer loads from hash-pinned graph artifact; demo runs from clean checkout without network except API arms; abstention state reachable and tested |

---

## Work Packages (from SSoT §17.1 — Lead Items)

| ID | Deliverable / Acceptance | Dependencies | Effort | Fallback |
|----|--------------------------|--------------|--------|----------|
| **L13′** | Re-scope to shift-aware selector over frozen scorers; edited item text + link to §8 | None | 1h | None |
| **L36** | 150-case audit: all independent labels/adjudication/cluster intervals/H1 table | Raters (L41), pools (M-B), PR01–03 resolved | 30–40h | Shrink to 100 cases |
| **L37** | Deterministic strata and interval-reported metrics; optional endpoint-frequency split | Identity/counts (M-B) | 10h | Drop frequency sub-split |
| **L38** | Frozen calibration, all-cell status, diagnostics, cost-of-trust figure | Audit/judge/arms, PR04–08 resolved | 25h | Report only ok cells; label rest |
| **L39** | Matched adjudicated allergen outputs; expansion to n≥299 or honestly bounded interval | API keys, reviewers (L41), rights | 30h | Report interval at n=208 |
| **L40** | Frozen ~100-case native-reviewed Vietnamese pilot | Authors/raters/rights (L41), U8, U9 | 30h | Qualitative appendix only |
| **L41** | Written ethics outcome; named raters; handbook + T0/T1 results | Human dependencies | 12h | Fallbacks per §14.8 |
| **L42** | Hashed novelty funnel; full-text/citation chase; independent adjudication | Sources, reviewer | 15h | Narrow claims |
| **L43** | Frozen judge prompt + evaluated gates, or explicit unusable outcome | Leak-free human comparison (L36) | 12h | Humans only on smaller set |
| **L44** | Validated Stage 3; 200-case timing; matched ranked outputs for val/test subsamples | Quota, adapter, arm construction | 12h + waiting | Stage-2 adapter arm |
| **L45** | 14 evidence-bound thesis chapters (§21) | All results/claims | 80+h | Cut by cut line (§3.6) |
| **L46** | Source-bound defense answers; timed human rehearsal; zero forbidden claims | Thesis (L45) | 15h | None |
| **L47** | ARR paper draft per §23 | Evidence + venue check | 40h | Thesis only |
| **L48** | Integrated strategy + completed claims register | This migration completes strategy | 10h | None |

**Critical dependency path (SSoT §17.4):**
```
L41 (raters/ethics) → L36 (audit) → L43 (judge) → L38 (calibration) → L45 (writing) → L46 (defense)
         ↑                              ↑
    M-B (pools)                   M-D (arm outputs) ← L44 (inference)
```
**L41 → L36 → L43 → L38 is the critical path.** Everything on it waits on human availability, not compute.

---

## Schedule & Capacity (from SSoT §18)

**Today:** 2026-10-12. Defense assumed on/after Dec 8 (U3). All dates are proposals `[P]`.

| Week | Dates | Lead Focus | Member Focus | Gates / Freezes |
|------|-------|------------|--------------|-----------------|
| **W0** | Oct 8–11 | Answer U1–U3, U7, U11 (hours, advisor, defense, quota, ethics). Back up SSoT. Start asking raters. | Start M-A, M-F | **Decision: compression level** |
| **W1** | Oct 12–18 | L48 first pass. L42 query runs. Read Rita §3 + Fatemi data (U4). Check val scores + unseen share (U5). Draft rater handbook. | M-A recompute; M-B strata/identity; M-D GISMo/lookup outputs; M-F inventory | **Oct 15: two raters named. Ethics answer by Oct 12** |
| **W2** | Oct 19–25 | Obtain API keys; run API LLM #1 on 150 audit cases. Rater T0/T1. Draft allergen expansion. Write Ch2/Ch3 drafts. | Pool builder fixture test → real run. M-I runner fixture test. | **Oct 22: FREEZE A** (audit protocol, Δmin, arms, normalization). **Oct 23: Vietnamese raters/authors named, licenses checked** |
| **W3** | Oct 26–Nov 1 | Audit labeling. Judge prompt dry run. 200-case timing (by Oct 29). Allergen cases frozen/hashed. | M-C Explorer build; M-G register paths/hashes | **Oct 29: allergen reviewers named; timing done. Nov 1: profiles complete** |
| **W4** | Nov 2–8 | Adjudicate audit; κ with cluster CI; first H1 table. Judge calibration (L43). Allergen arms (GPT-4o, Gemini). Stage 3 check. | M-C Explorer T-03–T-09; M-E demo skeleton | **Nov 6: FREEZE B** (conformal protocol, §8.9) before any test coverage. Advisor interim. |
| **W5** | Nov 9–15 | Judge-label val pools; per-cell calibration (rank arms); diagnostics §8.12; test-subsample eval. Vietnamese freeze. H4 prep if ethics. | M-E recorded Act 3 from frozen files | **Nov 10: usefulness participants recruited (or dropped)** |
| **W6** | Nov 16–22 | Mistral inference + calibration (or Stage-2 adapter). Allergen adjudication. Vietnamese labeling. Cost-of-trust. | Explorer polish T-10–T-12 | **Nov 20: EXPERIMENT FREEZE.** No new runs. Analysis/writing only. |
| **W7** | Nov 23–29 | Red-team claims (§24.2). Rebuild defense answers (§22). Chapters Ch5–Ch9. | M-H reproducibility kit; clean-machine reproduction | **Nov 27: register script exits 0; zero forbidden claims** |
| **W8** | Nov 30–Dec 6 | Write Ch1, Ch4, Ch11, Ch13, Ch14. Defense dry run 1. | Demo hardening | **Dec 1: ARR service-contributor decision** |
| **W9** | Dec 7–13 | **Dec 8: thesis freeze.** Start ARR draft from thesis artifacts. | Support | Thesis frozen |
| **W10–12** | Dec 14–Jan 3 | ARR v1; internal review. ARR v2; limitations/datasheet. Final read, hashes. | Figures/tables regenerate. | Service-contributor registration ready (W10, W11, W12) |
| **W13** | **Jan 4, 2027** | **ARR submission (AoE)** | | Registration form open until 48h post-deadline |

**Capacity arithmetic (SSoT §18.1):** 225 MUST + 32 SHOULD + 70 COULD = 327 lead hours; 91 member hours. 8 weeks at 30h/week = 240h (short 87h). At 40h/week = 320h (fits). **Run arithmetic with real hours on Oct 12 (U1), pick compression level before starting.**

**Compression protocol (SSoT §18.4):** Level 0 (full), Level 1 (drop COULD except 20-case Vietnamese appendix), Level 2 (GISMo/lookup only calibration, no judge, n=208 allergen, minimal Explorer), Level 3 (collapse map + 100-case audit + allergen interval + bounded negatives + claims register). Gates never weakened.

---

## Risks & Pre-mortem (from SSoT §19)

| ID | Risk | Likelihood | Impact | Early Warning | Mitigation |
|----|------|------------|--------|---------------|------------|
| K1 | Raters not found | M | H | <2 named by Oct 15 | Ask earlier; shrink to 100; label exploratory |
| K2 | Rater agreement too low (κ/kappa < 0.5 twice; kappa < 0.6 twice review trigger) | M | H | T1 failure | Rewrite handbook; negative anchors; shrink to audited subset |
| K3 | H1 inconclusive (CI straddles Δmin) | M | M | CI straddles 15pp | Extend to 300 only if time; report inconclusive |
| K4 | Judge fails gates (§7.5) | M | M | Gates fail | Humans-only on smaller calibration; state power loss |
| K5 | All cells vacuous | M | M | Early GISMo diagnostics | Report quantified limit; policy = abstain-by-default |
| K6 | Calibration/test not exchangeable | M | H | KS diagnostic on gold rank | Merge cells or weighted conformal; state limit |
| K7 | Validation scores missing | M | H | M-B check fails | Regenerate GISMo val (cheap); LLM arms on subsample |
| K8 | Stage 3 late/fails | M | M | No 327 by Nov 1 | **OBSOLETE — 327 recorded verified.** Stage-2 adapter hedge exists |
| K9 | Inference too slow | M | M | 200-case timing | Stratified subsample (§13.3) |
| K10 | API keys/budget unavailable | M | H | No key by Oct 22 | Use free/open-weight LLM; pin version; say so |
| K11 | Ethics blocks usefulness study | M | L | No answer by Oct 12 | Drop H4 |
| K12 | Allergen n too small to certify | H | M | Arithmetic fixed | Expand to ≥299 or report interval |
| K13 | Claim breaks under review | M | H | Red-team finds forbidden | §24.2 audit; claims register |
| K14 | Novelty duplicate found | L–M | H | L42 search | Re-scope C1 to Vietnamese+allergen+shift combo |
| K15 | Advisor says scope mismatch | L–M | H | U2 reply | Re-scope before more spend |
| K16 | No qualified ARR contributor | M | M | None by Dec 1 | Thesis only or later cycle |
| K17 | Licenses block release | M | L–M | M-F, §10.2 | Release code/aggregates only |
| K18 | Scope creep | H | H | >MUST tier in progress | Cut from bottom, weekly |
| K19 | Overnight job overspend | L | M | Receipt > cap | Per-job caps; refuse-to-start |
| K20 | Teammate unavailable | M | M | Tasks slip 2 weeks | Reassign M-A, M-B to Lead; compress |
| K21 | R0 numbers leak into thesis | M | H | Figure without claim ID | Register rule (§15.2) |
| K22 | Non-thesis workload | H | H | Weekly hours below plan | Compression level (§18.4) |

**Pre-mortem triggers (SSoT §19.3 — reopen architecture only on these):**
- H1 lower Wilson bound ≥ Δmin → lead with benchmark validity
- H1 upper bound < Δmin → lead with collapse map + abstention
- H1 straddles Δmin → report inconclusive; extend only if time
- κ < 0.6 twice → rewrite handbook once; shrink to audited subset
- All cells vacuous → reframe C3 as quantified limit
- Novelty duplicate → re-scope C1
- Advisor mismatch → re-scope before spend
- Exchangeability diagnostic fails → weighted conformal or merge; state limit
- No ARR contributor by Dec 1 → thesis only

---

## Protocol Resolutions (from SSoT §16 — must resolve before affected work)

| Flag | Conflict | Resolution Required |
|------|----------|---------------------|
| PR01 | §6.1 conditional rescue vs §6.9 joint rescue; > vs ≥ Δmin | Choose estimand, denominator, operator prospectively |
| PR02 | Gold forced A yet both-rater N = label noise | Specify raw/adjudicated/gold policy; implications for V≥H |
| PR03 | Equal allocation vs population inference; top-4 vs top-5 | Resolve allocation, weighting, stratum minima, consistent k |
| PR04 | Cell uses gold substitute (unavailable at deployment) | Separate descriptive oracle strata from deployable partition |
| PR05 | Judge chosen on test audit; claim freeze before test | Establish dev/audit/cal/eval separation; disclose exposure |
| PR06 | Filter/rerank/truncation/empty-pool mismatch; infinity handling | Define returned-set success, filter ordering, guarantee population |
| PR07 | KS called exchangeability check; test-driven merging | Diagnostics cannot prove assumption; freeze variants or label exploratory |
| PR08 | Coverage grid .50–.95 vs α={.05,.1,.2,.3,.5} | Freeze single grid and status semantics |
| PR09 | Lossy normalization vs v23 collision findings | Validate identity/food-form behavior before freeze |
| PR10 | Different κ point/lower-bound gates; Wilson vs exact | State each gate's population/statistic/interval method |
| PR11 | Train-only graph vs "no val/test pair" despite seen overlap | Resolve row provenance leakage test without changing splits |
| PR12 | Endpoint example seen=true/count=0/unseen simultaneously | Replace only via validated contract design |
| PR13 | Annotation pending ethics vs clearance-before-study | Obtain institutional authorization; prep continues |
| PR14 | Seed placeholders marked VERIFIED; checker accepts dummy | Implement real artifact validation separately |
| PR15 | Synthetic tests omit bounded-M/infinite/missing-cell | Independent validation before reuse |

---

## Operating Rhythm (from SSoT §24)

- **Daily (10 min):** Morning review of overnight jobs (§13.8)
- **Monday (30 min):** Claims register diff; overnight receipts; plan week against cut line
- **Wednesday (15 min):** Check gates due; chase people
- **Friday (30 min):** Update status block (generated); write one thesis paragraph from week's evidence; decide cuts
- **Biweekly advisor update (5 lines):** Evidence IDs, gates moved, plan changes/trigger, precise request, next action

**Freeze A (Oct 22):** Audit protocol, Δmin, strict/lenient, arms, blinding, normalization, primary arm/stratum, κ bar, timestamp/hash.

**Freeze B (Nov 6):** Deployable cells, n floor, M, α grid, labeling function/selection, filter/rank/set semantics, infinity/missing, informativeness, diagnostics/sensitivities, evaluation access. **Neither freeze completed here — resolve §16 first.**

**Forbidden claims (§24.2 — red-team every sentence):** No "zero/<1% violations" without n≥299; no "safe/safer/clinical"; no "human-valid coverage" from judge; no "first/novel" before 4.6+second reviewer; no Vietnamese guarantee; no "measured contamination"; no R0 figures without reconstruction; no cross-task rankings; no "four trained models/production-ready AI"; no "zero cost/local-only" without runtime proof; no user outcome claims; no untraceable numbers; no "calibrated" without cell/label/assumption (never claim "calibrated without" its cell, label, or assumption); no "hypothesis confirmed" when interval includes null.

---

## Evidence Integrity Rules

- Never mark COMPLETE because code was written — require executable evidence (checkpoint, hash-bound predictions, scored evaluation).
- Never compare cancelled run or paper number to locally executed result.
- Never report metric without dataset, split, protocol version, seed count.
- Negative results (SAG, dual encoder, CLIP, flavor, VLM, behavioral, voice) are thesis contributions — report honestly.
- Do NOT cite: 22.04% Senath (unverified journal abstract); 0.2222 HGAT (synthetic); 0.00% allergen (self-labeled, withdrawn).
- Every number in thesis carries a claim ID from register (§15). Unverified = say so.

---

## Appendix: Mapping from v3 Backlog (for reference only)

| v3 Epic | v4 Treatment |
|---------|--------------|
| EPIC 3/3b/3c (M1 GNN/SAG/dual encoder) | Bounded negatives chapter; L13′ = shift-aware selector |
| EPIC 10 (M2 Mistral Stage 1–3) | L44 (Stage 3 completion + inference); Stage-2 adapter hedge |
| EPIC 11 (M3 VLM) | Bounded negative; no further spend |
| EPIC 12 (M4 CLIP) | Bounded negative; no further spend |
| EPIC 5/5b (Allergen) | L39 (matched arms + expansion + adjudication) |
| EPIC 4 (Compound explanation) | C4/H4 (usefulness study if ethics) |
| EPIC 9 (Thesis 14 chapters) | L45 (14 v4 chapters per §21) |
| L29 (Photo chain) | Bounded negative; §12 product proof replaces |
| L31 (AI runtime cert) | Separate infra work; not thesis contribution |
| L35 (Defense pack) | L46 (rebuilt answers + rehearsal) |
