"""Synthetic-only integrity and archive-safety tests for T535 bootstrap."""

from __future__ import annotations

import hashlib
import json
import os
import sys
import tarfile
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bootstrap_t535_kaggle as bootstrap


def _put(path: Path, content: bytes) -> str:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(content)
    return hashlib.sha256(content).hexdigest()


def _package(root: Path):
    package_root = root / "input" / "package"
    mkl_license = (Path(__file__).resolve().parent / "third_party_licenses" /
                   "intel-mkl-2020.2-license-april-2018.txt").read_bytes()
    openmp_license = (Path(__file__).resolve().parent / "third_party_licenses" /
                      "intel-openmp-2025.0.0-license-october-2022.txt").read_bytes()
    jpeg_license = (Path(__file__).resolve().parent / "third_party_licenses" /
                    "jpeg-9b-ijg-license-notice.txt").read_bytes()
    zlib_license = (Path(__file__).resolve().parent / "third_party_licenses" /
                    "zlib-1.3.2-license.txt").read_bytes()
    files = {
        "code/export_c3_recipe1m_order.py": b"# fixture exporter\n",
        "code/bootstrap_t535_kaggle.py": b"# fixture bootstrap\n",
        "gismo/inv_cooking/__init__.py": b"# fixture GISMo\n",
        "gismo/LICENSE.md": b"Attribution-NonCommercial 4.0 International\n",
        "licenses/intel-mkl-2020.2-license-april-2018.txt": mkl_license,
        "licenses/intel-openmp-2025.0.0-license-october-2022.txt": openmp_license,
        "licenses/jpeg-9b-ijg-license-notice.txt": jpeg_license,
        "licenses/zlib-1.3.2-license.txt": zlib_license,
        "gismo/data/splits/recipe1m/train.txt": b"0\n",
        "gismo/data/splits/recipe1m/val_all.txt": b"0\n",
        "gismo/data/splits/recipe1m/val.txt": b"0\n",
        "gismo/data/splits/recipe1m/test.txt": b"0\n",
        "runtime/kaggle-linux-runtime-v6.tar.gz": b"not a real runtime archive\n",
        "runtime/t535-kaggle-runtime-linux.yml": b"name: fixture\n",
    }
    entries = []
    for relative, content in files.items():
        path = package_root / relative
        digest = _put(path, content)
        entries.append({"path": relative, "size_bytes": len(content), "sha256": digest})
    exporter_sha = next(item["sha256"] for item in entries if item["path"].startswith("code/"))
    bootstrap_sha = next(item["sha256"] for item in entries if item["path"].endswith("bootstrap_t535_kaggle.py"))
    archive = next(item for item in entries if item["path"] == "runtime/kaggle-linux-runtime-v6.tar.gz")
    manifest = {
        "schema": bootstrap.SCHEMA,
        "version": bootstrap.PACKAGE_VERSION,
        "gismo_commit": "f5d2394c7ade897987ce584d2a75d8bbbbae058e",
        "exporter": {"path": "code/export_c3_recipe1m_order.py", "sha256": exporter_sha},
        "bootstrap": {"path": "code/bootstrap_t535_kaggle.py", "sha256": bootstrap_sha},
        "runtime": {
            "archive": {"path": archive["path"], "sha256": archive["sha256"]},
            "platform_spec": {
                "path": "runtime/t535-kaggle-runtime-linux.yml",
                "sha256": next(item["sha256"] for item in entries
                                if item["path"] == "runtime/t535-kaggle-runtime-linux.yml"),
            },
            "resolution_record": bootstrap.RUNTIME_RESOLUTION_RECORD,
            "provenance": "synthetic fixture",
        },
        "third_party_licenses": bootstrap.THIRD_PARTY_LICENSES,
        "source_data": {
            "recipe1m_release_files_included": False,
            "gismo_source_derived_split_indices_included": True,
            "gismo_source_derived_split_index_paths": [
                "gismo/data/splits/recipe1m/train.txt",
                "gismo/data/splits/recipe1m/val_all.txt",
                "gismo/data/splits/recipe1m/val.txt",
                "gismo/data/splits/recipe1m/test.txt",
            ],
            "recipe1m_release_mount_required_later": True,
        },
        "credentials_included": False,
        "files": entries,
    }
    raw = (json.dumps(manifest, sort_keys=True, indent=2) + "\n").encode("utf-8")
    package_sha = _put(package_root / bootstrap.PACKAGE_MANIFEST, raw)
    return package_root, package_sha


def _make_tar(path: Path, members):
    with tarfile.open(str(path), "w:gz") as tar:
        for name, kind, payload in members:
            info = tarfile.TarInfo(name)
            if kind == "dir":
                info.type = tarfile.DIRTYPE
                tar.addfile(info)
            elif kind == "symlink":
                info.type = tarfile.SYMTYPE
                info.linkname = payload
                tar.addfile(info)
            elif kind == "hardlink":
                info.type = tarfile.LNKTYPE
                info.linkname = payload
                tar.addfile(info)
            else:
                data = payload
                info.size = len(data)
                import io
                tar.addfile(info, io.BytesIO(data))


class T535BootstrapTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.input_root = self.root / "input"
        self.input_root.mkdir()
        self.working = self.root / "working"
        self.working.mkdir()

    def test_exact_package_manifest_and_inventory_validate(self):
        package_root, digest = _package(self.root)
        manifest, entries = bootstrap._manifest(package_root, digest, self.input_root)
        self.assertEqual(manifest["gismo_commit"], "f5d2394c7ade897987ce584d2a75d8bbbbae058e")
        self.assertEqual(bootstrap.THIRD_PARTY_LICENSES[3]["source_url"],
                         "http://www.ijg.org/files/jpegsrc.v9b.tar.gz")
        self.assertEqual(set(entries), {"code/export_c3_recipe1m_order.py",
                                        "code/bootstrap_t535_kaggle.py",
                                        "gismo/inv_cooking/__init__.py", "gismo/LICENSE.md",
                                        "licenses/intel-mkl-2020.2-license-april-2018.txt",
                                        "licenses/intel-openmp-2025.0.0-license-october-2022.txt",
                                        "licenses/jpeg-9b-ijg-license-notice.txt",
                                        "licenses/zlib-1.3.2-license.txt",
                                        "gismo/data/splits/recipe1m/train.txt",
                                        "gismo/data/splits/recipe1m/val_all.txt",
                                        "gismo/data/splits/recipe1m/val.txt",
                                        "gismo/data/splits/recipe1m/test.txt",
                                        "runtime/kaggle-linux-runtime-v6.tar.gz",
                                        "runtime/t535-kaggle-runtime-linux.yml"})

    def test_runtime_manifest_rejects_reintroduced_source_host_lockfiles(self):
        package_root, _digest = _package(self.root)
        manifest_path = package_root / bootstrap.PACKAGE_MANIFEST
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["runtime"]["locks"] = {
            "pip-freeze.lock": {"path": "runtime/pip-freeze.lock", "sha256": "a" * 64}
        }
        manifest_path.write_text(json.dumps(manifest, sort_keys=True, indent=2) + "\n", encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "runtime archive"):
            bootstrap._manifest(package_root, bootstrap.sha256_file(manifest_path), self.input_root)

    def test_missing_declared_source_derived_index_is_rejected(self):
        package_root, _digest = _package(self.root)
        (package_root / "gismo/data/splits/recipe1m/train.txt").unlink()
        manifest_path = package_root / bootstrap.PACKAGE_MANIFEST
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["files"] = [entry for entry in manifest["files"]
                             if entry["path"] != "gismo/data/splits/recipe1m/train.txt"]
        manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
        digest = bootstrap.sha256_file(manifest_path)
        with self.assertRaisesRegex(bootstrap.BootstrapError, "split-index inventory"):
            bootstrap._manifest(package_root, digest, self.input_root)

    def test_boolean_manifest_version_is_rejected(self):
        package_root, _digest = _package(self.root)
        manifest_path = package_root / bootstrap.PACKAGE_MANIFEST
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        manifest["version"] = True
        manifest_path.write_text(json.dumps(manifest), encoding="utf-8")
        digest = bootstrap.sha256_file(manifest_path)
        with self.assertRaisesRegex(bootstrap.BootstrapError, "unsupported schema"):
            bootstrap._manifest(package_root, digest, self.input_root)

    def test_modified_package_file_is_rejected(self):
        package_root, digest = _package(self.root)
        (package_root / "gismo/inv_cooking/__init__.py").write_bytes(b"# changed GISMo\n")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "hash"):
            bootstrap._manifest(package_root, digest, self.input_root)

    def test_modified_mkl_license_notice_is_rejected(self):
        package_root, digest = _package(self.root)
        notice = package_root / "licenses/intel-mkl-2020.2-license-april-2018.txt"
        notice.write_bytes(b"x" * notice.stat().st_size)
        with self.assertRaisesRegex(bootstrap.BootstrapError, "hash"):
            bootstrap._manifest(package_root, digest, self.input_root)

    def test_modified_openmp_license_notice_is_rejected(self):
        package_root, digest = _package(self.root)
        notice = package_root / "licenses/intel-openmp-2025.0.0-license-october-2022.txt"
        notice.write_bytes(b"x" * notice.stat().st_size)
        with self.assertRaisesRegex(bootstrap.BootstrapError, "hash"):
            bootstrap._manifest(package_root, digest, self.input_root)

    def test_modified_jpeg_and_zlib_license_notices_are_rejected(self):
        for relative in ("licenses/jpeg-9b-ijg-license-notice.txt",
                         "licenses/zlib-1.3.2-license.txt"):
            package_root, digest = _package(self.root)
            notice = package_root / relative
            notice.write_bytes(b"x" * notice.stat().st_size)
            with self.subTest(path=relative):
                with self.assertRaisesRegex(bootstrap.BootstrapError, "hash"):
                    bootstrap._manifest(package_root, digest, self.input_root)

    def test_unexpected_file_and_symlink_are_rejected(self):
        package_root, digest = _package(self.root)
        (package_root / "extra.txt").write_text("extra", encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "inventory"):
            bootstrap._manifest(package_root, digest, self.input_root)
        (package_root / "extra.txt").unlink()
        outside = self.root / "outside"
        outside.write_text("outside", encoding="utf-8")
        try:
            (package_root / "extra-link").symlink_to(outside)
        except (OSError, NotImplementedError):
            self.skipTest("symlink creation unavailable")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "symlink"):
            bootstrap._manifest(package_root, digest, self.input_root)

    def test_wrong_mount_is_rejected(self):
        package_root, digest = _package(self.root)
        elsewhere = self.root / "elsewhere"
        elsewhere.mkdir()
        with self.assertRaisesRegex(bootstrap.BootstrapError, "mounted read-only"):
            bootstrap._manifest(package_root, digest, elsewhere)

    def test_live_preflight_attestation_binds_exact_mount_and_manifest_before_runtime(self):
        input_root = self.root / "input"
        dataset_root = input_root / "source"
        dataset_root.mkdir(parents=True)
        working = self.root / "working"
        source_manifest = dataset_root / "source-manifest.json"
        source_manifest.write_bytes(b"synthetic metadata manifest only\n")
        package_root, package_sha = _package(self.root)
        exporter_sha = json.loads((package_root / bootstrap.PACKAGE_MANIFEST).read_text(encoding="utf-8"))["exporter"]["sha256"]
        observed = bootstrap.dt.datetime(2026, 10, 27, 12, 30, 0, tzinfo=bootstrap.dt.timezone.utc)
        value = {
            "schema_version": 1, "observed_at_utc": observed.isoformat().replace("+00:00", "Z"),
            "owner": "owner", "dataset_slug": "owner/source", "kernel_slug": "owner/job",
            "dataset_private": True, "kernel_private": True,
            "dataset_collaborators": [], "kernel_collaborators": [],
            "gpu_enabled": False, "internet_enabled": False,
            "source_manifest_sha256": bootstrap.sha256_file(source_manifest),
            "gismo_commit": "a" * 40, "exporter_sha256": exporter_sha,
            "package_manifest_sha256": package_sha,
        }
        attestation = working / "attestation.json"
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        bootstrap.verify_preflight_attestation(
            attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
            package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
            working, input_root, now_utc=observed,
        )
        value["observed_at_utc"] = (observed - bootstrap.PREFLIGHT_ATTESTATION_MAX_AGE).isoformat().replace("+00:00", "Z")
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        bootstrap.verify_preflight_attestation(
            attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
            package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
            working, input_root, now_utc=observed,
        )
        value["observed_at_utc"] = (observed - bootstrap.PREFLIGHT_ATTESTATION_MAX_AGE
                                    - bootstrap.dt.timedelta(seconds=1)).isoformat().replace("+00:00", "Z")
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "stale"):
            bootstrap.verify_preflight_attestation(
                attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
                package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
                working, input_root, now_utc=observed,
            )
        value["observed_at_utc"] = (observed + bootstrap.dt.timedelta(seconds=1)).isoformat().replace("+00:00", "Z")
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "future"):
            bootstrap.verify_preflight_attestation(
                attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
                package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
                working, input_root, now_utc=observed,
            )
        value["observed_at_utc"] = observed.isoformat().replace("+00:00", "Z")
        value["observed_at_utc"] = "not-a-timestamp"
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "observation time is invalid"):
            bootstrap.verify_preflight_attestation(
                attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
                package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
                working, input_root, now_utc=observed,
            )
        value["observed_at_utc"] = observed.isoformat().replace("+00:00", "Z")
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        value.pop("observed_at_utc")
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "schema mismatch"):
            bootstrap.verify_preflight_attestation(
                attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
                package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
                working, input_root, now_utc=observed,
            )
        value["observed_at_utc"] = observed.isoformat().replace("+00:00", "Z")
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        value["observed_at_utc"] = observed.astimezone(
            bootstrap.dt.timezone(bootstrap.dt.timedelta(hours=1))).isoformat()
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "observation time must be UTC"):
            bootstrap.verify_preflight_attestation(
                attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
                package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
                working, input_root, now_utc=observed,
            )
        value["observed_at_utc"] = observed.isoformat().replace("+00:00", "Z")
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        non_utc = observed.astimezone(bootstrap.dt.timezone(bootstrap.dt.timedelta(hours=1)))
        with self.assertRaisesRegex(bootstrap.BootstrapError, "current preflight clock must be UTC"):
            bootstrap.verify_preflight_attestation(
                attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
                package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
                working, input_root, now_utc=non_utc,
            )
        value["observed_at_utc"] = observed.isoformat().replace("+00:00", "Z")
        value["internet_enabled"] = True
        attestation.write_text(json.dumps(value, sort_keys=True), encoding="utf-8")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "private, zero-collaborator"):
            bootstrap.verify_preflight_attestation(
                attestation, bootstrap.sha256_file(attestation), source_manifest, dataset_root,
                package_sha, exporter_sha, "a" * 40, "owner", "owner/source", "owner/job",
                working, input_root, now_utc=observed,
            )

    def test_runtime_archive_hash_mismatch_preserves_no_destination(self):
        archive = self.root / "runtime.tar.gz"
        archive.write_bytes(b"tampered")
        destination = self.working / "runtime"
        with self.assertRaisesRegex(bootstrap.BootstrapError, "hash"):
            bootstrap.safe_extract_runtime(archive, destination, "0" * 64, self.working)
        self.assertFalse(destination.exists())

    def test_safe_archive_extraction_handles_confined_links(self):
        archive = self.root / "runtime.tar.gz"
        _make_tar(archive, [
            ("env/bin", "dir", b""), ("env/lib", "dir", b""),
            ("env/lib/python", "file", b"runtime"),
            ("env/lib/python-copy", "hardlink", "env/lib/python"),
        ])
        destination = self.working / "runtime"
        bootstrap.safe_extract_runtime(archive, destination, bootstrap.sha256_file(archive), self.working)
        self.assertEqual((destination / "env/lib/python").read_bytes(), b"runtime")
        self.assertEqual((destination / "env/lib/python-copy").read_bytes(), b"runtime")

    def test_symlink_targets_are_confined_and_windows_account_limitation_is_visible(self):
        target = bootstrap._link_target(Path("env/bin/python"), "../lib/python")
        self.assertEqual(target.as_posix(), "env/lib/python")
        with self.assertRaisesRegex(bootstrap.BootstrapError, "escapes"):
            bootstrap._link_target(Path("env/bin/python"), "../../../../outside")

    def test_archive_path_traversal_is_rejected_without_escape(self):
        archive = self.root / "runtime.tar.gz"
        _make_tar(archive, [("../escape.txt", "file", b"escape")])
        destination = self.working / "runtime"
        with self.assertRaisesRegex(bootstrap.BootstrapError, "escapes"):
            bootstrap.safe_extract_runtime(archive, destination, bootstrap.sha256_file(archive), self.working)
        self.assertFalse((self.root / "escape.txt").exists())
        self.assertFalse(destination.exists())

    def test_archive_absolute_or_special_entries_are_rejected(self):
        for name in ("/tmp/escape", "env/device"):
            with self.subTest(name=name):
                archive = self.root / ("bad-" + str(abs(hash(name))) + ".tar.gz")
                if name.startswith("/"):
                    _make_tar(archive, [(name, "file", b"x")])
                else:
                    with tarfile.open(str(archive), "w:gz") as tar:
                        info = tarfile.TarInfo(name)
                        info.type = tarfile.FIFOTYPE
                        tar.addfile(info)
                with self.assertRaises(bootstrap.BootstrapError):
                    bootstrap.safe_extract_runtime(archive, self.working / ("out-" + str(abs(hash(name)))),
                                                   bootstrap.sha256_file(archive), self.working)


if __name__ == "__main__":
    unittest.main()
