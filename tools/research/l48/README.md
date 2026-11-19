# L48 Claims Register Adjudication & Evidence Ceiling Evaluator

## Purpose

Implements the Lead L48 claims adjudication, evidence ceiling, and status transition rules under active SSoT Claims Register v20:
1. **SSoT Hash Verification:**
   - Validates that the active claims register matches byte-for-byte the pinned SHA-256 `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`.
2. **Schema & Integrity Invariants:**
   - Exactly 45 claims (sequential R01 to R45).
   - Exact 14 required columns.
   - Status baseline: 10 VERIFIED, 7 PARTIAL, 28 UNVERIFIED.
   - Outcome baseline: 1 MIXED, 32 NA, 4 NEGATIVE, 1 POSITIVE, 7 REPRODUCTION.
3. **Epistemic Evidence Ceilings (E0-E4):**
   - Rejects any VERIFIED status that lacks a valid, verified `reproduction_path` and a 64-character hex `reproduction_sha256`.
   - **R28 & R29 Lock:** Refuted priority claims ("first substitution system", "first human-evaluated substitution") contradicted by published literature (S43-S62); must remain strictly `UNVERIFIED / NA`.
   - **R04 Non-Publication Boundary:** Excluded from public repository distribution; cannot be published or upgraded.
   - **R18 Bounded Reproduction:** Count-output digest vs source pins strictly bounded.
   - **R38 Bounded Demonstration:** Recipe1M-dependent material remains local-only; equivalence to 6,632-node candidate unestablished.
   - **R32 Protocol v1 Candidate Gate:** Unblinded candidate evaluation not permitted under Protocol v1.
4. **Promotion Request Gatekeeper:**
   - Evaluates prospective status promotions fail-closed before any claims register edit.

## Pinned Hashes & Evidence

- **Claims Register v20 SHA-256:** `72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023`
- **Defense Dossier SHA-256:** `7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2`
- **Protocol v1 SHA-256:** `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`

## Testing

From `chefkix-monolith`:
```bash
python -m pytest tools/research/l48/
```
