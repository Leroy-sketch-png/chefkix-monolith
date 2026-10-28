from __future__ import annotations

import importlib.util
import contextlib
import io
import json
import shutil
import subprocess
import sys
import tempfile
from types import ModuleType
import unittest
from unittest import mock
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("export_c3_recipe1m_order.py")
MKL_LICENSE_PATH = Path(__file__).with_name("third_party_licenses") / "intel-mkl-2020.2-license-april-2018.txt"
OPENMP_LICENSE_PATH = Path(__file__).with_name("third_party_licenses") / "intel-openmp-2025.0.0-license-october-2022.txt"
JPEG_LICENSE_PATH = Path(__file__).with_name("third_party_licenses") / "jpeg-9b-ijg-license-notice.txt"
ZLIB_LICENSE_PATH = Path(__file__).with_name("third_party_licenses") / "zlib-1.3.2-license.txt"
SPEC = importlib.util.spec_from_file_location("export_c3_recipe1m_order", MODULE_PATH)
assert SPEC and SPEC.loader
module = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = module
SPEC.loader.exec_module(module)


def _runtime_manifest(package_root, entries, archive_rel):
    spec_rel = "runtime/t535-kaggle-runtime-linux.yml"
    spec_path = package_root / spec_rel
    spec_path.parent.mkdir(parents=True, exist_ok=True)
    spec_path.write_text("name: fixture\n", encoding="utf-8")
    spec_entry = {"path": spec_rel, "size_bytes": spec_path.stat().st_size,
                  "sha256": module.sha256_file(spec_path)}
    entries.append(spec_entry)
    archive_entry = next(entry for entry in entries if entry["path"] == archive_rel)
    return {
        "archive": {"path": archive_rel, "sha256": archive_entry["sha256"]},
        "platform_spec": {"path": spec_rel, "sha256": spec_entry["sha256"]},
        "resolution_record": module.PACKAGE_RUNTIME_RESOLUTION,
        "provenance": "synthetic fixture",
    }


def _kaggle_export_fixture(root: Path, observed_at=None):
    """Create a complete source-free exporter fixture; no real inputs are used."""
    working = root / "working"
    package_root = working / "package"
    gismo_root = package_root / "gismo"
    code_root = package_root / "code"
    input_root = root / "input"
    source_root = input_root / "source"
    worktree = working / "invocation-root"
    output_dir = working / "output" / "result"
    for path in (gismo_root, code_root, source_root, worktree, output_dir.parent):
        path.mkdir(parents=True, exist_ok=True)

    exporter_path = code_root / "export_c3_recipe1m_order.py"
    exporter_path.write_bytes(MODULE_PATH.read_bytes())
    package_manifest = package_root / "package-manifest.json"
    package_manifest.write_text('{"fixture":true}\n', encoding="utf-8")

    required = {"layer1.json", "layer2.json", "det_ingrs.json", *module.INDEX_FILES.values(),
                "preprocess.py", "recipe1m.yaml", "nodes_191120.csv"}
    canonical = {
        "preprocess.py": "inv_cooking/datasets/recipe1m/preprocess.py",
        "recipe1m.yaml": "conf/dataset/recipe1m.yaml",
        **{filename: f"data/splits/recipe1m/{filename}" for filename in module.INDEX_FILES.values()},
    }
    source_files = {}
    for name in sorted(required):
        relative = canonical.get(name, name)
        is_gismo = name in {"preprocess.py", "recipe1m.yaml", *module.INDEX_FILES.values()}
        target = (gismo_root if is_gismo else source_root) / Path(*relative.split("/"))
        target.parent.mkdir(parents=True, exist_ok=True)
        content = {
            "train.txt": b"0\n",
            "val_all.txt": b"0\n1\n",
            "val.txt": b"0\n",
            "test.txt": b"0\n",
        }.get(name, f"synthetic:{name}\n".encode("utf-8"))
        target.write_bytes(content)
        source_files[name] = {
            "relative_path": relative,
            "byte_count": target.stat().st_size,
            "sha256": module.sha256_file(target),
        }

    source_manifest = input_root / "source-manifest.json"
    manifest_value = {
        "schema_version": 1,
        "protocol_v1_sha256": module.PROTOCOL_SHA256,
        "analysis_spec_v1_sha256": module.ANALYSIS_SPEC_SHA256,
        "terms_status": module.TERMS_STATUS,
        "terms_reference": "receipt:synthetic-terms-v1",
        "release_id": "recipe1m-synthetic-v1",
        "source_reference": "receipt:synthetic-source-v1",
        "gismo_commit": module.GISMO_COMMIT,
        "source_files": source_files,
    }
    source_manifest.write_text(json.dumps(manifest_value, sort_keys=True), encoding="utf-8")
    source_manifest_sha256 = module.sha256_file(source_manifest)
    attestation_path = working / "attestation.json"
    attestation_value = {
        "schema_version": 1,
        "observed_at_utc": (observed_at or module.dt.datetime.now(module.dt.timezone.utc))
            .isoformat().replace("+00:00", "Z"),
        "owner": "owner", "dataset_slug": "owner/source", "kernel_slug": "owner/job",
        "dataset_private": True, "kernel_private": True,
        "dataset_collaborators": [], "kernel_collaborators": [],
        "gpu_enabled": False, "internet_enabled": False,
        "source_manifest_sha256": source_manifest_sha256,
        "gismo_commit": module.GISMO_COMMIT,
        "exporter_sha256": module.sha256_file(exporter_path),
        "package_manifest_sha256": module.sha256_file(package_manifest),
    }
    attestation_path.write_text(json.dumps(attestation_value, sort_keys=True), encoding="utf-8")
    kwargs = {
        "source_root": source_root,
        "gismo_root": gismo_root,
        "output_dir": output_dir,
        "manifest_path": source_manifest,
        "worktree": worktree,
        "allow_private_real_inputs": True,
        "execution_profile": "kaggle-private",
        "kaggle_dataset_root": input_root,
        "kaggle_working_root": working,
        "kaggle_attestation": attestation_path,
        "kaggle_package_manifest": package_manifest,
        "expected_kaggle_attestation_sha256": module.sha256_file(attestation_path),
        "expected_kaggle_owner": "owner",
        "expected_kaggle_dataset": "owner/source",
        "expected_kaggle_kernel": "owner/job",
    }
    layer1 = [
        {"id": "train-1", "partition": "train"},
        {"id": "val-1", "partition": "val"},
        {"id": "val-2", "partition": "val"},
        {"id": "test-1", "partition": "test"},
    ]
    processed = {
        "train": [{"id": "train-1"}],
        "val": [{"id": "val-1"}, {"id": "val-2"}],
        "test": [{"id": "test-1"}],
    }
    return {
        "kwargs": kwargs, "exporter_path": exporter_path,
        "package_manifest": package_manifest, "source_manifest": source_manifest,
        "source_root": source_root, "attestation_path": attestation_path,
        "output_dir": output_dir, "layer1": layer1, "processed": processed,
        "attestation_value": attestation_value,
    }


def _complete_kaggle_package_fixture(fixture):
    """Bind the exporter fixture to a complete synthetic source-only package."""
    package_root = fixture["package_manifest"].parent
    gismo_root = fixture["kwargs"]["gismo_root"]
    bootstrap_path = package_root / "code" / "bootstrap_t535_kaggle.py"
    bootstrap_path.write_bytes(b"# synthetic bootstrap package member\n")
    (gismo_root / "LICENSE.md").write_bytes(b"Synthetic GISMo license\n")
    license_root = package_root / "licenses"
    license_root.mkdir()
    for source, name in (
        (MKL_LICENSE_PATH, "intel-mkl-2020.2-license-april-2018.txt"),
        (OPENMP_LICENSE_PATH, "intel-openmp-2025.0.0-license-october-2022.txt"),
        (JPEG_LICENSE_PATH, "jpeg-9b-ijg-license-notice.txt"),
        (ZLIB_LICENSE_PATH, "zlib-1.3.2-license.txt"),
    ):
        shutil.copyfile(source, license_root / name)
    runtime_archive = package_root / "runtime" / "kaggle-linux-runtime-v6.tar.gz"
    runtime_archive.parent.mkdir()
    runtime_archive.write_bytes(b"synthetic runtime archive\n")

    files = []
    for path in package_root.rglob("*"):
        if path.is_file() and path != fixture["package_manifest"]:
            files.append({
                "path": path.relative_to(package_root).as_posix(),
                "size_bytes": path.stat().st_size,
                "sha256": module.sha256_file(path),
            })
    manifest = {
        "schema": "chefkix.t535.kaggle-source-free-package",
        "version": module.PACKAGE_VERSION,
        "gismo_commit": module.GISMO_COMMIT,
        "exporter": {
            "path": "code/export_c3_recipe1m_order.py",
            "sha256": module.sha256_file(fixture["exporter_path"]),
        },
        "bootstrap": {
            "path": "code/bootstrap_t535_kaggle.py",
            "sha256": module.sha256_file(bootstrap_path),
        },
        "source_data": module.PACKAGE_SOURCE_DATA,
        "third_party_licenses": module.PACKAGE_THIRD_PARTY_LICENSES,
        "files": files,
    }
    manifest["runtime"] = _runtime_manifest(
        package_root, manifest["files"], "runtime/kaggle-linux-runtime-v6.tar.gz"
    )
    fixture["package_manifest"].write_text(
        json.dumps(manifest, sort_keys=True, indent=2) + "\n", encoding="utf-8"
    )
    attestation = fixture["attestation_value"]
    attestation["package_manifest_sha256"] = module.sha256_file(fixture["package_manifest"])
    fixture["attestation_path"].write_text(
        json.dumps(attestation, sort_keys=True) + "\n", encoding="utf-8"
    )
    fixture["kwargs"]["expected_kaggle_attestation_sha256"] = module.sha256_file(
        fixture["attestation_path"]
    )
    fixture["kwargs"]["kaggle_package_manifest"] = fixture["package_manifest"]
    return fixture


class OrderExtractionTests(unittest.TestCase):
    def setUp(self) -> None:
        self.layer1 = [
            {"id": "r1", "partition": "train", "title": "not emitted"},
            {"id": "r2", "partition": "train", "title": "not emitted"},
            {"id": "r3", "partition": "val", "title": "not emitted"},
            {"id": "r4", "partition": "val", "title": "not emitted"},
            {"id": "r5", "partition": "test", "title": "not emitted"},
        ]
        self.processed = {
            "train": [{"id": "r1", "instructions": ["private"]}, {"id": "r2"}],
            "val": [{"id": "r4"}],
            "test": [{"id": "r5"}],
        }

    def test_preserves_source_order_and_filtered_positions(self) -> None:
        result = module._extract_order(self.layer1, self.processed)
        self.assertEqual(
            [(row["source_ordinal"], row["filtered_position"]) for row in result["train"]],
            [(0, 0), (1, 1)],
        )
        self.assertEqual(result["validation"][0]["source_ordinal"], 3)
        self.assertEqual(result["validation"][0]["filtered_position"], 0)
        self.assertEqual(set(result["train"][0]), {
            "source_split", "source_ordinal", "filtered_position", "recipe_id"
        })

    def test_repeated_source_identity_fails_without_guess(self) -> None:
        self.layer1.append({"id": "r1", "partition": "train"})
        with self.assertRaisesRegex(module.ExportError, "duplicate recipe ID"):
            module._extract_order(self.layer1, self.processed)

    def test_missing_retained_identity_fails(self) -> None:
        self.processed["train"].append({"id": "missing"})
        with self.assertRaisesRegex(module.ExportError, "missing from or conflicts"):
            module._extract_order(self.layer1, self.processed)

    def test_wrong_split_fails(self) -> None:
        self.processed["test"] = [{"id": "r4"}]
        with self.assertRaisesRegex(module.ExportError, "missing from or conflicts"):
            module._extract_order(self.layer1, self.processed)

    def test_reordered_retained_identity_fails(self) -> None:
        self.processed["train"] = [{"id": "r2"}, {"id": "r1"}]
        with self.assertRaisesRegex(module.ExportError, "do not preserve exact source order"):
            module._extract_order(self.layer1, self.processed)

    def test_output_id_repeated_across_processed_splits_fails(self) -> None:
        self.processed["val"] = [{"id": "r1"}]
        with self.assertRaisesRegex(module.ExportError, "missing from or conflicts"):
            module._extract_order(self.layer1, self.processed)

    def test_upstream_validation_partition_is_val_not_validation(self) -> None:
        self.layer1[2]["partition"] = "validation"
        with self.assertRaisesRegex(module.ExportError, "supported partition"):
            module._extract_order(self.layer1, self.processed)


class IndexAndBoundaryTests(unittest.TestCase):
    def test_jpeg_source_locator_matches_pinned_recipe_metadata(self) -> None:
        jpeg = next(
            record for record in module.PACKAGE_THIRD_PARTY_LICENSES
            if record.get("artifact_filename") == "jpeg-9b-h024ee3a_2.conda"
        )
        self.assertEqual(module.JPEG_SOURCE_URL, "http://www.ijg.org/files/jpegsrc.v9b.tar.gz")
        self.assertEqual(jpeg["source_url"], module.JPEG_SOURCE_URL)
        self.assertEqual(jpeg["source_md5"], "6a9996ce116ec5c52b4870dbcd6d3ddb")

    def test_kaggle_attestation_requires_exact_private_cpu_identity_and_source_pin(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "attestation.json"
            value = {
                "schema_version": 1,
                "observed_at_utc": module.dt.datetime.now(module.dt.timezone.utc).isoformat(),
                "owner": "owner",
                "dataset_slug": "owner/private-source",
                "kernel_slug": "owner/private-kernel",
                "dataset_private": True,
                "kernel_private": True,
                "dataset_collaborators": [],
                "kernel_collaborators": [],
                "gpu_enabled": False,
                "internet_enabled": False,
                "source_manifest_sha256": "a" * 64,
                "gismo_commit": module.GISMO_COMMIT,
                "exporter_sha256": module.sha256_file(MODULE_PATH),
                "package_manifest_sha256": "c" * 64,
            }
            path.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
            digest = module.sha256_file(path)
            accepted = module._verify_kaggle_attestation(
                path, digest, owner="owner", dataset="owner/private-source",
                kernel="owner/private-kernel", source_manifest_sha256="a" * 64,
                package_manifest_sha256="c" * 64,
            )
            self.assertEqual(accepted["dataset_slug"], "owner/private-source")
            with self.assertRaisesRegex(module.ExportError, "identity mismatch"):
                module._verify_kaggle_attestation(
                    path, digest, owner="other", dataset="owner/private-source",
                    kernel="owner/private-kernel",
                )
            with self.assertRaisesRegex(module.ExportError, "different source manifest"):
                module._verify_kaggle_attestation(
                    path, digest, owner="owner", dataset="owner/private-source",
                    kernel="owner/private-kernel", source_manifest_sha256="b" * 64,
                )
            with self.assertRaisesRegex(module.ExportError, "different code package"):
                module._verify_kaggle_attestation(
                    path, digest, owner="owner", dataset="owner/private-source",
                    kernel="owner/private-kernel", package_manifest_sha256="d" * 64,
                )
            with self.assertRaisesRegex(module.ExportError, "hash mismatch"):
                module._verify_kaggle_attestation(
                    path, "b" * 64, owner="owner", dataset="owner/private-source",
                    kernel="owner/private-kernel",
                )
            with self.assertRaisesRegex(module.ExportError, "identity is malformed"):
                module._verify_kaggle_attestation(
                    path, digest, owner="owner", dataset="../private-source",
                    kernel="owner/private-kernel",
                )

    def test_kaggle_attestation_enforces_freshness_and_utc_clock(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "attestation.json"
            now = module.dt.datetime(2026, 10, 27, 12, 30, 0, tzinfo=module.dt.timezone.utc)
            value = {
                "schema_version": 1, "observed_at_utc": now.isoformat().replace("+00:00", "Z"),
                "owner": "owner", "dataset_slug": "owner/source", "kernel_slug": "owner/kernel",
                "dataset_private": True, "kernel_private": True,
                "dataset_collaborators": [], "kernel_collaborators": [],
                "gpu_enabled": False, "internet_enabled": False,
                "source_manifest_sha256": "a" * 64, "gismo_commit": module.GISMO_COMMIT,
                "exporter_sha256": module.sha256_file(MODULE_PATH),
                "package_manifest_sha256": "c" * 64,
            }

            def verify(current=now):
                path.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
                return module._verify_kaggle_attestation(
                    path, module.sha256_file(path), owner="owner", dataset="owner/source",
                    kernel="owner/kernel", now_utc=current,
                )

            value["observed_at_utc"] = (now - module.PREFLIGHT_ATTESTATION_MAX_AGE).isoformat().replace("+00:00", "Z")
            self.assertEqual(verify()["owner"], "owner")
            value["observed_at_utc"] = (now - module.PREFLIGHT_ATTESTATION_MAX_AGE
                                        - module.dt.timedelta(seconds=1)).isoformat().replace("+00:00", "Z")
            with self.assertRaisesRegex(module.ExportError, "stale"):
                verify()
            value["observed_at_utc"] = (now + module.dt.timedelta(seconds=1)).isoformat().replace("+00:00", "Z")
            with self.assertRaisesRegex(module.ExportError, "future"):
                verify()
            value["observed_at_utc"] = "not-a-timestamp"
            with self.assertRaisesRegex(module.ExportError, "observation time is invalid"):
                verify()
            value.pop("observed_at_utc")
            with self.assertRaisesRegex(module.ExportError, "schema mismatch"):
                verify()
            value["observed_at_utc"] = now.isoformat().replace("+00:00", "Z")
            value["observed_at_utc"] = now.astimezone(
                module.dt.timezone(module.dt.timedelta(hours=1))).isoformat()
            with self.assertRaisesRegex(module.ExportError, "observation time must be UTC"):
                verify()
            value["observed_at_utc"] = now.isoformat().replace("+00:00", "Z")
            non_utc_clock = now.astimezone(module.dt.timezone(module.dt.timedelta(hours=1)))
            with self.assertRaisesRegex(module.ExportError, "current clock must be UTC"):
                verify(current=non_utc_clock)

    def test_kaggle_attestation_size_is_bounded(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "oversized-attestation.json"
            path.write_bytes(b" " * 65537)
            with self.assertRaisesRegex(module.ExportError, "size limit"):
                module._verify_kaggle_attestation(
                    path, "a" * 64, owner="owner", dataset="owner/source",
                    kernel="owner/kernel",
                )

    def test_kaggle_attestation_rejects_collaborators_and_gpu(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "attestation.json"
            value = {
                "schema_version": 1,
                "observed_at_utc": "2026-10-27T12:30:00Z",
                "owner": "owner", "dataset_slug": "owner/private-source",
                "kernel_slug": "owner/private-kernel", "dataset_private": True,
                "kernel_private": True, "dataset_collaborators": ["collaborator"],
                "kernel_collaborators": [], "gpu_enabled": True, "internet_enabled": False,
                "source_manifest_sha256": "a" * 64,
                "gismo_commit": module.GISMO_COMMIT,
                "exporter_sha256": module.sha256_file(MODULE_PATH),
                "package_manifest_sha256": "c" * 64,
            }
            path.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
            with self.assertRaisesRegex(module.ExportError, "required private offline CPU configuration"):
                module._verify_kaggle_attestation(
                    path, module.sha256_file(path), owner="owner",
                    dataset="owner/private-source", kernel="owner/private-kernel",
                )

    def test_kaggle_attestation_rejects_internet_enabled(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "attestation.json"
            value = {
                "schema_version": 1,
                "observed_at_utc": "2026-10-27T12:30:00Z",
                "owner": "owner", "dataset_slug": "owner/private-source",
                "kernel_slug": "owner/private-kernel", "dataset_private": True,
                "kernel_private": True, "dataset_collaborators": [],
                "kernel_collaborators": [], "gpu_enabled": False, "internet_enabled": True,
                "source_manifest_sha256": "a" * 64,
                "gismo_commit": module.GISMO_COMMIT,
                "exporter_sha256": module.sha256_file(MODULE_PATH),
                "package_manifest_sha256": "c" * 64,
            }
            path.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
            with self.assertRaisesRegex(module.ExportError, "private offline CPU configuration"):
                module._verify_kaggle_attestation(
                    path, module.sha256_file(path), owner="owner",
                    dataset="owner/private-source", kernel="owner/private-kernel",
                )

    def test_kaggle_attestation_rejects_boolean_schema_version(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "attestation.json"
            value = {
                "schema_version": True,
                "observed_at_utc": "2026-10-27T12:30:00Z",
                "owner": "owner", "dataset_slug": "owner/private-source",
                "kernel_slug": "owner/private-kernel", "dataset_private": True,
                "kernel_private": True, "dataset_collaborators": [],
                "kernel_collaborators": [], "gpu_enabled": False, "internet_enabled": False,
                "source_manifest_sha256": "a" * 64,
                "gismo_commit": module.GISMO_COMMIT,
                "exporter_sha256": module.sha256_file(MODULE_PATH),
                "package_manifest_sha256": "c" * 64,
            }
            path.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
            with self.assertRaisesRegex(module.ExportError, "schema mismatch"):
                module._verify_kaggle_attestation(
                    path, module.sha256_file(path), owner="owner",
                    dataset="owner/private-source", kernel="owner/private-kernel",
                )

    def test_kaggle_packaged_gismo_is_verified_without_git_metadata(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            package_root = root / "package"
            code_root = package_root / "code"
            gismo_root = package_root / "gismo"
            code_root.mkdir(parents=True)
            (gismo_root / "inv_cooking").mkdir(parents=True)
            exporter_path = code_root / "export_c3_recipe1m_order.py"
            exporter_path.write_bytes(MODULE_PATH.read_bytes())
            bootstrap_path = code_root / "bootstrap_t535_kaggle.py"
            bootstrap_path.write_bytes(b"# synthetic bootstrap package member\n")
            gismo_file = gismo_root / "inv_cooking" / "preprocess.py"
            gismo_file.write_bytes(b"# pinned GISMo source-free fixture\n")
            license_file = gismo_root / "LICENSE.md"
            license_file.write_bytes(b"Attribution-NonCommercial 4.0 International\n")
            mkl_license_file = package_root / "licenses" / "intel-mkl-2020.2-license-april-2018.txt"
            mkl_license_file.parent.mkdir(parents=True)
            mkl_license_file.write_bytes(MKL_LICENSE_PATH.read_bytes())
            openmp_license_file = package_root / "licenses" / "intel-openmp-2025.0.0-license-october-2022.txt"
            openmp_license_file.write_bytes(OPENMP_LICENSE_PATH.read_bytes())
            jpeg_license_file = package_root / "licenses" / "jpeg-9b-ijg-license-notice.txt"
            jpeg_license_file.write_bytes(JPEG_LICENSE_PATH.read_bytes())
            zlib_license_file = package_root / "licenses" / "zlib-1.3.2-license.txt"
            zlib_license_file.write_bytes(ZLIB_LICENSE_PATH.read_bytes())
            split_files = []
            for relative in module.PACKAGE_SPLIT_INDEX_PATHS:
                path = package_root / Path(*relative.split("/"))
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("0\n1\n", encoding="utf-8")
                split_files.append(path)
            runtime_file = package_root / "runtime" / "kaggle-linux-runtime-v6.tar.gz"
            runtime_file.parent.mkdir()
            runtime_file.write_bytes(b"synthetic runtime fixture\n")
            exporter_sha = module.sha256_file(exporter_path)
            bootstrap_sha = module.sha256_file(bootstrap_path)
            gismo_sha = module.sha256_file(gismo_file)
            license_sha = module.sha256_file(license_file)
            runtime_sha = module.sha256_file(runtime_file)
            manifest = {
                "schema": "chefkix.t535.kaggle-source-free-package",
                "version": module.PACKAGE_VERSION,
                "gismo_commit": module.GISMO_COMMIT,
                "exporter": {"path": "code/export_c3_recipe1m_order.py", "sha256": exporter_sha},
                "bootstrap": {"path": "code/bootstrap_t535_kaggle.py", "sha256": bootstrap_sha},
                "source_data": module.PACKAGE_SOURCE_DATA,
                "third_party_licenses": module.PACKAGE_THIRD_PARTY_LICENSES,
                "files": [
                    {"path": "code/export_c3_recipe1m_order.py", "size_bytes": exporter_path.stat().st_size,
                     "sha256": exporter_sha},
                    {"path": "code/bootstrap_t535_kaggle.py", "size_bytes": bootstrap_path.stat().st_size,
                     "sha256": bootstrap_sha},
                    {"path": "gismo/inv_cooking/preprocess.py", "size_bytes": gismo_file.stat().st_size,
                     "sha256": gismo_sha},
                    {"path": "gismo/LICENSE.md", "size_bytes": license_file.stat().st_size,
                     "sha256": license_sha},
                    {"path": "licenses/intel-mkl-2020.2-license-april-2018.txt",
                     "size_bytes": mkl_license_file.stat().st_size,
                     "sha256": module.sha256_file(mkl_license_file)},
                    {"path": "licenses/intel-openmp-2025.0.0-license-october-2022.txt",
                     "size_bytes": openmp_license_file.stat().st_size,
                     "sha256": module.sha256_file(openmp_license_file)},
                    {"path": "licenses/jpeg-9b-ijg-license-notice.txt",
                     "size_bytes": jpeg_license_file.stat().st_size,
                     "sha256": module.sha256_file(jpeg_license_file)},
                    {"path": "licenses/zlib-1.3.2-license.txt",
                     "size_bytes": zlib_license_file.stat().st_size,
                     "sha256": module.sha256_file(zlib_license_file)},
                    *[{"path": path.relative_to(package_root).as_posix(),
                       "size_bytes": path.stat().st_size, "sha256": module.sha256_file(path)}
                      for path in split_files],
                    {"path": "runtime/kaggle-linux-runtime-v6.tar.gz", "size_bytes": runtime_file.stat().st_size,
                     "sha256": runtime_sha},
                ],
            }
            manifest["runtime"] = _runtime_manifest(
                package_root, manifest["files"], "runtime/kaggle-linux-runtime-v6.tar.gz")
            manifest_path = package_root / "package-manifest.json"
            manifest_path.write_text(json.dumps(manifest, sort_keys=True, indent=2) + "\n", encoding="utf-8")
            package_sha = module.sha256_file(manifest_path)
            with mock.patch.object(module, "__file__", str(exporter_path)):
                module._check_packaged_gismo(gismo_root, manifest_path, package_sha)
                manifest["runtime"]["locks"] = {
                    "pip-freeze.lock": {"path": "runtime/pip-freeze.lock", "sha256": "a" * 64}
                }
                manifest_path.write_text(json.dumps(manifest, sort_keys=True, indent=2) + "\n",
                                         encoding="utf-8")
                with self.assertRaisesRegex(module.ExportError, "runtime declaration"):
                    module._check_packaged_gismo(gismo_root, manifest_path,
                                                  module.sha256_file(manifest_path))
                manifest["runtime"].pop("locks")
                manifest_path.write_text(json.dumps(manifest, sort_keys=True, indent=2) + "\n",
                                         encoding="utf-8")
                package_sha = module.sha256_file(manifest_path)
                with self.assertRaisesRegex(module.ExportError, "manifest hash mismatch"):
                    module._check_packaged_gismo(gismo_root, manifest_path, "0" * 64)
                runtime_file.write_bytes(b"altered runtime fixture\n")
                with self.assertRaisesRegex(module.ExportError, "package file differs"):
                    module._check_packaged_gismo(gismo_root, manifest_path, package_sha)
                runtime_file.write_bytes(b"synthetic runtime fixture\n")
                mkl_license_file.write_bytes(b"modified license notice\n")
                with self.assertRaisesRegex(module.ExportError, "package file differs"):
                    module._check_packaged_gismo(gismo_root, manifest_path, package_sha)
                mkl_license_file.write_bytes(MKL_LICENSE_PATH.read_bytes())
                openmp_license_file.write_bytes(b"modified OpenMP notice\n")
                with self.assertRaisesRegex(module.ExportError, "package file differs"):
                    module._check_packaged_gismo(gismo_root, manifest_path, package_sha)
                openmp_license_file.write_bytes(OPENMP_LICENSE_PATH.read_bytes())
                jpeg_license_file.write_bytes(b"modified JPEG notice\n")
                with self.assertRaisesRegex(module.ExportError, "package file differs"):
                    module._check_packaged_gismo(gismo_root, manifest_path, package_sha)
                jpeg_license_file.write_bytes(JPEG_LICENSE_PATH.read_bytes())
                zlib_license_file.write_bytes(b"modified Zlib license\n")
                with self.assertRaisesRegex(module.ExportError, "package file differs"):
                    module._check_packaged_gismo(gismo_root, manifest_path, package_sha)
                zlib_license_file.write_bytes(ZLIB_LICENSE_PATH.read_bytes())
                (gismo_root / "unlisted.py").write_text("unexpected", encoding="utf-8")
                with self.assertRaisesRegex(module.ExportError, "unexpected files"):
                    module._check_packaged_gismo(gismo_root, manifest_path, package_sha)

    def test_packaged_source_data_boundary_rejects_ambiguous_metadata(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            package_root = root / "package"
            code_root = package_root / "code"
            gismo_root = package_root / "gismo"
            code_root.mkdir(parents=True)
            (gismo_root / "inv_cooking").mkdir(parents=True)
            exporter_path = code_root / "export_c3_recipe1m_order.py"
            exporter_path.write_bytes(MODULE_PATH.read_bytes())
            bootstrap_path = code_root / "bootstrap_t535_kaggle.py"
            bootstrap_path.write_bytes(b"# synthetic bootstrap package member\n")
            (gismo_root / "inv_cooking" / "preprocess.py").write_bytes(b"synthetic\n")
            (gismo_root / "LICENSE.md").write_bytes(b"Attribution-NonCommercial 4.0 International\n")
            mkl_license_file = package_root / "licenses" / "intel-mkl-2020.2-license-april-2018.txt"
            mkl_license_file.parent.mkdir(parents=True)
            mkl_license_file.write_bytes(MKL_LICENSE_PATH.read_bytes())
            openmp_license_file = package_root / "licenses" / "intel-openmp-2025.0.0-license-october-2022.txt"
            openmp_license_file.write_bytes(OPENMP_LICENSE_PATH.read_bytes())
            jpeg_license_file = package_root / "licenses" / "jpeg-9b-ijg-license-notice.txt"
            jpeg_license_file.write_bytes(JPEG_LICENSE_PATH.read_bytes())
            zlib_license_file = package_root / "licenses" / "zlib-1.3.2-license.txt"
            zlib_license_file.write_bytes(ZLIB_LICENSE_PATH.read_bytes())
            runtime_path = package_root / "runtime" / "kaggle-linux-runtime-v6.tar.gz"
            runtime_path.parent.mkdir()
            runtime_path.write_bytes(b"synthetic runtime\n")
            files = []
            for path in (exporter_path, bootstrap_path, mkl_license_file, openmp_license_file,
                         jpeg_license_file, zlib_license_file,
                         *gismo_root.rglob("*"), runtime_path):
                if path.is_file():
                    files.append({"path": path.relative_to(package_root).as_posix(),
                                  "size_bytes": path.stat().st_size,
                                  "sha256": module.sha256_file(path)})
            manifest = {
                "schema": "chefkix.t535.kaggle-source-free-package", "version": module.PACKAGE_VERSION,
                "gismo_commit": module.GISMO_COMMIT,
                "exporter": {"path": "code/export_c3_recipe1m_order.py",
                             "sha256": module.sha256_file(exporter_path)},
                "bootstrap": {"path": "code/bootstrap_t535_kaggle.py",
                              "sha256": module.sha256_file(bootstrap_path)},
                "source_data": {"included": False, "mount_required_later": True},
                "third_party_licenses": module.PACKAGE_THIRD_PARTY_LICENSES,
                "files": files,
            }
            manifest["runtime"] = _runtime_manifest(
                package_root, manifest["files"], "runtime/kaggle-linux-runtime-v6.tar.gz")
            manifest_path = package_root / "package-manifest.json"
            manifest_path.write_text(json.dumps(manifest, sort_keys=True, indent=2) + "\n", encoding="utf-8")
            with mock.patch.object(module, "__file__", str(exporter_path)):
                with self.assertRaisesRegex(module.ExportError, "source-data boundary"):
                    module._check_packaged_gismo(gismo_root, manifest_path,
                                                  module.sha256_file(manifest_path))

    def test_kaggle_profile_accepts_canonical_package_layout_before_source_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            working = root / "working"
            package_root = working / "package"
            code_root = package_root / "code"
            gismo_root = package_root / "gismo"
            input_root = root / "input"
            dataset_root = input_root / "data"
            worktree = working / "invocation-root"
            output = working / "output" / "result"
            for path in (code_root, gismo_root / "inv_cooking", dataset_root,
                         worktree, output.parent):
                path.mkdir(parents=True, exist_ok=True)
            exporter_path = code_root / "export_c3_recipe1m_order.py"
            exporter_path.write_bytes(MODULE_PATH.read_bytes())
            bootstrap_path = code_root / "bootstrap_t535_kaggle.py"
            bootstrap_path.write_bytes(b"# synthetic bootstrap package member\n")
            gismo_file = gismo_root / "inv_cooking" / "preprocess.py"
            gismo_file.write_bytes(b"# synthetic canonical GISMo package\n")
            license_file = gismo_root / "LICENSE.md"
            license_file.write_bytes(b"Attribution-NonCommercial 4.0 International\n")
            mkl_license_file = package_root / "licenses" / "intel-mkl-2020.2-license-april-2018.txt"
            mkl_license_file.parent.mkdir(parents=True)
            mkl_license_file.write_bytes(MKL_LICENSE_PATH.read_bytes())
            openmp_license_file = package_root / "licenses" / "intel-openmp-2025.0.0-license-october-2022.txt"
            openmp_license_file.write_bytes(OPENMP_LICENSE_PATH.read_bytes())
            jpeg_license_file = package_root / "licenses" / "jpeg-9b-ijg-license-notice.txt"
            jpeg_license_file.write_bytes(JPEG_LICENSE_PATH.read_bytes())
            zlib_license_file = package_root / "licenses" / "zlib-1.3.2-license.txt"
            zlib_license_file.write_bytes(ZLIB_LICENSE_PATH.read_bytes())
            split_files = []
            for relative in module.PACKAGE_SPLIT_INDEX_PATHS:
                path = package_root / Path(*relative.split("/"))
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("0\n", encoding="utf-8")
                split_files.append(path)
            runtime_file = package_root / "runtime" / "kaggle-linux-runtime-v6.tar.gz"
            runtime_file.parent.mkdir()
            runtime_file.write_bytes(b"synthetic archive fixture\n")
            exporter_sha = module.sha256_file(exporter_path)
            bootstrap_sha = module.sha256_file(bootstrap_path)
            entries = []
            for path in (exporter_path, bootstrap_path, mkl_license_file, openmp_license_file,
                         jpeg_license_file, zlib_license_file, gismo_file, license_file,
                         *split_files, runtime_file):
                relative = path.relative_to(package_root).as_posix()
                entries.append({"path": relative, "size_bytes": path.stat().st_size,
                                "sha256": module.sha256_file(path)})
            manifest_value = {
                "schema": "chefkix.t535.kaggle-source-free-package", "version": module.PACKAGE_VERSION,
                "gismo_commit": module.GISMO_COMMIT,
                "exporter": {"path": "code/export_c3_recipe1m_order.py", "sha256": exporter_sha},
                "bootstrap": {"path": "code/bootstrap_t535_kaggle.py", "sha256": bootstrap_sha},
                "source_data": module.PACKAGE_SOURCE_DATA,
                "third_party_licenses": module.PACKAGE_THIRD_PARTY_LICENSES,
                "files": entries,
            }
            manifest_value["runtime"] = _runtime_manifest(
                package_root, manifest_value["files"], "runtime/kaggle-linux-runtime-v6.tar.gz")
            package_manifest = package_root / "package-manifest.json"
            package_manifest.write_text(json.dumps(manifest_value, sort_keys=True, indent=2) + "\n",
                                       encoding="utf-8")
            attestation = working / "attestation.json"
            attestation_value = {
                "schema_version": 1,
                "observed_at_utc": module.dt.datetime.now(module.dt.timezone.utc).isoformat(),
                "owner": "owner", "dataset_slug": "owner/source", "kernel_slug": "owner/job",
                "dataset_private": True, "kernel_private": True,
                "dataset_collaborators": [], "kernel_collaborators": [],
                "gpu_enabled": False, "internet_enabled": False,
                "source_manifest_sha256": "a" * 64, "gismo_commit": module.GISMO_COMMIT,
                "exporter_sha256": exporter_sha,
                "package_manifest_sha256": module.sha256_file(package_manifest),
            }
            attestation.write_text(json.dumps(attestation_value, sort_keys=True), encoding="utf-8")
            source_manifest = input_root / "source-manifest.json"
            source_manifest.write_text("{}\n", encoding="utf-8")
            class ReachedManifestLoad(Exception):
                pass
            with mock.patch.object(module, "__file__", str(exporter_path)), \
                    mock.patch.object(module, "_require_private_local_path"), \
                    mock.patch.object(module, "_load_manifest", side_effect=ReachedManifestLoad):
                with self.assertRaises(ReachedManifestLoad):
                    module.export_order(
                        source_root=dataset_root, gismo_root=gismo_root, output_dir=output,
                        manifest_path=source_manifest, worktree=worktree,
                        allow_private_real_inputs=True, execution_profile="kaggle-private",
                        kaggle_dataset_root=input_root, kaggle_working_root=working,
                        kaggle_attestation=attestation, kaggle_package_manifest=package_manifest,
                        expected_kaggle_attestation_sha256=module.sha256_file(attestation),
                        expected_kaggle_owner="owner", expected_kaggle_dataset="owner/source",
                        expected_kaggle_kernel="owner/job",
                    )

    def test_kaggle_profile_accepts_only_readonly_input_and_writable_working_mounts(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            input_root, working_root = root / "input", root / "working"
            dataset_root, output_root = input_root / "source", working_root / "package"
            dataset_root.mkdir(parents=True)
            output_root.mkdir(parents=True)
            source = dataset_root / "layer1.json"
            output = output_root / "result"
            source.write_text("[]", encoding="utf-8")
            output.mkdir()
            with mock.patch.object(module, "KAGGLE_INPUT_ROOT", input_root), \
                    mock.patch.object(module, "KAGGLE_WORKING_ROOT", working_root):
                with mock.patch.object(module, "_kaggle_mount_readonly", return_value=True):
                    module._require_private_local_path(
                        source, execution_profile="kaggle-private",
                        dataset_root=dataset_root, working_root=output_root,
                    )
                with mock.patch.object(module, "_kaggle_mount_readonly", return_value=False):
                    module._require_private_local_path(
                        output, execution_profile="kaggle-private",
                        dataset_root=dataset_root, working_root=output_root,
                    )
                    with self.assertRaisesRegex(module.ExportError, "not read-only"):
                        module._require_private_local_path(
                            source, execution_profile="kaggle-private",
                            dataset_root=dataset_root, working_root=output_root,
                        )
                outside = root / "outside"
                outside.mkdir()
                with self.assertRaisesRegex(module.ExportError, "outside the pinned dataset and output roots"):
                    module._require_private_local_path(
                        outside, execution_profile="kaggle-private",
                        dataset_root=dataset_root, working_root=output_root,
                    )

    def test_kaggle_profile_requires_explicit_mount_roots(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory)
            with self.assertRaisesRegex(module.ExportError, "mount roots must be explicit"):
                module._require_private_local_path(
                    path, execution_profile="kaggle-private",
                )

    def test_private_acl_allowlist_accepts_user_system_and_local_admins(self) -> None:
        module._validate_private_acl("S-1-user", [
            ("S-1-user", 0), ("S-1-5-18", 0x02), ("S-1-5-32-544", 0x10),
        ], "S-1-user")

    def test_private_acl_rejects_broad_principal_and_wrong_owner(self) -> None:
        with self.assertRaisesRegex(module.ExportError, "unapproved principal"):
            module._validate_private_acl("S-1-user", [("S-1-user", 0), ("S-1-1-0", 0)], "S-1-user")
        with self.assertRaisesRegex(module.ExportError, "owned by the current user"):
            module._validate_private_acl("S-1-other", [("S-1-user", 0)], "S-1-user")

    def test_private_acl_requires_effective_not_inherit_only_user_allow(self) -> None:
        with self.assertRaisesRegex(module.ExportError, "explicitly grant the current user"):
            module._validate_private_acl("S-1-user", [("S-1-user", 0x08)], "S-1-user")

    def test_mixed_or_preloaded_gismo_package_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory) / "pinned"
            other = Path(directory) / "other"
            root.mkdir()
            other.mkdir()
            imported_module = ModuleType("inv_cooking.datasets.recipe1m.preprocess")
            outside_file = other / "preprocess.py"
            outside_file.write_text("# synthetic path fixture\n", encoding="utf-8")
            imported_module.__file__ = str(outside_file)
            with self.assertRaisesRegex(module.ExportError, "outside the pinned checkout"):
                module._check_imported_gismo_tree(
                    {"inv_cooking.datasets.recipe1m.preprocess": imported_module}, root
                )

    @mock.patch.object(module.subprocess, "run")
    def test_dirty_frozen_gismo_checkout_is_rejected(self, run: mock.Mock) -> None:
        run.side_effect = [
            subprocess.CompletedProcess([], 0, module.GISMO_COMMIT + "\n", ""),
            subprocess.CompletedProcess([], 0, " M inv_cooking/datasets/recipe1m/preprocess.py\n", ""),
        ]
        with self.assertRaisesRegex(module.ExportError, "local modifications"):
            module._check_git_commit(Path("gismo"))

    @mock.patch.object(module.subprocess, "run")
    def test_untracked_frozen_gismo_checkout_is_rejected(self, run: mock.Mock) -> None:
        run.side_effect = [
            subprocess.CompletedProcess([], 0, module.GISMO_COMMIT + "\n", ""),
            subprocess.CompletedProcess([], 0, "?? alternate.py\n", ""),
        ]
        with self.assertRaisesRegex(module.ExportError, "local modifications"):
            module._check_git_commit(Path("gismo"))

    def test_val_subset_is_distinct_and_bounded(self) -> None:
        module._check_indices(
            {"train": [0, 1], "val_all": [0, 1], "val": [1], "test": [0]},
            {"train": 2, "validation": 2, "test": 1},
        )

    def test_val_outside_full_is_rejected(self) -> None:
        with self.assertRaisesRegex(module.ExportError, "not a subset"):
            module._check_indices(
                {"train": [0], "val_all": [0, 1], "val": [2], "test": [0]},
                {"train": 1, "validation": 3, "test": 1},
            )

    def test_val_all_domain_must_be_valid(self) -> None:
        with self.assertRaisesRegex(module.ExportError, "outside its pinned"):
            module._check_indices(
                {"train": [0], "val_all": [3], "val": [0], "test": [0]},
                {"train": 1, "validation": 3, "test": 1},
            )

    def test_default_off_refuses_before_touching_paths(self) -> None:
        with self.assertRaisesRegex(module.ExportError, "disabled"):
            module.export_order(
                source_root=Path("missing-source"), gismo_root=Path("missing-gismo"),
                output_dir=Path("missing-output"), manifest_path=Path("missing-manifest"),
                worktree=Path("."), allow_private_real_inputs=False,
            )

    def test_unsafe_relative_path_rejected(self) -> None:
        with self.assertRaisesRegex(module.ExportError, "safe relative path"):
            module._safe_relative("../outside.json", "test")

    def test_source_manifest_pins_exact_schema_and_frozen_contracts(self) -> None:
        required = {"layer1.json", "layer2.json", "det_ingrs.json", *module.INDEX_FILES.values(),
                    "preprocess.py", "recipe1m.yaml", "nodes_191120.csv"}
        files = {}
        for name in required:
            relative = {
                "preprocess.py": "inv_cooking/datasets/recipe1m/preprocess.py",
                "recipe1m.yaml": "conf/dataset/recipe1m.yaml",
                "nodes_191120.csv": "flavorgraph/nodes_191120.csv",
            }.get(name, name)
            files[name] = {"relative_path": relative, "byte_count": 1, "sha256": "0" * 64}
        manifest = {
            "schema_version": 1,
            "protocol_v1_sha256": module.PROTOCOL_SHA256,
            "analysis_spec_v1_sha256": module.ANALYSIS_SPEC_SHA256,
            "terms_status": module.TERMS_STATUS,
            "terms_reference": "receipt:local-terms-v1",
            "release_id": "recipe1m-release-v1",
            "source_reference": "receipt:source-release-v1",
            "gismo_commit": module.GISMO_COMMIT,
            "source_files": files,
        }
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "source-manifest.json"
            path.write_text(json.dumps(manifest), encoding="utf-8")
            loaded, digest = module._load_manifest(path)
            self.assertEqual(loaded, manifest)
            self.assertEqual(digest, module.hashlib.sha256(path.read_bytes()).hexdigest())

            manifest["protocol_v1_sha256"] = "f" * 64
            path.write_text(json.dumps(manifest), encoding="utf-8")
            with self.assertRaisesRegex(module.ExportError, "Protocol v1 hash mismatch"):
                module._load_manifest(path)

            manifest["protocol_v1_sha256"] = module.PROTOCOL_SHA256
            manifest["schema_version"] = True
            path.write_text(json.dumps(manifest), encoding="utf-8")
            with self.assertRaisesRegex(module.ExportError, "unsupported source manifest schema"):
                module._load_manifest(path)

    def test_source_file_hash_drift_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = root / "source.bin"
            path.write_bytes(b"synthetic")
            record = {"relative_path": "source.bin", "byte_count": 9, "sha256": "0" * 64}
            with self.assertRaisesRegex(module.ExportError, "size or hash mismatch"):
                module._verify_source_file(root, "source.bin", record)

    def test_pinned_index_parser_hashes_the_exact_bytes_it_parses(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "train.txt"
            raw = b"0\n2\n"
            path.write_bytes(raw)
            record = {"byte_count": len(raw), "sha256": module.hashlib.sha256(raw).hexdigest()}
            self.assertEqual(module._load_pinned_indices({"train": path}, {"train": record}), {"train": [0, 2]})
            path.write_bytes(b"0\n3\n")
            with self.assertRaisesRegex(module.ExportError, "differ from the pinned receipt"):
                module._load_pinned_indices({"train": path}, {"train": record})

    def test_atomic_writer_refuses_to_recreate_a_missing_parent(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            parent = Path(directory) / "removed-parent"
            output = parent / "result"
            with self.assertRaisesRegex(module.ExportError, "parent disappeared"):
                module._write_atomic_new_directory(output, {"manifest.json": b"{}\n"})
            self.assertFalse(parent.exists())

    def test_entry_check_treats_only_not_found_as_absent(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            missing = Path(directory) / "missing"
            self.assertFalse(module._entry_exists(missing))
            with mock.patch.object(Path, "lstat", side_effect=PermissionError("denied")):
                with self.assertRaisesRegex(module.ExportError, "state could not be verified"):
                    module._entry_exists(missing)

    def test_inaccessible_output_is_refused_before_manifest_or_source_processing(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            worktree = root / "worktree"
            source_root = root / "recipe1m"
            gismo_root = root / "gismo"
            worktree.mkdir()
            source_root.mkdir()
            gismo_root.mkdir()
            manifest = root / "source-manifest.json"
            manifest.write_text("{}", encoding="utf-8")
            output = root / "private-output"

            def resolve_input(path: Path, _worktree: Path, *, exists: bool) -> Path:
                return path.resolve(strict=exists)

            with mock.patch.object(module, "_require_private_outside_worktree", side_effect=resolve_input), \
                    mock.patch.object(module, "_entry_exists", side_effect=module.ExportError(
                        "generated directory state could not be verified"
                    )) as entry_check, \
                    mock.patch.object(module, "_load_manifest") as load_manifest, \
                    mock.patch.object(module, "_invoke_frozen_gismo") as invoke_gismo:
                with self.assertRaisesRegex(module.ExportError, "state could not be verified"):
                    module.export_order(
                        source_root=source_root,
                        gismo_root=gismo_root,
                        output_dir=output,
                        manifest_path=manifest,
                        worktree=worktree,
                        allow_private_real_inputs=True,
                    )

            entry_check.assert_called_once_with(output.resolve())
            load_manifest.assert_not_called()
            invoke_gismo.assert_not_called()

    def test_atomic_writer_checks_each_staged_file_and_promoted_file(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"
            checked: list[Path] = []

            def record_check(path: Path) -> None:
                checked.append(path)

            with mock.patch.object(module, "_enforce_windows_acl", return_value=True), \
                    mock.patch.object(module, "_require_private_local_path", side_effect=record_check):
                module._write_atomic_new_directory(output, {"train.jsonl": b"id\n", "manifest.json": b"{}\n"})

            self.assertTrue(output.is_dir())
            self.assertEqual([path.name for path in checked].count("train.jsonl"), 2)
            self.assertEqual([path.name for path in checked].count("manifest.json"), 2)

    def test_atomic_writer_rolls_back_just_promoted_output_on_acl_failure(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"

            def fail_for_final_output(path: Path) -> None:
                if path == output:
                    raise module.ExportError("simulated final ACL failure")

            with mock.patch.object(module, "_enforce_windows_acl", return_value=True), \
                    mock.patch.object(module, "_require_private_local_path", side_effect=fail_for_final_output):
                with self.assertRaisesRegex(module.ExportError, "rolled back"):
                    module._write_atomic_new_directory(output, {"train.jsonl": b"private-id\n"})

            self.assertFalse(output.exists())

    def test_atomic_writer_rolls_back_on_interrupt_during_final_acl_check(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"

            def interrupt_for_final_output(path: Path) -> None:
                if path == output:
                    raise KeyboardInterrupt()

            with mock.patch.object(module, "_enforce_windows_acl", return_value=True), \
                    mock.patch.object(module, "_require_private_local_path", side_effect=interrupt_for_final_output):
                with self.assertRaises(KeyboardInterrupt):
                    module._write_atomic_new_directory(output, {"train.jsonl": b"private-id\n"})

            self.assertFalse(output.exists())

    def test_atomic_writer_rolls_back_on_system_exit_during_final_file_acl_check(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"

            def exit_for_final_file(path: Path) -> None:
                if path == output / "train.jsonl":
                    raise SystemExit(9)

            with mock.patch.object(module, "_enforce_windows_acl", return_value=True), \
                    mock.patch.object(module, "_require_private_local_path", side_effect=exit_for_final_file):
                with self.assertRaises(SystemExit):
                    module._write_atomic_new_directory(output, {"train.jsonl": b"private-id\n"})

            self.assertFalse(output.exists())

    def test_atomic_writer_reconciles_interrupt_between_move_and_state_update(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"

            def move_then_interrupt(staging: Path, target: Path) -> None:
                staging.rename(target)
                raise KeyboardInterrupt()

            with mock.patch.object(module, "_enforce_windows_acl", return_value=False), \
                    mock.patch.object(module, "_atomic_promote_directory", side_effect=move_then_interrupt):
                with self.assertRaises(KeyboardInterrupt):
                    module._write_atomic_new_directory(output, {"train.jsonl": b"private-id\n"})

            self.assertFalse(output.exists())

    def test_atomic_writer_surfaces_unverifiable_promotion_state(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"
            real_entry_exists = module._entry_exists
            calls = 0

            def interrupt_state_check(path: Path) -> bool:
                nonlocal calls
                calls += 1
                if calls == 1:
                    return real_entry_exists(path)
                raise KeyboardInterrupt()

            def move_then_fail(staging: Path, target: Path) -> None:
                staging.rename(target)
                raise RuntimeError("synthetic post-move interruption boundary")

            with mock.patch.object(module, "_enforce_windows_acl", return_value=False), \
                    mock.patch.object(module, "_entry_exists", side_effect=interrupt_state_check), \
                    mock.patch.object(module, "_atomic_promote_directory", side_effect=move_then_fail):
                with self.assertRaisesRegex(module.ExportError, "state is unknown; secure cleanup is required"):
                    module._write_atomic_new_directory(output, {"train.jsonl": b"private-id\n"})

            # The output is deliberately preserved because state was unknowable;
            # the explicit error is the recovery signal and the fixture is test-owned.
            self.assertTrue(output.is_dir())
            shutil.rmtree(output)

    def test_atomic_writer_retries_cleanup_interrupted_once(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"
            real_rmtree = shutil.rmtree
            calls = 0

            def interrupt_first_cleanup(path: Path) -> None:
                nonlocal calls
                calls += 1
                if calls == 1:
                    raise KeyboardInterrupt()
                real_rmtree(path)

            def fail_final_acl(path: Path) -> None:
                if path == output:
                    raise module.ExportError("simulated ACL failure")

            with mock.patch.object(module, "_enforce_windows_acl", return_value=True), \
                    mock.patch.object(module, "_require_private_local_path", side_effect=fail_final_acl), \
                    mock.patch.object(module.shutil, "rmtree", side_effect=interrupt_first_cleanup):
                with self.assertRaises(KeyboardInterrupt):
                    module._write_atomic_new_directory(output, {"train.jsonl": b"private-id\n"})

            self.assertFalse(output.exists())

    def test_atomic_writer_surfaces_secure_rollback_failure(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "result"

            def fail_for_final_output(path: Path) -> None:
                if path == output:
                    raise module.ExportError("simulated final ACL failure")

            with mock.patch.object(module, "_enforce_windows_acl", return_value=True), \
                    mock.patch.object(module, "_require_private_local_path", side_effect=fail_for_final_output), \
                    mock.patch.object(module.shutil, "rmtree", side_effect=OSError("simulated cleanup failure")):
                with self.assertRaisesRegex(module.ExportError, "rollback cleanup failed; secure cleanup is required"):
                    module._write_atomic_new_directory(output, {"train.jsonl": b"private-id\n"})

            # The test owns and removes the deliberately stranded synthetic fixture.
            shutil.rmtree(output, ignore_errors=True)

    def test_atomic_directory_writer_never_overwrites_existing_output(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "existing"
            output.mkdir()
            marker = output / "keep.txt"
            marker.write_text("preserve", encoding="utf-8")
            with self.assertRaisesRegex(module.ExportError, "already exists"):
                module._write_atomic_new_directory(output, {"new.jsonl": b"not written\n"})
            self.assertEqual(marker.read_text(encoding="utf-8"), "preserve")
            self.assertFalse((output / "new.jsonl").exists())

    @mock.patch.object(module.importlib, "import_module")
    def test_upstream_exception_diagnostic_does_not_leak_row_text(self, importer: mock.Mock) -> None:
        importer.side_effect = [object(), RuntimeError("private recipe title")]
        with tempfile.TemporaryDirectory() as directory:
            with self.assertRaises(module.ExportError) as caught:
                module._invoke_frozen_gismo(Path(directory), Path(directory))
        self.assertIn("RuntimeError", str(caught.exception))
        self.assertNotIn("private recipe title", str(caught.exception))

    @mock.patch.object(module.importlib, "import_module")
    def test_upstream_stdout_and_stderr_are_suppressed(self, importer: mock.Mock) -> None:
        class Omega:
            class OmegaConf:
                @staticmethod
                def load(_path: Path) -> object:
                    return type("Config", (), {"pre_processing": object()})()

        class Preprocess:
            @staticmethod
            def load_unprocessed_dataset(_path: str) -> tuple[list, list, list]:
                print("private title")
                print("private ingredient", file=sys.stderr)
                return [], [], []

            @staticmethod
            def build_vocab_recipe1m(*_args: object) -> tuple[None, None, None, dict]:
                print("private processed row")
                return None, None, None, {"train": [], "val": [], "test": []}

        importer.side_effect = [Omega, Preprocess]
        captured_out = io.StringIO()
        captured_err = io.StringIO()
        with tempfile.TemporaryDirectory() as directory:
            preprocess_file = Path(directory) / "inv_cooking" / "datasets" / "recipe1m" / "preprocess.py"
            preprocess_file.parent.mkdir(parents=True)
            preprocess_file.write_text("# synthetic GISMo module path\n", encoding="utf-8")
            Preprocess.__file__ = str(preprocess_file)
            with contextlib.redirect_stdout(captured_out), contextlib.redirect_stderr(captured_err):
                layer1, processed = module._invoke_frozen_gismo(Path(directory), Path(directory))
        self.assertEqual(layer1, [])
        self.assertEqual(set(processed), {"train", "val", "test"})
        self.assertEqual(captured_out.getvalue(), "")
        self.assertEqual(captured_err.getvalue(), "")

    def test_jsonl_contains_only_explicit_id_order_fields(self) -> None:
        encoded = module._canonical_jsonl([
            {"source_split": "validation", "source_ordinal": 3,
             "filtered_position": 0, "recipe_id": "r4"}
        ])
        self.assertTrue(encoded.endswith(b"\n"))
        self.assertEqual(encoded, b'{"filtered_position":0,"recipe_id":"r4","source_ordinal":3,"source_split":"validation"}\n')
        self.assertNotIn(b"instructions", encoded)
        self.assertNotIn(b"title", encoded)


class KaggleAdmissionLifecycleTests(unittest.TestCase):
    def _dependencies(self, fixture, invoke_side_effect, *, clock_class=None, monotonic=None):
        stack = contextlib.ExitStack()
        stack.enter_context(mock.patch.object(module, "__file__", str(fixture["exporter_path"])))
        stack.enter_context(mock.patch.object(module, "_require_private_local_path"))
        stack.enter_context(mock.patch.object(module, "_check_packaged_gismo"))
        stack.enter_context(mock.patch.object(module, "_check_git_commit"))
        if clock_class is not None:
            stack.enter_context(mock.patch.object(module.dt, "datetime", clock_class))
        if monotonic is not None:
            stack.enter_context(mock.patch.object(module.time, "monotonic", side_effect=monotonic))
        invoke = stack.enter_context(mock.patch.object(
            module, "_invoke_frozen_gismo", side_effect=invoke_side_effect
        ))
        return stack, invoke

    def test_stale_and_future_receipts_fail_before_manifest_or_source_use(self) -> None:
        real_datetime = module.dt.datetime
        now = real_datetime(2026, 10, 28, 12, 0, tzinfo=module.dt.timezone.utc)

        class FakeDateTime(real_datetime):
            current = None

            @classmethod
            def now(cls, tz=None):
                return cls.current if tz else cls.current.replace(tzinfo=None)

        for observed_at, message in (
            (now - module.PREFLIGHT_ATTESTATION_MAX_AGE - module.dt.timedelta(seconds=1), "stale"),
            (now + module.dt.timedelta(seconds=1), "future"),
        ):
            with self.subTest(message=message), tempfile.TemporaryDirectory() as directory:
                FakeDateTime.current = now
                fixture = _kaggle_export_fixture(Path(directory), observed_at)
                invoke = mock.Mock(return_value=(fixture["layer1"], fixture["processed"]))
                stack, _ = self._dependencies(fixture, invoke, clock_class=FakeDateTime)
                with stack, mock.patch.object(module, "_load_manifest") as load_manifest:
                    with self.assertRaisesRegex(module.ExportError, message):
                        module.export_order(**fixture["kwargs"])
                    load_manifest.assert_not_called()
                invoke.assert_not_called()

    def test_fresh_admission_reaches_publication_after_eleven_minutes(self) -> None:
        real_datetime = module.dt.datetime
        now = real_datetime(2026, 10, 28, 12, 0, tzinfo=module.dt.timezone.utc)

        class FakeDateTime(real_datetime):
            current = None

            @classmethod
            def now(cls, tz=None):
                return cls.current if tz else cls.current.replace(tzinfo=None)

        FakeDateTime.current = now
        with tempfile.TemporaryDirectory() as directory:
            fixture = _kaggle_export_fixture(Path(directory), now)

            def process(_gismo, _source):
                FakeDateTime.current = now + module.dt.timedelta(minutes=11)
                return fixture["layer1"], fixture["processed"]

            stack, invoke = self._dependencies(
                fixture, process, clock_class=FakeDateTime, monotonic=[100.0, 101.0, 102.0]
            )
            with stack:
                result = module.export_order(**fixture["kwargs"])

            invoke.assert_called_once()
            self.assertEqual(result["status"], "PRIVATE_REAL_ORDER_EXPORTED")
            self.assertTrue((fixture["output_dir"] / "manifest.json").is_file())

    def test_changed_attestation_exporter_package_source_and_output_path_fail_closed(self) -> None:
        now = module.dt.datetime.now(module.dt.timezone.utc)
        cases = ("attestation", "exporter", "package", "source", "source_manifest", "output")
        for changed in cases:
            with self.subTest(changed=changed), tempfile.TemporaryDirectory() as directory:
                fixture = _kaggle_export_fixture(Path(directory), now)

                def process(_gismo, _source):
                    if changed == "attestation":
                        value = json.loads(fixture["attestation_path"].read_text(encoding="utf-8"))
                        value["owner"] = "other"
                        fixture["attestation_path"].write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
                    elif changed == "exporter":
                        fixture["exporter_path"].write_bytes(b"changed exporter bytes")
                    elif changed == "package":
                        fixture["package_manifest"].write_text('{"fixture":false}\n', encoding="utf-8")
                    elif changed == "source":
                        (fixture["source_root"] / "layer1.json").write_bytes(b"changed source bytes")
                    elif changed == "source_manifest":
                        fixture["source_manifest"].write_text("{}\n", encoding="utf-8")
                    elif changed == "output":
                        fixture["output_dir"].mkdir()
                        (fixture["output_dir"] / "sentinel").write_text("preserve", encoding="utf-8")
                    return fixture["layer1"], fixture["processed"]

                stack, invoke = self._dependencies(
                    fixture, process, monotonic=[0.0, 1.0, 2.0]
                )
                with stack:
                    with self.assertRaises(module.ExportError):
                        module.export_order(**fixture["kwargs"])
                invoke.assert_called_once()
                if changed != "output":
                    self.assertFalse(fixture["output_dir"].exists())
                else:
                    self.assertEqual((fixture["output_dir"] / "sentinel").read_text(), "preserve")

    def test_timeout_after_processing_does_not_promote_or_retry(self) -> None:
        now = module.dt.datetime.now(module.dt.timezone.utc)
        with tempfile.TemporaryDirectory() as directory:
            fixture = _kaggle_export_fixture(Path(directory), now)
            stack, invoke = self._dependencies(
                fixture, lambda *_: (fixture["layer1"], fixture["processed"]),
                monotonic=[0.0, module.EXPORTER_MAX_RUNTIME_SECONDS + 1],
            )
            writer = mock.patch.object(module, "_write_atomic_new_directory")
            with stack, writer as promote:
                with self.assertRaisesRegex(module.ExportError, "time limit"):
                    module.export_order(**fixture["kwargs"])
            invoke.assert_called_once()
            promote.assert_not_called()
            self.assertFalse(fixture["output_dir"].exists())

    def test_admission_is_invocation_local_and_not_cli_supplied(self) -> None:
        real_datetime = module.dt.datetime
        now = real_datetime.now(module.dt.timezone.utc)

        class FakeDateTime(real_datetime):
            current = None

            @classmethod
            def now(cls, tz=None):
                return cls.current if tz else cls.current.replace(tzinfo=None)

        FakeDateTime.current = now
        with tempfile.TemporaryDirectory() as directory:
            fixture = _kaggle_export_fixture(Path(directory), now)
            stack, first_invoke = self._dependencies(
                fixture, lambda *_: (fixture["layer1"], fixture["processed"]),
                clock_class=FakeDateTime, monotonic=[0.0, 1.0, 2.0],
            )
            with stack:
                module.export_order(**fixture["kwargs"])
            first_invoke.assert_called_once()

            FakeDateTime.current = now + module.dt.timedelta(minutes=11)
            second_kwargs = dict(fixture["kwargs"])
            second_kwargs["output_dir"] = fixture["output_dir"].with_name("second-result")
            second_invoke = mock.Mock(return_value=(fixture["layer1"], fixture["processed"]))
            stack, _ = self._dependencies(fixture, second_invoke, clock_class=FakeDateTime)
            with stack:
                with self.assertRaisesRegex(module.ExportError, "stale"):
                    module.export_order(**second_kwargs)
            second_invoke.assert_not_called()

        parser_args = ["--source-root", "s", "--gismo-root", "g", "--manifest", "m",
                       "--output-dir", "o", "--worktree", "w", "--admission-receipt", "prior"]
        with mock.patch.object(module, "export_order") as export_call:
            with self.assertRaises(SystemExit):
                module.main(parser_args)
            export_call.assert_not_called()


class PackagedSnapshotIdentityTests(unittest.TestCase):
    def _run_kaggle_export(self, fixture, process):
        stack = contextlib.ExitStack()
        stack.enter_context(mock.patch.object(module, "__file__", str(fixture["exporter_path"])))
        stack.enter_context(mock.patch.object(module, "_require_private_local_path"))
        stack.enter_context(mock.patch.object(module, "_invoke_frozen_gismo", side_effect=process))
        return stack

    def test_kaggle_snapshot_reaches_publication_without_git_metadata_or_git_calls(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            fixture = _complete_kaggle_package_fixture(
                _kaggle_export_fixture(Path(directory))
            )
            gismo_root = fixture["kwargs"]["gismo_root"]
            self.assertFalse((gismo_root / ".git").exists())
            with self._run_kaggle_export(
                fixture, lambda *_: (fixture["layer1"], fixture["processed"])
            ), mock.patch.object(
                module, "_check_packaged_gismo", wraps=module._check_packaged_gismo
            ) as package_check, mock.patch.object(
                module, "_check_git_commit", side_effect=AssertionError("Git verifier called")
            ) as git_check, mock.patch.object(
                module.subprocess, "run", side_effect=AssertionError("Git subprocess called")
            ) as subprocess_run:
                result = module.export_order(**fixture["kwargs"])

            self.assertEqual(result["status"], "PRIVATE_REAL_ORDER_EXPORTED")
            self.assertTrue((fixture["output_dir"] / "manifest.json").is_file())
            self.assertEqual(package_check.call_count, 3)
            git_check.assert_not_called()
            subprocess_run.assert_not_called()

    def test_post_transform_package_drift_fails_before_order_extraction_or_publication(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            fixture = _complete_kaggle_package_fixture(
                _kaggle_export_fixture(Path(directory))
            )
            pinned_preprocessor = (
                fixture["kwargs"]["gismo_root"]
                / "inv_cooking" / "datasets" / "recipe1m" / "preprocess.py"
            )

            def mutate_after_transform(*_args):
                pinned_preprocessor.write_bytes(b"changed after package admission\n")
                return fixture["layer1"], fixture["processed"]

            with self._run_kaggle_export(fixture, mutate_after_transform), \
                    mock.patch.object(
                        module, "_check_packaged_gismo", wraps=module._check_packaged_gismo
                    ) as package_check, \
                    mock.patch.object(module, "_check_git_commit") as git_check, \
                    mock.patch.object(module, "_extract_order", wraps=module._extract_order) as extract, \
                    mock.patch.object(module, "_write_atomic_new_directory") as promote:
                with self.assertRaisesRegex(module.ExportError, "source-only package file differs"):
                    module.export_order(**fixture["kwargs"])

            self.assertEqual(package_check.call_count, 2)
            git_check.assert_not_called()
            extract.assert_not_called()
            promote.assert_not_called()
            self.assertFalse(fixture["output_dir"].exists())

    def test_windows_profile_retains_post_transform_git_revision_check(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            fixture = _kaggle_export_fixture(Path(directory))
            fixture["kwargs"]["execution_profile"] = "windows-owner-only"
            git_failure = module.ExportError("GISMo checkout is not at the frozen source commit")
            with mock.patch.object(module, "__file__", str(fixture["exporter_path"])), \
                    mock.patch.object(module, "_require_private_local_path"), \
                    mock.patch.object(
                        module, "_check_git_commit", side_effect=[None, git_failure]
                    ) as git_check, \
                    mock.patch.object(module, "_check_packaged_gismo") as package_check, \
                    mock.patch.object(
                        module, "_invoke_frozen_gismo",
                        return_value=(fixture["layer1"], fixture["processed"]),
                    ) as invoke, \
                    mock.patch.object(module, "_write_atomic_new_directory") as promote:
                with self.assertRaisesRegex(module.ExportError, "frozen source commit"):
                    module.export_order(**fixture["kwargs"])

            self.assertEqual(git_check.call_count, 2)
            package_check.assert_not_called()
            invoke.assert_called_once()
            promote.assert_not_called()
            self.assertFalse(fixture["output_dir"].exists())


if __name__ == "__main__":
    unittest.main()
