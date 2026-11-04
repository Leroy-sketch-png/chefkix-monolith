#!/usr/bin/env python3
"""Private, source-pinned export of IDs retained by GISMo Recipe1M preprocessing.

This is deliberately default-off. It invokes the pinned upstream preprocessing
functions in memory and writes only recipe IDs, source ordinals, and filtered
positions to a caller-selected private directory. It never writes or loads a
preprocessed pickle and never emits recipe text, ingredients, labels, pairs,
profiles, images, credentials, or IDs to logs/manifests.
"""

from __future__ import annotations

import argparse
import contextlib
import ctypes
import datetime as dt
import errno
import hashlib
import importlib
import io
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
from collections.abc import Mapping, Sequence
from pathlib import Path
from types import ModuleType
from typing import Any, Optional


PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
ANALYSIS_SPEC_SHA256 = "1a6646cef69c0d4cc539966d567ef4ee4426bc76979bf7aad21aa4f6ec161255"
GISMO_COMMIT = "f5d2394c7ade897987ce584d2a75d8bbbbae058e"
PACKAGE_VERSION = 10
PREFLIGHT_ATTESTATION_MAX_AGE = dt.timedelta(minutes=10)
EXPORTER_MAX_RUNTIME_SECONDS = 60 * 60 * 6
JPEG_SOURCE_URL = "http://www.ijg.org/files/jpegsrc.v9b.tar.gz"
SPLITS = ("train", "validation", "test")
GISMO_SPLITS = {"train": "train", "validation": "val", "test": "test"}
PROCESSED_SPLITS = {"train": "train", "validation": "val", "test": "test"}
SOURCE_PARTITIONS = {"train": "train", "validation": "val", "test": "test"}
PARTITION_TO_SOURCE = {value: key for key, value in SOURCE_PARTITIONS.items()}
INDEX_FILES = {"train": "train.txt", "val_all": "val_all.txt", "val": "val.txt", "test": "test.txt"}
PACKAGE_SPLIT_INDEX_PATHS = [
    "gismo/data/splits/recipe1m/train.txt",
    "gismo/data/splits/recipe1m/val_all.txt",
    "gismo/data/splits/recipe1m/val.txt",
    "gismo/data/splits/recipe1m/test.txt",
]
PACKAGE_SOURCE_DATA = {
    "recipe1m_release_files_included": False,
    "gismo_source_derived_split_indices_included": True,
    "gismo_source_derived_split_index_paths": PACKAGE_SPLIT_INDEX_PATHS,
    "recipe1m_release_mount_required_later": True,
}
PACKAGE_RUNTIME_RESOLUTION = {
    "kind": "hash-pinned-prebuilt-environment-archive",
    "conda_metadata_embedded_in_archive": True,
    "platform_spec_is_declaration_not_a_recreation_lock": True,
    "windows_source_environment_lockfiles_packaged": False,
}
PACKAGE_THIRD_PARTY_LICENSES = [
    {
        "name": "GISMo", "identifier": "CC-BY-NC-4.0", "path": "gismo/LICENSE.md",
        "upstream_commit": GISMO_COMMIT,
    },
    {
        "name": "Intel MKL 2020.2 package artifact",
        "package_metadata_license": "LicenseRef-ProprietaryIntel",
        "embedded_notice_title": "Intel Simplified Software License (Version April 2018)",
        "path": "licenses/intel-mkl-2020.2-license-april-2018.txt",
        "artifact_filename": "mkl-2020.2-256.conda",
        "artifact_sha256": "e8734ce44ebca78c43464ef1e0ecf89be67bf0e394be1a08572c6a3f3857a31a",
        "notice_sha256": "bfda9286e31251fdc9eaacddb1856c5e73342126efcf6e34a53f1d55ddd54f4b",
        "governing_terms_status": "UNKNOWN",
    },
    {
        "name": "Intel OpenMP 2025.0.0 package artifact",
        "package_metadata_license": "LicenseRef-IntelSimplifiedSoftwareOct2022",
        "embedded_notice_title": "Intel Simplified Software License (Version October 2022)",
        "path": "licenses/intel-openmp-2025.0.0-license-october-2022.txt",
        "artifact_filename": "intel-openmp-2025.0.0-h06a4308_1172.conda",
        "artifact_sha256": "81b0d2706b41817d3038a920495bb5c9ea67b80e7c4006285230874fe8b85d2c",
        "notice_sha256": "63be632f292b6b5439ae0133b280b30e27403fecf7c71f77b8d3fe64aa079707",
        "official_notice_url": "https://cdrdv2-public.intel.com/749362/intel-simplified-license-software-october-2022.pdf",
        "governing_terms_status": "UNKNOWN",
    },
    {
        "name": "Independent JPEG Group jpeg 9b package artifact",
        "package_metadata_license": "Custom free software license",
        "path": "licenses/jpeg-9b-ijg-license-notice.txt",
        "artifact_filename": "jpeg-9b-h024ee3a_2.conda",
        "artifact_sha256": "3771b5c80500813ee442f6360a1453fc32d30e84d9f6ea71ef0c67de1860a0a0",
        "source_url": JPEG_SOURCE_URL,
        "source_md5": "6a9996ce116ec5c52b4870dbcd6d3ddb",
        "source_sha256": "240fd398da741669bf3c90366f58452ea59041cacc741a489b99f2f6a0bad052",
        "source_readme_sha256": "6783159a601b23fb42abbcc9e7f57b89a23ddf39cfa862c364844580520a7b43",
        "notice_sha256": "558452b4c18f02d85c0a0d7410f0f321b96f6449b421c1bd02033106088673c4",
        "governing_terms_status": "UNKNOWN",
    },
    {
        "name": "libzlib 1.3.2 package artifact",
        "package_metadata_license": "Zlib",
        "license_family": "Other",
        "path": "licenses/zlib-1.3.2-license.txt",
        "artifact_filename": "libzlib-1.3.2-h25fd6f3_3.conda",
        "artifact_sha256": "eb8a0db0aa570124f7d2a93d7c7f596e3390df5e047818d873baad32985fc736",
        "embedded_notice_path": "info/licenses/LICENSE",
        "notice_sha256": "e32ff4e00d9d94930537635291da39e7e612703334bf6fde8c7f1686fe8a45a2",
        "governing_terms_status": "UNKNOWN",
    },
    {
        "name": "zlib 1.3.2 package artifact",
        "package_metadata_license": "Zlib",
        "license_family": "Other",
        "path": "licenses/zlib-1.3.2-license.txt",
        "artifact_filename": "zlib-1.3.2-h25fd6f3_3.conda",
        "artifact_sha256": "16080a1c7724f7d25727cdc23c7658e0cec2db52448c1dc0c33467ee2c6e1c62",
        "embedded_notice_path": "info/licenses/LICENSE",
        "notice_sha256": "e32ff4e00d9d94930537635291da39e7e612703334bf6fde8c7f1686fe8a45a2",
        "governing_terms_status": "UNKNOWN",
    },
]
HEX64 = re.compile(r"[0-9a-f]{64}\Z")
SAFE_REF = re.compile(r"receipt:[A-Za-z0-9][A-Za-z0-9._/-]{0,180}\Z")
SAFE_ID = re.compile(r"[A-Za-z0-9][A-Za-z0-9._:-]{0,127}\Z")
KAGGLE_OWNER = re.compile(r"[A-Za-z0-9][A-Za-z0-9_-]{0,63}\Z")
KAGGLE_SLUG = re.compile(r"[A-Za-z0-9][A-Za-z0-9_-]{0,63}/[A-Za-z0-9][A-Za-z0-9_-]{0,63}\Z")
TERMS_STATUS = "HUMAN_REPORTED_PRIVATE_NONCOMMERCIAL_NO_PUBLICITY"
KAGGLE_INPUT_ROOT = Path("/kaggle/input")
KAGGLE_WORKING_ROOT = Path("/kaggle/working")


class ExportError(ValueError):
    """Fail-closed error whose message must not contain source-row values."""


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_json(value: Any) -> bytes:
    try:
        return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False).encode("utf-8")
    except (TypeError, ValueError, UnicodeEncodeError) as exc:
        raise ExportError("canonical JSON encoding failed") from exc


def _is_sha(value: Any) -> bool:
    return type(value) is str and HEX64.fullmatch(value) is not None


def _safe_relative(value: Any, where: str) -> str:
    if type(value) is not str or not value or "\\" in value or ":" in value or "\x00" in value:
        raise ExportError(f"{where} must be a safe relative path")
    path = Path(value)
    if path.is_absolute() or any(part in {"", ".", ".."} for part in path.parts):
        raise ExportError(f"{where} must be a safe relative path")
    return path.as_posix()


def _inside(path: Path, root: Path) -> bool:
    try:
        path.relative_to(root)
        return True
    except ValueError:
        return False


def _require_private_outside_worktree(path: Path, worktree: Path, *, exists: bool) -> Path:
    resolved = path.resolve(strict=exists)
    if _inside(resolved, worktree.resolve(strict=True)):
        raise ExportError("real source and output paths must remain outside the Git worktree")
    return resolved


def _load_manifest(path: Path) -> tuple[dict[str, Any], str]:
    try:
        raw = path.read_bytes()
        value = json.loads(raw.decode("utf-8", errors="strict"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ExportError("source manifest is unreadable UTF-8 JSON") from exc
    top_fields = {"schema_version", "protocol_v1_sha256", "analysis_spec_v1_sha256",
                  "terms_status", "terms_reference", "release_id", "source_reference",
                  "gismo_commit", "source_files"}
    if (type(value) is not dict or set(value) != top_fields
            or type(value.get("schema_version")) is not int or value["schema_version"] != 1):
        raise ExportError("unsupported source manifest schema")
    if value.get("protocol_v1_sha256") != PROTOCOL_SHA256:
        raise ExportError("frozen Protocol v1 hash mismatch")
    if value.get("analysis_spec_v1_sha256") != ANALYSIS_SPEC_SHA256:
        raise ExportError("frozen C3 analysis-spec hash mismatch")
    if value.get("terms_status") != TERMS_STATUS:
        raise ExportError("source-specific local-use terms receipt is required")
    if type(value.get("terms_reference")) is not str or not SAFE_REF.fullmatch(value["terms_reference"]):
        raise ExportError("terms_reference must be a non-secret receipt reference")
    if type(value.get("release_id")) is not str or not SAFE_ID.fullmatch(value["release_id"]):
        raise ExportError("exact Recipe1M release identifier must use the safe ID form")
    if type(value.get("source_reference")) is not str or not SAFE_REF.fullmatch(value["source_reference"]):
        raise ExportError("source_reference must be a non-secret receipt reference")
    for reference in (value["terms_reference"], value["source_reference"]):
        if any(part in {".", ".."} for part in reference.split(":", 1)[1].split("/")):
            raise ExportError("receipt references must not traverse paths")
    if value.get("gismo_commit") != GISMO_COMMIT:
        raise ExportError("GISMo source commit must equal the frozen revision")

    files = value.get("source_files")
    required = {"layer1.json", "layer2.json", "det_ingrs.json", *INDEX_FILES.values(),
                "preprocess.py", "recipe1m.yaml", "nodes_191120.csv"}
    if type(files) is not dict or set(files) != required:
        raise ExportError("source_files must pin the exact Recipe1M, GISMo index, and preprocessing inputs")
    for name, record in files.items():
        if type(record) is not dict or set(record) != {"relative_path", "byte_count", "sha256"}:
            raise ExportError("source file pin has an invalid shape")
        _safe_relative(record["relative_path"], "source file relative_path")
        if type(record["byte_count"]) is not int or record["byte_count"] <= 0:
            raise ExportError("source file byte_count must be positive")
        if not _is_sha(record["sha256"]):
            raise ExportError("source file SHA-256 is invalid")
    roots = {"recipe1m": set(), "gismo": set()}
    for name, record in files.items():
        root_name = "gismo" if name in {"preprocess.py", "recipe1m.yaml", *INDEX_FILES.values()} else "recipe1m"
        relative = _safe_relative(record["relative_path"], "source file relative_path")
        if relative in roots[root_name]:
            raise ExportError("source manifest contains duplicate relative file paths")
        roots[root_name].add(relative)
    if files["preprocess.py"]["relative_path"] != "inv_cooking/datasets/recipe1m/preprocess.py":
        raise ExportError("preprocess source path is not the pinned canonical path")
    if files["recipe1m.yaml"]["relative_path"] != "conf/dataset/recipe1m.yaml":
        raise ExportError("GISMo config path is not the pinned canonical path")
    # Bind the parsed object to the exact bytes read, then compare this digest
    # again before publishing so a replaced receipt cannot authorize another input.
    return value, hashlib.sha256(raw).hexdigest()


def _load_kaggle_attestation(
    path: Optional[Path], expected_sha256: Optional[str], *, owner: Optional[str],
    dataset: Optional[str], kernel: Optional[str], source_manifest_sha256: Optional[str] = None,
    package_manifest_sha256: Optional[str] = None,
) -> dict[str, Any]:
    """Validate immutable attestation bytes and identity without deciding freshness."""
    if path is None or not _is_sha(expected_sha256) or not owner or not dataset or not kernel:
        raise ExportError("Kaggle profile requires a complete pinned preflight attestation")
    resolved = path.resolve(strict=True)
    if resolved.stat().st_size > 65536:
        raise ExportError("Kaggle preflight attestation exceeds the size limit")
    raw = resolved.read_bytes()
    if hashlib.sha256(raw).hexdigest() != expected_sha256:
        raise ExportError("Kaggle preflight attestation hash mismatch")
    try:
        value = json.loads(raw.decode("utf-8", errors="strict"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ExportError("Kaggle preflight attestation is not valid UTF-8 JSON") from exc
    required = {
        "schema_version", "observed_at_utc", "owner", "dataset_slug", "kernel_slug",
        "dataset_private", "kernel_private", "dataset_collaborators", "kernel_collaborators",
        "gpu_enabled", "internet_enabled", "source_manifest_sha256", "gismo_commit",
        "exporter_sha256", "package_manifest_sha256",
    }
    if (type(value) is not dict or set(value) != required
            or type(value.get("schema_version")) is not int or value.get("schema_version") != 1):
        raise ExportError("Kaggle preflight attestation schema mismatch")
    if (not KAGGLE_OWNER.fullmatch(owner) or not KAGGLE_SLUG.fullmatch(dataset)
            or not KAGGLE_SLUG.fullmatch(kernel)):
        raise ExportError("Kaggle preflight expected identity is malformed")
    if (value["owner"], value["dataset_slug"], value["kernel_slug"]) != (owner, dataset, kernel):
        raise ExportError("Kaggle preflight attestation identity mismatch")
    if (value["dataset_private"] is not True or value["kernel_private"] is not True
            or value["gpu_enabled"] is not False or value["internet_enabled"] is not False
            or value["dataset_collaborators"] != []
            or value["kernel_collaborators"] != []):
        raise ExportError("Kaggle preflight attestation does not prove the required private offline CPU configuration")
    try:
        observed_at = dt.datetime.fromisoformat(value["observed_at_utc"].replace("Z", "+00:00"))
    except (AttributeError, TypeError, ValueError):
        raise ExportError("Kaggle preflight observation time is invalid") from None
    if observed_at.tzinfo is None or observed_at.utcoffset() != dt.timedelta(0):
        raise ExportError("Kaggle preflight observation time must be UTC")
    if (not _is_sha(value["source_manifest_sha256"])
            or value["gismo_commit"] != GISMO_COMMIT or not _is_sha(value["exporter_sha256"])
            or not _is_sha(value["package_manifest_sha256"])):
        raise ExportError("Kaggle preflight attestation has invalid evidence fields")
    if sha256_file(Path(__file__).resolve(strict=True)) != value["exporter_sha256"]:
        raise ExportError("Kaggle preflight attestation is bound to a different exporter")
    if source_manifest_sha256 is not None and value["source_manifest_sha256"] != source_manifest_sha256:
        raise ExportError("Kaggle preflight attestation is bound to a different source manifest")
    if (package_manifest_sha256 is not None
            and value["package_manifest_sha256"] != package_manifest_sha256):
        raise ExportError("Kaggle preflight attestation is bound to a different code package")
    return value


def _verify_kaggle_attestation(
    path: Optional[Path], expected_sha256: Optional[str], *, owner: Optional[str],
    dataset: Optional[str], kernel: Optional[str], source_manifest_sha256: Optional[str] = None,
    package_manifest_sha256: Optional[str] = None,
    now_utc: Optional[dt.datetime] = None,
) -> dict[str, Any]:
    """Admit a live-preflight receipt only while its observation is fresh."""
    value = _load_kaggle_attestation(
        path, expected_sha256, owner=owner, dataset=dataset, kernel=kernel,
        source_manifest_sha256=source_manifest_sha256,
        package_manifest_sha256=package_manifest_sha256,
    )
    observed_at = dt.datetime.fromisoformat(value["observed_at_utc"].replace("Z", "+00:00"))
    now = now_utc if now_utc is not None else dt.datetime.now(dt.timezone.utc)
    if now.tzinfo is None or now.utcoffset() != dt.timedelta(0):
        raise ExportError("Kaggle preflight current clock must be UTC")
    age = now - observed_at
    if age < dt.timedelta(0):
        raise ExportError("Kaggle preflight observation time is in the future")
    if age > PREFLIGHT_ATTESTATION_MAX_AGE:
        raise ExportError("Kaggle preflight attestation is stale")
    return value


def _verify_admitted_kaggle_attestation(
    path: Optional[Path], expected_sha256: Optional[str], *, owner: Optional[str],
    dataset: Optional[str], kernel: Optional[str], admitted_path: Path,
    admitted_sha256: str, admitted_value: Mapping[str, Any],
    source_manifest_sha256: str, package_manifest_sha256: str,
) -> None:
    """Recheck the exact admitted receipt at publication without re-admitting it."""
    if path is None or path.resolve(strict=True) != admitted_path or expected_sha256 != admitted_sha256:
        raise ExportError("Kaggle preflight admission binding changed during source processing")
    value = _load_kaggle_attestation(
        path, expected_sha256, owner=owner, dataset=dataset, kernel=kernel,
        source_manifest_sha256=source_manifest_sha256,
        package_manifest_sha256=package_manifest_sha256,
    )
    if value != admitted_value:
        raise ExportError("Kaggle preflight attestation changed after this invocation's admission")


def _check_export_elapsed(started_monotonic: float) -> None:
    elapsed = time.monotonic() - started_monotonic
    if elapsed < 0 or elapsed > EXPORTER_MAX_RUNTIME_SECONDS:
        raise ExportError("exporter execution time limit exceeded; output was not published")


def _verify_source_file(root: Path, name: str, record: Mapping[str, Any]) -> Path:
    relative = _safe_relative(record["relative_path"], f"{name}.relative_path")
    path = (root / relative).resolve(strict=True)
    if not _inside(path, root):
        raise ExportError("source file escapes declared source root")
    if not path.is_file():
        raise ExportError("a pinned source path is not a file")
    stat = path.stat()
    if stat.st_size != record["byte_count"] or sha256_file(path) != record["sha256"]:
        raise ExportError("pinned source file size or hash mismatch")
    return path


def _check_git_commit(gismo_root: Path) -> None:
    try:
        result = subprocess.run(
            ["git", "-C", str(gismo_root), "rev-parse", "HEAD"],
            check=True, capture_output=True, text=True, encoding="ascii", timeout=10,
        )
        dirty = subprocess.run(
            ["git", "-C", str(gismo_root), "status", "--porcelain", "--untracked-files=all"],
            check=True, capture_output=True, text=True, encoding="utf-8", timeout=10,
        )
    except (OSError, subprocess.SubprocessError) as exc:
        raise ExportError("GISMo checkout revision could not be verified") from exc
    if result.stdout.strip() != GISMO_COMMIT:
        raise ExportError("GISMo checkout is not at the frozen source commit")
    if dirty.stdout.strip():
        raise ExportError("GISMo checkout has local modifications or untracked files")


def _check_packaged_gismo(
    gismo_root: Path, package_manifest: Path, expected_package_sha256: str,
) -> None:
    """Verify the source-only snapshot against its digest-bound package manifest."""
    # Keep imports from mutating the attested package inventory with .pyc files.
    sys.dont_write_bytecode = True
    package_root = package_manifest.resolve(strict=True).parent
    if package_manifest.resolve(strict=True) != package_root / "package-manifest.json":
        raise ExportError("Kaggle package manifest has an unexpected filename")
    if gismo_root.resolve(strict=True) != (package_root / "gismo").resolve(strict=True):
        raise ExportError("GISMo code root is not the package's pinned gismo directory")
    if sha256_file(package_manifest) != expected_package_sha256:
        raise ExportError("Kaggle code package manifest hash mismatch")
    try:
        manifest = json.loads(package_manifest.read_text(encoding="utf-8", errors="strict"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise ExportError("Kaggle code package manifest is invalid") from exc
    if (type(manifest) is not dict or manifest.get("schema") != "chefkix.t535.kaggle-source-free-package"
            or type(manifest.get("version")) is not int or manifest.get("version") != PACKAGE_VERSION
            or manifest.get("gismo_commit") != GISMO_COMMIT):
        raise ExportError("Kaggle code package does not identify the frozen GISMo snapshot")
    runtime = manifest.get("runtime")
    archive = runtime.get("archive") if type(runtime) is dict else None
    platform_spec = runtime.get("platform_spec") if type(runtime) is dict else None
    if (type(runtime) is not dict
            or set(runtime) != {"archive", "platform_spec", "resolution_record", "provenance"}
            or runtime.get("resolution_record") != PACKAGE_RUNTIME_RESOLUTION
            or type(runtime.get("provenance")) is not str
            or type(archive) is not dict
            or set(archive) != {"path", "sha256"}
            or archive.get("path") != "runtime/kaggle-linux-runtime-v6.tar.gz"
            or not _is_sha(archive.get("sha256"))
            or type(platform_spec) is not dict
            or set(platform_spec) != {"path", "sha256"}
            or platform_spec.get("path") != "runtime/t535-kaggle-runtime-linux.yml"
            or not _is_sha(platform_spec.get("sha256"))):
        raise ExportError("Kaggle runtime declaration is ambiguous or unsupported")
    if manifest.get("source_data") != PACKAGE_SOURCE_DATA:
        raise ExportError("Kaggle package source-data boundary is ambiguous or unsupported")
    exporter = manifest.get("exporter")
    if type(exporter) is not dict or exporter.get("path") != "code/export_c3_recipe1m_order.py":
        raise ExportError("Kaggle code package does not identify the expected exporter")
    exporter_path = package_root / "code" / "export_c3_recipe1m_order.py"
    if exporter_path.resolve(strict=True) != Path(__file__).resolve(strict=True):
        raise ExportError("running exporter is not the copy inside the pinned code package")
    if (not _is_sha(exporter.get("sha256"))
            or exporter.get("sha256") != sha256_file(Path(__file__).resolve(strict=True))
            or not exporter_path.is_file()
            or sha256_file(exporter_path) != exporter.get("sha256")):
        raise ExportError("packaged exporter does not match its pinned hash")
    bootstrap = manifest.get("bootstrap")
    if (type(bootstrap) is not dict or bootstrap.get("path") != "code/bootstrap_t535_kaggle.py"
            or not _is_sha(bootstrap.get("sha256"))):
        raise ExportError("package does not identify its pinned Kaggle bootstrap")

    entries = manifest.get("files")
    if type(entries) is not list:
        raise ExportError("Kaggle package inventory is missing")
    inventory: dict[str, tuple[str, int]] = {}
    for entry in entries:
        if type(entry) is not dict or set(entry) != {"path", "size_bytes", "sha256"}:
            raise ExportError("Kaggle package inventory entry is malformed")
        relative = _safe_relative(entry["path"], "package file path")
        if relative in inventory or type(entry["size_bytes"]) is not int or entry["size_bytes"] < 0:
            raise ExportError("Kaggle package inventory has duplicate or invalid entries")
        if not _is_sha(entry["sha256"]):
            raise ExportError("Kaggle package inventory has an invalid file hash")
        inventory[relative] = (entry["sha256"], entry["size_bytes"])
    if inventory.get("code/export_c3_recipe1m_order.py", (None, None))[0] != exporter["sha256"]:
        raise ExportError("package inventory and exporter pin disagree")
    if inventory.get("code/bootstrap_t535_kaggle.py", (None, None))[0] != bootstrap["sha256"]:
        raise ExportError("package inventory and bootstrap pin disagree")
    for artifact in (archive, platform_spec):
        if inventory.get(artifact["path"], (None, None))[0] != artifact["sha256"]:
            raise ExportError("package runtime declaration and file inventory disagree")
    if not set(PACKAGE_SPLIT_INDEX_PATHS).issubset(inventory):
        raise ExportError("package source-data metadata does not match its split-index inventory")
    licenses = manifest.get("third_party_licenses")
    if (licenses != PACKAGE_THIRD_PARTY_LICENSES
            or "gismo/LICENSE.md" not in inventory
            or "licenses/intel-mkl-2020.2-license-april-2018.txt" not in inventory
            or inventory["licenses/intel-mkl-2020.2-license-april-2018.txt"][0]
            != PACKAGE_THIRD_PARTY_LICENSES[1]["notice_sha256"]
            or "licenses/intel-openmp-2025.0.0-license-october-2022.txt" not in inventory
            or inventory["licenses/intel-openmp-2025.0.0-license-october-2022.txt"][0]
            != PACKAGE_THIRD_PARTY_LICENSES[2]["notice_sha256"]
            or "licenses/jpeg-9b-ijg-license-notice.txt" not in inventory
            or inventory["licenses/jpeg-9b-ijg-license-notice.txt"][0]
            != PACKAGE_THIRD_PARTY_LICENSES[3]["notice_sha256"]
            or "licenses/zlib-1.3.2-license.txt" not in inventory
            or inventory["licenses/zlib-1.3.2-license.txt"][0]
            != PACKAGE_THIRD_PARTY_LICENSES[4]["notice_sha256"]
            or PACKAGE_THIRD_PARTY_LICENSES[4]["notice_sha256"]
            != PACKAGE_THIRD_PARTY_LICENSES[5]["notice_sha256"]):
        raise ExportError("package does not preserve exact third-party license notices")

    actual_package: set[str] = set()
    for path in package_root.rglob("*"):
        if path.is_symlink():
            raise ExportError("source-only package contains a symlink")
        if path.is_file():
            actual_package.add(path.relative_to(package_root).as_posix())
    if actual_package != set(inventory) | {"package-manifest.json"}:
        raise ExportError("source-only package contains missing or unexpected files")

    # The digest-pinned package manifest binds every packaged artifact, not only
    # the executable GISMo subset. Rehash locks/runtime/spec files as well.
    for relative, (expected_sha, expected_size) in inventory.items():
        package_path = package_root / Path(*relative.split("/"))
        if (not package_path.is_file() or package_path.stat().st_size != expected_size
                or sha256_file(package_path) != expected_sha):
            raise ExportError("a source-only package file differs from its pinned inventory")

    gismo_entries = {name: value for name, value in inventory.items() if name.startswith("gismo/")}
    if not gismo_entries:
        raise ExportError("Kaggle package contains no GISMo sources")
    actual: set[str] = set()
    for path in gismo_root.rglob("*"):
        if path.is_symlink():
            raise ExportError("packaged GISMo tree contains a symlink")
        if path.is_file():
            actual.add("gismo/" + path.relative_to(gismo_root).as_posix())
    if actual != set(gismo_entries):
        raise ExportError("packaged GISMo inventory differs from its manifest")
    for relative, (expected_sha, expected_size) in gismo_entries.items():
        package_path = gismo_root / Path(*relative[6:].split("/"))
        if (not package_path.is_file() or package_path.stat().st_size != expected_size
                or sha256_file(package_path) != expected_sha):
            raise ExportError("a packaged GISMo file differs from its pinned inventory")


def _sid_string(sid: int) -> str:
    advapi = ctypes.WinDLL("advapi32", use_last_error=True)
    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
    output = ctypes.c_wchar_p()
    advapi.ConvertSidToStringSidW.argtypes = [ctypes.c_void_p, ctypes.POINTER(ctypes.c_wchar_p)]
    advapi.ConvertSidToStringSidW.restype = ctypes.c_int
    if not advapi.ConvertSidToStringSidW(ctypes.c_void_p(sid), ctypes.byref(output)):
        raise OSError(ctypes.get_last_error(), "SID conversion failed")
    try:
        return output.value
    finally:
        kernel32.LocalFree.argtypes = [ctypes.c_void_p]
        kernel32.LocalFree.restype = ctypes.c_void_p
        kernel32.LocalFree(ctypes.cast(output, ctypes.c_void_p))


def _current_user_sid() -> str:
    class SID_AND_ATTRIBUTES(ctypes.Structure):
        _fields_ = [("Sid", ctypes.c_void_p), ("Attributes", ctypes.c_uint32)]

    class TOKEN_USER(ctypes.Structure):
        _fields_ = [("User", SID_AND_ATTRIBUTES)]

    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
    advapi = ctypes.WinDLL("advapi32", use_last_error=True)
    token = ctypes.c_void_p()
    kernel32.GetCurrentProcess.restype = ctypes.c_void_p
    advapi.OpenProcessToken.argtypes = [ctypes.c_void_p, ctypes.c_uint32, ctypes.POINTER(ctypes.c_void_p)]
    advapi.OpenProcessToken.restype = ctypes.c_int
    if not advapi.OpenProcessToken(kernel32.GetCurrentProcess(), 0x0008, ctypes.byref(token)):
        raise OSError(ctypes.get_last_error(), "current user token could not be opened")
    try:
        needed = ctypes.c_uint32()
        advapi.GetTokenInformation.argtypes = [ctypes.c_void_p, ctypes.c_int, ctypes.c_void_p,
                                               ctypes.c_uint32, ctypes.POINTER(ctypes.c_uint32)]
        advapi.GetTokenInformation.restype = ctypes.c_int
        advapi.GetTokenInformation(token, 1, None, 0, ctypes.byref(needed))
        if needed.value <= 0:
            raise OSError(ctypes.get_last_error(), "current user token information is unavailable")
        buffer = ctypes.create_string_buffer(needed.value)
        if not advapi.GetTokenInformation(token, 1, buffer, needed.value, ctypes.byref(needed)):
            raise OSError(ctypes.get_last_error(), "current user token information is unavailable")
        token_user = ctypes.cast(buffer, ctypes.POINTER(TOKEN_USER)).contents
        return _sid_string(token_user.User.Sid)
    finally:
        kernel32.CloseHandle.argtypes = [ctypes.c_void_p]
        kernel32.CloseHandle(token)


def _private_acl_sids(path: Path) -> tuple[str, list[tuple[str, int]]]:
    """Read the owner and allow-list-only DACL for one existing Windows path."""
    if os.name != "nt":
        raise ExportError("private real-input processing requires a verifiable Windows ACL")
    class ACL_SIZE_INFORMATION(ctypes.Structure):
        _fields_ = [("AceCount", ctypes.c_uint32), ("AclBytesInUse", ctypes.c_uint32),
                    ("AclBytesFree", ctypes.c_uint32)]

    class ACE_HEADER(ctypes.Structure):
        _fields_ = [("AceType", ctypes.c_ubyte), ("AceFlags", ctypes.c_ubyte),
                    ("AceSize", ctypes.c_uint16)]

    advapi = ctypes.WinDLL("advapi32", use_last_error=True)
    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
    owner = ctypes.c_void_p()
    dacl = ctypes.c_void_p()
    descriptor = ctypes.c_void_p()
    advapi.GetNamedSecurityInfoW.argtypes = [ctypes.c_wchar_p, ctypes.c_int, ctypes.c_uint32,
        ctypes.POINTER(ctypes.c_void_p), ctypes.c_void_p, ctypes.POINTER(ctypes.c_void_p),
        ctypes.c_void_p, ctypes.POINTER(ctypes.c_void_p)]
    advapi.GetNamedSecurityInfoW.restype = ctypes.c_uint32
    error = advapi.GetNamedSecurityInfoW(str(path), 1, 0x00000001 | 0x00000004,
        ctypes.byref(owner), None, ctypes.byref(dacl), None, ctypes.byref(descriptor))
    if error != 0:
        raise OSError(error, "Windows ACL could not be read")
    try:
        if not owner.value or not dacl.value:
            raise ExportError("path has a null owner or permissive null DACL")
        owner_sid = _sid_string(owner.value)
        info = ACL_SIZE_INFORMATION()
        if not advapi.GetAclInformation(ctypes.c_void_p(dacl.value), ctypes.byref(info),
                                        ctypes.sizeof(info), 2):
            raise OSError(ctypes.get_last_error(), "Windows DACL could not be enumerated")
        allowed_entries: list[tuple[str, int]] = []
        current_sid = _current_user_sid()
        for index in range(info.AceCount):
            ace = ctypes.c_void_p()
            if not advapi.GetAce(ctypes.c_void_p(dacl.value), index, ctypes.byref(ace)):
                raise OSError(ctypes.get_last_error(), "Windows DACL entry could not be read")
            header = ctypes.cast(ace, ctypes.POINTER(ACE_HEADER)).contents
            # Only basic allow ACEs for the user, SYSTEM, and local Administrators are accepted.
            if header.AceType != 0 or header.AceSize < ctypes.sizeof(ACE_HEADER) + 8:
                raise ExportError("path DACL contains a deny, callback, object, or unsupported ACE")
            if header.AceFlags & ~0x1F:
                raise ExportError("path DACL contains unsupported access-control flags")
            mask = ctypes.c_uint32.from_address(ace.value + ctypes.sizeof(ACE_HEADER)).value
            sid = _sid_string(ace.value + ctypes.sizeof(ACE_HEADER) + ctypes.sizeof(ctypes.c_uint32))
            if mask and sid not in {"S-1-5-18", "S-1-5-32-544", current_sid}:
                raise ExportError("path DACL grants access to an unapproved principal")
            if mask:
                # Keep flags: INHERIT_ONLY entries do not grant access to this
                # object, though their principal still matters to descendants.
                allowed_entries.append((sid, header.AceFlags))
        return owner_sid, allowed_entries
    finally:
        kernel32.LocalFree.argtypes = [ctypes.c_void_p]
        kernel32.LocalFree.restype = ctypes.c_void_p
        kernel32.LocalFree(descriptor)


def _validate_private_acl(owner_sid: str, allowed_entries: Sequence[tuple[str, int]], current_sid: str) -> None:
    if owner_sid != current_sid:
        raise ExportError("private path must be owned by the current user")
    permitted = {current_sid, "S-1-5-18", "S-1-5-32-544"}
    if not allowed_entries or any(sid not in permitted for sid, _ in allowed_entries):
        raise ExportError("path DACL grants access to an unapproved principal")
    effective_sids = {sid for sid, flags in allowed_entries if not flags & 0x08}
    if current_sid not in effective_sids:
        raise ExportError("private path DACL does not explicitly grant the current user access")


def _require_private_local_path(
    path: Path, *, execution_profile: str = "windows-owner-only",
    dataset_root: Path | None = None, working_root: Path | None = None,
) -> None:
    """Require the selected private execution boundary before real data access."""
    if execution_profile == "kaggle-private":
        if dataset_root is None or working_root is None:
            raise ExportError("Kaggle private mount roots must be explicit")
        resolved = path.resolve(strict=True)
        input_root = KAGGLE_INPUT_ROOT.resolve(strict=True)
        kaggle_working = KAGGLE_WORKING_ROOT.resolve(strict=True)
        pinned_dataset = dataset_root.resolve(strict=True)
        output_root = working_root.resolve(strict=True)
        if not _inside(pinned_dataset, input_root) or not _inside(output_root, kaggle_working):
            raise ExportError("Kaggle roots are outside the expected input/working mounts")
        if not _inside(resolved, pinned_dataset) and not _inside(resolved, output_root):
            raise ExportError("Kaggle private path is outside the pinned dataset and output roots")
        readonly = _kaggle_mount_readonly(resolved)
        if _inside(resolved, pinned_dataset) and not readonly:
            raise ExportError("Kaggle source mount is not read-only")
        if _inside(resolved, output_root) and readonly:
            raise ExportError("Kaggle output mount is read-only")
        return
    if execution_profile != "windows-owner-only":
        raise ExportError("unknown private execution profile")
    if os.name != "nt":
        raise ExportError("private real-input processing requires a verifiable Windows ACL")
    resolved = path.resolve(strict=True)
    if str(resolved).startswith("\\\\") or not resolved.drive:
        raise ExportError("private data paths must use a fixed local Windows volume")
    kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
    volume_root = ctypes.create_unicode_buffer(32768)
    kernel32.GetVolumePathNameW.argtypes = [ctypes.c_wchar_p, ctypes.c_wchar_p, ctypes.c_uint32]
    kernel32.GetVolumePathNameW.restype = ctypes.c_int
    if not kernel32.GetVolumePathNameW(str(resolved), volume_root, len(volume_root)):
        raise OSError(ctypes.get_last_error(), "Windows volume mount point could not be resolved")
    kernel32.GetDriveTypeW.argtypes = [ctypes.c_wchar_p]
    kernel32.GetDriveTypeW.restype = ctypes.c_uint32
    drive_type = kernel32.GetDriveTypeW(volume_root.value)
    if drive_type != 3:  # DRIVE_FIXED
        raise ExportError("private data paths must use a fixed local Windows volume")
    owner_sid, allowed_sids = _private_acl_sids(resolved)
    current_sid = _current_user_sid()
    _validate_private_acl(owner_sid, allowed_sids, current_sid)


def _enforce_windows_acl() -> bool:
    return os.name == "nt"


def _kaggle_mount_readonly(path: Path) -> bool:
    statvfs = getattr(os, "statvfs", None)
    if statvfs is None:
        raise ExportError("Kaggle mount flags are unavailable on this platform")
    flags = statvfs(path).f_flag
    return bool(flags & getattr(os, "ST_RDONLY", 1))


def _check_imported_gismo_tree(modules: Mapping[str, Any], gismo_root: Path) -> None:
    """Reject a mixed/preloaded inv_cooking package assembled from another checkout."""
    root = gismo_root.resolve(strict=True)
    for name, module in modules.items():
        if name != "inv_cooking" and not name.startswith("inv_cooking."):
            continue
        if not isinstance(module, ModuleType):
            continue
        module_file = getattr(module, "__file__", None)
        if module_file is not None and not _inside(Path(module_file).resolve(strict=True), root):
            raise ExportError("an imported GISMo module is outside the pinned checkout")
        module_paths = getattr(module, "__path__", ())
        for item in module_paths:
            if not _inside(Path(item).resolve(strict=True), root):
                raise ExportError("an imported GISMo package path is outside the pinned checkout")


def _load_indices(paths: Mapping[str, Path]) -> dict[str, list[int]]:
    result: dict[str, list[int]] = {}
    for name, path in paths.items():
        try:
            text = path.read_text(encoding="utf-8", errors="strict")
        except (OSError, UnicodeDecodeError) as exc:
            raise ExportError("GISMo index file is not valid UTF-8 text") from exc
        values: list[int] = []
        for line in text.splitlines():
            stripped = line.strip()
            if not stripped or not stripped.isdecimal():
                raise ExportError("GISMo index file contains a non-integer row")
            values.append(int(stripped))
        if not values or len(values) != len(set(values)):
            raise ExportError("GISMo index file is empty or contains duplicate positions")
        result[name] = values
    return result


def _load_pinned_indices(paths: Mapping[str, Path], records: Mapping[str, Mapping[str, Any]]) -> dict[str, list[int]]:
    """Parse the exact index bytes whose digest and size are pinned in the receipt."""
    result: dict[str, list[int]] = {}
    for name, path in paths.items():
        try:
            raw = path.read_bytes()
            text = raw.decode("utf-8", errors="strict")
        except (OSError, UnicodeDecodeError) as exc:
            raise ExportError("GISMo index file is not valid UTF-8 text") from exc
        record = records[name]
        if len(raw) != record["byte_count"] or hashlib.sha256(raw).hexdigest() != record["sha256"]:
            raise ExportError("GISMo index bytes differ from the pinned receipt")
        values: list[int] = []
        for line in text.splitlines():
            stripped = line.strip()
            if not stripped or not stripped.isdecimal():
                raise ExportError("GISMo index file contains a non-integer row")
            values.append(int(stripped))
        if not values or len(values) != len(set(values)):
            raise ExportError("GISMo index file is empty or contains duplicate positions")
        result[name] = values
    return result


def _extract_order(layer1: Sequence[Mapping[str, Any]], processed: Mapping[str, Sequence[Mapping[str, Any]]]) -> dict[str, list[dict[str, Any]]]:
    """Reconcile the actual preprocessing output to source ordinals by exact ID."""
    source_maps: dict[str, dict[str, int]] = {split: {} for split in SPLITS}
    global_splits: dict[str, str] = {}
    for ordinal, row in enumerate(layer1):
        if not isinstance(row, Mapping):
            raise ExportError("Recipe1M layer1 has a non-object record")
        recipe_id = row.get("id")
        partition = row.get("partition")
        split = PARTITION_TO_SOURCE.get(partition) if type(partition) is str else None
        if type(recipe_id) is not str or not recipe_id or split not in source_maps:
            raise ExportError("Recipe1M source record lacks an exact ID or supported partition")
        if recipe_id in global_splits:
            raise ExportError("Recipe1M source has a duplicate recipe ID; no ordinal is guessed")
        source_maps[split][recipe_id] = ordinal
        global_splits[recipe_id] = split

    output: dict[str, list[dict[str, Any]]] = {}
    seen_output_ids: dict[str, str] = {}
    for split in SPLITS:
        processed_split = PROCESSED_SPLITS[split]
        if processed_split not in processed or not isinstance(processed[processed_split], Sequence):
            raise ExportError("GISMo preprocessing output is missing a declared split")
        rows: list[dict[str, Any]] = []
        prior_ordinal = -1
        for position, record in enumerate(processed[processed_split]):
            if not isinstance(record, Mapping):
                raise ExportError("GISMo preprocessing output has a non-object record")
            recipe_id = record.get("id")
            if type(recipe_id) is not str or not recipe_id:
                raise ExportError("GISMo preprocessing output lacks an exact recipe ID")
            observed_split = global_splits.get(recipe_id)
            if observed_split != split:
                raise ExportError("GISMo retained ID is missing from or conflicts with its Recipe1M split")
            if recipe_id in seen_output_ids:
                raise ExportError("GISMo retained output repeats a recipe ID across splits")
            seen_output_ids[recipe_id] = split
            ordinal = source_maps[split].get(recipe_id)
            if ordinal is None or ordinal <= prior_ordinal:
                raise ExportError("GISMo retained IDs do not preserve exact source order")
            prior_ordinal = ordinal
            rows.append({
                "source_split": split,
                "source_ordinal": ordinal,
                "filtered_position": position,
                "recipe_id": recipe_id,
            })
        if not rows:
            raise ExportError("GISMo preprocessing produced an empty split")
        output[split] = rows
    return output


def _check_indices(indices: Mapping[str, Sequence[int]], counts: Mapping[str, int]) -> None:
    domains = {"train": counts["train"], "val_all": counts["validation"],
               "val": counts["validation"], "test": counts["test"]}
    if set(indices) != set(INDEX_FILES):
        raise ExportError("GISMo index inventory mismatch")
    for name, values in indices.items():
        if any(type(value) is not int or value < 0 or value >= domains[name] for value in values):
            raise ExportError("GISMo index is outside its pinned post-filter domain")
    if not set(indices["val"]).issubset(indices["val_all"]):
        raise ExportError("GISMo val indices are not a subset of val_all")
    if len(indices["val"]) == len(indices["val_all"]):
        raise ExportError("GISMo val subset and val_all must remain distinct")


def _canonical_jsonl(rows: Sequence[Mapping[str, Any]]) -> bytes:
    return b"".join(canonical_json(dict(row)) + b"\n" for row in rows)


def _invoke_frozen_gismo(gismo_root: Path, recipe1m_root: Path) -> tuple[list[Mapping[str, Any]], Mapping[str, Sequence[Mapping[str, Any]]]]:
    """Call upstream functions; no subprocess or output-pickle cache is used."""
    gismo_text = str(gismo_root)
    sys.path.insert(0, gismo_text)
    try:
        with contextlib.redirect_stdout(io.StringIO()), contextlib.redirect_stderr(io.StringIO()):
            omega = importlib.import_module("omegaconf")
            module = importlib.import_module("inv_cooking.datasets.recipe1m.preprocess")
            module_path = Path(module.__file__).resolve(strict=True)
            if not _inside(module_path, gismo_root.resolve(strict=True)):
                raise ExportError("imported GISMo preprocessor is outside the pinned checkout")
            _check_imported_gismo_tree(sys.modules, gismo_root)
            config_path = gismo_root / "conf" / "dataset" / "recipe1m.yaml"
            config = omega.OmegaConf.load(config_path).pre_processing
            dets, layer1, layer2 = module.load_unprocessed_dataset(str(recipe1m_root))
            _, _, _, processed = module.build_vocab_recipe1m(
                dets, layer1, layer2, config, str(recipe1m_root)
            )
        if type(layer1) is not list or type(processed) is not dict:
            raise ExportError("GISMo preprocessing returned an unexpected container type")
        return layer1, processed
    except ExportError:
        raise
    except Exception as exc:
        # Upstream exception text may embed source values; do not propagate it.
        raise ExportError(f"pinned GISMo preprocessing failed ({type(exc).__name__})") from None
    finally:
        try:
            sys.path.remove(str(gismo_root))
        except ValueError:
            pass


def _atomic_promote_directory(staging: Path, output: Path) -> None:
    """Promote within one parent without ever replacing an existing output."""
    if os.name == "nt":
        move_file = ctypes.WinDLL("kernel32", use_last_error=True).MoveFileW
        move_file.argtypes = [ctypes.c_wchar_p, ctypes.c_wchar_p]
        move_file.restype = ctypes.c_int
        if not move_file(str(staging), str(output)):
            error = ctypes.get_last_error()
            if error in (80, 183):
                raise FileExistsError("output directory already exists")
            raise OSError(error, "atomic no-replace promotion failed")
    elif sys.platform.startswith("linux"):
        libc = ctypes.CDLL(None, use_errno=True)
        renameat2 = getattr(libc, "renameat2", None)
        if renameat2 is None:
            raise OSError(errno.ENOTSUP, "renameat2 no-replace support is unavailable")
        renameat2.argtypes = [ctypes.c_int, ctypes.c_char_p, ctypes.c_int, ctypes.c_char_p, ctypes.c_uint]
        renameat2.restype = ctypes.c_int
        if renameat2(-100, os.fsencode(staging), -100, os.fsencode(output), 1) != 0:
            error = ctypes.get_errno()
            if error == errno.EEXIST:
                raise FileExistsError("output directory already exists")
            raise OSError(error, os.strerror(error))
    else:
        raise OSError(errno.ENOTSUP, "atomic no-replace promotion is unsupported")


def _entry_exists(path: Path) -> bool:
    """Only a confirmed missing entry is absence; access/stat errors are not."""
    try:
        path.lstat()
    except FileNotFoundError:
        return False
    except OSError as exc:
        raise ExportError("generated directory state could not be verified") from exc
    return True


def _remove_generated_directory(path: Path, label: str) -> None:
    """Remove an exact generated directory, retry once, and verify absence."""
    first_error: BaseException | None = None
    for attempt in range(2):
        try:
            if not _entry_exists(path):
                break
            shutil.rmtree(path)
            if _entry_exists(path):
                raise OSError("generated directory remains after cleanup")
            break
        except BaseException as exc:
            if attempt == 0:
                first_error = exc
                continue
            raise ExportError(f"{label} cleanup failed; secure cleanup is required") from exc
    if isinstance(first_error, (KeyboardInterrupt, SystemExit)):
        raise first_error


def _rollback_published_directory(output: Path) -> None:
    _remove_generated_directory(output, "published output rollback")


def _write_atomic_new_directory(
    output: Path, files: Mapping[str, bytes], *, execution_profile: str = "windows-owner-only",
    dataset_root: Optional[Path] = None, working_root: Optional[Path] = None,
) -> None:
    if _entry_exists(output):
        raise ExportError("output directory already exists")
    if not output.parent.is_dir():
        raise ExportError("output parent disappeared before staging")
    staging = Path(tempfile.mkdtemp(prefix=".c3-order-", dir=output.parent))
    promoted = False
    promotion_attempted = False
    rollback_attempted = False
    try:
        for name, data in files.items():
            target = staging / name
            with target.open("xb") as stream:
                stream.write(data)
                stream.flush()
                os.fsync(stream.fileno())
        # Check the inherited ACL before any data becomes visible at its final name.
        if execution_profile == "windows-owner-only" and _enforce_windows_acl():
            _require_private_local_path(staging)
            for name in files:
                _require_private_local_path(staging / name)
        elif execution_profile == "kaggle-private":
            _require_private_local_path(staging, execution_profile=execution_profile,
                                        dataset_root=dataset_root, working_root=working_root)
            for name in files:
                _require_private_local_path(staging / name, execution_profile=execution_profile,
                                            dataset_root=dataset_root, working_root=working_root)
        # Set intent before the OS call. If an async interrupt lands after the
        # move but before the next Python assignment, infer success from the
        # source disappearing and the requested destination appearing.
        promotion_attempted = True
        _atomic_promote_directory(staging, output)
        promoted = True
        if execution_profile == "windows-owner-only" and _enforce_windows_acl():
            try:
                _require_private_local_path(output)
                for name in files:
                    _require_private_local_path(output / name)
            except BaseException as cause:
                # The exact final directory was created by this invocation. Remove
                # it rather than leave an output whose privacy check failed.
                try:
                    _rollback_published_directory(output)
                except ExportError:
                    raise ExportError("published output failed ACL validation and rollback failed; secure cleanup is required") from cause
                rollback_attempted = True
                if isinstance(cause, (KeyboardInterrupt, SystemExit)):
                    raise
                raise ExportError("published output failed ACL validation and was rolled back") from None
        elif execution_profile == "kaggle-private":
            try:
                _require_private_local_path(output, execution_profile=execution_profile,
                                            dataset_root=dataset_root, working_root=working_root)
                for name in files:
                    _require_private_local_path(output / name, execution_profile=execution_profile,
                                                dataset_root=dataset_root, working_root=working_root)
            except BaseException as cause:
                try:
                    _rollback_published_directory(output)
                except ExportError:
                    raise ExportError("published Kaggle output failed mount validation and rollback failed; secure cleanup is required") from cause
                rollback_attempted = True
                if isinstance(cause, (KeyboardInterrupt, SystemExit)):
                    raise
                raise ExportError("published Kaggle output failed mount validation and was rolled back") from None
    except BaseException:
        try:
            staging_exists = _entry_exists(staging)
            output_exists = _entry_exists(output)
        except BaseException as state_error:
            if promotion_attempted or promoted:
                raise ExportError("atomic promotion state is unknown; secure cleanup is required") from state_error
            _remove_generated_directory(staging, "staging output")
            raise
        moved_by_this_call = promotion_attempted and not staging_exists and output_exists
        if (promoted or moved_by_this_call) and not rollback_attempted:
            rollback_attempted = True
            _rollback_published_directory(output)
        elif not promoted and not moved_by_this_call:
            _remove_generated_directory(staging, "staging output")
        raise


def export_order(
    *, source_root: Path, gismo_root: Path, output_dir: Path, manifest_path: Path,
    worktree: Path, allow_private_real_inputs: bool = False,
    execution_profile: str = "windows-owner-only", kaggle_dataset_root: Optional[Path] = None,
    kaggle_working_root: Optional[Path] = None,
    kaggle_attestation: Optional[Path] = None,
    kaggle_package_manifest: Optional[Path] = None,
    expected_kaggle_attestation_sha256: Optional[str] = None,
    expected_kaggle_owner: Optional[str] = None, expected_kaggle_dataset: Optional[str] = None,
    expected_kaggle_kernel: Optional[str] = None,
) -> dict[str, Any]:
    if not allow_private_real_inputs:
        raise ExportError("real Recipe1M processing is disabled; pass --allow-private-real-inputs after the source handoff")
    source_root = _require_private_outside_worktree(source_root, worktree, exists=True)
    gismo_root = _require_private_outside_worktree(gismo_root, worktree, exists=True)
    output_dir = _require_private_outside_worktree(output_dir, worktree, exists=False)
    manifest_path = manifest_path.resolve(strict=True)
    worktree = worktree.resolve(strict=True)
    if _inside(manifest_path, worktree):
        raise ExportError("source manifest must remain outside the Git worktree")
    if _inside(output_dir, source_root) or _inside(output_dir, gismo_root):
        raise ExportError("output directory must not be nested in either source tree")
    if _entry_exists(output_dir):
        raise ExportError("output directory already exists")
    if _inside(manifest_path, source_root) or _inside(manifest_path, gismo_root):
        raise ExportError("source manifest must remain outside both source trees")
    output_parent = output_dir.parent.resolve(strict=True)
    if not output_parent.is_dir():
        raise ExportError("output parent must already exist; private ACL is checked before processing")
    admitted_attestation: Optional[dict[str, Any]] = None
    admitted_attestation_path: Optional[Path] = None
    admitted_attestation_sha256: Optional[str] = None
    if execution_profile == "kaggle-private":
        required = (kaggle_dataset_root, kaggle_working_root, kaggle_attestation,
                    kaggle_package_manifest, expected_kaggle_attestation_sha256, expected_kaggle_owner,
                    expected_kaggle_dataset, expected_kaggle_kernel)
        if any(value is None for value in required):
            raise ExportError("Kaggle profile requires a pinned private attestation and exact runtime identity")
        kaggle_dataset_root = kaggle_dataset_root.resolve(strict=True)
        kaggle_working_root = kaggle_working_root.resolve(strict=True)
        if not _inside(source_root, kaggle_dataset_root):
            raise ExportError("Recipe1M source is outside the pinned Kaggle dataset mount")
        if not _inside(manifest_path, kaggle_dataset_root):
            raise ExportError("source manifest is outside the pinned Kaggle dataset mount")
        if not _inside(gismo_root, kaggle_working_root) or not _inside(output_parent, kaggle_working_root):
            raise ExportError("GISMo code and output must remain under Kaggle working storage")
        if not _inside(kaggle_attestation.resolve(strict=True), kaggle_working_root):
            raise ExportError("Kaggle preflight attestation must remain in the pinned code package")
        kaggle_package_manifest = kaggle_package_manifest.resolve(strict=True)
        if (gismo_root.name != "gismo" or kaggle_package_manifest.name != "package-manifest.json"
                or kaggle_package_manifest.parent.resolve(strict=True) != gismo_root.parent.resolve(strict=True)):
            raise ExportError("Kaggle package manifest must sit beside the packaged GISMo directory")
        package_manifest_sha256 = sha256_file(kaggle_package_manifest)
        _require_private_local_path(kaggle_dataset_root, execution_profile=execution_profile,
                                    dataset_root=kaggle_dataset_root, working_root=kaggle_working_root)
        _require_private_local_path(kaggle_working_root, execution_profile=execution_profile,
                                    dataset_root=kaggle_dataset_root, working_root=kaggle_working_root)
        for path in (source_root, manifest_path, gismo_root, output_parent):
            _require_private_local_path(path, execution_profile=execution_profile,
                                        dataset_root=kaggle_dataset_root, working_root=kaggle_working_root)
        _verify_kaggle_attestation(
            kaggle_attestation, expected_kaggle_attestation_sha256,
            owner=expected_kaggle_owner, dataset=expected_kaggle_dataset,
            kernel=expected_kaggle_kernel,
            package_manifest_sha256=package_manifest_sha256,
        )
    else:
        _require_private_local_path(source_root)
        _require_private_local_path(manifest_path)
        _require_private_local_path(output_parent)
    manifest, parsed_manifest_sha256 = _load_manifest(manifest_path)
    if execution_profile == "kaggle-private":
        _verify_kaggle_attestation(
            kaggle_attestation, expected_kaggle_attestation_sha256,
            owner=expected_kaggle_owner, dataset=expected_kaggle_dataset,
            kernel=expected_kaggle_kernel,
            source_manifest_sha256=parsed_manifest_sha256,
            package_manifest_sha256=package_manifest_sha256,
        )
        _check_packaged_gismo(gismo_root, kaggle_package_manifest, package_manifest_sha256)
    else:
        _check_git_commit(gismo_root)

    gismo_paths: dict[str, Path] = {}
    for name, record in manifest["source_files"].items():
        root = gismo_root if name in {"preprocess.py", "recipe1m.yaml", *INDEX_FILES.values()} else source_root
        path = _verify_source_file(root, name, record)
        if name in {"layer1.json", "layer2.json", "det_ingrs.json"}:
            _require_private_local_path(path, execution_profile=execution_profile,
                                        dataset_root=kaggle_dataset_root, working_root=kaggle_working_root)
        if name in INDEX_FILES.values():
            gismo_paths[name] = path
    preprocessing_file = gismo_root / "inv_cooking" / "datasets" / "recipe1m" / "preprocess.py"
    config_file = gismo_root / "conf" / "dataset" / "recipe1m.yaml"
    # Task manifest names these two by their canonical relative paths.
    if manifest["source_files"]["preprocess.py"]["relative_path"] != "inv_cooking/datasets/recipe1m/preprocess.py":
        raise ExportError("preprocess source path is not the pinned canonical path")
    if manifest["source_files"]["recipe1m.yaml"]["relative_path"] != "conf/dataset/recipe1m.yaml":
        raise ExportError("GISMo config path is not the pinned canonical path")
    if sha256_file(preprocessing_file) != manifest["source_files"]["preprocess.py"]["sha256"]:
        raise ExportError("GISMo preprocessor hash mismatch")
    if sha256_file(config_file) != manifest["source_files"]["recipe1m.yaml"]["sha256"]:
        raise ExportError("GISMo configuration hash mismatch")

    # Hashes are checked again after preprocessing to detect source mutation during the run.
    if execution_profile == "kaggle-private":
        # This is the operation's admission point: require a still-fresh receipt
        # after source/package setup and immediately before touching source data.
        admitted_attestation = _verify_kaggle_attestation(
            kaggle_attestation, expected_kaggle_attestation_sha256,
            owner=expected_kaggle_owner, dataset=expected_kaggle_dataset,
            kernel=expected_kaggle_kernel,
            source_manifest_sha256=parsed_manifest_sha256,
            package_manifest_sha256=package_manifest_sha256,
        )
        admitted_attestation_path = kaggle_attestation.resolve(strict=True)
        admitted_attestation_sha256 = expected_kaggle_attestation_sha256
        export_started_monotonic = time.monotonic()
    layer1, processed = _invoke_frozen_gismo(gismo_root, source_root)
    if execution_profile == "kaggle-private":
        _check_export_elapsed(export_started_monotonic)
        _check_packaged_gismo(gismo_root, kaggle_package_manifest, package_manifest_sha256)
    else:
        _check_git_commit(gismo_root)
    for name, record in manifest["source_files"].items():
        root = gismo_root if name in {"preprocess.py", "recipe1m.yaml", *INDEX_FILES.values()} else source_root
        path = _verify_source_file(root, name, record)
        if name in {"layer1.json", "layer2.json", "det_ingrs.json"}:
            _require_private_local_path(path, execution_profile=execution_profile,
                                        dataset_root=kaggle_dataset_root, working_root=kaggle_working_root)
    rows = _extract_order(layer1, processed)
    indices = _load_pinned_indices(
        {name: gismo_paths[filename] for name, filename in INDEX_FILES.items()},
        {name: manifest["source_files"][filename] for name, filename in INDEX_FILES.items()},
    )
    counts = {split: len(rows[split]) for split in SPLITS}
    _check_indices(indices, counts)

    data_files = {f"{split}.jsonl": _canonical_jsonl(rows[split]) for split in SPLITS}
    index_summary = {
        name: {"sha256": manifest["source_files"][filename]["sha256"], "entry_count": len(indices[name])}
        for name, filename in INDEX_FILES.items()
    }
    summary = {
        "schema_version": 1,
        "artifact": "private-c3-recipe1m-filtered-order-id-only",
        "protocol_v1_sha256": PROTOCOL_SHA256,
        "analysis_spec_v1_sha256": ANALYSIS_SPEC_SHA256,
        "release_id": manifest["release_id"],
        "source_manifest_sha256": parsed_manifest_sha256,
        "release_source_reference": manifest["source_reference"],
        "terms_status": TERMS_STATUS,
        "terms_reference": manifest["terms_reference"],
        "gismo_commit": GISMO_COMMIT,
        "preprocess_sha256": manifest["source_files"]["preprocess.py"]["sha256"],
        "configuration_sha256": manifest["source_files"]["recipe1m.yaml"]["sha256"],
        "source_files": {name: {"sha256": record["sha256"], "byte_count": record["byte_count"]}
                         for name, record in sorted(manifest["source_files"].items())},
        "index_domain": "split_local_post_filter_zero_based_position",
        "source_split_order": list(SPLITS),
        "source_to_gismo_split": GISMO_SPLITS,
        "index_files": index_summary,
        "split_counts": {
            split: {"retained_recipe_count": counts[split],
                    "output_record_count": counts[split],
                    "output_sha256": hashlib.sha256(data_files[f"{split}.jsonl"]).hexdigest()}
            for split in SPLITS
        },
        "validation_index_relation": {
            "full": "val_all", "subset": "val",
            "subset_of_full": True, "subset_count": len(indices["val"]),
            "full_count": len(indices["val_all"]),
        },
        "raw_recipe_ids_in_manifest": False,
        "recipe_text_or_ingredients_in_output": False,
        "pickle_output_loaded": False,
        "execution_profile": execution_profile,
        "private_kaggle_source_mount_used": execution_profile == "kaggle-private",
        "kaggle_preflight_attestation_sha256": (
            sha256_file(kaggle_attestation) if execution_profile == "kaggle-private" else None
        ),
        "kaggle_code_package_manifest_sha256": (
            package_manifest_sha256 if execution_profile == "kaggle-private" else None
        ),
        "state": "PRIVATE_REAL_ID_ORDER_EXPORT_NOT_C3_ACCEPTANCE",
    }
    data_files["manifest.json"] = canonical_json(summary) + b"\n"
    # Recheck the publication boundary immediately before atomic promotion.
    if execution_profile == "windows-owner-only":
        _require_private_local_path(source_root)
        _require_private_local_path(manifest_path)
        _require_private_local_path(output_parent)
    else:
        if (admitted_attestation is None or admitted_attestation_path is None
                or admitted_attestation_sha256 is None):
            raise ExportError("Kaggle publication requires this invocation's fresh admission")
        _verify_admitted_kaggle_attestation(
            kaggle_attestation, expected_kaggle_attestation_sha256,
            owner=expected_kaggle_owner, dataset=expected_kaggle_dataset,
            kernel=expected_kaggle_kernel, admitted_path=admitted_attestation_path,
            admitted_sha256=admitted_attestation_sha256, admitted_value=admitted_attestation,
            source_manifest_sha256=parsed_manifest_sha256,
            package_manifest_sha256=package_manifest_sha256,
        )
        if sha256_file(kaggle_package_manifest) != package_manifest_sha256:
            raise ExportError("Kaggle code package manifest changed during source processing")
        for path in (source_root, manifest_path, output_parent):
            _require_private_local_path(path, execution_profile=execution_profile,
                                        dataset_root=kaggle_dataset_root, working_root=kaggle_working_root)
    if sha256_file(manifest_path) != parsed_manifest_sha256:
        raise ExportError("source manifest changed after it was parsed")
    if execution_profile == "kaggle-private":
        _check_packaged_gismo(gismo_root, kaggle_package_manifest, package_manifest_sha256)
    for name, record in manifest["source_files"].items():
        root = gismo_root if name in {"preprocess.py", "recipe1m.yaml", *INDEX_FILES.values()} else source_root
        _verify_source_file(root, name, record)
    if execution_profile == "kaggle-private":
        _check_export_elapsed(export_started_monotonic)
    _write_atomic_new_directory(output_dir, data_files, execution_profile=execution_profile,
                                dataset_root=kaggle_dataset_root, working_root=kaggle_working_root)
    return {"status": "PRIVATE_REAL_ORDER_EXPORTED", "output_dir": str(output_dir),
            "manifest_sha256": hashlib.sha256(data_files["manifest.json"]).hexdigest(),
            "split_counts": counts, "index_counts": {name: len(values) for name, values in indices.items()}}


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--gismo-root", type=Path, required=True)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--worktree", type=Path, required=True)
    parser.add_argument("--allow-private-real-inputs", action="store_true")
    parser.add_argument("--execution-profile", choices=("windows-owner-only", "kaggle-private"),
                        default="windows-owner-only")
    parser.add_argument("--kaggle-dataset-root", type=Path)
    parser.add_argument("--kaggle-working-root", type=Path)
    parser.add_argument("--kaggle-attestation", type=Path)
    parser.add_argument("--kaggle-package-manifest", type=Path)
    parser.add_argument("--expected-kaggle-attestation-sha256")
    parser.add_argument("--expected-kaggle-owner")
    parser.add_argument("--expected-kaggle-dataset")
    parser.add_argument("--expected-kaggle-kernel")
    args = parser.parse_args(argv)
    try:
        result = export_order(source_root=args.source_root, gismo_root=args.gismo_root,
                              output_dir=args.output_dir, manifest_path=args.manifest,
                              worktree=args.worktree,
                              allow_private_real_inputs=args.allow_private_real_inputs,
                              execution_profile=args.execution_profile,
                              kaggle_dataset_root=args.kaggle_dataset_root,
                              kaggle_working_root=args.kaggle_working_root,
                              kaggle_attestation=args.kaggle_attestation,
                              kaggle_package_manifest=args.kaggle_package_manifest,
                              expected_kaggle_attestation_sha256=args.expected_kaggle_attestation_sha256,
                              expected_kaggle_owner=args.expected_kaggle_owner,
                              expected_kaggle_dataset=args.expected_kaggle_dataset,
                              expected_kaggle_kernel=args.expected_kaggle_kernel)
    except (ExportError, OSError) as exc:
        print(json.dumps({"status": "REFUSED", "reason": str(exc)}, sort_keys=True))
        return 2
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
