#!/usr/bin/env python3
"""Resolve a release tag into the manifest-backed KissMod publication plan."""

from __future__ import annotations

import argparse
import datetime
import json
import pathlib
import re
import sys
from dataclasses import dataclass
from typing import Any

from manifest_matrix import (
    ROOT,
    iter_matrix_rows,
    load_manifest,
    parse_properties,
    validate_manifest,
)
from release_support.metadata import dependencies_for_loader, load_release_metadata


TAG_PATTERN = re.compile(
    r"^v(?P<version>\d+\.\d+\.\d+(?:-(?P<channel>alpha|beta)\.(?P<sequence>\d+))?)"
    r"(?:\+mc(?P<minecraft>(?:\d+\.)+\d+|(?:\d+\.)+X)(?:-(?P<loader>fabric|neoforge))?)?$"
)
CHANGELOG_HEADING = re.compile(
    r"^## \[(?P<version>[^]]+)] - (?P<date>\d{4}-\d{2}-\d{2})\s*$"
)


@dataclass(frozen=True)
class ParsedTag:
    tag: str
    mod_version: str
    release_type: str
    prerelease: bool
    minecraft_selector: str | None
    loader: str | None


def parse_tag(tag: str) -> ParsedTag:
    match = TAG_PATTERN.fullmatch(tag)
    if not match:
        raise ValueError(
            "Invalid release tag. Expected vX.Y.Z[-alpha.N|-beta.N]"
            "[+mc<exact|family.X>[-fabric|-neoforge]]"
        )
    channel = match.group("channel")
    return ParsedTag(
        tag=tag,
        mod_version=match.group("version"),
        release_type=channel or "release",
        prerelease=channel is not None,
        minecraft_selector=match.group("minecraft"),
        loader=match.group("loader"),
    )


def matches_minecraft(version: str, selector: str | None) -> bool:
    if selector is None:
        return True
    if selector.endswith(".X"):
        return version.startswith(selector[:-1])
    return version == selector


def extract_changelog(path: pathlib.Path, version: str) -> tuple[str, str]:
    if not path.is_file():
        raise ValueError(f"Changelog file does not exist: {path}")
    lines = path.read_text(encoding="utf-8").splitlines()
    sections: list[tuple[int, str]] = []
    for index, line in enumerate(lines):
        match = CHANGELOG_HEADING.fullmatch(line)
        if match and match.group("version") == version:
            sections.append((index, match.group("date")))
    if not sections:
        raise ValueError(f"CHANGELOG.md has no release section for [{version}]")
    if len(sections) > 1:
        raise ValueError(f"CHANGELOG.md has duplicate release sections for [{version}]")
    heading_index, release_date = sections[0]
    try:
        datetime.date.fromisoformat(release_date)
    except ValueError as exc:
        raise ValueError(
            f"CHANGELOG.md release section [{version}] has an invalid calendar date"
        ) from exc
    start = heading_index + 1
    end = next(
        (
            index
            for index in range(start, len(lines))
            if CHANGELOG_HEADING.fullmatch(lines[index])
        ),
        len(lines),
    )
    body = "\n".join(lines[start:end]).strip()
    if not body or not any(
        line.strip() and not line.lstrip().startswith("#") for line in body.splitlines()
    ):
        raise ValueError(f"CHANGELOG.md release section [{version}] is empty")
    return release_date, body


def load_project_properties() -> dict[str, str]:
    return parse_properties(ROOT / "gradle.properties")


def create_release_plan(
    tag: str, changelog_path: pathlib.Path | None = None
) -> dict[str, Any]:
    parsed = parse_tag(tag)
    project = load_project_properties()
    configured_version = project.get("version", "")
    if configured_version != parsed.mod_version:
        raise ValueError(
            f"Tag version '{parsed.mod_version}' does not match gradle.properties version "
            f"'{configured_version}'"
        )

    changelog_path = changelog_path or ROOT / "CHANGELOG.md"
    release_date, changelog = extract_changelog(changelog_path, parsed.mod_version)
    release_metadata = load_release_metadata(ROOT / "release-metadata.json")

    manifest = load_manifest()
    validate_manifest(manifest)
    rows = [
        row
        for row in iter_matrix_rows(manifest, loader_filter=parsed.loader)
        if matches_minecraft(row["minecraft_version"], parsed.minecraft_selector)
    ]
    if not rows:
        scope = parsed.minecraft_selector or "all supported Minecraft versions"
        raise ValueError(f"Release tag selects no enabled manifest rows for '{scope}'")

    mod_id = project.get("mod_id", "kissmod")
    enriched_rows: list[dict[str, Any]] = []
    for row in rows:
        minecraft_version = row["minecraft_version"]
        loader = row["loader"]
        external_version = f"{parsed.mod_version}+mc{minecraft_version}-{loader}"
        enriched_rows.append(
            {
                **row,
                "external_version": external_version,
                "artifact_name": f"{mod_id}-{external_version}.jar",
                "release_type": parsed.release_type,
                "environment": release_metadata["environment"],
                "dependencies": dependencies_for_loader(release_metadata, loader),
            }
        )

    return {
        "schema_version": 1,
        "tag": parsed.tag,
        "mod_version": parsed.mod_version,
        "release_type": parsed.release_type,
        "prerelease": parsed.prerelease,
        "minecraft_selector": parsed.minecraft_selector,
        "loader_selector": parsed.loader,
        "release_date": release_date,
        "changelog": changelog,
        "rows": enriched_rows,
        "matrix": {"include": enriched_rows},
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", required=True)
    parser.add_argument("--changelog", type=pathlib.Path)
    parser.add_argument("--output", type=pathlib.Path)
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Resolve and print the plan without calling any publication API (the default behavior).",
    )
    return parser.parse_args()


def main() -> int:
    try:
        args = parse_args()
        plan = create_release_plan(args.tag, args.changelog)
        rendered = json.dumps(plan, ensure_ascii=False, indent=2) + "\n"
        if args.output:
            args.output.parent.mkdir(parents=True, exist_ok=True)
            args.output.write_text(rendered, encoding="utf-8")
        sys.stdout.write(rendered)
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
