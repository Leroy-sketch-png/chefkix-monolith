# Recipe1MSubs use-terms review — 2026-10-25

## Question and scope

The user states that Recipe1MSubs will be used for a non-commercial university graduation thesis, with no paid product or fee. This review distinguishes that local academic research use from redistribution or transfer to a third-party compute provider such as Kaggle.

## Source evidence

1. The official [Facebook Research GISMo repository](https://github.com/facebookresearch/gismo) links the train, validation, and test substitution pickle files from `dl.fbaipublicfiles.com/gismo`. Its [GISMo license](https://raw.githubusercontent.com/facebookresearch/gismo/main/LICENSE.md) is Creative Commons Attribution-NonCommercial 4.0. The license permits non-commercial use of material to which the licensor applied it, with attribution and the other license conditions. The repository README describes the majority of the ingredient-substitution project as CC-BY-NC.
2. The [GISMo data-preparation instructions](https://github.com/facebookresearch/gismo/blob/main/gismo/README.md) link the Recipe1MSubs pickle files separately from the original Recipe1M dataset. The [GISMo top-level README](https://github.com/facebookresearch/gismo) says Recipe1M requires registration. This shows that code/checkpoint, substitution splits, and Recipe1M source data have distinct provenance; the GISMo code license alone does not prove every underlying recipe record is covered.
3. The [Recipe1MSubs reproduction repository](https://github.com/human-agent-teaming/efficient-ingredient-substitution) distributes reproduction code under Apache-2.0 and provides commands to fetch the pickles. It does not state a separate license for those downloaded data files. The Apache license is therefore treated as a code license, not a data grant.
4. A separate [Recipe1M reproduction README](https://github.com/Cadene/recipe1m.bootstrap.pytorch/blob/master/README.md) instructs users to create an account, agree to the dataset terms, and describes Recipe1M as for research and not commercial use. This corroborates the research-only boundary but is not the original data access agreement.
5. The original [MIT Recipe1M dataset access page](https://im2recipe.csail.mit.edu/dataset/login/) returned HTTP 403 from this environment, so the exact current form text and accepted agreement could not be inspected. No access request or external data transfer was performed for this review.

## Assessment

The stated non-commercial thesis use fits the published academic/research and CC-BY-NC scope. Private/local research with source attribution is supported by the available evidence. “Free” does not mean unrestricted: no explicit per-file license was found for the three downloaded `.pkl` artifacts, and the original Recipe1M access terms could not be read here. Accordingly, this review does **not** clear public redistribution of raw files or transfer of them to Kaggle or another external provider. Those uses remain gated on the applicable accepted terms or written permission from the data steward.

## Backlog disposition

Record the intended local thesis research scope as supported; retain `UNKNOWN` for redistribution and external-provider transfer rights. This does not change the separate LEAD C2 dispatch gates or claim that Recipe1M+ approval has been received.
