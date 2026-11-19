# L41 Institutional, Permission, and Data Source Governance Evaluator

This tool provides deterministic, fail-closed enforcement of the L41 governance
contract under Protocol v1 and `BACKLOG_LEAD.md`.

## Key Invariants

1. **Standing Authority:** HUMAN grants standing authority to read/use already-accessible
   project artifacts for the existing LEAD mission.
2. **Confidential Stores Prohibition:** Agents are strictly prohibited from probing or
   searching confidential internal stores excluded by the HUMAN.
3. **Attestation vs Documentary Verification:** Ethics and consent coverage are tracked as
   `ATTESTED_HUMAN_DECLARATION`. It is not conflated with independent documentary verification,
   nor is research validity or clinical safety inferred from it.
4. **Operation Scoping:** External publication, redistribution, spending, destructive actions,
   and human participant contact remain separately gated and are never inferred from standing
   read authority.
5. **Source Rights Ceilings:** Tracks known dataset terms (Recipe1M non-commercial private scope,
   FooDB CC BY-NC 4.0, WHO/IUIS nomenclature only). None establish clinical safety.

## Test Execution

```powershell
python -m unittest discover -s tools/research/l41 -p "test_*.py" -v
```
