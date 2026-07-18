from __future__ import annotations

import json
import pathlib
from typing import Any


DEPENDENCY_TYPES = {
    "required",
    "recommended",
    "optional",
    "embedded",
    "conflicting",
    "incompatible",
}
LOADERS = {"fabric", "neoforge"}
ENVIRONMENTS = {"client", "server", "client_and_server"}

MODRINTH_DEPENDENCY_TYPES = {
    "required": "required",
    "recommended": "optional",
    "optional": "optional",
    "embedded": "embedded",
    "conflicting": "incompatible",
    "incompatible": "incompatible",
}
CURSEFORGE_DEPENDENCY_TYPES = {
    "required": "requiredDependency",
    "recommended": "optionalDependency",
    "optional": "optionalDependency",
    "embedded": "embeddedLibrary",
    "conflicting": "incompatible",
    "incompatible": "incompatible",
}
MODRINTH_ENVIRONMENTS = {
    "client": "client_only",
    "server": "server_only",
    "client_and_server": "client_and_server",
}
CURSEFORGE_ENVIRONMENTS = {
    "client": "Client",
    "server": "Server",
    "client_and_server": None,
}


def load_release_metadata(path: pathlib.Path) -> dict[str, Any]:
    try:
        raw = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise ValueError(f"Invalid release metadata JSON: {exc}") from exc
    if raw.get("schema_version") != 1:
        raise ValueError("release-metadata.json schema_version must be 1")
    environment = raw.get("environment")
    if environment not in ENVIRONMENTS:
        raise ValueError(
            "release-metadata.json environment must be client, server, or client_and_server"
        )
    dependencies = raw.get("dependencies")
    if not isinstance(dependencies, list):
        raise ValueError("release-metadata.json dependencies must be an array")

    normalized: list[dict[str, Any]] = []
    occupied: set[tuple[str, str]] = set()
    for index, dependency in enumerate(dependencies):
        label = f"release-metadata.json dependencies[{index}]"
        if not isinstance(dependency, dict):
            raise ValueError(f"{label} must be an object")
        dependency_id = dependency.get("id")
        dependency_type = dependency.get("type")
        loaders = dependency.get("loaders")
        aliases = dependency.get("aliases")
        if not isinstance(dependency_id, str) or not dependency_id.strip():
            raise ValueError(f"{label}.id must be a non-empty string")
        if dependency_type not in DEPENDENCY_TYPES:
            raise ValueError(f"{label}.type is unsupported: {dependency_type}")
        if (
            not isinstance(loaders, list)
            or not loaders
            or any(loader not in LOADERS for loader in loaders)
            or len(loaders) != len(set(loaders))
        ):
            raise ValueError(f"{label}.loaders must contain unique supported loaders")
        if not isinstance(aliases, dict) or any(
            not isinstance(aliases.get(platform), str) or not aliases[platform].strip()
            for platform in ("modrinth", "curseforge")
        ):
            raise ValueError(
                f"{label}.aliases must define non-empty modrinth and curseforge IDs"
            )
        for loader in loaders:
            key = (loader, dependency_id)
            if key in occupied:
                raise ValueError(f"Duplicate dependency {dependency_id!r} for {loader}")
            occupied.add(key)
        normalized.append(
            {
                "id": dependency_id,
                "type": dependency_type,
                "loaders": list(loaders),
                "aliases": {
                    "modrinth": aliases["modrinth"],
                    "curseforge": aliases["curseforge"],
                },
            }
        )
    return {"environment": environment, "dependencies": normalized}


def dependencies_for_loader(
    metadata: dict[str, Any], loader: str
) -> list[dict[str, Any]]:
    return [
        {
            "id": dependency["id"],
            "type": dependency["type"],
            "aliases": dependency["aliases"],
        }
        for dependency in metadata["dependencies"]
        if loader in dependency["loaders"]
    ]


def modrinth_dependencies(row: dict[str, Any]) -> list[dict[str, str]]:
    return [
        {
            "project_id": dependency["aliases"]["modrinth"],
            "dependency_type": MODRINTH_DEPENDENCY_TYPES[dependency["type"]],
        }
        for dependency in row.get("dependencies", [])
    ]


def curseforge_relations(row: dict[str, Any]) -> list[dict[str, str]]:
    return [
        {
            "slug": dependency["aliases"]["curseforge"],
            "type": CURSEFORGE_DEPENDENCY_TYPES[dependency["type"]],
        }
        for dependency in row.get("dependencies", [])
    ]


def modrinth_environment(row: dict[str, Any]) -> str:
    return MODRINTH_ENVIRONMENTS[row["environment"]]


def curseforge_environment(row: dict[str, Any]) -> str | None:
    return CURSEFORGE_ENVIRONMENTS[row["environment"]]


def curseforge_game_version_names(row: dict[str, Any]) -> list[str]:
    names = [
        row["minecraft_version"],
        "Fabric" if row["loader"] == "fabric" else "NeoForge",
        f"Java {row['java_version']}",
    ]
    environment = curseforge_environment(row)
    if environment:
        names.append(environment)
    return names
