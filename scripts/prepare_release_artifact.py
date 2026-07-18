#!/usr/bin/env python3
"""Select, validate, rename, and checksum one deployable mod JAR."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import shutil
import sys
import zipfile


EXCLUDED_SUFFIXES = (
    "-sources.jar",
    "-javadoc.jar",
    "-dev.jar",
    "-shadow.jar",
    "-all.jar",
)


def select_deployable_jar(directory: pathlib.Path) -> pathlib.Path:
    candidates = sorted(
        path
        for path in directory.glob("*.jar")
        if path.is_file() and not path.name.endswith(EXCLUDED_SUFFIXES)
    )
    if len(candidates) != 1:
        names = ", ".join(path.name for path in candidates) or "none"
        raise ValueError(
            f"Expected exactly one deployable JAR in {directory}, found {len(candidates)}: {names}"
        )
    return candidates[0]


def validate_loader_metadata(path: pathlib.Path, loader: str) -> None:
    expected = (
        "fabric.mod.json" if loader == "fabric" else "META-INF/neoforge.mods.toml"
    )
    legacy = "META-INF/mods.toml"
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
    if expected not in names and not (loader == "neoforge" and legacy in names):
        raise ValueError(f"{path.name} does not contain {expected}")


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def prepare(
    source_dir: pathlib.Path,
    output_dir: pathlib.Path,
    artifact_name: str,
    loader: str,
) -> dict[str, str]:
    source = select_deployable_jar(source_dir)
    validate_loader_metadata(source, loader)
    output_dir.mkdir(parents=True, exist_ok=True)
    destination = output_dir / artifact_name
    shutil.copy2(source, destination)
    checksum = sha256(destination)
    checksum_path = destination.with_suffix(destination.suffix + ".sha256")
    checksum_path.write_text(f"{checksum}  {destination.name}\n", encoding="utf-8")
    return {
        "source": str(source),
        "artifact": str(destination),
        "artifact_name": destination.name,
        "sha256": checksum,
        "checksum": str(checksum_path),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-dir", type=pathlib.Path, required=True)
    parser.add_argument("--output-dir", type=pathlib.Path, required=True)
    parser.add_argument("--artifact-name", required=True)
    parser.add_argument("--loader", choices=("fabric", "neoforge"), required=True)
    args = parser.parse_args()
    try:
        result = prepare(
            args.source_dir, args.output_dir, args.artifact_name, args.loader
        )
        json.dump(result, sys.stdout, indent=2)
        sys.stdout.write("\n")
        return 0
    except (OSError, ValueError, zipfile.BadZipFile) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
