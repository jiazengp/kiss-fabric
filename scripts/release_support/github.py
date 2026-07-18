from __future__ import annotations

import json
import mimetypes
import pathlib
import urllib.parse
from typing import Any

from .core import HttpClient, PublicationError, RecoveryJournal


GITHUB_API = "https://api.github.com"


def render_release_body(plan: dict[str, Any]) -> str:
    lines = [
        plan["changelog"].strip(),
        "",
        "### Published targets",
        "",
        "| Minecraft | Loader | File |",
        "|---|---|---|",
    ]
    for row in plan["rows"]:
        lines.append(
            f"| {row['minecraft_version']} | {row['loader']} | `{row['artifact_name']}` |"
        )
    environment = plan["rows"][0]["environment"].replace("_", " ")
    lines.extend(
        [
            "",
            f"All files target the {environment} environment. "
            "See `SHA256SUMS.txt` for checksums.",
        ]
    )
    return "\n".join(lines) + "\n"


class GitHubPlatform:
    """GitHub Release replacement and rollback adapter."""

    def __init__(
        self, client: HttpClient, repository: str, token: str, backup_dir: pathlib.Path
    ):
        self.client = client
        self.repository = repository
        self.backup_dir = backup_dir / "github"
        self.headers = {
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {token}",
            "X-GitHub-Api-Version": "2022-11-28",
            "User-Agent": "kissmod-release",
        }
        self._pending_state: dict[str, Any] | None = None

    def _url(self, path: str) -> str:
        return f"{GITHUB_API}/repos/{self.repository}{path}"

    def _get_release(self, tag: str) -> dict[str, Any] | None:
        status, payload = self.client.json(
            "GET",
            self._url(f"/releases/tags/{urllib.parse.quote(tag)}"),
            headers=self.headers,
            expected=(200, 404),
        )
        return None if status == 404 else payload

    def preflight(self, plan: dict[str, Any]) -> None:
        releases: list[dict[str, Any]] = []
        page = 1
        while True:
            query = urllib.parse.urlencode({"per_page": 100, "page": page})
            _, batch = self.client.json(
                "GET", self._url(f"/releases?{query}"), headers=self.headers
            )
            releases.extend(batch)
            if len(batch) < 100:
                break
            page += 1
        base_tag = f"v{plan['mod_version']}"
        conflicting_tags = sorted(
            release["tag_name"]
            for release in releases
            if (
                release["tag_name"] == base_tag
                or release["tag_name"].startswith(base_tag + "+mc")
            )
            and release["tag_name"] != plan["tag"]
        )
        if conflicting_tags:
            raise PublicationError(
                f"Version {plan['mod_version']} was already released with a different scope: "
                + ", ".join(conflicting_tags)
                + ". Bump the patch version instead of splitting one version across tags."
            )

    def publish(
        self, plan: dict[str, Any], assets: list[pathlib.Path], journal: RecoveryJournal
    ) -> dict[str, Any]:
        self._pending_state = None
        try:
            return self._publish(plan, assets, journal)
        except Exception as exc:
            state = self._pending_state
            if state and state.get("release_id"):
                try:
                    self.rollback(state, assets)
                except Exception as rollback_exc:
                    journal.rollback_errors.append(
                        f"GitHub immediate rollback failed: {rollback_exc}"
                    )
                    raise PublicationError(
                        f"GitHub publish failed ({exc}) and immediate rollback failed ({rollback_exc})"
                    ) from exc
            raise

    def _publish(
        self, plan: dict[str, Any], assets: list[pathlib.Path], journal: RecoveryJournal
    ) -> dict[str, Any]:
        existing = self._get_release(plan["tag"])
        state: dict[str, Any] = {
            "created": existing is None,
            "old_release": existing,
            "old_assets": [],
            "changed_names": [],
            "new_asset_names": [],
        }
        if existing:
            state["release_id"] = existing["id"]
        self._pending_state = state
        self.backup_dir.mkdir(parents=True, exist_ok=True)
        target_names = {asset.name for asset in assets}
        if existing:
            snapshot = {
                key: existing.get(key)
                for key in ("id", "tag_name", "name", "body", "draft", "prerelease")
            }
            snapshot["assets"] = [
                asset.get("name") for asset in existing.get("assets", [])
            ]
            (self.backup_dir / "release.json").write_text(
                json.dumps(snapshot, indent=2) + "\n", encoding="utf-8"
            )
            for asset in existing.get("assets", []):
                if asset["name"] not in target_names:
                    continue
                # This repository is public. Use the public download URL so the
                # GitHub token is never forwarded to a CDN redirect target.
                _, _, content = self.client.request(
                    "GET", asset["browser_download_url"], expected=(200,)
                )
                backup = self.backup_dir / asset["name"]
                backup.write_bytes(content)
                state["old_assets"].append({"name": asset["name"], "path": str(backup)})
                self.client.request(
                    "DELETE", asset["url"], headers=self.headers, expected=(204,)
                )
                state["changed_names"].append(asset["name"])
            _, release = self.client.json(
                "PATCH",
                self._url(f"/releases/{existing['id']}"),
                headers=self.headers,
                payload=self._release_metadata(plan),
                expected=(200,),
            )
        else:
            payload = {"tag_name": plan["tag"], **self._release_metadata(plan)}
            _, release = self.client.json(
                "POST",
                self._url("/releases"),
                headers=self.headers,
                payload=payload,
                expected=(201,),
            )
        state["release_id"] = release["id"]
        upload_url = release["upload_url"].split("{", 1)[0]
        for asset in assets:
            url = f"{upload_url}?{urllib.parse.urlencode({'name': asset.name})}"
            self.client.request(
                "POST",
                url,
                headers={
                    **self.headers,
                    "Content-Type": mimetypes.guess_type(asset.name)[0]
                    or "application/octet-stream",
                },
                body=asset.read_bytes(),
                expected=(201,),
            )
            state["new_asset_names"].append(asset.name)
        journal.record(
            "github", "publish", release_id=release["id"], replaced=existing is not None
        )
        self._pending_state = None
        return state

    @staticmethod
    def _release_metadata(plan: dict[str, Any]) -> dict[str, Any]:
        return {
            "name": plan["tag"],
            "body": render_release_body(plan),
            "draft": False,
            "prerelease": plan["prerelease"],
        }

    def rollback(self, state: dict[str, Any], assets: list[pathlib.Path]) -> None:
        release_id = state["release_id"]
        if state["created"]:
            self.client.request(
                "DELETE",
                self._url(f"/releases/{release_id}"),
                headers=self.headers,
                expected=(204, 404),
            )
            return
        _, release = self.client.json(
            "GET", self._url(f"/releases/{release_id}"), headers=self.headers
        )
        target_names = set(state.get("changed_names", [])) | set(
            state.get("new_asset_names", [])
        )
        for asset in release.get("assets", []):
            if asset["name"] in target_names:
                self.client.request(
                    "DELETE", asset["url"], headers=self.headers, expected=(204, 404)
                )
        old = state["old_release"]
        _, restored = self.client.json(
            "PATCH",
            self._url(f"/releases/{release_id}"),
            headers=self.headers,
            payload={
                "name": old.get("name"),
                "body": old.get("body"),
                "draft": old.get("draft", False),
                "prerelease": old.get("prerelease", False),
            },
        )
        upload_url = restored["upload_url"].split("{", 1)[0]
        for old_asset in state["old_assets"]:
            path = pathlib.Path(old_asset["path"])
            self.client.request(
                "POST",
                f"{upload_url}?{urllib.parse.urlencode({'name': old_asset['name']})}",
                headers={**self.headers, "Content-Type": "application/octet-stream"},
                body=path.read_bytes(),
                expected=(201,),
            )
