# Iron Chef Provider Selection - 2026-10-26

## Operating Decision

Retain `gpt-4o-2024-08-06` for the already authorized, bounded research comparator, using the existing OpenAI Batch route when available and compatible. Do not introduce a new provider into this week's critical path merely because its token rate is lower. This is an ARCHITECT delivery recommendation within the existing model selection, not a finding that GPT-4o is uniquely capable, cheapest, strongest, or suitable for production.

No new HUMAN model-choice approval is needed to continue the already approved arm. No protocol amendment, paid call, credential read, upload or increased budget occurred in this assessment. The existing dispatch checks remain. Prepare the actual all-in preflight; do not make the HUMAN perform provider research or request another broad approval.

## Provenance And Scientific Value

Inspected in the research checkout:

- `academic_vision_and_strategy.md` section 5 names GPT-4o and Gemini among matched arms; the 208-case allergen comparison also names them.
- Consultant authority-resolution v2 PR decisions and section 4 freeze the exact GPT-4o comparator and cost-only full-test/2,000-case fallback. The playbook recommends a 2,000-case GPT run. Neither supplies a measured GPT-versus-DeepSeek quality or cost-effectiveness selection study.
- Adopted Protocol v1 sections 4/6 preserve the exact model, full-test cost test, seeded fallback, one recorded attempt and provider-specific ceilings. The current 10,747-case candidate contains 10,499 requests and 248 retained preparation errors. These do not become a different dataset by selecting a cheaper provider.
- T253 and T430 show an existing Responses Batch package/transport, not just a planned synchronous caller. Current output cap is 256 tokens/request; T224 calculated costs using standard pricing.

Thus GPT-4o is a deliberately selected comparator, not a unique technical dependency. Retaining it avoids changing the research question, request/response contract, recipient and implementation while useful work is pending. It supports only conclusions about that historical model snapshot and these matched tasks; it cannot establish superiority to today's frontier, all LLMs, or human food/allergen safety.

## Rates And Honest Arithmetic

Checked public primary sources on 2026-10-26:

| Route | Uncached input USD / 1M tokens | Output USD / 1M tokens |
|---|---:|---:|
| GPT-4o standard | 2.50 | 10.00 |
| GPT-4o Batch | 1.25 | 5.00 |
| DeepSeek Flash off-peak | 0.15 | 0.60 |
| DeepSeek Flash peak | 0.30 | 1.20 |

Sources: [official OpenAI model documentation](https://developers.openai.com/api/docs/models/gpt-4o), [Batch guide: 50% discount and up-to-24-hour turnaround](https://developers.openai.com/api/docs/guides/batch), [DeepSeek pricing](https://api-docs.deepseek.com/quick_start/pricing/). Account/model access, actual rates at dispatch, capacity and funding remain unverified.

**Illustration, NOT a request-bound forecast:** assume exactly 1,000 input tokens per request, all 256 output tokens used, no cache hits, no tools/taxes/other work, and the same token counts across providers:

| Illustrative cohort | GPT standard | GPT Batch | DeepSeek Flash off-peak / peak |
|---|---:|---:|---:|
| 10,499 callable requests in the full candidate | 53.12 | 26.56 | 3.19 / 6.37 |
| 2,000 callable requests | 10.12 | 5.06 | 0.61 / 1.21 |

Different tokenizers, reasoning usage and failure rates can change actual totals. The existing full-candidate Batch **output-only** ceiling is 10,499 x 256 x USD 5/M = USD 13.43872; inputs, other experiments and reservations must be added. No claim that USD 10 funds the full arm is supported. The illustrative 2,000 requests are not a selected cohort; preparation errors must remain in the selected denominator. Do not underfund the account or shrink the authorized envelope to manufacture the cost-only fallback.

The relevant savings versus the existing Batch path are roughly USD 20-23 in the full-cohort illustration, not the larger synchronous-versus-DeepSeek comparison. Engineering/integration time, protocol review, source-recipient scope, reproducibility and completion delay also cost resources. This is the reason to retain the current bounded arm, not loyalty to a vendor.

## Alternatives And Reversal Trigger

1. **Retain GPT Batch - recommended now.** Reuse reviewed work and unchanged comparator; obtain the actual conservative forecast. Batch delay can be up to 24 hours, so dispatch as soon as genuinely eligible rather than waiting until the presentation.
2. **Replace with DeepSeek - viable scientific amendment, not a drop-in price switch.** Preserve the research question, freeze the exact served model/mode, prompt/output/scoring/cohort/attempt rules, disclose exposed-test status and review affected implementation. Published DeepSeek docs say thinking is enabled by default and temperature has no effect in thinking mode; its model alias can move. OpenAI-format compatibility alone does not prove equivalent experimental behavior or Batch compatibility. [Pricing/version notice](https://api-docs.deepseek.com/quick_start/pricing/), [Responses compatibility](https://api-docs.deepseek.com/guides/responses_api/).
3. **Add DeepSeek as another arm - defer.** Potential future cost-quality evidence, but it adds execution/review work and has no demonstrated dependency on this week's five missing milestones. Do not expand the study simply to avoid choosing.

Reconsider recommendation 1 if actual conservative cost exceeds the existing envelope after its legitimate cost-only fallback, required snapshot/account access is unavailable, the research question materially benefits from a modern alternative, or a concrete integration estimate shows savings outweigh delivery cost. Prepare an exact amendment only then; do not report a silent substitution as the frozen GPT arm. No restart of independent completed GISMo/lookup/C3 implementation is implied by a separately versioned arm.

## Execution Handoff

- **LEAD:** keep this comparator decision, resolve source/identity semantics and accepted terms, review the actual forecast/cohort/release and interpret results narrowly. An unset lead release pin is not a new HUMAN decision.
- **M-I:** reuse existing estimation/transport/journal tools. Bind pricing to the actual standard/Batch route; retain standard-price estimates if explicitly used as conservative overestimates rather than silently calling them Batch prices. Complete only missing preflight/release checks with focused tests; never globally halve constants or remove fail-closed controls. Include current output cap, framing allowance/uncertainty, actual balance, spent/reserved/uncertain amounts and other planned arms. No mandatory exact-count API dependency if a reviewed conservative approach satisfies the protocol.
- **HUMAN:** account login/payment/private key placement only when needed. USD 10 remains an optional small initial setup amount, not a full-run purchase recommendation. JARVIS must supply the actual additional funding amount before a larger top-up. Existing USD 100 is a ceiling, not an instruction to spend it.
- **Product/exploration:** this comparator does not select a production vendor. Future product selection should use a small authorized representative acceptance evaluation and cost per usable answer, latency and failure/recovery behavior. Do not pay for a new bake-off or add a provider abstraction before there is an eligible need.

No new provider was funded, contacted or configured, and no native Goal/thread prompt was sent. Arithmetic was calculated locally from stated assumptions; no source rows or labels were opened.

Verification: both operating-contract structural checks passed 170/170. All eight illustrative price-table values were recalculated and asserted; these are arithmetic checks, not measured usage or model-quality tests. Protocol SHA-256 remains `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`. No transport code changed and no product/provider tests were run.
