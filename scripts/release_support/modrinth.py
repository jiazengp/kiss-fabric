from __future__ import annotations

import json
import pathlib
import urllib.parse
from typing import Any

from .core import HttpClient, PublicationError, RecoveryJournal
from .metadata import modrinth_dependencies, modrinth_environment


MODRINTH_API = "https://api.modrinth.com/v2"


class ModrinthPlatform:
    """Modrinth version replacement and rollback adapter."""

    def __init__(
        self,
        client: HttpClient,
        project_id: str,
        token: str,
        backup_dir: pathlib.Path,
        repository: str,
    ):
        self.client = client
        self.project_id = project_id
        self.backup_dir = backup_dir / "modrinth"
        self.headers = {
            "Authorization": token,
            "User-Agent": f"KissModRelease/1.0 (https://github.com/{repository})",
        }

    def _versions(self) -> list[dict[str, Any]]:
        _, versions = self.client.json(
            "GET",
            f"{MODRINTH_API}/project/{self.project_id}/version",
            headers=self.headers,
        )
        return versions

    def _exact_version(self, version_number: str) -> dict[str, Any] | None:
        matches = [
            version
            for version in self._versions()
            if version["version_number"] == version_number
        ]
        if len(matches) > 1:
            raise PublicationError(
                f"Modrinth has duplicate version_number {version_number}"
            )
        return matches[0] if matches else None

    def preflight(self, plan: dict[str, Any]) -> None:
        self._validate_targets(plan)
        targets = {row["external_version"] for row in plan["rows"]}
        prefix = f"{plan['mod_version']}+mc"
        outside_scope = sorted(
            version["version_number"]
            for version in self._versions()
            if version["version_number"].startswith(prefix)
            and version["version_number"] not in targets
        )
        if outside_scope:
            raise PublicationError(
                f"Modrinth already contains {plan['mod_version']} rows outside this tag scope: "
                + ", ".join(outside_scope)
                + ". Bump the patch version instead of adding another scope."
            )

    def _validate_targets(self, plan: dict[str, Any]) -> None:
        _, game_version_tags = self.client.json(
            "GET", f"{MODRINTH_API}/tag/game_version", headers=self.headers
        )
        _, loader_tags = self.client.json(
            "GET", f"{MODRINTH_API}/tag/loader", headers=self.headers
        )
        available_versions = {tag.get("version") for tag in game_version_tags}
        available_loaders = {tag.get("name") for tag in loader_tags}
        requested_versions = {row["minecraft_version"] for row in plan["rows"]}
        requested_loaders = {row["loader"] for row in plan["rows"]}
        missing_versions = sorted(requested_versions - available_versions)
        missing_loaders = sorted(requested_loaders - available_loaders)
        if missing_versions or missing_loaders:
            details = []
            if missing_versions:
                details.append("game versions: " + ", ".join(missing_versions))
            if missing_loaders:
                details.append("loaders: " + ", ".join(missing_loaders))
            raise PublicationError("Modrinth does not recognize " + "; ".join(details))

        aliases = {
            dependency["aliases"]["modrinth"]
            for row in plan["rows"]
            for dependency in row.get("dependencies", [])
        }
        missing_projects = []
        for alias in sorted(aliases):
            status, _ = self.client.json(
                "GET",
                f"{MODRINTH_API}/project/{urllib.parse.quote(alias)}",
                headers=self.headers,
                expected=(200, 404),
            )
            if status == 404:
                missing_projects.append(alias)
        if missing_projects:
            raise PublicationError(
                "Modrinth dependency projects do not exist: "
                + ", ".join(missing_projects)
            )

    def publish_row(
        self,
        plan: dict[str, Any],
        row: dict[str, Any],
        artifact: pathlib.Path,
        journal: RecoveryJournal,
    ) -> dict[str, Any]:
        old = self._exact_version(row["external_version"])
        state: dict[str, Any] = {"old": old, "old_files": [], "new_id": None}
        if old:
            try:
                self._backup_and_delete(old, row["external_version"], state)
            except Exception as exc:
                self._recover_ambiguous_delete(
                    old, row["external_version"], state, journal, exc
                )
                raise
        try:
            created = self._create(
                self._metadata(plan, row),
                [(artifact.name, artifact.read_bytes(), True)],
            )
        except Exception as exc:
            # A timeout can happen after Modrinth accepted the multipart request.
            # Resolve the exact version before restoring the old one, otherwise
            # recovery could race with a successfully-created replacement.
            try:
                confirmed = self._exact_version(row["external_version"])
            except Exception:
                confirmed = None
            if confirmed:
                state["new_id"] = confirmed["id"]
                journal.record(
                    "modrinth",
                    "confirmed_after_ambiguous_response",
                    version_id=confirmed["id"],
                    version_number=row["external_version"],
                    replaced=old is not None,
                )
                return state
            if old:
                try:
                    self.rollback_row(state)
                except Exception as rollback_exc:
                    journal.rollback_errors.append(
                        f"Modrinth immediate restore failed: {rollback_exc}"
                    )
                    raise PublicationError(
                        f"Modrinth replacement failed ({exc}) and immediate restore failed ({rollback_exc})"
                    ) from exc
            raise
        state["new_id"] = created["id"]
        journal.record(
            "modrinth",
            "publish",
            version_id=created["id"],
            version_number=row["external_version"],
            replaced=old is not None,
        )
        return state

    def _recover_ambiguous_delete(
        self,
        old: dict[str, Any],
        version_number: str,
        state: dict[str, Any],
        journal: RecoveryJournal,
        original_error: Exception,
    ) -> None:
        try:
            current = self._exact_version(version_number)
        except Exception as check_error:
            journal.rollback_errors.append(
                f"Could not determine whether Modrinth deleted {version_number}: {check_error}"
            )
            journal.manual_actions.append(
                f"Inspect Modrinth version {version_number}; restore it from the recovery backup if absent."
            )
            raise PublicationError(
                f"Modrinth delete failed ambiguously ({original_error}) and its state could not be checked"
            ) from original_error
        if current is not None:
            if current.get("id") != old.get("id"):
                journal.manual_actions.append(
                    f"Inspect unexpected Modrinth replacement {current.get('id')} for {version_number}."
                )
            return
        try:
            self.rollback_row(state)
        except Exception as rollback_error:
            journal.rollback_errors.append(
                f"Modrinth restore after ambiguous delete failed: {rollback_error}"
            )
            journal.manual_actions.append(
                f"Restore Modrinth version {version_number} from the recovery backup."
            )
            raise PublicationError(
                f"Modrinth delete failed ambiguously ({original_error}) and restore failed ({rollback_error})"
            ) from original_error

    def _backup_and_delete(
        self, old: dict[str, Any], version_number: str, state: dict[str, Any]
    ) -> None:
        row_backup = self.backup_dir / version_number
        row_backup.mkdir(parents=True, exist_ok=True)
        (row_backup / "version.json").write_text(
            json.dumps(old, indent=2) + "\n", encoding="utf-8"
        )
        for file in old["files"]:
            _, _, content = self.client.request("GET", file["url"], expected=(200,))
            path = row_backup / file["filename"]
            path.write_bytes(content)
            state["old_files"].append(
                {
                    "filename": file["filename"],
                    "path": str(path),
                    "primary": file.get("primary", False),
                }
            )
        self.client.request(
            "DELETE",
            f"{MODRINTH_API}/version/{old['id']}",
            headers=self.headers,
            expected=(204,),
        )

    @staticmethod
    def _metadata(plan: dict[str, Any], row: dict[str, Any]) -> dict[str, Any]:
        return {
            "name": row["external_version"],
            "version_number": row["external_version"],
            "changelog": plan["changelog"],
            "dependencies": modrinth_dependencies(row),
            "game_versions": [row["minecraft_version"]],
            "version_type": plan["release_type"],
            "loaders": [row["loader"]],
            "featured": False,
            "status": "listed",
            "environment": modrinth_environment(row),
        }

    def _create(
        self, metadata: dict[str, Any], files: list[tuple[str, bytes, bool]]
    ) -> dict[str, Any]:
        parts: list[tuple[str, tuple[str, bytes, str]]] = []
        file_parts: list[str] = []
        primary = None
        for index, (filename, content, is_primary) in enumerate(files):
            part = f"file{index}"
            file_parts.append(part)
            if is_primary:
                primary = part
            parts.append((part, (filename, content, "application/java-archive")))
        data = {**metadata, "project_id": self.project_id, "file_parts": file_parts}
        if primary:
            data["primary_file"] = primary
        _, _, response = self.client.request(
            "POST",
            f"{MODRINTH_API}/version",
            headers=self.headers,
            form={"data": json.dumps(data)},
            files=parts,
            expected=(200,),
        )
        return json.loads(response)

    def rollback_row(self, state: dict[str, Any]) -> None:
        if state.get("new_id"):
            self.client.request(
                "DELETE",
                f"{MODRINTH_API}/version/{state['new_id']}",
                headers=self.headers,
                expected=(204, 404),
            )
        old = state.get("old")
        if not old:
            return
        allowed = {
            "name",
            "version_number",
            "changelog",
            "dependencies",
            "game_versions",
            "version_type",
            "loaders",
            "featured",
            "status",
            "requested_status",
            "environment",
        }
        metadata = {
            key: value
            for key, value in old.items()
            if key in allowed and value is not None
        }
        files = [
            (item["filename"], pathlib.Path(item["path"]).read_bytes(), item["primary"])
            for item in state["old_files"]
        ]
        self._create(metadata, files)
