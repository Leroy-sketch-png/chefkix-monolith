# Member T56 and T04 local follow-through

## T56 Explore

Source baseline: FE a1ede886. Follow-through: FE 56bba82.

| Browser viewport | Before | After |
| --- | --- | --- |
| 250 CSS px | Tabs shared a row with sort, and tab content overflowed its button bounds. Tonight's Pick mounted and requested data while hidden. | Sort moves below the tabs. Both tab labels fit, the brand link and bottom navigation fit within their containers, and the hidden optional widget sends no request. |
| 360 CSS px | Tabs and sort shared a row. The hidden optional widget sent a request. | The compact row stays in place, and the hidden optional widget sends no request. |
| 1536 CSS px | The visible desktop rail mounted Tonight's Pick. | The visible desktop rail still mounts Tonight's Pick and requests data. |

Measurements came from an isolated local Chromium browser with synthetic recipe responses. During a separate loopback check, the primary recipes GET returned a connection refusal. That observation establishes the current local failure mode only. A separate synthetic 503-to-200 check verified that pressing Retry issues a fresh GET and clears the error. The historical primary-error cause, desktop optional timeout cause, actual 200% browser UI and physical-device acceptance remain open.

## T04 accepted graph refusal

AI-service 9492fe4 adds a repeatable local protected-route check. A disposable copy of the exact five-file M-A bundle retained the manifest pin 5e3e5a905de6f60a26ceb7abc2d5c9db100fbd4379bc5d2908ad8c2f14574b5e while one edges output was changed. With synthetic service and research credentials, the HTTP 200 response reported not configured, abstention, no provenance, and an empty candidate list. The original package was unchanged. Focused AI checks reported 17 passed and one skipped historical external-bundle test. The positive and negative local cases use the accepted M-A lane. T395 retains its separate staged Explorer bundle and runtime acceptance gate.
