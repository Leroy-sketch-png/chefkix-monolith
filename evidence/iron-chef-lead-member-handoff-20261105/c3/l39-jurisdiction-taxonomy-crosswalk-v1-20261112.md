# L39 frozen-group jurisdiction crosswalk v1

Status: **Lead-adopted metadata crosswalk** for the 13 labels in the frozen 208-case benchmark. It compares category names and published category scope only. It does not re-label cases, map ingredients, establish medical risk, or implement a legal-compliance filter.

## Frozen input and decision

Input identity is the existing R19 reproduction receipt [`r19-benchmark-protocol-reproduction-20261102.json`](../../lead-shareable-claim-evidence-20261105/followup-r09-r11-r18-r19/claims/r19-benchmark-protocol-reproduction-20261102.json): 208 assigned queries, 13 declared groups, protocol SHA-256 `0e97c829cccfd6b43bf1dd974db9ae54dd9b561d037ebbca18eb4fbd2bc14fc9`. Its `group_occurrences` totals are: celery 14; eggs 19; fish 18; lupin 13; milk 19; mustard 18; peanuts 20; sesame 20; shellfish 19; soybeans 18; sulphites 15; tree_nuts 19; wheat 19. These sum to 231 declared-group occurrences, not 231 independent cases or mutually exclusive cells; the aggregate does not establish the overlap pattern. The fixed assigned-case denominator remains 208. This note does not inspect or reproduce query payloads.

**Decision:** retain all 13 benchmark group names as their own frozen study vocabulary. For cross-jurisdiction discussion, classify correspondence as `direct-category`, `scope-difference`, `conditional-threshold`, `not-in-US-major-allergen-list`, or `VN-text-unverified`. Never translate a benchmark group into another jurisdiction's ingredient-level decision without an explicit source and entity-level mapping. “Not in the US major-allergen list” means only that it is outside that list; it is not a claim that it cannot cause allergy or is unregulated in every respect.

## 13-group crosswalk

| Frozen benchmark label | US FDA major-allergen correspondence | EU category correspondence | Vietnam evidence status | Crosswalk decision |
|---|---|---|---|---|
| `tree_nuts` | FDA category exists, but membership is a specific Table 1 list, not every item a dataset might call a tree nut. FDA's Oct. 29, 2026 FAQ says coconut and the other named excluded nuts are not in the current major-allergen Tree Nut List. | EU names eight nut types/groups (almond, hazelnut, walnut, cashew, pecan, Brazil, pistachio, macadamia/Queensland); this is not identical to the FDA list. | Per-group mapping **UNKNOWN**; see Vietnam evidence note. | `scope-difference`: blanket benchmark name does not identify exact membership. |
| `peanuts` | Direct named category. | Direct named category. | Per-group mapping **UNKNOWN**. | `direct-category` at group-name level only. |
| `milk` | Direct category; FDA guidance includes domesticated cow, goat, sheep and other ruminant milk. | Direct category in the Commission's 14-group summary; detailed product/derivative scope is not assessed here. | Per-group mapping **UNKNOWN**. | `direct-category`, with jurisdiction-specific scope. |
| `eggs` | Direct category; FDA guidance includes domesticated chicken, duck, goose, quail and other fowl. | Direct category in the Commission's 14-group summary. | Per-group mapping **UNKNOWN**. | `direct-category`, with jurisdiction-specific scope. |
| `wheat` | Direct named category; FDA defines wheat by *Triticum* species and includes triticale as described in its guidance. | Wheat is inside the broader EU “cereals containing gluten” category: wheat (including spelt/Khorasan), rye, barley, oats, and their hybridised strains. | Per-group mapping **UNKNOWN**. | `scope-difference`: wheat is narrower than the EU category. |
| `fish` | Direct category; FDA guidance requires species-level source naming and covers jawless, bony and cartilaginous fish. | Direct category in the Commission's 14-group summary. | Per-group mapping **UNKNOWN**. | `direct-category`, not species-level identity. |
| `shellfish` | FDA major-allergen category is **Crustacean shellfish**; molluscan shellfish are excluded from that US major-allergen category. | EU has two separate categories: crustaceans and molluscs. | Per-group mapping **UNKNOWN**. | `scope-difference`: the generic benchmark label cannot be equated to either legal grouping without item identity. |
| `soybeans` | Direct named category; FDA guidance also recognizes “soy” and “soya” as synonyms. | Direct category in the Commission's 14-group summary; detailed product/derivative scope is not assessed here. | Per-group mapping **UNKNOWN**. | `direct-category`, with jurisdiction-specific scope. |
| `sesame` | Direct named category; ninth US major food allergen effective Jan. 1, 2023. | Direct category (“sesame seeds” and products). | Per-group mapping **UNKNOWN**. | `direct-category` at group-name level only. |
| `mustard` | Not one of FDA's nine statutory major-allergen categories. This is not a statement that mustard cannot cause allergy. | Direct named EU category. | Per-group mapping **UNKNOWN**. | `not-in-US-major-allergen-list`; EU correspondence is direct. |
| `celery` | Not one of FDA's nine statutory major-allergen categories. This is not a statement that celery cannot cause allergy. | Direct named EU category. | Per-group mapping **UNKNOWN**. | `not-in-US-major-allergen-list`; EU correspondence is direct. |
| `sulphites` | Not one of FDA's nine statutory major-allergen categories; FDA guidance says the FD&C Act allergen-label provisions do not set an allergen threshold. This does not resolve other US rules. | EU category is conditional: above 10 mg/kg or 10 mg/L total SO₂, measured in the food as proposed ready for consumption or reconstituted per instructions. | Per-group mapping **UNKNOWN**. | `conditional-threshold`: the group label alone contains no dose, so it cannot decide whether the EU threshold is met. |
| `lupin` | Not one of FDA's nine statutory major-allergen categories. This is not a statement that lupin cannot cause allergy. | Direct named EU category. | Per-group mapping **UNKNOWN**. | `not-in-US-major-allergen-list`; EU correspondence is direct. |

## Vietnam evidence ceiling

The official Vietnam Standards, Metrology and Quality Institute catalogue identifies **TCVN 7087:2013** as active, equivalent to **CODEX STAN 1-1985 amended 2010**, and as replacing TCVN 7087:2008. The catalogue entry is metadata, not the normative text. It does not settle which of the 13 frozen labels correspond to TCVN groups, their detailed scope, exceptions, or applicability. Therefore all 13 Vietnam per-group entries remain **UNKNOWN / deferred**, not “absent” and not copied from either FDA or EU. The Codex origin is recorded as provenance only; this note does not assume Vietnam adopts later Codex amendments or infer domestic legal effect.

## Owner use rule and boundary

1. Preserve the frozen group string, case identity, 208 denominator, outputs and failures unchanged.
2. For descriptive cross-jurisdiction prose, use the row's exact correspondence class and spell out any broader/narrower or threshold distinction.
3. Keep `tree_nuts`, generic `shellfish`, `wheat`, and `sulphites` unmapped to exact ingredient identities unless a separate versioned identity/measurement source supplies the missing detail.
4. Keep Vietnam as `UNKNOWN` until its normative text/version is available and reviewed. This does not block the documented US/EU category comparison.
5. Do not use this table to determine whether a recipe/candidate contains an allergen, to change a filter, to label a case, or to claim safety/compliance. It adds no benchmark rows, tags, labels, results, tests, or milestone credit.

## Sources inspected (accessed 2026-11-12)

- FDA, *Guidance for Industry: Questions and Answers Regarding Food Allergens*, Edition 5, especially A.1–A.2, B.8–B.10, C.1–C.11 and Table 1 (PDF pp. 3, 9–10, 13–15). FDA marks this guidance as nonbinding recommendations; this note reports its category wording: <https://www.fda.gov/media/117410/download?attachment=&os=0>.
- FDA, *Frequently Asked Questions: Food Allergen Labeling Guidance for Industry*, updated 2026-10-29; current tree-nut exclusions and ingredient-list distinction: <https://www.fda.gov/food/food-allergensgluten-free-guidance-documents-regulatory-information/frequently-asked-questions-food-allergen-labeling-guidance-industry>.
- European Commission, DG SANTE, *Allergies* (23 February 2026 factsheet/page): current 14 categories, named nut groups, shellfish split and sulphite threshold: <https://food.ec.europa.eu/food-safety/campaign-2026/allergies_en>.
- European Union, Regulation (EU) No 1169/2011, Annex II, official EUR-Lex source; the consolidated HTML was gated in this inspection, so detailed wording is limited to the indexed Annex II excerpt and the Commission 2026 summary: <https://eur-lex.europa.eu/eli/reg/2011/1169/oj/eng>.
- European Union, Commission Regulation (EU) No 78/2014, Article 1 (cereal-group amendment naming wheat including spelt/Khorasan, rye, barley, oats and hybrids): <https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:32014R0078>.
- Food Safety Authority of Ireland, *Food Information to Consumers* (official Annex II explanatory page; the page returned 403 to direct retrieval in this inspection and is cited only as indexed corroboration): <https://www.fsai.ie/business-advice/labelling/food-information-to-consumers>.
- Vietnam Standards, Metrology and Quality Institute, official catalogue entry for TCVN 7087:2013: <https://tieuchuan.vsqi.gov.vn/tieuchuan/view?sohieu=TCVN+7087%3A2013>; catalogue index PDF entry: <https://tcvn.gov.vn/wp-content/uploads/2020/02/TCVN02122019.pdf>.
