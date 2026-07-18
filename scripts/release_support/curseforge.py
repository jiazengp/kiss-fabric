from __future__ import annotations

import json
import os
import pathlib
from typing import Any

from .core import HttpClient, PublicationError, RecoveryJournal
from .metadata import curseforge_game_version_names, curseforge_relations


CURSEFORGE_API = "https://minecraft.curseforge.com/api"
class CurseForgePlatform:
    """CurseForge author API adapter for tag resolution and immutable uploads."""

    def __init__(
        self,
        client: HttpClient,
        project_id: str,
        token: str,
    ):
        self.client = client
        self.project_id = project_id
        self.base = os.environ.get("CURSEFORGE_API_BASE", CURSEFORGE_API).rstrip("/")
        self.headers = {"X-Api-Token": token, "User-Agent": "kissmod-release"}
        self._resolved_game_versions: dict[str, list[str]] = {}

    def preflight(self, plan: dict[str, Any]) -> None:
        # The author API token can validate upload tags, but cannot list existing project files.
        # Authors must manually remove conflicting versions before pushing a release tag.
        self._resolve_game_version_names(plan)

    @staticmethod
    def _normalized_name(value: str) -> str:
        return "".join(character.lower() for character in value if character.isalnum())

    def _resolve_game_version_names(self, plan: dict[str, Any]) -> None:
        _, tags = self.client.json(
            "GET", f"{self.base}/game/versions?cache=true", headers=self.headers
        )
        available = {
            self._normalized_name(tag["name"]): tag["name"]
            for tag in tags
            if isinstance(tag, dict) and isinstance(tag.get("name"), str)
        }
        resolved: dict[str, list[str]] = {}
        for row in plan["rows"]:
            requested = curseforge_game_version_names(row)
            required = requested[:2]
            missing = [
                name
                for name in required
                if self._normalized_name(name) not in available
            ]
            if missing:
                raise PublicationError(
                    f"CurseForge does not recognize tags for {row['external_version']}: "
                    + ", ".join(missing)
                )
            resolved[row["external_version"]] = [
                available[self._normalized_name(name)]
                for name in requested
                if self._normalized_name(name) in available
            ]
        self._resolved_game_versions = resolved

    def upload_row(
        self,
        plan: dict[str, Any],
        row: dict[str, Any],
        artifact: pathlib.Path,
        journal: RecoveryJournal,
    ) -> int:
        metadata: dict[str, Any] = {
            "changelog": plan["changelog"],
            "changelogType": "markdown",
            "displayName": row["external_version"],
            "gameVersionNames": self._resolved_game_versions.get(
                row["external_version"], curseforge_game_version_names(row)
            ),
            "releaseType": plan["release_type"],
        }
        relations = curseforge_relations(row)
        if relations:
            metadata["relations"] = {"projects": relations}
        _, _, response = self.client.request(
            "POST",
            f"{self.base}/projects/{self.project_id}/upload-file",
            headers=self.headers,
            form={"metadata": json.dumps(metadata)},
            files=[
                (
                    "file",
                    (artifact.name, artifact.read_bytes(), "application/java-archive"),
                )
            ],
            expected=(200, 201),
        )
        file_id = int(json.loads(response)["id"])
        journal.record(
            "curseforge", "upload", file_id=file_id, version=row["external_version"]
        )
        return file_id
