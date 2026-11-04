# M-B GISMo vocabulary source receipt

**Observed:** 2026-11-04. **Owner:** Member. **Use:** local source/provenance check only; no Recipe1M images or Recipe1MSubs data were downloaded.

The official `facebookresearch/gismo` repository links its pretrained ingredient vocabulary to:

`https://dl.fbaipublicfiles.com/gismo/final_recipe1m_vocab_ingrs.pkl`

The asset was downloaded to local-only path `C:\Users\LENOVO\ResearchData\gismo-final_recipe1m_vocab_ingrs-20261104.pkl`. HTTP HEAD returned 200, 318,134 bytes, last modified `2023-08-11T15:43:42Z`, ETag `f20518394d4b00b3380072e6ffc4dfc1`. SHA-256 is `ad8c62dee7d1314be6e173c3f3bbde473c2c79352bd6e3ccf656a6ac9a488ed4`, matching both the already-pinned `gismo-runtime-20261024/vocab_ingrs.pkl` and its runtime manifest.

This confirms the exact ingredient-vocabulary bytes are publicly retrievable and already match the pinned runtime. It does **not** supply GISMo's `final_recipe1m_train.pkl`, `final_recipe1m_val.pkl`, or `final_recipe1m_test.pkl`, nor a historical positional index → Recipe1M ID map. The current replay gaps (train 1,044; validation 230; test 220) therefore remain unexplained. Do not treat the vocabulary file as the missing row-order cache or regenerate strata from a candidate map.

Source references: [official GISMo repository](https://github.com/facebookresearch/gismo) (archived; its pretrained-model section links the asset) and the direct asset URL above. The repository states Recipe1M access requires registration and describes its ingredient-substitution component as CC-BY-NC; this local vocabulary download does not grant broader transfer, redistribution, or product rights.
