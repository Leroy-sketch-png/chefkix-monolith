#!/usr/bin/env python3
"""Build a source-free, hash-bound GISMo CPU package for T535.

This helper only packages pinned code/configuration and a prebuilt runtime. It
does not accept source data, create attestations, upload files, or launch jobs.
Recipe1M source inputs must be mounted separately after the independent live
pre-access/privacy/resource gates have passed.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import stat
import subprocess
import sys
import tempfile
from pathlib import Path, PurePosixPath
from typing import Dict, Iterable, List, Mapping, Optional, Tuple


GISMO_COMMIT = "f5d2394c7ade897987ce584d2a75d8bbbbae058e"
EXPORTER_REL = "ai/tools/export_c3_recipe1m_order.py"
EXPORTER_SHA256 = "2beec6f07db4caba7ef88b61a27c9f4134019d390144356f93a207518ca11aee"
BOOTSTRAP_REL = "ai/tools/bootstrap_t535_kaggle.py"
BOOTSTRAP_SHA256 = "5456fd8701a37ac29843726fb5978f88c5421c2ed037a73b3d47c33c095e3320"
MKL_LICENSE_REL = "ai/tools/third_party_licenses/intel-mkl-2020.2-license-april-2018.txt"
MKL_LICENSE_SHA256 = "bfda9286e31251fdc9eaacddb1856c5e73342126efcf6e34a53f1d55ddd54f4b"
MKL_LICENSE_PACKAGE_SHA256 = "e8734ce44ebca78c43464ef1e0ecf89be67bf0e394be1a08572c6a3f3857a31a"
OPENMP_LICENSE_REL = "ai/tools/third_party_licenses/intel-openmp-2025.0.0-license-october-2022.txt"
OPENMP_LICENSE_SHA256 = "63be632f292b6b5439ae0133b280b30e27403fecf7c71f77b8d3fe64aa079707"
OPENMP_LICENSE_PACKAGE_SHA256 = "81b0d2706b41817d3038a920495bb5c9ea67b80e7c4006285230874fe8b85d2c"
JPEG_LICENSE_REL = "ai/tools/third_party_licenses/jpeg-9b-ijg-license-notice.txt"
JPEG_LICENSE_SHA256 = "558452b4c18f02d85c0a0d7410f0f321b96f6449b421c1bd02033106088673c4"
JPEG_PACKAGE_SHA256 = "3771b5c80500813ee442f6360a1453fc32d30e84d9f6ea71ef0c67de1860a0a0"
JPEG_SOURCE_URL = "http://www.ijg.org/files/jpegsrc.v9b.tar.gz"
JPEG_SOURCE_MD5 = "6a9996ce116ec5c52b4870dbcd6d3ddb"
JPEG_SOURCE_SHA256 = "240fd398da741669bf3c90366f58452ea59041cacc741a489b99f2f6a0bad052"
JPEG_README_SHA256 = "6783159a601b23fb42abbcc9e7f57b89a23ddf39cfa862c364844580520a7b43"
ZLIB_LICENSE_REL = "ai/tools/third_party_licenses/zlib-1.3.2-license.txt"
ZLIB_LICENSE_SHA256 = "e32ff4e00d9d94930537635291da39e7e612703334bf6fde8c7f1686fe8a45a2"
LIBZLIB_PACKAGE_SHA256 = "eb8a0db0aa570124f7d2a93d7c7f596e3390df5e047818d873baad32985fc736"
ZLIB_PACKAGE_SHA256 = "16080a1c7724f7d25727cdc23c7658e0cec2db52448c1dc0c33467ee2c6e1c62"
RUNTIME_ARCHIVE = "kaggle-linux-runtime-v6.tar.gz"
RUNTIME_PINS = {
    # The target platform is Linux. The archive is the executable runtime
    # artifact; Windows-source-environment lockfiles stay local and are not
    # copied into the remote package.
    "kaggle-linux-runtime-v6.tar.gz": "6cce29ac80bf7526c9055f66e8ca589be4938b2aa859e46137fc8d33b95be9a3",
}
RUNTIME_YAML_REL = "ai/tools/t535-kaggle-runtime-linux.yml"
RUNTIME_YAML_SHA256 = "05b2f992af50e49af9e0373d9cc4d897e39a19aa81318925510983582b605489"
MANIFEST_NAME = "package-manifest.json"

# Explicit, minimal import/config/index closure for the pinned upstream call.
# The upstream recipe1m/__init__.py eagerly imports the image dataset and
# Lightning data-module stack, neither of which preprocessing needs. Omit that
# initializer so Python treats recipe1m as a namespace package; the exact
# pinned preprocess/parser implementations remain untouched. This avoids
# pulling runtime-only image loaders into the source-processing package.
GISMO_FILES = (
    "inv_cooking/__init__.py",
    "inv_cooking/datasets/__init__.py",
    "inv_cooking/datasets/vocabulary.py",
    "inv_cooking/datasets/recipe1m/preprocess.py",
    "inv_cooking/datasets/recipe1m/parsing/__init__.py",
    "inv_cooking/datasets/recipe1m/parsing/ingredient.py",
    "inv_cooking/datasets/recipe1m/parsing/instructions.py",
    "inv_cooking/datasets/recipe1m/parsing/titles.py",
    "conf/dataset/recipe1m.yaml",
    "data/splits/recipe1m/train.txt",
    "data/splits/recipe1m/val_all.txt",
    "data/splits/recipe1m/val.txt",
    "data/splits/recipe1m/test.txt",
    "LICENSE.md",
)


class PackageError(ValueError):
    """Fail-closed package construction error."""


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _safe_rel(value: str) -> str:
    if not isinstance(value, str) or not value or "\\" in value or "\x00" in value:
        raise PackageError("unsafe package-relative path")
    parsed = PurePosixPath(value)
    if parsed.is_absolute() or any(part in ("", ".", "..") for part in parsed.parts):
        raise PackageError("unsafe package-relative path")
    return parsed.as_posix()


def _assert_no_symlink_components(path: Path, boundary: Path) -> None:
    """Reject a symlink in the path from boundary through the final component."""
    boundary = boundary.absolute()
    path = path.absolute()
    try:
        relative = path.relative_to(boundary)
    except ValueError as exc:
        raise PackageError("input path escapes its declared root") from exc
    cursor = boundary
    if cursor.exists() and stat.S_ISLNK(cursor.lstat().st_mode):
        raise PackageError("symlinked input root is not allowed")
    for part in relative.parts:
        cursor = cursor / part
        try:
            mode = cursor.lstat().st_mode
        except OSError as exc:
            raise PackageError("required package input is missing") from exc
        if stat.S_ISLNK(mode):
            raise PackageError("symlinked package input is not allowed")


def _assert_absolute_path_has_no_symlinks(path: Path) -> None:
    cursor = Path(path.anchor)
    for part in path.parts[1:]:
        cursor = cursor / part
        try:
            if stat.S_ISLNK(cursor.lstat().st_mode):
                raise PackageError("symlinked path component is not allowed")
        except OSError as exc:
            raise PackageError("path component does not exist") from exc


def _input_file(root: Path, relative: str) -> Path:
    relative = _safe_rel(relative)
    path = root / Path(*PurePosixPath(relative).parts)
    _assert_no_symlink_components(path, root)
    if not stat.S_ISREG(path.lstat().st_mode):
        raise PackageError("package input is not a regular file")
    return path


def _run_git(root: Path, *args: str) -> str:
    try:
        result = subprocess.run(
            ["git", "-C", str(root), *args],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            encoding="utf-8",
        )
    except (OSError, subprocess.CalledProcessError) as exc:
        raise PackageError("pinned GISMo checkout could not be verified") from exc
    return result.stdout.strip()


def _verify_gismo_checkout(root: Path) -> None:
    head = _run_git(root, "rev-parse", "HEAD")
    if head != GISMO_COMMIT:
        raise PackageError("GISMo checkout is not at the required commit")
    if _run_git(root, "status", "--porcelain", "--untracked-files=all"):
        raise PackageError("GISMo checkout is not clean")
    tracked = set(_run_git(root, "ls-files").splitlines())
    if not set(GISMO_FILES).issubset(tracked):
        raise PackageError("required GISMo files are not tracked at the pinned commit")


def _copy_verified(source: Path, destination: Path, expected_sha256: Optional[str]) -> Dict[str, object]:
    before = _sha256(source)
    if expected_sha256 and before != expected_sha256:
        raise PackageError("an input does not match its pinned SHA-256")
    destination.parent.mkdir(parents=True, exist_ok=True)
    with source.open("rb") as src, destination.open("xb") as dst:
        shutil.copyfileobj(src, dst, length=1024 * 1024)
        dst.flush()
        os.fsync(dst.fileno())
    copied = _sha256(destination)
    after = _sha256(source)
    if before != copied or before != after:
        raise PackageError("input changed while package was being copied")
    return {"sha256": copied}


def _inventory(root: Path) -> List[str]:
    paths = []
    for path in root.rglob("*"):
        if path.is_symlink():
            raise PackageError("staging directory unexpectedly contains a symlink")
        if path.is_file():
            paths.append(path.relative_to(root).as_posix())
    return sorted(paths)


def prepare_package(workspace_root: Path, gismo_root: Path, runtime_root: Path, output_dir: Path) -> Tuple[Path, str]:
    workspace_root = workspace_root.absolute()
    gismo_root = gismo_root.absolute()
    runtime_root = runtime_root.absolute()
    output_dir = output_dir.absolute()
    for root in (workspace_root, gismo_root, runtime_root):
        _assert_absolute_path_has_no_symlinks(root)
    if output_dir.exists() or output_dir.is_symlink():
        raise PackageError("output directory must be new and must not exist")
    if not workspace_root.is_dir() or not gismo_root.is_dir() or not runtime_root.is_dir():
        raise PackageError("a declared input root is not a directory")
    _verify_gismo_checkout(gismo_root)

    exporter = _input_file(workspace_root, EXPORTER_REL)
    if _sha256(exporter) != EXPORTER_SHA256:
        raise PackageError("exporter differs from the reviewed profile pin")
    bootstrap = _input_file(workspace_root, BOOTSTRAP_REL)
    if _sha256(bootstrap) != BOOTSTRAP_SHA256:
        raise PackageError("bootstrap differs from the reviewed launcher pin")
    runtime_sources = {}
    for name, expected in RUNTIME_PINS.items():
        source = _input_file(runtime_root, name)
        if expected and _sha256(source) != expected:
            raise PackageError("runtime input differs from its reviewed profile pin")
        runtime_sources[name] = (source, expected or _sha256(source))
    runtime_yaml = _input_file(workspace_root, RUNTIME_YAML_REL)
    runtime_yaml_expected = RUNTIME_YAML_SHA256
    if runtime_yaml_expected and _sha256(runtime_yaml) != runtime_yaml_expected:
        raise PackageError("runtime specification differs from its reviewed profile pin")
    mkl_license = _input_file(workspace_root, MKL_LICENSE_REL)
    if _sha256(mkl_license) != MKL_LICENSE_SHA256:
        raise PackageError("MKL license notice differs from its exact package-artifact pin")
    openmp_license = _input_file(workspace_root, OPENMP_LICENSE_REL)
    if _sha256(openmp_license) != OPENMP_LICENSE_SHA256:
        raise PackageError("OpenMP license notice differs from its exact package-artifact pin")
    jpeg_license = _input_file(workspace_root, JPEG_LICENSE_REL)
    if _sha256(jpeg_license) != JPEG_LICENSE_SHA256:
        raise PackageError("JPEG license notice differs from its exact source/package pins")
    zlib_license = _input_file(workspace_root, ZLIB_LICENSE_REL)
    if _sha256(zlib_license) != ZLIB_LICENSE_SHA256:
        raise PackageError("Zlib license notice differs from its exact package-artifact pins")

    input_roots = (workspace_root.resolve(), gismo_root.resolve(), runtime_root.resolve())
    for root in input_roots:
        if output_dir == root or root in output_dir.parents or output_dir in root.parents:
            raise PackageError("output location overlaps a package input root")
    out_parent = output_dir.parent
    out_parent.mkdir(parents=True, exist_ok=True)
    _assert_absolute_path_has_no_symlinks(out_parent)
    resolved_parent = out_parent.resolve(strict=True)
    candidate_output = resolved_parent / output_dir.name
    for root in input_roots:
        if candidate_output == root or root in candidate_output.parents or candidate_output in root.parents:
            raise PackageError("output location overlaps a package input root")

    staging = Path(tempfile.mkdtemp(prefix="." + output_dir.name + ".staging-", dir=str(out_parent)))
    try:
        files: List[Dict[str, object]] = []
        copy_plan = [
            (exporter, "code/export_c3_recipe1m_order.py", EXPORTER_SHA256),
            (bootstrap, "code/bootstrap_t535_kaggle.py", BOOTSTRAP_SHA256),
            (mkl_license, "licenses/intel-mkl-2020.2-license-april-2018.txt", MKL_LICENSE_SHA256),
            (openmp_license, "licenses/intel-openmp-2025.0.0-license-october-2022.txt", OPENMP_LICENSE_SHA256),
            (jpeg_license, "licenses/jpeg-9b-ijg-license-notice.txt", JPEG_LICENSE_SHA256),
            (zlib_license, "licenses/zlib-1.3.2-license.txt", ZLIB_LICENSE_SHA256),
        ]
        for relative in GISMO_FILES:
            copy_plan.append((_input_file(gismo_root, relative), "gismo/" + relative, None))
        copy_plan.append((runtime_yaml, "runtime/t535-kaggle-runtime-linux.yml", runtime_yaml_expected))
        for name, (source, expected) in runtime_sources.items():
            copy_plan.append((source, "runtime/" + name, expected))
        for source, relative, expected in copy_plan:
            target = staging / Path(*PurePosixPath(_safe_rel(relative)).parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            _copy_verified(source, target, expected)
            files.append({"path": _safe_rel(relative), "size_bytes": target.stat().st_size, "sha256": _sha256(target)})
        files.sort(key=lambda entry: str(entry["path"]))
        expected_inventory = sorted(str(entry["path"]) for entry in files)
        if _inventory(staging) != expected_inventory:
            raise PackageError("staging inventory contains missing or unexpected files")

        runtime_hashes = {name: _sha256(source) for name, (source, _expected) in sorted(runtime_sources.items())}
        manifest = {
            "schema": "chefkix.t535.kaggle-source-free-package",
            "version": 10,
            "gismo_commit": GISMO_COMMIT,
            "exporter": {"path": "code/export_c3_recipe1m_order.py", "sha256": EXPORTER_SHA256},
            "bootstrap": {"path": "code/bootstrap_t535_kaggle.py", "sha256": BOOTSTRAP_SHA256},
            "runtime": {
                "archive": {"path": "runtime/" + RUNTIME_ARCHIVE, "sha256": runtime_hashes[RUNTIME_ARCHIVE]},
                "platform_spec": {"path": "runtime/t535-kaggle-runtime-linux.yml", "sha256": _sha256(runtime_yaml)},
                "resolution_record": {
                    "kind": "hash-pinned-prebuilt-environment-archive",
                    "conda_metadata_embedded_in_archive": True,
                    "platform_spec_is_declaration_not_a_recreation_lock": True,
                    "windows_source_environment_lockfiles_packaged": False,
                },
                "provenance": "T535 source-free Linux CPU runtime profile; no claim of Kaggle execution",
            },
            "third_party_licenses": [
                {
                    "name": "GISMo",
                    "identifier": "CC-BY-NC-4.0",
                    "path": "gismo/LICENSE.md",
                    "upstream_commit": GISMO_COMMIT,
                },
                {
                    "name": "Intel MKL 2020.2 package artifact",
                    "package_metadata_license": "LicenseRef-ProprietaryIntel",
                    "embedded_notice_title": "Intel Simplified Software License (Version April 2018)",
                    "path": "licenses/intel-mkl-2020.2-license-april-2018.txt",
                    "artifact_filename": "mkl-2020.2-256.conda",
                    "artifact_sha256": MKL_LICENSE_PACKAGE_SHA256,
                    "notice_sha256": MKL_LICENSE_SHA256,
                    "governing_terms_status": "UNKNOWN",
                },
                {
                    "name": "Intel OpenMP 2025.0.0 package artifact",
                    "package_metadata_license": "LicenseRef-IntelSimplifiedSoftwareOct2022",
                    "embedded_notice_title": "Intel Simplified Software License (Version October 2022)",
                    "path": "licenses/intel-openmp-2025.0.0-license-october-2022.txt",
                    "artifact_filename": "intel-openmp-2025.0.0-h06a4308_1172.conda",
                    "artifact_sha256": OPENMP_LICENSE_PACKAGE_SHA256,
                    "notice_sha256": OPENMP_LICENSE_SHA256,
                    "official_notice_url": "https://cdrdv2-public.intel.com/749362/intel-simplified-license-software-october-2022.pdf",
                    "governing_terms_status": "UNKNOWN",
                },
                {
                    "name": "Independent JPEG Group jpeg 9b package artifact",
                    "package_metadata_license": "Custom free software license",
                    "path": "licenses/jpeg-9b-ijg-license-notice.txt",
                    "artifact_filename": "jpeg-9b-h024ee3a_2.conda",
                    "artifact_sha256": JPEG_PACKAGE_SHA256,
                    "source_url": JPEG_SOURCE_URL,
                    "source_md5": JPEG_SOURCE_MD5,
                    "source_sha256": JPEG_SOURCE_SHA256,
                    "source_readme_sha256": JPEG_README_SHA256,
                    "notice_sha256": JPEG_LICENSE_SHA256,
                    "governing_terms_status": "UNKNOWN",
                },
                {
                    "name": "libzlib 1.3.2 package artifact",
                    "package_metadata_license": "Zlib",
                    "license_family": "Other",
                    "path": "licenses/zlib-1.3.2-license.txt",
                    "artifact_filename": "libzlib-1.3.2-h25fd6f3_3.conda",
                    "artifact_sha256": LIBZLIB_PACKAGE_SHA256,
                    "embedded_notice_path": "info/licenses/LICENSE",
                    "notice_sha256": ZLIB_LICENSE_SHA256,
                    "governing_terms_status": "UNKNOWN",
                },
                {
                    "name": "zlib 1.3.2 package artifact",
                    "package_metadata_license": "Zlib",
                    "license_family": "Other",
                    "path": "licenses/zlib-1.3.2-license.txt",
                    "artifact_filename": "zlib-1.3.2-h25fd6f3_3.conda",
                    "artifact_sha256": ZLIB_PACKAGE_SHA256,
                    "embedded_notice_path": "info/licenses/LICENSE",
                    "notice_sha256": ZLIB_LICENSE_SHA256,
                    "governing_terms_status": "UNKNOWN",
                },
            ],
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
            "files": files,
        }
        manifest_bytes = (json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
        manifest_path = staging / MANIFEST_NAME
        with manifest_path.open("xb") as stream:
            stream.write(manifest_bytes)
            stream.flush()
            os.fsync(stream.fileno())
        if _inventory(staging) != sorted(expected_inventory + [MANIFEST_NAME]):
            raise PackageError("final package contains missing or unexpected files")
        manifest_sha = _sha256(manifest_path)
        os.rename(str(staging), str(output_dir))
        return output_dir / MANIFEST_NAME, manifest_sha
    except Exception:
        # Keep no partial package at the requested destination. This only removes
        # the private temporary tree created by this invocation.
        shutil.rmtree(str(staging), ignore_errors=True)
        raise


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace-root", type=Path, required=True)
    parser.add_argument("--gismo-root", type=Path, required=True, help="exact clean GISMo checkout at the pinned commit")
    parser.add_argument("--runtime-root", type=Path, required=True, help="owner-private runtime directory containing the pinned Linux archive")
    parser.add_argument("--output-dir", type=Path, required=True, help="new destination; must not already exist")
    args = parser.parse_args(argv)
    try:
        manifest_path, manifest_sha = prepare_package(args.workspace_root, args.gismo_root, args.runtime_root, args.output_dir)
    except (PackageError, OSError) as exc:
        print("T535 package preparation stopped: {}".format(exc), file=sys.stderr)
        return 2
    print(json.dumps({"package_dir": str(args.output_dir.absolute()), "manifest": str(manifest_path), "manifest_sha256": manifest_sha}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
