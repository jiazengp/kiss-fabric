import json
import pathlib
import subprocess
import sys
import tempfile
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "release_plan.py"
sys.path.insert(0, str(ROOT / "scripts"))

from release_plan import extract_changelog, parse_tag  # noqa: E402
from release_support.metadata import load_release_metadata  # noqa: E402


def project_version() -> str:
    for line in (ROOT / "gradle.properties").read_text(encoding="utf-8").splitlines():
        key, _, value = line.partition("=")
        if key.strip() == "version":
            return value.strip()
    raise AssertionError("version= missing from gradle.properties")


TAG = f"v{project_version()}"


class ReleasePlanTests(unittest.TestCase):
    def run_plan(
        self, tag: str, changelog: pathlib.Path | None = None
    ) -> subprocess.CompletedProcess[str]:
        args = [sys.executable, str(SCRIPT), "--tag", tag]
        if changelog:
            args.extend(("--changelog", str(changelog)))
        return subprocess.run(
            args, cwd=ROOT, text=True, capture_output=True, check=False
        )

    def test_full_release_contains_every_enabled_manifest_row(self) -> None:
        result = self.run_plan(TAG)
        self.assertEqual(result.returncode, 0, result.stderr)
        plan = json.loads(result.stdout)
        self.assertEqual(len(plan["rows"]), 25)
        self.assertTrue(plan["prerelease"])
        self.assertEqual(plan["release_type"], "alpha")
        self.assertNotIn("v26_3", {row["version_id"] for row in plan["rows"]})
        fabric = next(row for row in plan["rows"] if row["loader"] == "fabric")
        neoforge = next(row for row in plan["rows"] if row["loader"] == "neoforge")
        self.assertEqual(fabric["environment"], "server")
        self.assertEqual(
            fabric["dependencies"],
            [
                {
                    "id": "fabric-api",
                    "type": "required",
                    "aliases": {
                        "modrinth": "P7dR8mSH",
                        "curseforge": "fabric-api",
                    },
                }
            ],
        )
        self.assertEqual(neoforge["dependencies"], [])

    def test_family_and_loader_selector(self) -> None:
        result = self.run_plan(f"{TAG}+mc26.X-fabric")
        self.assertEqual(result.returncode, 0, result.stderr)
        plan = json.loads(result.stdout)
        self.assertEqual(
            {(row["minecraft_version"], row["loader"]) for row in plan["rows"]},
            {("26.1.2", "fabric"), ("26.2", "fabric")},
        )

    def test_exact_version_selects_all_enabled_loaders(self) -> None:
        result = self.run_plan(f"{TAG}+mc1.21.1")
        self.assertEqual(result.returncode, 0, result.stderr)
        plan = json.loads(result.stdout)
        self.assertEqual(
            {row["loader"] for row in plan["rows"]}, {"fabric", "neoforge"}
        )

    def test_alpha_and_beta_channels(self) -> None:
        alpha = parse_tag("v1.2.0-alpha.1+mc26.X-fabric")
        beta = parse_tag("v1.2.0-beta.2")
        self.assertEqual(alpha.release_type, "alpha")
        self.assertTrue(alpha.prerelease)
        self.assertEqual(beta.release_type, "beta")
        self.assertTrue(beta.prerelease)
        with tempfile.TemporaryDirectory() as directory:
            changelog = pathlib.Path(directory) / "CHANGELOG.md"
            changelog.write_text(
                "# Changelog\n\n## [1.2.0-alpha.1] - 2026-07-16\n\n- Alpha.\n",
                encoding="utf-8",
            )
            original = (ROOT / "gradle.properties").read_text(encoding="utf-8")
            # A tag must match the project version exactly, including its prerelease suffix.
            result = self.run_plan("v1.2.0-alpha.1", changelog)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("does not match", result.stderr)
            self.assertIn(f"version={project_version()}", original)

    def test_invalid_tags_fail(self) -> None:
        for tag in (
            "1.2.0",
            "v1.2.0-rc.1",
            "v1.2.0+mc26.x",
            "v1.2.0+mc26.X-Forge",
            "v1.2.0+metadata",
        ):
            with self.subTest(tag=tag):
                result = self.run_plan(tag)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("Invalid release tag", result.stderr)

    def test_empty_selection_fails(self) -> None:
        result = self.run_plan(f"{TAG}+mc99.X")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("selects no enabled manifest rows", result.stderr)

    def test_missing_or_empty_changelog_section_fails(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            missing = pathlib.Path(directory) / "missing.md"
            missing.write_text(
                "# Changelog\n\n## [9.9.9] - 2026-01-01\n\n- Other.\n", encoding="utf-8"
            )
            result = self.run_plan(TAG, missing)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("no release section", result.stderr)

            empty = pathlib.Path(directory) / "empty.md"
            empty.write_text(
                f"# Changelog\n\n## [{project_version()}] - 2026-07-18\n\n### Added\n",
                encoding="utf-8",
            )
            result = self.run_plan(TAG, empty)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn("is empty", result.stderr)

    def test_changelog_rejects_duplicate_section_and_invalid_date(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "CHANGELOG.md"
            path.write_text(
                "## [1.2.0] - 2026-07-16\n\n- One.\n\n"
                "## [1.2.0] - 2026-07-17\n\n- Two.\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "duplicate release sections"):
                extract_changelog(path, "1.2.0")
            path.write_text(
                "## [1.2.0] - 2026-02-31\n\n- Invalid date.\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "invalid calendar date"):
                extract_changelog(path, "1.2.0")

    def test_release_metadata_rejects_missing_platform_alias(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = pathlib.Path(directory) / "release-metadata.json"
            path.write_text(
                json.dumps(
                    {
                        "schema_version": 1,
                        "environment": "server",
                        "dependencies": [
                            {
                                "id": "example",
                                "type": "required",
                                "loaders": ["fabric"],
                                "aliases": {"modrinth": "project"},
                            }
                        ],
                    }
                ),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "modrinth and curseforge"):
                load_release_metadata(path)


if __name__ == "__main__":
    unittest.main()
