# L42 S04 — ArcSubs expert-validation comparability disposition

Date: 2026-11-06
Decision owner: Lead
Status: bounded prior-art adjudication; no novelty or acceptance conclusion

## Question and source boundary

Does S04, Loesch et al., “Enhancing GISMo: Integrating Recipe Contexts into a Graph-Based Ingredient Substitution Module,” materially overlap the proposed human-validity audit, and which exact differences remain supportable?

Primary source: [official CEUR-WS full paper, Vol. 4196, paper 20](https://ceur-ws.org/Vol-4196/paper_20.pdf), especially §3.1–3.3 (PDF pp. 2–3). This is a full-text reading of the paper, not a dataset inspection, reproduction, or independent/domain-expert review. No ArcSubs rows, survey-response export, labels, repository data files, or private materials were opened or acquired. Existing T446/T351 mission receipts remain the basis for the bounded artifact/rights observations; they do not establish global artifact absence.

## What the paper reports

- Four domain experts working at Arçelik participated. The survey showed a recipe title, ingredient set with quantities, cooking instructions, and a suggested recipe-specific substitution pair generated from Recipe1MSubs.
- The five yes/no questions ask whether the suggested pair fits the recipe; changes taste substantially; changes nutritional profile substantially; requires major cooking-process modifications; or changes food category (with examples including meat-to-vegetarian and avoiding ingredients that cause the same cross-allergies).
- The paper says participants were uncomfortable answering the nutrition question without more information. It reports an average of approximately 3.89 responses per question, excluding that question. It reports Fleiss’ κ=.73 for fit, .58 for cooking-process change, and .67 for food-category/cross-allergy change; taste agreement is described as poorer without a numerical value in the §3.2 narrative.
- ArcSubs retains samples where a majority agrees the suggested substitution fits, regardless of taste change. Participants also selected a preferred substitute from three options or proposed a more suitable alternative to enrich the ground truth. The reported final set is 656 samples over 448 recipes and 182 substitution pairs.

## Comparability decision

S04 is **direct prior art** for human judgments of recipe-context substitution fit and several related dimensions. It defeats broad wording such as “first expert-validated recipe substitution,” “first recipe-context human evaluation,” or “first human judgments of substitute pairs.” Its human-validation evidence must not be minimized to “model evaluation only.”

A narrower methodological distinction remains at the design level: the frozen study proposes, for each fixed case, a shared candidate pool containing outputs from multiple arms plus recorded gold; blinded arm/rank; separate per-candidate culinary-function A/C/N/U judgments; and exact-gold versus any-acceptable-candidate outcomes by identity stratum. The paper describes judgments on Recipe1MSubs-derived suggestions and a three-option/preferred-substitute enrichment step, but does not describe that common cross-method slate, hidden arm/rank, this four-state candidate rubric, or the same estimands. ArcSubs’ majority-fit inclusion also means its final 656-sample benchmark is a selected positive-fit set, not an exhaustive fixed-denominator candidate-validity audit.

The comparison is not a novelty result. Exact-design novelty remains `UNKNOWN / UNESTABLISHED` pending the full source/citation funnel, version/lineage and duplicate adjudication, and qualified independent review. The paper does not report whether raters were independent of the study team, blinded to candidate source/method/rank, or how assignments were distributed; its approximate response average is not an exact case-level missingness table. These are reporting unknowns, not proof the procedures did not occur. Do not map its five binary items or majority vote to the frozen A/C/N/U rubric. The food-category/cross-allergy item is not a per-ingredient allergen profile, allergen-source evidence, or safety validation.

## Existing source and rights boundary

Prior T446/T351 receipts classify ArcSubs as a source lead only: they did not establish a versioned accessible annotation export, complete case/candidate crosswalk, hard-allergen profiles, or data-specific reuse/transfer terms. Paper copyright for the PDF does not itself license the underlying survey data. No acquisition, contact, rights decision, C3 input, C1/C4 acceptance, score, milestone, claim-register status, or protocol change follows from this disposition.

## Challenge record

- Strongest alternative: ArcSubs may already contain candidate/preference structure closer to the planned audit than the paper’s compact description reveals; the unpublished survey artifact could change the comparison materially.
- Disconfirming evidence currently in hand: the paper explicitly describes a suggested pair for binary dimension questions, majority-fit filtering, and a three-option preference/alternative enrichment, while its final report is an aggregate 656 selected set. Existing T446/T351 metadata checks did not locate the underlying per-response export.
- Decision-changing evidence / falsifier: a versioned, rights-permitted source export and codebook showing a common cross-method candidate slate, masked source/rank, per-candidate judgments, complete raw denominators/missingness, and case lineage comparable to the frozen protocol would collapse some or all of the claimed design distinction. Conversely, source documentation confirming pairwise survey assignments and the stated positive-only inclusion would strengthen the distinction but still would not establish novelty beyond reviewed literature.
- Reversibility: wording and source coding are reversible; the protocol remains frozen and unchanged.
- Next trigger: continue the existing L42 full-funnel and source/case-lineage/duplicate adjudication from new or already-collected evidence. Do not repeat the S04 paper or repository screen, and do not claim exact-design novelty from this comparison.

## Evidence ceiling

E1 for source-method description (primary paper text); no local PDF byte/hash receipt or second reviewer is claimed. The documented kappas and counts are author-reported, not independently recomputed. This is a Lead disposition, not external/domain-expert adjudication.
