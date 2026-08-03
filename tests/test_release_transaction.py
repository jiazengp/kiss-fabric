import pathlib
import sys
import tempfile
import unittest
from unittest.mock import Mock

import httpx


ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from release_support.core import HttpClient, PublicationError, RecoveryJournal  # noqa: E402
from release_support.curseforge import CurseForgePlatform  # noqa: E402
from release_support.github import GitHubPlatform  # noqa: E402
from release_support.modrinth import ModrinthPlatform  # noqa: E402
from release_support.transaction import run_transaction, write_checksums  # noqa: E402


class FakeGitHub:
    def __init__(self) -> None:
        self.rolled_back = False

    def publish(self, plan, assets, journal):
        journal.record("github", "publish", release_id=1)
        return {"release_id": 1}

    def rollback(self, state, assets):
        self.rolled_back = True


class FakeModrinth:
    def __init__(
        self, fail_at: int | None = None, rollback_fails: bool = False
    ) -> None:
        self.fail_at = fail_at
        self.rollback_fails = rollback_fails
        self.published = 0
        self.rolled_back = 0

    def publish_row(self, plan, row, artifact, journal):
        self.published += 1
        if self.fail_at == self.published:
            raise PublicationError("modrinth failure")
        journal.record("modrinth", "publish", version_id=self.published)
        return {"new_id": self.published}

    def rollback_row(self, state):
        self.rolled_back += 1
        if self.rollback_fails:
            raise PublicationError("rollback failure")


class FakeCurseForge:
    def __init__(self, fail_at: int | None = None) -> None:
        self.fail_at = fail_at
        self.uploaded = 0

    def upload_row(self, plan, row, artifact, journal):
        self.uploaded += 1
        if self.fail_at == self.uploaded:
            raise PublicationError("curseforge failure")
        journal.record("curseforge", "upload", file_id=self.uploaded)
        return self.uploaded

class FakeHttpClient:
    def __init__(self, payload) -> None:
        self.payload = payload

    def json(self, *args, **kwargs):
        return 200, self.payload


class ReleaseTransactionTests(unittest.TestCase):
    def test_http_client_delegates_multipart_encoding_to_httpx(self) -> None:
        captured: dict[str, object] = {}

        def handler(request: httpx.Request) -> httpx.Response:
            captured["content_type"] = request.headers["content-type"]
            captured["body"] = request.read()
            return httpx.Response(200, json={"id": "created"})

        transport = httpx.MockTransport(handler)
        client = HttpClient(client=httpx.Client(transport=transport))
        _, _, response = client.request(
            "POST",
            "https://example.invalid/upload",
            form={"metadata": '{"name":"test"}'},
            files=[("file", ("test.jar", b"jar-content", "application/java-archive"))],
        )

        self.assertEqual(response, b'{"id":"created"}')
        self.assertTrue(
            str(captured["content_type"]).startswith("multipart/form-data;")
        )
        self.assertIn(b'name="metadata"', captured["body"])
        self.assertIn(b'filename="test.jar"', captured["body"])
        self.assertIn(b"jar-content", captured["body"])

    def make_plan_and_artifacts(self, root: pathlib.Path, count: int = 2):
        rows = []
        artifacts = []
        for index in range(count):
            artifact = root / f"row-{index}.jar"
            artifact.write_bytes(f"jar-{index}".encode())
            artifacts.append(artifact)
            rows.append({"external_version": f"1.2.0+mc1.21.{index}-fabric"})
        write_checksums(artifacts, root / "SHA256SUMS.txt")
        return {"tag": "v1.2.0", "rows": rows}, artifacts

    def test_modrinth_failure_rolls_back_prior_writes(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            plan, artifacts = self.make_plan_and_artifacts(pathlib.Path(directory))
            github = FakeGitHub()
            modrinth = FakeModrinth(fail_at=2)
            journal = RecoveryJournal(plan["tag"])
            with self.assertRaisesRegex(PublicationError, "compensating rollback"):
                run_transaction(
                    plan, artifacts, github, modrinth, FakeCurseForge(), journal
                )
            self.assertTrue(github.rolled_back)
            self.assertEqual(modrinth.rolled_back, 1)

    def test_modrinth_timeout_confirms_created_version_before_restoring(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            artifact = pathlib.Path(directory) / "kissmod.jar"
            artifact.write_bytes(b"jar")
            old = {
                "id": "old-id",
                "version_number": "1.2.0+mc1.21.1-fabric",
            }
            created = {
                "id": "new-id",
                "version_number": "1.2.0+mc1.21.1-fabric",
            }
            platform = ModrinthPlatform.__new__(ModrinthPlatform)
            platform._versions = Mock(side_effect=[[old], [created]])
            platform._backup_and_delete = Mock()
            platform._create = Mock(side_effect=PublicationError("request timed out"))
            platform.rollback_row = Mock()
            journal = RecoveryJournal("v1.2.0")

            state = platform.publish_row(
                {"changelog": "Changes", "release_type": "release"},
                {
                    "external_version": "1.2.0+mc1.21.1-fabric",
                    "minecraft_version": "1.21.1",
                    "loader": "fabric",
                    "environment": "server",
                    "dependencies": [],
                },
                artifact,
                journal,
            )

            self.assertEqual(state["new_id"], "new-id")
            platform.rollback_row.assert_not_called()
            self.assertEqual(
                journal.operations[-1]["action"], "confirmed_after_ambiguous_response"
            )

    def test_modrinth_ambiguous_delete_restores_missing_old_version(self) -> None:
        old = {"id": "old-id", "version_number": "1.2.0+mc1.21.1-fabric"}
        platform = ModrinthPlatform.__new__(ModrinthPlatform)
        platform._exact_version = Mock(return_value=None)
        platform.rollback_row = Mock()
        journal = RecoveryJournal("v1.2.0")

        platform._recover_ambiguous_delete(
            old,
            old["version_number"],
            {"old": old, "old_files": [], "new_id": None},
            journal,
            PublicationError("delete timed out"),
        )

        platform.rollback_row.assert_called_once()

    def test_partial_curseforge_failure_records_manual_recovery(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            plan, artifacts = self.make_plan_and_artifacts(pathlib.Path(directory))
            github = FakeGitHub()
            modrinth = FakeModrinth()
            journal = RecoveryJournal(plan["tag"])
            with self.assertRaises(PublicationError):
                run_transaction(
                    plan,
                    artifacts,
                    github,
                    modrinth,
                    FakeCurseForge(fail_at=2),
                    journal,
                )
            self.assertTrue(journal.manual_actions)
            self.assertEqual(modrinth.rolled_back, 2)
            self.assertTrue(github.rolled_back)

    def test_rollback_failure_is_recorded(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            plan, artifacts = self.make_plan_and_artifacts(pathlib.Path(directory), 1)
            journal = RecoveryJournal(plan["tag"])
            with self.assertRaises(PublicationError):
                run_transaction(
                    plan,
                    artifacts,
                    FakeGitHub(),
                    FakeModrinth(rollback_fails=True),
                    FakeCurseForge(fail_at=1),
                    journal,
                )
            self.assertTrue(
                any(
                    "Modrinth rollback failed" in error
                    for error in journal.rollback_errors
                )
            )
            self.assertTrue(
                any("Check CurseForge manually" in action for action in journal.manual_actions)
            )

    def test_platform_metadata_uses_generic_dependencies_and_environment(self) -> None:
        row = {
            "external_version": "1.2.0+mc1.21.1-fabric",
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
        metadata = ModrinthPlatform._metadata(
            {"changelog": "Changes", "release_type": "release"}, row
        )
        self.assertEqual(metadata["environment"], "server_only")
        self.assertEqual(
            metadata["dependencies"],
            [{"project_id": "P7dR8mSH", "dependency_type": "required"}],
        )

    def test_modrinth_preflight_rejects_unknown_platform_target(self) -> None:
        def handler(request: httpx.Request) -> httpx.Response:
            if request.url.path.endswith("/tag/game_version"):
                return httpx.Response(200, json=[{"version": "1.21.1"}])
            if request.url.path.endswith("/tag/loader"):
                return httpx.Response(200, json=[{"name": "fabric"}])
            if "/project/" in request.url.path and not request.url.path.endswith(
                "/version"
            ):
                return httpx.Response(200, json={"id": "dependency"})
            return httpx.Response(200, json=[])

        client = HttpClient(client=httpx.Client(transport=httpx.MockTransport(handler)))
        with tempfile.TemporaryDirectory() as directory:
            platform = ModrinthPlatform(
                client,
                "project",
                "token",
                pathlib.Path(directory),
                "owner/repository",
            )
            with self.assertRaisesRegex(PublicationError, "game versions: 26.2"):
                platform.preflight(
                    {
                        "mod_version": "1.2.0",
                        "rows": [
                            {
                                "minecraft_version": "26.2",
                                "loader": "fabric",
                                "dependencies": [],
                                "external_version": "1.2.0+mc26.2-fabric",
                            }
                        ],
                    }
                )

    def test_curseforge_resolves_required_tags_and_omits_unknown_optional_tags(
        self,
    ) -> None:
        def handler(request: httpx.Request) -> httpx.Response:
            return httpx.Response(
                200,
                json=[{"name": "1.21.1"}, {"name": "Fabric"}, {"name": "Server"}],
            )

        client = HttpClient(client=httpx.Client(transport=httpx.MockTransport(handler)))
        platform = CurseForgePlatform(client, "1288675", "upload-token")
        row = {
            "external_version": "1.2.0+mc1.21.1-fabric",
            "minecraft_version": "1.21.1",
            "java_version": 21,
            "loader": "fabric",
            "environment": "server",
            "dependencies": [],
        }
        platform._resolve_game_version_names({"rows": [row]})

        self.assertEqual(
            platform._resolved_game_versions[row["external_version"]],
            ["1.21.1", "Fabric", "Server"],
        )

    def test_curseforge_upload_contains_dependencies_java_and_environment(self) -> None:
        captured: dict[str, bytes] = {}

        def handler(request: httpx.Request) -> httpx.Response:
            captured["body"] = request.read()
            return httpx.Response(200, json={"id": 99})

        client = HttpClient(client=httpx.Client(transport=httpx.MockTransport(handler)))
        platform = CurseForgePlatform(client, "1288675", "upload-token")
        with tempfile.TemporaryDirectory() as directory:
            artifact = pathlib.Path(directory) / "kissmod.jar"
            artifact.write_bytes(b"jar")
            row = {
                "external_version": "1.2.0+mc1.21.1-fabric",
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
            file_id = platform.upload_row(
                {"changelog": "Changes", "release_type": "release"},
                row,
                artifact,
                RecoveryJournal("v1.2.0"),
            )

        self.assertEqual(file_id, 99)
        self.assertIn(b'"Java 21"', captured["body"])
        self.assertIn(b'"Server"', captured["body"])
        self.assertIn(b'"slug": "fabric-api"', captured["body"])
        self.assertIn(b'"type": "requiredDependency"', captured["body"])

    def test_github_preflight_blocks_second_scope_for_same_version(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            platform = GitHubPlatform(
                FakeHttpClient([{"tag_name": "v1.2.0+mc26.X-fabric"}]),
                "owner/repository",
                "token",
                pathlib.Path(directory),
            )
            with self.assertRaisesRegex(
                PublicationError, "already released with a different scope"
            ):
                platform.preflight(
                    {"tag": "v1.2.0+mc1.21.1-neoforge", "mod_version": "1.2.0"}
                )

    def test_modrinth_preflight_allows_exact_scope_rerun_but_blocks_other_rows(
        self,
    ) -> None:
        with tempfile.TemporaryDirectory() as directory:
            platform = ModrinthPlatform(
                FakeHttpClient([{"version_number": "1.2.0+mc26.1.2-fabric"}]),
                "project",
                "token",
                pathlib.Path(directory),
                "owner/repository",
            )
            platform._validate_targets = Mock()
            exact = {
                "mod_version": "1.2.0",
                "rows": [{"external_version": "1.2.0+mc26.1.2-fabric"}],
            }
            platform.preflight(exact)
            with self.assertRaisesRegex(PublicationError, "outside this tag scope"):
                platform.preflight(
                    {
                        "mod_version": "1.2.0",
                        "rows": [{"external_version": "1.2.0+mc26.2-neoforge"}],
                    }
                )


if __name__ == "__main__":
    unittest.main()
