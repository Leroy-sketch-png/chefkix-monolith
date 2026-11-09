# L46 Defense Rehearsal Pack & Hostile Committee Challenge Evaluator

## Purpose

Implements the Lead L46 defense answers, adversarial committee rubric, and oral rehearsal gates under active SSoT:
1. **SSoT Hash Verification:**
   - Validates that the active Defense Rehearsal Pack matches byte-for-byte the pinned SHA-256 `7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2`.
   - Protocol v1 SHA-256: `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`.
   - Clean Graph Manifest SHA-256: `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`.
2. **28 Hostile Committee Challenges (Q01-Q28) Verification:**
   - Validates all 28 challenges Q01–Q28 across 7 mandatory epistemic fields:
     - `Committee Question:`
     - `Naive / Fatal Trap:`
     - `Defensible SSoT Answer:`
     - `Epistemic Status:`
     - `Evidence Pointers:`
     - `Falsifier / Reversal:`
     - `Forbidden Claim Boundary:`
3. **42/48 Scoring Rubric with Critical Zero-Score Disqualification:**
   - Evaluates across 8 categories (C1-C8, 6 points max each, total 48 points).
   - Passing floor: $\ge 42 / 48$ points (87.5%).
   - Critical disqualification: A score of 0 on C2, C3, C4, or C7 triggers immediate failure regardless of total score.
4. **12 Non-Negotiable Forbidden Claims Scanner:**
   - Scans defensible answers for prohibited claims (clinical safety, 100% safe, judge human validity, algorithmic priority, universal conformal guarantee, etc.).
5. **Oral Rehearsal Status Boundary:**
   - Enforces that passing automated pack tests does NOT grant oral presentation credit; status remains `PACK_VALIDATED_ORAL_REHEARSAL_PENDING` until an actual timed human walkthrough is recorded (preserves legacy T47 boundary 9/24).

## Pinned Hashes & Evidence

- **Defense Dossier SHA-256:** `7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2`
- **Protocol v1 SHA-256:** `b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12`
- **Clean Graph Manifest SHA-256:** `ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1`

## Testing

From `chefkix-monolith`:
```bash
python -m pytest tools/research/l46/
```
