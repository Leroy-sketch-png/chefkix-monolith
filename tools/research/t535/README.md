# T535 source-free tooling handoff

This bundle contains the exact 11 reviewed, data-free tooling/specification/
notice files from ChefKix workspace commit
`c73bf260e6ef1dbc429045e76b1680959e13649b`. Per-file SHA-256 values and source
and destination paths are listed in [`SOURCE.json`](SOURCE.json).

## Local checks

From this bundle root, run the source-free focused tests:

```bash
python -m pytest -q ai/tools/test_prepare_t535_kaggle_package.py ai/tools/test_export_c3_recipe1m_order.py ai/tools/test_bootstrap_t535_kaggle.py
```

The tests require Python and `pytest`; they use synthetic/local fixtures and do
not process Recipe1M or Recipe1MSubs rows. The scripts use the Python standard
library. The target runtime specification is separately pinned in
`ai/tools/t535-kaggle-runtime-linux.yml` (Linux, Python 3.8.11 and listed
packages); that target lock is not the host test environment.

## Source-free package preparation interface

When the separately controlled inputs and operation-specific gates are
available, invoke the builder from this bundle root with portable paths:

```bash
python ai/tools/prepare_t535_kaggle_package.py --workspace-root . --gismo-root <clean-gismo-checkout-at-f5d2394c7ade897987ce584d2a75d8bbbbae058e> --runtime-root <owner-private-runtime-directory> --output-dir <new-output-directory>
```

The GISMo checkout must be at the pinned commit shown above. The external
runtime directory must contain `kaggle-linux-runtime-v6.tar.gz` with SHA-256
`6cce29ac80bf7526c9055f66e8ca589be4938b2aa859e46137fc8d33b95be9a3`, as
required by the builder. The output directory must not already exist. The
bundle does not include that runtime archive or GISMo checkout.

This is tooling distribution only. It ships no dataset, source rows, generated
outputs, runtime binary/archive, credentials, private account material, or
session notes. Obtaining these tools does not authorize a source transfer,
source-processing run, Kaggle upload/job, API call, or spending. Those actions
remain subject to their independent provenance, permission, privacy, capacity,
resource, and protocol gates.
