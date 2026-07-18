import argparse
import json
import pathlib
import sys
from typing import Any


ROOT = pathlib.Path(__file__).resolve().parents[1]
VERSIONS_INDEX_PATH = ROOT / "versions.json"
VERSIONS_DIR = ROOT / "versions"
LOADERS = ("fabric", "neoforge")
VERSION_FIELDS = (
    "minecraft_version",
    "minecraft_version_range",
    "java_version",
)
LOADER_FIELDS = {
    "fabric": (
        "fabric_loader_version",
        "fabric_api_version",
        "fabric_permissions_api_version",
    ),
    "neoforge": ("neoforge_version", "neoforge_loader_version_range"),
}


def parse_properties(path: pathlib.Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        props[key.strip()] = value.strip()
    return props


def parse_boolean(props: dict[str, str], key: str, version_id: str) -> bool:
    value = props.get(key)
    if value not in ("true", "false"):
        raise ValueError(
            f"version '{version_id}' has invalid boolean field '{key}'; "
            "expected 'true' or 'false'"
        )
    return value == "true"


def load_manifest() -> dict[str, Any]:
    root = json.loads(VERSIONS_INDEX_PATH.read_text(encoding="utf-8"))
    versions: list[dict[str, Any]] = []
    for version_id in root["versions"]:
        props_path = VERSIONS_DIR / version_id / "gradle.properties"
        props = parse_properties(props_path)
        versions.append(
            {
                "id": version_id,
                "minecraft_version": props.get("minecraft_version", ""),
                "minecraft_version_range": props.get("minecraft_version_range", ""),
                "java_version": int(props.get("java_version", "0")),
                "neo_form_version": props.get("neo_form_version", ""),
                "parchment_minecraft": props.get("parchment_minecraft", ""),
                "parchment_version": props.get("parchment_version", ""),
                "fabric": {
                    "enabled": parse_boolean(props, "enable_fabric", version_id),
                    "fabric_loader_version": props.get("fabric_loader_version", ""),
                    "fabric_api_version": props.get("fabric_api_version", ""),
                    "fabric_permissions_api_version": props.get(
                        "fabric_permissions_api_version", ""
                    ),
                },
                "neoforge": {
                    "enabled": parse_boolean(props, "enable_neoforge", version_id),
                    "neoforge_version": props.get("neoforge_version", ""),
                    "neoforge_loader_version_range": props.get("neoforge_loader_version_range", ""),
                },
            }
        )
    return {"versions": versions}


def validate_manifest(manifest: dict[str, Any]) -> None:
    versions = manifest.get("versions")
    if not isinstance(versions, list) or not versions:
        raise ValueError("versions.json must contain a non-empty 'versions' list")

    seen_ids: set[str] = set()
    for version in versions:
        for field in VERSION_FIELDS:
            value = version.get(field)
            if value in (None, ""):
                raise ValueError(f"version entry is missing required field '{field}'")

        expected_java_version = 25 if str(version["minecraft_version"]).startswith("26.") else 21
        if version["java_version"] != expected_java_version:
            raise ValueError(
                f"version '{version['id']}' must use java_version={expected_java_version} "
                f"for minecraft_version={version['minecraft_version']}"
            )

        version_id = version["id"]
        if version_id in seen_ids:
            raise ValueError(f"duplicate version id '{version_id}'")
        seen_ids.add(version_id)

        for loader in LOADERS:
            loader_config = version.get(loader, {})
            enabled = bool(loader_config.get("enabled", False))
            if enabled:
                for field in LOADER_FIELDS[loader]:
                    if loader_config.get(field) in (None, ""):
                        raise ValueError(
                            f"enabled loader '{loader}' in version '{version_id}' is missing '{field}'"
                        )


def iter_matrix_rows(
    manifest: dict[str, Any],
    loader_filter: str | None = None,
    version_id_filter: str | None = None,
    java_version_filter: int | None = None,
) -> list[dict]:
    rows: list[dict] = []
    matched_version = version_id_filter is None
    for version in manifest["versions"]:
        if version_id_filter and version["id"] != version_id_filter:
            continue
        matched_version = True
        if java_version_filter is not None and version["java_version"] != java_version_filter:
            continue

        for loader in LOADERS:
            if loader_filter and loader != loader_filter:
                continue
            if not version.get(loader, {}).get("enabled", False):
                continue

            rows.append(
                {
                    "version_id": version["id"],
                    "minecraft_version": version["minecraft_version"],
                    "java_version": version["java_version"],
                    "loader": loader,
                    "gradle_task": f":{version['id']}:{loader}:build",
                    "gametest_task": (
                        f":{version['id']}:{loader}:runGameTest"
                        if loader == "fabric"
                        else f":{version['id']}:{loader}:runGameTestServer"
                    ),
                }
            )
    if not matched_version:
        raise ValueError(f"Unknown version id '{version_id_filter}'")
    return rows


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("validate")

    matrix_parser = subparsers.add_parser("matrix")
    matrix_parser.add_argument("--loader", choices=LOADERS)
    matrix_parser.add_argument("--version-id")
    matrix_parser.add_argument("--java-version", type=int, choices=(21, 25))
    return parser.parse_args()


def main() -> int:
    try:
        args = parse_args()
        manifest = load_manifest()
        validate_manifest(manifest)

        if args.command == "validate":
            return 0

        payload = {
            "include": iter_matrix_rows(
                manifest,
                loader_filter=getattr(args, "loader", None),
                version_id_filter=getattr(args, "version_id", None),
                java_version_filter=getattr(args, "java_version", None),
            )
        }
        json.dump(payload, sys.stdout, indent=2)
        sys.stdout.write("\n")
        return 0
    except ValueError as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
