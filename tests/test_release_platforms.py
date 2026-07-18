import pathlib
import sys
import tempfile
import unittest

import httpx


ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from release_support.core import HttpClient, RecoveryJournal  # noqa: E402
from release_support.github import GitHubPlatform  # noqa: E402
from release_support.metadata import (  # noqa: E402
    curseforge_relations,
    modrinth_dependencies,
)
from release_support.modrinth import ModrinthPlatform  # noqa: E402


def release_row() -> dict[str, object]:
    return {
        "external_version": "1.2.0+mc1.21.1-fabric",
        "artifact_name": "kissmod-1.2.0+mc1.21.1-fabric.jar",
        "minecraft_version": "1.21.1",
        "java_version": 21,
        "loader": "fabric",
        "environment": "server",
        "dependencies": [
            {
                "id": "fabric-api",
                "type": "required",
                "aliases": {
                    "modrinth": "P7dR8mSH",
                    "curseforge": "fabric-api",
                },
            }
        ],
    }


class ReleasePlatformTests(unittest.TestCase):
    def test_every_generic_dependency_type_maps_to_both_platforms(self) -> None:
        expected = {
            "required": ("required", "requiredDependency"),
            "recommended": ("optional", "optionalDependency"),
            "optional": ("optional", "optionalDependency"),
            "embedded": ("embedded", "embeddedLibrary"),
            "conflicting": ("incompatible", "incompatible"),
            "incompatible": ("incompatible", "incompatible"),
        }
        for dependency_type, platform_types in expected.items():
            with self.subTest(dependency_type=dependency_type):
                row = release_row()
                row["dependencies"][0]["type"] = dependency_type
                self.assertEqual(
                    modrinth_dependencies(row)[0]["dependency_type"],
                    platform_types[0],
                )
                self.assertEqual(
                    curseforge_relations(row)[0]["type"], platform_types[1]
                )

    def test_github_existing_release_can_be_replaced_and_rolled_back(self) -> None:
        events: list[tuple[str, str, bytes]] = []
        artifact_name = release_row()["artifact_name"]

        def handler(request: httpx.Request) -> httpx.Response:
            body = request.read()
            events.append((request.method, str(request.url), body))
            path = request.url.path
            if request.method == "GET" and path.endswith("/releases/tags/v1.2.0"):
                return httpx.Response(
                    200,
                    json={
                        "id": 1,
                        "tag_name": "v1.2.0",
                        "name": "old name",
                        "body": "old body",
                        "draft": False,
                        "prerelease": False,
                        "upload_url": "https://uploads.example/releases/1/assets{?name}",
                        "assets": [
                            {
                                "name": artifact_name,
                                "url": "https://api.github.com/assets/10",
                                "browser_download_url": "https://cdn.example/old.jar",
                            }
                        ],
                    },
                )
            if request.method == "GET" and request.url.host == "cdn.example":
                return httpx.Response(200, content=b"old-jar")
            if request.method == "DELETE" and path.startswith("/assets/"):
                return httpx.Response(204)
            if request.method == "PATCH" and path.endswith("/releases/1"):
                return httpx.Response(
                    200,
                    json={
                        "id": 1,
                        "upload_url": "https://uploads.example/releases/1/assets{?name}",
                    },
                )
            if request.method == "POST" and request.url.host == "uploads.example":
                return httpx.Response(201, json={"id": 20})
            if request.method == "GET" and path.endswith("/releases/1"):
                return httpx.Response(
                    200,
                    json={
                        "assets": [
                            {
                                "name": artifact_name,
                                "url": "https://api.github.com/assets/20",
                            }
                        ]
                    },
                )
            raise AssertionError(f"Unexpected request: {request.method} {request.url}")

        client = HttpClient(client=httpx.Client(transport=httpx.MockTransport(handler)))
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            artifact = root / artifact_name
            artifact.write_bytes(b"new-jar")
            platform = GitHubPlatform(
                client, "owner/repository", "token", root / "backup"
            )
            plan = {
                "tag": "v1.2.0",
                "prerelease": False,
                "changelog": "Changes",
                "rows": [release_row()],
            }
            state = platform.publish(plan, [artifact], RecoveryJournal("v1.2.0"))
            platform.rollback(state, [artifact])

        uploaded_bodies = [
            body
            for method, url, body in events
            if method == "POST" and "uploads.example" in url
        ]
        self.assertEqual(uploaded_bodies, [b"new-jar", b"old-jar"])

    def test_modrinth_existing_version_can_be_replaced_and_rolled_back(self) -> None:
        deleted_ids: list[str] = []
        uploads = 0
        old = {
            "id": "old-id",
            "name": "old",
            "version_number": "1.2.0+mc1.21.1-fabric",
            "changelog": "old changes",
            "dependencies": [],
            "game_versions": ["1.21.1"],
            "version_type": "release",
            "loaders": ["fabric"],
            "featured": False,
            "status": "listed",
            "environment": "server_only",
            "files": [
                {
                    "filename": "old.jar",
                    "url": "https://cdn.example/old.jar",
                    "primary": True,
                }
            ],
        }

        def handler(request: httpx.Request) -> httpx.Response:
            nonlocal uploads
            path = request.url.path
            if request.method == "GET" and path.endswith("/project/project/version"):
                return httpx.Response(200, json=[old])
            if request.method == "GET" and request.url.host == "cdn.example":
                return httpx.Response(200, content=b"old-jar")
            if request.method == "DELETE" and "/version/" in path:
                deleted_ids.append(path.rsplit("/", 1)[-1])
                return httpx.Response(204)
            if request.method == "POST" and path.endswith("/version"):
                uploads += 1
                return httpx.Response(200, json={"id": f"upload-{uploads}"})
            raise AssertionError(f"Unexpected request: {request.method} {request.url}")

        client = HttpClient(client=httpx.Client(transport=httpx.MockTransport(handler)))
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            artifact = root / release_row()["artifact_name"]
            artifact.write_bytes(b"new-jar")
            platform = ModrinthPlatform(
                client, "project", "token", root / "backup", "owner/repository"
            )
            state = platform.publish_row(
                {"changelog": "Changes", "release_type": "release"},
                release_row(),
                artifact,
                RecoveryJournal("v1.2.0"),
            )
            platform.rollback_row(state)

        self.assertEqual(deleted_ids, ["old-id", "upload-1"])
        self.assertEqual(uploads, 2)


if __name__ == "__main__":
    unittest.main()
