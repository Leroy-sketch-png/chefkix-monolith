#!/usr/bin/env python3
"""Fail-closed, source-free bootstrap for the pinned T535 Kaggle CPU package.

This is preparation tooling, not permission to upload source or execute GISMo.
It verifies an operator-supplied package digest, the exact package inventory,
and the bundled runtime archive before relocation. Real-source execution still
requires the independent live preflight and signed task attestation.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import re
import shutil
import stat
import subprocess
import sys
import tarfile
import tempfile
from pathlib import Path, PurePosixPath
from typing import Dict, Iterable, List, Optional, Tuple


SCHEMA = "chefkix.t535.kaggle-source-free-package"
PACKAGE_VERSION = 10
JPEG_SOURCE_URL = "http://www.ijg.org/files/jpegsrc.v9b.tar.gz"
PACKAGE_MANIFEST = "package-manifest.json"
PREFLIGHT_ATTESTATION_MAX_AGE = dt.timedelta(minutes=10)
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
OWNER_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}$")
SLUG_RE = re.compile(r"^[A-Za-z0-9_-]{1,64}/[A-Za-z0-9_-]{1,64}$")
SOURCE_DERIVED_SPLIT_PATHS = {
    "gismo/data/splits/recipe1m/train.txt",
    "gismo/data/splits/recipe1m/val_all.txt",
    "gismo/data/splits/recipe1m/val.txt",
    "gismo/data/splits/recipe1m/test.txt",
}
RUNTIME_RESOLUTION_RECORD = {
    "kind": "hash-pinned-prebuilt-environment-archive",
    "conda_metadata_embedded_in_archive": True,
    "platform_spec_is_declaration_not_a_recreation_lock": True,
    "windows_source_environment_lockfiles_packaged": False,
}
THIRD_PARTY_LICENSES = [
    {
        "name": "GISMo", "identifier": "CC-BY-NC-4.0", "path": "gismo/LICENSE.md",
        "upstream_commit": "f5d2394c7ade897987ce584d2a75d8bbbbae058e",
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


class BootstrapError(RuntimeError):
    pass


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _safe_rel(raw: str) -> PurePosixPath:
    if not isinstance(raw, str) or not raw or "\\" in raw or "\x00" in raw:
        raise BootstrapError("archive/package path is malformed")
    path = PurePosixPath(raw)
    if path.is_absolute() or any(part in ("", ".", "..") for part in path.parts):
        raise BootstrapError("archive/package path escapes its root")
    return path


def _archive_rel(raw: str) -> PurePosixPath:
    if raw.startswith("./"):
        raw = raw[2:]
    return _safe_rel(raw)


def _no_symlink_components(path: Path, boundary: Path) -> None:
    boundary = boundary.resolve(strict=True)
    try:
        rel = path.absolute().relative_to(boundary)
    except ValueError as exc:
        raise BootstrapError("path is outside its declared root") from exc
    current = boundary
    if stat.S_ISLNK(current.lstat().st_mode):
        raise BootstrapError("symlinked root is not allowed")
    for part in rel.parts:
        current = current / part
        try:
            mode = current.lstat().st_mode
        except OSError as exc:
            raise BootstrapError("required path is missing") from exc
        if stat.S_ISLNK(mode):
            raise BootstrapError("symlinked path is not allowed")


def _manifest(package_root: Path, expected_sha256: str,
              kaggle_input_root: Path = Path("/kaggle/input")) -> Tuple[dict, dict]:
    if not SHA256_RE.fullmatch(expected_sha256 or ""):
        raise BootstrapError("expected package-manifest SHA-256 is invalid")
    package_root = package_root.resolve(strict=True)
    kaggle_input_root = kaggle_input_root.resolve(strict=True)
    try:
        package_root.relative_to(kaggle_input_root)
    except ValueError as exc:
        raise BootstrapError("package must be mounted read-only below /kaggle/input") from exc
    if not package_root.is_dir() or package_root == kaggle_input_root:
        raise BootstrapError("package must be mounted read-only below /kaggle/input")
    manifest_path = package_root / PACKAGE_MANIFEST
    _no_symlink_components(manifest_path, package_root)
    raw = manifest_path.read_bytes()
    if hashlib.sha256(raw).hexdigest() != expected_sha256:
        raise BootstrapError("package manifest does not match the pinned digest")
    try:
        manifest = json.loads(raw.decode("utf-8", errors="strict"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise BootstrapError("package manifest is not valid UTF-8 JSON") from exc
    if (type(manifest) is not dict or manifest.get("schema") != SCHEMA
            or type(manifest.get("version")) is not int or manifest.get("version") != PACKAGE_VERSION
            or manifest.get("source_data") != {
                "recipe1m_release_files_included": False,
                "gismo_source_derived_split_indices_included": True,
                "gismo_source_derived_split_index_paths": [
                    "gismo/data/splits/recipe1m/train.txt",
                    "gismo/data/splits/recipe1m/val_all.txt",
                    "gismo/data/splits/recipe1m/val.txt",
                    "gismo/data/splits/recipe1m/test.txt",
                ],
                "recipe1m_release_mount_required_later": True,
            }
            or manifest.get("credentials_included") is not False):
        raise BootstrapError("package manifest has an unsupported schema or boundary")
    entries = manifest.get("files")
    if type(entries) is not list:
        raise BootstrapError("package manifest has no exact file inventory")
    inventory = {}
    for entry in entries:
        if (type(entry) is not dict or set(entry) != {"path", "size_bytes", "sha256"}
                or type(entry.get("size_bytes")) is not int or entry["size_bytes"] < 0
                or not SHA256_RE.fullmatch(entry.get("sha256", ""))):
            raise BootstrapError("package file entry is malformed")
        rel = _safe_rel(entry["path"]).as_posix()
        if rel in inventory:
            raise BootstrapError("package inventory contains duplicate paths")
        inventory[rel] = entry
    if not inventory:
        raise BootstrapError("package inventory is empty")
    if not SOURCE_DERIVED_SPLIT_PATHS.issubset(inventory):
        raise BootstrapError("package source-data metadata does not match its split-index inventory")

    actual = set()
    for path in package_root.rglob("*"):
        if path.is_symlink():
            raise BootstrapError("mounted package contains a symlink")
        if path.is_file():
            actual.add(path.relative_to(package_root).as_posix())
    if actual != set(inventory) | {PACKAGE_MANIFEST}:
        raise BootstrapError("mounted package inventory differs from its manifest")
    for rel, entry in inventory.items():
        path = package_root.joinpath(*PurePosixPath(rel).parts)
        _no_symlink_components(path, package_root)
        if not path.is_file() or path.stat().st_size != entry["size_bytes"]:
            raise BootstrapError("a mounted package file has the wrong type or size")
        if sha256_file(path) != entry["sha256"]:
            raise BootstrapError("a mounted package file hash does not match")
    runtime = manifest.get("runtime")
    archive = runtime.get("archive") if type(runtime) is dict else None
    platform_spec = runtime.get("platform_spec") if type(runtime) is dict else None
    if (type(runtime) is not dict
            or set(runtime) != {"archive", "platform_spec", "resolution_record", "provenance"}
            or runtime.get("resolution_record") != RUNTIME_RESOLUTION_RECORD
            or type(runtime.get("provenance")) is not str
            or type(archive) is not dict
            or set(archive) != {"path", "sha256"}
            or archive.get("path") != "runtime/kaggle-linux-runtime-v6.tar.gz"
            or archive.get("path") not in inventory
            or archive.get("sha256") != inventory[archive["path"]]["sha256"]
            or type(platform_spec) is not dict
            or set(platform_spec) != {"path", "sha256"}
            or platform_spec.get("path") != "runtime/t535-kaggle-runtime-linux.yml"
            or platform_spec.get("path") not in inventory
            or platform_spec.get("sha256") != inventory[platform_spec["path"]]["sha256"]):
        raise BootstrapError("runtime archive is not bound by the package manifest")
    exporter = manifest.get("exporter")
    if (type(exporter) is not dict or exporter.get("path") != "code/export_c3_recipe1m_order.py"
            or exporter.get("path") not in inventory
            or exporter.get("sha256") != inventory[exporter["path"]]["sha256"]):
        raise BootstrapError("pinned exporter is absent or inconsistently bound")
    bootstrap = manifest.get("bootstrap")
    if (type(bootstrap) is not dict or bootstrap.get("path") != "code/bootstrap_t535_kaggle.py"
            or bootstrap.get("path") not in inventory
            or bootstrap.get("sha256") != inventory[bootstrap["path"]]["sha256"]):
        raise BootstrapError("pinned bootstrap is absent or inconsistently bound")
    if not any(path.startswith("gismo/") for path in inventory):
        raise BootstrapError("pinned GISMo files are absent")
    if (manifest.get("third_party_licenses") != THIRD_PARTY_LICENSES
            or "gismo/LICENSE.md" not in inventory
            or "licenses/intel-mkl-2020.2-license-april-2018.txt" not in inventory
            or inventory["licenses/intel-mkl-2020.2-license-april-2018.txt"]["sha256"]
            != THIRD_PARTY_LICENSES[1]["notice_sha256"]
            or "licenses/intel-openmp-2025.0.0-license-october-2022.txt" not in inventory
            or inventory["licenses/intel-openmp-2025.0.0-license-october-2022.txt"]["sha256"]
            != THIRD_PARTY_LICENSES[2]["notice_sha256"]):
        raise BootstrapError("package does not preserve exact third-party license notices")
    return manifest, inventory


def _phase(path: Path, phase: str, state: str, code: str) -> None:
    if not path.exists():
        mode = "xb"
    else:
        mode = "ab"
    record = {
        "schema": "chefkix.t535.bootstrap-phase.v1",
        "time_utc": dt.datetime.utcnow().replace(microsecond=0).isoformat() + "Z",
        "phase": phase, "state": state, "code": code,
    }
    with path.open(mode) as stream:
        stream.write((json.dumps(record, sort_keys=True) + "\n").encode("utf-8"))
        stream.flush()
        os.fsync(stream.fileno())


def verify_preflight_attestation(
    attestation_path: Path, expected_attestation_sha256: str,
    source_manifest_path: Path, expected_source_mount: Path,
    package_manifest_sha256: str, exporter_sha256: str, gismo_commit: str,
    owner: str, dataset_slug: str, kernel_slug: str,
    kaggle_working_root: Path, kaggle_input_root: Path = Path("/kaggle/input"),
    now_utc: Optional[dt.datetime] = None,
) -> dict:
    """Bind a fresh observed private/offline CPU receipt before runtime unpacking."""
    if (not SHA256_RE.fullmatch(expected_attestation_sha256 or "")
            or not SHA256_RE.fullmatch(package_manifest_sha256 or "")
            or not SHA256_RE.fullmatch(exporter_sha256 or "")):
        raise BootstrapError("preflight digest pin is malformed")
    if (not OWNER_RE.fullmatch(owner or "") or not SLUG_RE.fullmatch(dataset_slug or "")
            or not SLUG_RE.fullmatch(kernel_slug or "")):
        raise BootstrapError("preflight identity pin is malformed")
    working = kaggle_working_root.resolve(strict=True)
    attestation = attestation_path.resolve(strict=True)
    source_manifest = source_manifest_path.resolve(strict=True)
    source_mount = expected_source_mount.resolve(strict=True)
    input_root = kaggle_input_root.resolve(strict=True)
    try:
        attestation.relative_to(working)
        source_manifest.relative_to(source_mount)
        source_mount.relative_to(input_root)
    except ValueError as exc:
        raise BootstrapError("attestation/source manifest is outside its pinned Kaggle mount") from exc
    raw = attestation.read_bytes()
    if len(raw) > 65536 or hashlib.sha256(raw).hexdigest() != expected_attestation_sha256:
        raise BootstrapError("preflight attestation size or SHA-256 mismatch")
    try:
        value = json.loads(raw.decode("utf-8", errors="strict"))
    except (UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise BootstrapError("preflight attestation is invalid UTF-8 JSON") from exc
    required = {
        "schema_version", "observed_at_utc", "owner", "dataset_slug", "kernel_slug",
        "dataset_private", "kernel_private", "dataset_collaborators", "kernel_collaborators",
        "gpu_enabled", "internet_enabled", "source_manifest_sha256", "gismo_commit",
        "exporter_sha256", "package_manifest_sha256",
    }
    if (type(value) is not dict or set(value) != required
            or type(value.get("schema_version")) is not int or value["schema_version"] != 1):
        raise BootstrapError("preflight attestation schema mismatch")
    if (value["owner"], value["dataset_slug"], value["kernel_slug"]) != (owner, dataset_slug, kernel_slug):
        raise BootstrapError("preflight identity mismatch")
    if (value["dataset_private"] is not True or value["kernel_private"] is not True
            or value["gpu_enabled"] is not False or value["internet_enabled"] is not False
            or value["dataset_collaborators"] != [] or value["kernel_collaborators"] != []):
        raise BootstrapError("preflight does not attest private, zero-collaborator, offline CPU settings")
    try:
        observed = dt.datetime.fromisoformat(value["observed_at_utc"].replace("Z", "+00:00"))
    except (AttributeError, TypeError, ValueError):
        raise BootstrapError("preflight observation time is invalid") from None
    if observed.tzinfo is None or observed.utcoffset() != dt.timedelta(0):
        raise BootstrapError("preflight observation time must be UTC")
    now = now_utc if now_utc is not None else dt.datetime.now(dt.timezone.utc)
    if now.tzinfo is None or now.utcoffset() != dt.timedelta(0):
        raise BootstrapError("current preflight clock must be UTC")
    age = now - observed
    if age < dt.timedelta(0):
        raise BootstrapError("preflight observation time is in the future")
    if age > PREFLIGHT_ATTESTATION_MAX_AGE:
        raise BootstrapError("preflight attestation is stale")
    source_sha = sha256_file(source_manifest)
    if (value["source_manifest_sha256"] != source_sha
            or value["package_manifest_sha256"] != package_manifest_sha256
            or value["exporter_sha256"] != exporter_sha256
            or value["gismo_commit"] != gismo_commit):
        raise BootstrapError("preflight attestation is bound to different inputs")
    if not SHA256_RE.fullmatch(value.get("source_manifest_sha256", "")):
        raise BootstrapError("preflight source-manifest digest is malformed")
    return value


def _link_target(member_path: PurePosixPath, target: str, *, hardlink: bool = False) -> PurePosixPath:
    if not target or "\\" in target or "\x00" in target:
        raise BootstrapError("archive link target is malformed")
    raw = PurePosixPath(target)
    if raw.is_absolute():
        raise BootstrapError("absolute archive link target is refused")
    stack = [] if hardlink else list(member_path.parent.parts)
    for part in raw.parts:
        if part in ("", "."):
            continue
        if part == "..":
            if not stack:
                raise BootstrapError("archive link escapes extraction root")
            stack.pop()
        else:
            stack.append(part)
    if not stack:
        raise BootstrapError("archive link may not point at extraction root")
    return PurePosixPath(*stack)


def safe_extract_runtime(archive_path: Path, destination: Path, expected_sha256: str,
                         working_root: Path = Path("/kaggle/working")) -> Path:
    """Extract tar.gz without extractall; reject escapes, devices, duplicates."""
    if sha256_file(archive_path) != expected_sha256:
        raise BootstrapError("runtime archive hash does not match the package manifest")
    if destination.exists() or destination.is_symlink():
        raise BootstrapError("runtime destination must be new")
    working_root = working_root.resolve(strict=True)
    if not destination.parent.is_dir() or destination.parent.resolve() != working_root:
        raise BootstrapError("runtime destination must be a direct child of Kaggle working storage")
    staging = Path(tempfile.mkdtemp(prefix=".t535-runtime-", dir=str(destination.parent)))
    directories: List[Tuple[PurePosixPath, tarfile.TarInfo]] = []
    links: List[Tuple[PurePosixPath, tarfile.TarInfo, PurePosixPath]] = []
    seen = set()
    try:
        with tarfile.open(str(archive_path), mode="r:gz") as tar:
            members = tar.getmembers()
            for member in members:
                if member.name in ("", ".", "./"):
                    continue
                rel = _archive_rel(member.name)
                key = rel.as_posix()
                if key in seen:
                    raise BootstrapError("runtime archive contains duplicate paths")
                seen.add(key)
                if member.isdir():
                    directories.append((rel, member))
                elif member.issym() or member.islnk():
                    links.append((rel, member, _link_target(rel, member.linkname, hardlink=member.islnk())))
                elif not member.isfile():
                    raise BootstrapError("runtime archive contains a special file")

            for rel, member in sorted(directories, key=lambda item: len(item[0].parts)):
                path = staging.joinpath(*rel.parts)
                path.mkdir(parents=True, exist_ok=True)
                path.chmod(member.mode & 0o777)

            for member in members:
                if member.name in ("", ".", "./") or member.isdir() or member.issym() or member.islnk():
                    continue
                rel = _archive_rel(member.name)
                path = staging.joinpath(*rel.parts)
                path.parent.mkdir(parents=True, exist_ok=True)
                # Symlinks and hardlinks are deferred, so no archive member can
                # redirect a subsequent write through a link.
                _assert_no_link_ancestors(path.parent, staging)
                source = tar.extractfile(member)
                if source is None:
                    raise BootstrapError("runtime archive regular file could not be read")
                with source, path.open("xb") as output:
                    shutil.copyfileobj(source, output, length=1024 * 1024)
                path.chmod(member.mode & 0o777)

            for rel, member, target in links:
                link = staging.joinpath(*rel.parts)
                link.parent.mkdir(parents=True, exist_ok=True)
                _assert_no_link_ancestors(link.parent, staging)
                target_path = staging.joinpath(*target.parts)
                if member.issym():
                    # Preserve the link spelling after proving its normalized
                    # target is confined. It may resolve to a later entry.
                    os.symlink(member.linkname, str(link))
                else:
                    if not target_path.exists() or not target_path.is_file():
                        raise BootstrapError("runtime archive hardlink target is missing or not a file")
                    os.link(str(target_path), str(link))

        os.replace(str(staging), str(destination))
        return destination
    except Exception:
        shutil.rmtree(str(staging), ignore_errors=True)
        raise


def _assert_no_link_ancestors(path: Path, root: Path) -> None:
    current = root
    relative = path.relative_to(root)
    for part in relative.parts:
        current = current / part
        if current.is_symlink():
            raise BootstrapError("archive path traverses an extracted symlink")


def build_export_command(args: argparse.Namespace, package_root: Path, runtime_root: Path) -> List[str]:
    exporter = package_root / "code" / "export_c3_recipe1m_order.py"
    python = runtime_root / "bin" / "python"
    gismo = package_root / "gismo"
    output = Path(args.output_dir)
    code_root = package_root / "code"
    required = [exporter, python, gismo, Path(args.source_root), Path(args.source_manifest),
                Path(args.dataset_root), Path(args.attestation)]
    if not all(path.exists() for path in required):
        raise BootstrapError("a required pinned code, runtime, source, or attestation path is missing")
    kaggle_working = Path("/kaggle/working").resolve(strict=True)
    for path in (output.parent.resolve(strict=True), gismo.resolve(strict=True),
                 Path(args.attestation).resolve(strict=True)):
        try:
            path.relative_to(kaggle_working)
        except ValueError as exc:
            raise BootstrapError("output/code/attestation path is outside Kaggle working storage") from exc
    invocation_root = Path(args.invocation_root)
    invocation_root.mkdir(parents=True, exist_ok=True)
    command = [
        str(python), str(exporter),
        "--source-root", str(Path(args.source_root).resolve(strict=True)),
        "--gismo-root", str(gismo.resolve(strict=True)),
        "--manifest", str(Path(args.source_manifest).resolve(strict=True)),
        "--output-dir", str(output), "--worktree", str(invocation_root.resolve(strict=True)),
        "--allow-private-real-inputs", "--execution-profile", "kaggle-private",
        "--kaggle-dataset-root", str(Path(args.dataset_root).resolve(strict=True)),
        "--kaggle-working-root", str(kaggle_working),
        "--kaggle-attestation", str(Path(args.attestation).resolve(strict=True)),
        "--kaggle-package-manifest", str((package_root / PACKAGE_MANIFEST).resolve(strict=True)),
        "--expected-kaggle-attestation-sha256", args.attestation_sha256,
        "--expected-kaggle-owner", args.owner,
        "--expected-kaggle-dataset", args.dataset_slug,
        "--expected-kaggle-kernel", args.kernel_slug,
    ]
    return command


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--package-root", type=Path, required=True)
    parser.add_argument("--package-manifest-sha256", required=True)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--source-manifest", type=Path, required=True)
    parser.add_argument("--dataset-root", type=Path, required=True)
    parser.add_argument("--attestation", type=Path, required=True)
    parser.add_argument("--attestation-sha256", required=True)
    parser.add_argument("--owner", required=True)
    parser.add_argument("--dataset-slug", required=True)
    parser.add_argument("--kernel-slug", required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--invocation-root", type=Path, required=True)
    args = parser.parse_args(argv)

    phase_path = Path("/kaggle/working/t535-bootstrap-phases.jsonl")
    active_phase, phase_started = "startup", False
    try:
        if not Path("/kaggle/input").is_dir() or not Path("/kaggle/working").is_dir():
            raise BootstrapError("Kaggle input/working mounts are unavailable")
        if phase_path.exists():
            raise BootstrapError("a T535 bootstrap phase log already exists; do not overwrite or retry")
        _phase(phase_path, active_phase, "started", "BOOTSTRAP_START")
        phase_started = True
        active_phase = "package_verification"
        package_root = args.package_root.resolve(strict=True)
        manifest, inventory = _manifest(package_root, args.package_manifest_sha256)
        bootstrap_pin = manifest["bootstrap"]
        expected_bootstrap = (package_root / "code" / "bootstrap_t535_kaggle.py").resolve(strict=True)
        running_bootstrap = Path(__file__).resolve(strict=True)
        if (running_bootstrap != expected_bootstrap
                or sha256_file(running_bootstrap) != bootstrap_pin["sha256"]):
            raise BootstrapError("running bootstrap is not the pinned package copy")
        _phase(phase_path, active_phase, "complete", "PACKAGE_HASHES_VERIFIED")
        active_phase = "live_preflight_attestation"
        verify_preflight_attestation(
            args.attestation, args.attestation_sha256, args.source_manifest,
            args.dataset_root, args.package_manifest_sha256,
            manifest["exporter"]["sha256"], manifest["gismo_commit"], args.owner, args.dataset_slug,
            args.kernel_slug, Path("/kaggle/working"),
        )
        _phase(phase_path, active_phase, "complete", "PRIVATE_OFFLINE_CPU_ATTESTATION_VERIFIED")
        active_phase = "runtime_extraction"
        runtime_rel = manifest["runtime"]["archive"]["path"]
        runtime_archive = package_root.joinpath(*PurePosixPath(runtime_rel).parts)
        runtime_root = safe_extract_runtime(runtime_archive, Path("/kaggle/working/t535-runtime-v6"),
                                            manifest["runtime"]["archive"]["sha256"])
        _phase(phase_path, active_phase, "complete", "RUNTIME_HASH_AND_ARCHIVE_PATHS_VERIFIED")
        active_phase = "runtime_relocation"
        unpacker = runtime_root / "bin" / "conda-unpack"
        if not unpacker.is_file():
            raise BootstrapError("relocated runtime lacks its pinned conda-unpack entrypoint")
        environment = os.environ.copy()
        environment["PATH"] = str(runtime_root / "bin") + os.pathsep + environment.get("PATH", "")
        environment["PYTHONNOUSERSITE"] = "1"
        subprocess.run([str(unpacker)], check=True, env=environment, stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE, timeout=600)
        _phase(phase_path, active_phase, "complete", "CONDA_UNPACK_COMPLETE")
        active_phase = "export_invocation"
        command = build_export_command(args, package_root, runtime_root)
        _phase(phase_path, active_phase, "started", "PINNED_EXPORTER_START")
        result = subprocess.run(command, check=False, env=environment, stdout=subprocess.PIPE,
                                stderr=subprocess.PIPE, timeout=60 * 60 * 6)
        # Output may include paths and aggregate counts but is never uploaded by
        # this helper. Avoid returning recipe rows or source payloads to stdout.
        if result.returncode != 0:
            raise BootstrapError("pinned exporter stopped; inspect private phase log before any retry")
        _phase(phase_path, active_phase, "complete", "EXPORTER_RETURNED_SUCCESS")
        active_phase = "complete"
        _phase(phase_path, active_phase, "complete", "OUTPUT_REQUIRES_INDEPENDENT_VALIDATION")
        print(json.dumps({"status": "EXPORT_COMPLETED_REQUIRES_OUTPUT_VALIDATION",
                          "returncode": result.returncode,
                          "output_dir": str(args.output_dir)}, sort_keys=True))
        return 0
    except (BootstrapError, OSError, subprocess.SubprocessError, tarfile.TarError, KeyError, ValueError) as exc:
        if phase_started:
            try:
                _phase(phase_path, active_phase, "failed", type(exc).__name__.upper())
            except OSError:
                pass
        print(json.dumps({"status": "REFUSED_OR_FAILED", "reason": str(exc)}, sort_keys=True), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
