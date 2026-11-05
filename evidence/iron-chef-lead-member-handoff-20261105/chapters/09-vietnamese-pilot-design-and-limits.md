# Chapter 9 — Vietnamese Pilot: Design and Evidence Limits

**Working draft v0.2 — 2026-11-02**

**Evidence state:** updated to align with the canonical Protocol v1 specification (`research/protocols/IRON-CHEF-PROTOCOL-v1.md`, SHA-256 `b2952599...`) and resolved protocol decisions (PR01 conditional rescue estimand $P(V=1 \mid H=0)$, PR02 blinded gold/candidate adjudication, PR03 stratified allocation, PR04 deployable partition-1, PR05 recipe-grouped dev/cal split, PR09 GISMo primary identity, PR10 $\kappa \ge 0.50$ point estimate and cluster bootstrap lower 95% bound $\ge 0.60$). Human participant recruitment, native rater annotation, and agreement evaluation remain strictly gated on institutional ethics clearance and participant consent (PR13).

**Use:** canonical design and protocol scaffold for cross-cultural, low-resource culinary evaluation. This chapter specifies the sampling frame, linguistic contracts, hidden allergen taxonomy, and fail-closed stop conditions; it does not report observed human labels or comparative model claims.

---

## 9.1 Research Purpose

The Vietnamese pilot addresses a fundamental question in culinary artificial intelligence: can an ingredient-substitution evaluation instrument represent underrepresented culinary traditions, complex recipe contexts, and declared dietary constraints without treating English translation or Western ontology matching as ground truth?

This study is a scoped extension of the broader distribution-shift question explored in Chapters 4, 6, and 7. It does not claim that a bounded pilot sample represents the entirety of Vietnamese culinary culture or isolates a causal "cuisine effect." Rather, it establishes a rigorous, fail-closed methodological framework to evaluate whether substitution models generalize across linguistic and cultural boundaries.

The pilot target comprises approximately 100 cases:
- **60 culinary validity cases:** evaluating recipe coherence, culinary plausibility, and culinary role preservation across diverse preparations.
- **40 hidden-allergen constraint cases:** evaluating system behavior under safety-critical dietary constraints involving ingredients with non-obvious allergen profiles.

---

## 9.2 Linguistic Contracts, Regional Taxonomy, and Case Construction

### 9.2.1 Linguistic Integrity and Diacritics Preservation
Vietnamese is the primary case language. All recipe titles, ingredient lists, preparation steps, and target ingredients must preserve standard Vietnamese orthography, tone marks, and diacritics in full UTF-8 encoding (e.g., *phở bò*, *bún chả*, *cá kho tộ*, *canh chua cá lóc*, *bánh xèo*).

Normalization that strips diacritics (e.g., converting "bún bò" to "bun bo") destroys semantic disambiguation and is strictly prohibited in the case representation. English glosses serve exclusively as secondary documentation for cross-linguistic analysis and must never replace native terms in the model prompt or adjudication slate.

### 9.2.2 Regional Culinary Stratification
Vietnamese cuisine exhibits pronounced regional divergence in flavor balances, seasoning techniques, and staple ingredients. The sampling frame stratifies cases across three primary culinary macro-regions:
1. **Northern (*Miền Bắc*):** Characterized by subtle, harmonious, savory profiles, milder seasoning, black pepper over chili, and dishes such as *bún thang*, *phở Hà Nội*, and *chả cá Lã Vọng*.
2. **Central (*Miền Trung*):** Characterized by bold, complex, spicy and pungent profiles, heavy use of fermented pastes (*mắm ruốc*), shrimp powder, and lemongrass, exemplified by *bún bò Huế*, *mì Quảng*, and *bánh bèo*.
3. **Southern (*Miền Nam*):** Characterized by sweet and herbaceous profiles, generous use of coconut milk, palm sugar, tamarind, fresh herbs, and diverse dipping sauces, exemplified by *canh chua*, *thịt kho tàu*, and *hủ tiếu Nam Vang*.

Candidate coverage spans distinct preparation categories: noodle soups (*bún/phở/mì/hủ tiếu*), rice preparations (*cơm/xôi*), braised and caramelized preparations (*kho*), grilled items (*nướng*), fresh and fried rolls (*gỏi cuốn/chả giò*), dipping sauces (*nước chấm*), and sweet desserts (*chè*).

### 9.2.3 Hidden Allergen Taxonomy in Vietnamese Food
A critical challenge in cross-cultural culinary safety is the high prevalence of hidden or non-obvious allergens in standard condiments and seasonings. The pilot explicitly monitors five critical categories:
1. **Fermented Fish Condiments (*Nước mắm*):** Anchovy-derived fish allergen present in the overwhelming majority of savory marinades, dipping sauces, and braises.
2. **Fermented Crustacean Pastes (*Mắm tôm*, *mắm ruốc*, *mắm tép*):** Fermented shrimp or krill paste presenting severe crustacean/shellfish hazards, often dissolved into broths or dipping sauces without visual cues.
3. **Crushed Peanut Garnishes (*Đậu phộng*):** Widely sprinkled over noodle dishes (*bún thịt nướng*), salads (*gỏi*), and dessert soups (*chè*), posing severe peanut anaphylaxis risks.
4. **Sesame Inclusions (*Mè*, *dầu mè*):** Sesame seeds and toasted sesame oil frequently incorporated into marinades and grilled items.
5. **Soy and Wheat Condiments (*Nước tương*, *xì dầu*):** Fermented soy sauce containing both soy and gluten/wheat allergens.

Under Protocol v1, missing or ambiguous allergen information must resolve to `UNKNOWN` and fail closed (`WITHHOLD_CANDIDATE`), rather than defaulting to false negative safety assumptions.

---

## 9.3 Human Review Protocol and Adjudication Boundary

The planned evaluation follows the blinded adjudication architecture established in Chapter 4 v0.3:
1. **Reviewer Competence:** Independent evaluation by native Vietnamese speakers with documented cooking experience and cultural fluency.
2. **Standardized Rubric:** Independent scoring under the protocol's $A/C/N/U$ rubric ($A$: acceptable, $C$: conditionally acceptable with adjustments, $N$: unacceptable, $U$: uncertain/unusable) alongside independent allergen flags (`yes`, `no`, `unknown`).
3. **Double-Blind Adjudication:** Reviewers are blinded to candidate origin, candidate rank, and gold identity (PR02). Slates are presented in randomized presentation order with dual-pass order reversal to detect presentation instability.
4. **Agreement Thresholds:** Confirmatory claims require Cohen's kappa $\kappa \ge 0.50$ (point estimate) and cluster bootstrap lower 95% confidence interval bound $\ge 0.60$ (PR10).

---

## 9.4 Fail-Closed Ethics and Authorization Boundary (PR13)

In accordance with institutional research governance and the project's Epistemic Quality Guard:
- **Zero Human Label Collection Prior to Written Approval:** No recruitment of human raters, distribution of annotation packets, or collection of culinary judgments may occur until written institutional ethics determination and participant consent have been executed (PR13).
- **Prohibition of Synthetic Proxy Data as Human Truth:** LLM generations, simulated agent raters, or automated translations must never be recorded or represented as authentic native-cook evaluations.
- **Stop Condition:** If ethics clearance is delayed or unavailable, the Vietnamese pilot remains strictly a published protocol specification, desk-research taxonomy, and bilingual schema prototype.

---

## 9.5 Current Claim Boundary

| Claim Dimension | Status Under Protocol v1 | Evidentiary Basis |
|---|:---:|---|
| **Protocol Specification** | **DEFINED** | Pinned sampling framework, regional stratification, and hidden-allergen taxonomy. |
| **Bilingual Schema** | **DEFINED** | UTF-8 diacritic-preserving format with paired English gloss fields. |
| **Adjudication Gates** | **PINNED** | Pinned under PR01, PR02, PR03, PR04, PR05, PR09, PR10. |
| **Native-Cook Human Labels** | **BLOCKED (PR13)** | Strictly gated on institutional ethics clearance and participant consent. |
| **Comparative Performance Claims** | **UNSUPPORTED** | No observed model comparisons or cross-cultural superiority claims. |
| **Allergen Safety Guarantee** | **UNSUPPORTED** | Pilot design provides evaluation methods, not medical or clinical safety. |

---

## 9.6 Synthesis and Cross-References

- **Measurement Instrument & Protocol Resolutions:** Chapter 4 v0.3 ([`04-instrument-and-identity-contract.md`](04-instrument-and-identity-contract.md)).
- **Human Validity Audit & Ethics Clearance:** Chapter 6 v0.2 ([`06-human-validity-audit.md`](06-human-validity-audit.md)).
- **Allergen Guardrails & Benchmark Design:** Chapter 7 v0.3 ([`07-guardrail-design-and-assumptions.md`](07-guardrail-design-and-assumptions.md)) and Chapter 8 v0.2 ([`08-allergen-methods.md`](08-allergen-methods.md)).
- **Comprehensive Discussion & Threats:** Chapter 13 v0.2 ([`13-discussion-and-threats.md`](13-discussion-and-threats.md)).
- **Conclusion and Future Outlook:** Chapter 14 v0.7 ([`14-conclusion-and-future-work.md`](14-conclusion-and-future-work.md)).
