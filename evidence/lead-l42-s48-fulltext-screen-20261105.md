# L42 source screen: Vandeputte et al. (2023), S48

Date screened: 2026-11-05. Scope: one targeted full-text comparison against the proposed human candidate-validity audit. This is not a complete literature search, independent second review, or novelty conclusion.

## Source and prior record

Vandeputte et al., “Principles and Validations of an Artificial Intelligence-Based Recommender System Suggesting Acceptable Food Changes,” *The Journal of Nutrition* 153(2) (2023), 598–604, DOI `10.1016/j.tjnut.2022.12.022`, HAL record `hal-03975185v1`. The strategy's S48 entry previously relied on the PubMed abstract and stated that publisher full text had not been retrieved. This screen used the author-affiliated HAL full-text deposit; it did not download code or data. The publisher full-text URL returned HTTP 403 to the browser reader, so the HAL deposit is the screened full text, not a claim of publisher-hosted access.

Sources:

- [Author-affiliated HAL full text](https://hal.science/hal-03975185/document)
- [HAL version record](https://hal.science/hal-03975185v1)
- [Official PubMed record](https://pubmed.ncbi.nlm.nih.gov/36894251/)
- [Journal full-text page](https://jn.nutrition.org/article/S0022-3166%2822%2913767-3/fulltext) (access returned 403 in this screen)

## What the article actually evaluates

The paper describes three studies over two questions:

1. **Algorithmic swap plausibility / source-identification task.** A dietary substitution method mined from the French INCA2 food-consumption database. In the online task, 255 adults saw proposals tied to the same 12 meals. Proposals came from a dietitian or from the algorithm using high (“expert”) or low (“clumsy”) substitutability scores. Participants answered whether each proposed change was made by AI or a dietitian. The measured outcome was guessed emitter identity—not a direct A/C/N/U or candidate-validity judgment. The repeated meal contexts and multiple proposal sources are relevant prior art, but the paper does not describe a common per-case candidate pool containing each method's alternatives and a recorded gold, followed by blinded per-candidate culinary-validity assessment.
2. **In-app acceptance / interaction modality.** A separate three-week smartphone study compared simultaneous choice among four options, sequential presentation with refusal/justification, and a preference-request/single-option mode. Sixty candidates enrolled; 27 participants supplied 162 complete meal interactions. Participants could accept/refuse suggestions, and the paper reports higher acceptance for the sequential user-involvement modality (OR 3.168, 95% CI 1.688–6.061, p<0.0004). This is in-app dietary-swap acceptance evidence, not a method-blinded per-candidate validity benchmark. The article reports ethics committee approval and written informed consent for this study.
3. **Scope and limitations.** The source is food/dietary swaps mined from consumption context, not ingredient replacements evaluated inside a fixed recipe. The article notes the old French consumption data and limited generalizability, and distinguishes plausibility from nutritional quality. Its results are source-reported; this screen did not reproduce them.

## Lead disposition

- **Broad claim boundary:** reject “first human evaluation of substitutions,” “first human-evaluated dietary swaps,” and “first user-acceptance study of food substitution.” S48 joins the already-screened S43/S46/S47/S49 and other sources as direct or adjacent prior art.
- **Narrow candidate design:** S48 does not, in the screened methods, establish a fixed recipe plus target ingredient, a shared pooled slate across methods plus recorded gold, hidden arm/rank at per-candidate validity judgment, culinary-function A/C/N/U labels, or the exact single-gold-rescue/unseen-identity analysis. This source therefore does not falsify that exact proposed combination.
- **Novelty/evidence status:** the combination remains a candidate differentiator with novelty `UNKNOWN`/`UNESTABLISHED`. A one-paper screen cannot establish absence elsewhere. Human-audit construction and PR01/PR03 remain open; do not treat Protocol v1's conditional-rescue/A/C/N/U pieces as if they fully freeze the audit design.
- **No downstream changes:** no protocol, experiment, claim-register status, score, milestone, contribution acceptance, product behavior, or safety claim changed. No source data, code, participant material, API, or external service was accessed.

## Review trigger

Add this source to the existing L42 funnel and have the already-required independent adjudicator code it under the frozen DIRECT/ADJACENT rules. Reopen this disposition only if the full text, supplement, or linked implementation is shown to implement the exact shared-pool/per-candidate recipe-context audit, or if a source/version correction materially changes the described study.
