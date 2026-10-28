"""Synthetic-only tests for the T535 source-free package builder."""

from __future__ import annotations

import hashlib
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import prepare_t535_kaggle_package as package


def _write(path: Path, value: bytes) -> str:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(value)
    return hashlib.sha256(value).hexdigest()


class PackageBuilderTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.workspace = self.root / "workspace"
        self.gismo = self.root / "gismo"
        self.runtime = self.root / "runtime"
        self.workspace.mkdir()
        self.gismo.mkdir()
        self.runtime.mkdir()

        exporter_bytes = b"# synthetic exporter fixture; no source data\n"
        self.exporter_sha = _write(self.workspace / package.EXPORTER_REL, exporter_bytes)
        self.bootstrap_sha = _write(
            self.workspace / package.BOOTSTRAP_REL,
            b"# synthetic fail-closed bootstrap fixture\n",
        )
        self.runtime_yaml_sha = _write(
            self.workspace / package.RUNTIME_YAML_REL,
            b"name: synthetic-runtime\ndependencies: []\n",
        )
        self.runtime_pin_values = {}
        for index, name in enumerate(package.RUNTIME_PINS):
            self.runtime_pin_values[name] = _write(
                self.runtime / name,
                ("synthetic runtime artifact {}\n".format(index)).encode("ascii"),
            )

        for relative in package.GISMO_FILES:
            _write(self.gismo / relative, ("# synthetic pinned file {}\n".format(relative)).encode("utf-8"))
        _write(self.workspace / package.MKL_LICENSE_REL,
               (Path(__file__).resolve().parent / "third_party_licenses" /
                "intel-mkl-2020.2-license-april-2018.txt").read_bytes())
        _write(self.workspace / package.OPENMP_LICENSE_REL,
               (Path(__file__).resolve().parent / "third_party_licenses" /
                "intel-openmp-2025.0.0-license-october-2022.txt").read_bytes())
        _write(self.workspace / package.JPEG_LICENSE_REL,
               (Path(__file__).resolve().parent / "third_party_licenses" /
                "jpeg-9b-ijg-license-notice.txt").read_bytes())
        _write(self.workspace / package.ZLIB_LICENSE_REL,
               (Path(__file__).resolve().parent / "third_party_licenses" /
                "zlib-1.3.2-license.txt").read_bytes())
        subprocess.run(["git", "-C", str(self.gismo), "init", "-q"], check=True)
        subprocess.run(["git", "-C", str(self.gismo), "config", "core.autocrlf", "false"], check=True)
        subprocess.run(["git", "-C", str(self.gismo), "config", "user.name", "Fixture"], check=True)
        subprocess.run(["git", "-C", str(self.gismo), "config", "user.email", "fixture@example.invalid"], check=True)
        subprocess.run(["git", "-C", str(self.gismo), "add", "."], check=True, stderr=subprocess.DEVNULL)
        subprocess.run(["git", "-C", str(self.gismo), "commit", "-qm", "fixture"], check=True)
        self.commit = subprocess.run(
            ["git", "-C", str(self.gismo), "rev-parse", "HEAD"],
            check=True, stdout=subprocess.PIPE, text=True,
        ).stdout.strip()

        package.EXPORTER_SHA256 = self.exporter_sha
        package.BOOTSTRAP_SHA256 = self.bootstrap_sha
        package.GISMO_COMMIT = self.commit
        package.RUNTIME_PINS = self.runtime_pin_values.copy()
        package.RUNTIME_YAML_SHA256 = self.runtime_yaml_sha

    def _build(self, output: Path):
        return package.prepare_package(self.workspace, self.gismo, self.runtime, output)

    def test_builds_manifest_bound_exact_inventory_without_data_or_secrets(self) -> None:
        output = self.root / "package"
        manifest_path, observed_sha = self._build(output)
        self.assertEqual(package._sha256(manifest_path), observed_sha)
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        expected = {
            "code/export_c3_recipe1m_order.py",
            "code/bootstrap_t535_kaggle.py",
            "licenses/intel-mkl-2020.2-license-april-2018.txt",
            "licenses/intel-openmp-2025.0.0-license-october-2022.txt",
            "licenses/jpeg-9b-ijg-license-notice.txt",
            "licenses/zlib-1.3.2-license.txt",
            "runtime/t535-kaggle-runtime-linux.yml",
            *("runtime/" + name for name in package.RUNTIME_PINS),
            *("gismo/" + relative for relative in package.GISMO_FILES),
        }
        self.assertEqual(set(item["path"] for item in manifest["files"]), expected)
        self.assertEqual(package._inventory(output), sorted(expected | {package.MANIFEST_NAME}))
        self.assertNotIn("gismo/inv_cooking/datasets/recipe1m/__init__.py", expected)
        self.assertIn("gismo/inv_cooking/datasets/recipe1m/preprocess.py", expected)
        self.assertEqual(manifest["gismo_commit"], self.commit)
        self.assertEqual(manifest["bootstrap"]["sha256"], self.bootstrap_sha)
        self.assertEqual(set(manifest["runtime"]), {"archive", "platform_spec", "resolution_record", "provenance"})
        self.assertNotIn("locks", manifest["runtime"])
        self.assertEqual(manifest["runtime"]["resolution_record"], {
            "kind": "hash-pinned-prebuilt-environment-archive",
            "conda_metadata_embedded_in_archive": True,
            "platform_spec_is_declaration_not_a_recreation_lock": True,
            "windows_source_environment_lockfiles_packaged": False,
        })
        self.assertEqual(manifest["version"], 10)
        self.assertEqual(manifest["third_party_licenses"][1]["package_metadata_license"],
                         "LicenseRef-ProprietaryIntel")
        self.assertEqual(manifest["third_party_licenses"][1]["embedded_notice_title"],
                         "Intel Simplified Software License (Version April 2018)")
        self.assertEqual(manifest["third_party_licenses"][1]["governing_terms_status"], "UNKNOWN")
        self.assertEqual(manifest["third_party_licenses"][1]["artifact_sha256"],
                         package.MKL_LICENSE_PACKAGE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][1]["notice_sha256"],
                         package.MKL_LICENSE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][2]["package_metadata_license"],
                         "LicenseRef-IntelSimplifiedSoftwareOct2022")
        self.assertEqual(manifest["third_party_licenses"][2]["embedded_notice_title"],
                         "Intel Simplified Software License (Version October 2022)")
        self.assertEqual(manifest["third_party_licenses"][2]["governing_terms_status"], "UNKNOWN")
        self.assertEqual(manifest["third_party_licenses"][2]["artifact_sha256"],
                         package.OPENMP_LICENSE_PACKAGE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][2]["notice_sha256"],
                         package.OPENMP_LICENSE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][3]["artifact_sha256"],
                         package.JPEG_PACKAGE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][3]["source_url"],
                         "http://www.ijg.org/files/jpegsrc.v9b.tar.gz")
        self.assertEqual(manifest["third_party_licenses"][3]["source_md5"],
                         package.JPEG_SOURCE_MD5)
        self.assertEqual(manifest["third_party_licenses"][3]["notice_sha256"],
                         package.JPEG_LICENSE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][4]["artifact_sha256"],
                         package.LIBZLIB_PACKAGE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][5]["artifact_sha256"],
                         package.ZLIB_PACKAGE_SHA256)
        self.assertEqual(manifest["third_party_licenses"][4]["notice_sha256"],
                         manifest["third_party_licenses"][5]["notice_sha256"])
        self.assertEqual(manifest["source_data"], {
            "recipe1m_release_files_included": False,
            "gismo_source_derived_split_indices_included": True,
            "gismo_source_derived_split_index_paths": [
                "gismo/data/splits/recipe1m/train.txt",
                "gismo/data/splits/recipe1m/val_all.txt",
                "gismo/data/splits/recipe1m/val.txt",
                "gismo/data/splits/recipe1m/test.txt",
            ],
            "recipe1m_release_mount_required_later": True,
        })
        self.assertFalse(manifest["credentials_included"])
        encoded = manifest_path.read_bytes().lower()
        self.assertNotIn(str(self.root).encode("utf-8").lower(), encoded)
        self.assertNotIn(b"token", encoded)
        self.assertNotIn(b"recipe1m.json", encoded)

    def test_windows_source_environment_locks_are_not_copied_or_disclosed(self) -> None:
        local_paths = (
            b"C:\\Users\\YOGA\\ResearchData\\ChefKix\\T535-runtime-20261027\\wheelhouse\\package.whl\n"
            b"C:\\b\\abs-build-path\\package.whl\n"
        )
        for name in ("conda-explicit.lock", "pip-freeze.lock", "pip-artifact-lock.sha256"):
            (self.runtime / name).write_bytes(local_paths)
        output = self.root / "package-no-host-locks"
        manifest_path, _ = self._build(output)
        self.assertFalse(any((output / "runtime" / name).exists() for name in (
            "conda-explicit.lock", "pip-freeze.lock", "pip-artifact-lock.sha256")))
        manifest = manifest_path.read_bytes().lower()
        self.assertNotIn(b"c:\\\\users\\\\yoga", manifest)
        self.assertNotIn(b"c:\\\\b\\\\abs-build-path", manifest)

    def test_missing_or_modified_openmp_notice_fails_closed(self) -> None:
        source = self.workspace / package.OPENMP_LICENSE_REL
        original = source.read_bytes()
        source.unlink()
        with self.assertRaisesRegex(package.PackageError, "required package input is missing"):
            self._build(self.root / "package-missing-openmp-notice")
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_bytes(original + b"tampered")
        with self.assertRaisesRegex(package.PackageError, "OpenMP license notice"):
            self._build(self.root / "package-modified-openmp-notice")

    def test_missing_or_modified_jpeg_and_zlib_notices_fail_closed(self) -> None:
        for relative, message in ((package.JPEG_LICENSE_REL, "JPEG license notice"),
                                  (package.ZLIB_LICENSE_REL, "Zlib license notice")):
            source = self.workspace / relative
            original = source.read_bytes()
            source.unlink()
            with self.assertRaisesRegex(package.PackageError, "required package input is missing"):
                self._build(self.root / ("package-missing-" + source.name))
            source.parent.mkdir(parents=True, exist_ok=True)
            source.write_bytes(original + b"tampered")
            with self.assertRaisesRegex(package.PackageError, message):
                self._build(self.root / ("package-modified-" + source.name))
            source.write_bytes(original)

    def test_manifest_is_reproducible_across_new_destinations(self) -> None:
        first, _ = self._build(self.root / "package-a")
        second, _ = self._build(self.root / "package-b")
        self.assertEqual(first.read_bytes(), second.read_bytes())

    def test_existing_output_is_never_overwritten(self) -> None:
        output = self.root / "already-here"
        output.mkdir()
        marker = output / "keep.txt"
        marker.write_text("preserve", encoding="utf-8")
        with self.assertRaises(package.PackageError):
            self._build(output)
        self.assertEqual(marker.read_text(encoding="utf-8"), "preserve")

    def test_hash_mismatch_leaves_no_output_package(self) -> None:
        (self.runtime / package.RUNTIME_ARCHIVE).write_bytes(b"altered")
        output = self.root / "failed-package"
        with self.assertRaises(package.PackageError):
            self._build(output)
        self.assertFalse(output.exists())
        self.assertFalse(list(self.root.glob(".failed-package.staging-*")))

    def test_dirty_or_wrong_commit_checkout_is_rejected(self) -> None:
        (self.gismo / "unexpected.py").write_text("not allowed", encoding="utf-8")
        with self.assertRaises(package.PackageError):
            self._build(self.root / "dirty-package")
        self.assertFalse((self.root / "dirty-package").exists())

    def test_symlinked_allowlisted_file_is_rejected(self) -> None:
        target = self.runtime / package.RUNTIME_ARCHIVE
        target.unlink()
        outside = self.root / "outside.bin"
        outside.write_bytes(b"do not package")
        try:
            target.symlink_to(outside)
        except (OSError, NotImplementedError):
            self.skipTest("symlinks unavailable to this test account")
        with self.assertRaises(package.PackageError):
            self._build(self.root / "symlink-package")
        self.assertFalse((self.root / "symlink-package").exists())

    def test_path_traversal_is_rejected(self) -> None:
        for bad in ("../escape", "/absolute", "a/../../escape", "a\\..\\escape"):
            with self.subTest(path=bad), self.assertRaises(package.PackageError):
                package._safe_rel(bad)

    def test_output_may_not_overlap_input_roots(self) -> None:
        output = self.workspace / "package-under-input"
        with self.assertRaises(package.PackageError):
            self._build(output)
        self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
