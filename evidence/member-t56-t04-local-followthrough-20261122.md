# Member T56 and T04 local follow-through

## T56 Explore

Source baseline: FE a1ede886. Follow-through: FE 56bba82.

| Browser viewport | Before | After |
| --- | --- | --- |
| 250 CSS px | Tabs shared a row with sort, and tab content overflowed its button bounds. Tonight's Pick mounted and requested data while hidden. | Sort moves below the tabs. Both tab labels fit, the brand link and bottom navigation fit within their containers, and the hidden optional widget sends no request. |
| 360 CSS px | Tabs and sort shared a row. The hidden optional widget sent a request. | The compact row stays in place, and the hidden optional widget sends no request. |
| 1536 CSS px | The visible desktop rail mounted Tonight's Pick. | The visible desktop rail still mounts Tonight's Pick and requests data. |

Measurements came from an isolated local Chromium browser with synthetic recipe responses. During a separate loopback check, the primary recipes GET returned a connection refusal. That observation describes the original attempt before the local stack was recovered. A separate synthetic 503-to-200 check verified that pressing Retry issues a fresh GET and clears the error. The historical primary-error cause, desktop optional timeout cause, actual 200% browser UI and physical-device acceptance remain open.

## T56 recovered local recipe runtime

The previous local connection refusal came from an inactive recipe stack. After the existing Docker services were started, the monolith could reach MongoDB but authentication failed. At that time a separate Windows MongoDB service owned IPv4 loopback on 27017. The configured credential and Java driver failed against IPv4 but authenticated successfully against Docker’s IPv6 loopback listener. The QA process used the IPv6 endpoint only for its local launch. The application configuration files and the Windows MongoDB service were preserved.

The monolith was built from 0757ebc and the frontend ran 56bba82. A fresh guest Chromium session sent requests to the running local monolith. No recipe responses were mocked in these checks.

| Check | Observed result |
| --- | --- |
| Guest Browse at 360×640 | HTTP 200 with 12 records. Recipe detail returned 200. Back restored the same card. Tonight’s Pick sent zero requests. |
| Guest search at 360×640 | The Typesense-backed recipe search returned HTTP 200 and 10 hits. After opening recipe detail and going Back, the query and first result were restored. |
| Desktop Explore at 1536×860 | Browse returned HTTP 200 with 12 records. The visible rail issued two Tonight’s Pick GETs in this development run, both HTTP 200. |

This establishes recovery and guest behavior for the current local stack. The earlier Lead session’s primary error and optional timeout still have no established cause. Target deployment, physical-device and presenter/listener evidence remain open. No credentials, recipe rows, user IDs or screenshots are included.

## T04 accepted graph refusal

AI-service 9492fe4 adds a repeatable local protected-route check. A disposable copy of the exact five-file M-A bundle retained the manifest pin 5e3e5a905de6f60a26ceb7abc2d5c9db100fbd4379bc5d2908ad8c2f14574b5e while one edges output was changed. With synthetic service and research credentials, the HTTP 200 response reported not configured, abstention, no provenance, and an empty candidate list. The original package was unchanged. Focused AI checks reported 17 passed and one skipped historical external-bundle test. The positive and negative local cases use the accepted M-A lane. T395 retains its separate staged Explorer bundle and runtime acceptance gate.
