# M-A FooDB native output comparison — 2026-11-24

## Result

The two isolated private builds made on 2026-11-23 with the Lead-selected builder produced identical three-file inventories, manifests, file hashes, and aggregate counts. Each build's data outputs also match the exact shared package added at monolith commit `5e592ea`:

| File | Private build bytes | Private build SHA-256 | Shared package SHA-256 | Match |
| --- | ---: | --- | --- | --- |
| `nodes.jsonl` | 89,121 | `ca26f6001a50c0ebe668019541fe392e6aee87cecd78c89b2429c3090ddbac94` | `ca26f6001a50c0ebe668019541fe392e6aee87cecd78c89b2429c3090ddbac94` | yes |
| `edges.jsonl` | 27,110,650 | `d611e9effb7a0b2b7fd2d5bd9e09d78bb8822681d2955bb1f024cec39a8f403f` | `d611e9effb7a0b2b7fd2d5bd9e09d78bb8822681d2955bb1f024cec39a8f403f` | yes |

Both private manifests have SHA-256 `421ef9d984b6cc7a0a77a344aceade0eb83a40b5ca1706cac2908bf103f09ec0`. The preserved shared manifest remains unchanged at SHA-256 `a187fe108dc2e716169344f3cb120c881a8e481b923351acfd49881cf5a6a01f`.

## Pins and counts

- Selected builder: `b842151d81bef3696173fe84f533d326084220ec`, SHA-256 `dd77556bb8cfe50b4d1c942bfaabe7071c48be5b5de0e1d479f099e5b951d22a`; the same builder bytes are present at the pulled monolith HEAD.
- Both runs used Python 3.13.14 with `py -3 -I`, fresh extraction directories, and empty output directories.
- Input pins checked by the builder: FooDB archive SHA-256 `3a37305b2a46d7fbd41969bdc5c3f9233bd837cde7c6f46e34316d08879a999`, ID-bearing profile SHA-256 `8490cd6adadc5bad0e59f5f4edd8d05020b941b987ca47e38ac0cc80d85a04ac`, and provenance sidecar SHA-256 `5117b791bd825b5c7c87f4f816342ff4bc65100b74502a8652157a989ed9d208`.
- Aggregate counts in both new manifests: 971 profile identities, 971 Food nodes, 206 Compound nodes, 125,992 unique native Food–Compound pairs, 683,753 selected Content-row supports, 107,496 export-eligible pairs, and 18,496 present but not export-eligible pairs.

## Interpretation and limits

The preserved package's historical builder pin is `f2fbd52e8fde526b6f6787927ea5e207a861b30bdcb8d2237f7e2162acebd8ad`. The new manifest records the selected builder pin and adds `manifest_payload_sha256`; the old and new manifest bytes therefore differ. This comparison establishes repeatability of the selected builder and exact equality of the two data-file payloads with the preserved package. It does not reproduce the unrecovered historical builder, establish historical producer lineage, validate graph semantics or completeness, or change T01/T02, serving, R38, or thesis acceptance.

The new run directories remain private at `tmp/private-ma-foodb-native-v4-20261123/`. The shared package remains unchanged. No source archive, extracted source members, or private build outputs are included in this receipt.
