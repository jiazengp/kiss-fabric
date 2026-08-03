import json
import pathlib
import subprocess
import sys
import unittest

from scripts import manifest_matrix


ROOT = pathlib.Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "scripts" / "manifest_matrix.py"
VERSIONS_INDEX = ROOT / "versions.json"
VERSIONS_DIR = ROOT / "versions"


class ManifestMatrixTests(unittest.TestCase):
    def run_script(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [sys.executable, str(SCRIPT), *args],
            cwd=ROOT,
            text=True,
            capture_output=True,
            check=False,
        )

    def test_validate_accepts_manifest(self) -> None:
        result = self.run_script("validate")
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_boolean_fields_are_strict(self) -> None:
        with self.assertRaisesRegex(ValueError, "invalid boolean field 'enable_fabric'"):
            manifest_matrix.parse_boolean(
                {"enable_fabric": "yes"}, "enable_fabric", "vtest"
            )

    def test_java_version_policy(self) -> None:
        versions_index = json.loads(VERSIONS_INDEX.read_text(encoding="utf-8"))
        for version_id in versions_index["versions"]:
            props = {}
            for line in (
                (VERSIONS_DIR / version_id / "gradle.properties")
                .read_text(encoding="utf-8")
                .splitlines()
            ):
                stripped = line.strip()
                if not stripped or stripped.startswith("#") or "=" not in stripped:
                    continue
                key, value = stripped.split("=", 1)
                props[key.strip()] = value.strip()
            if props["minecraft_version"].startswith("26."):
                self.assertEqual(int(props["java_version"]), 25, version_id)
            else:
                self.assertEqual(int(props["java_version"]), 21, version_id)

    def test_matrix_preserves_minecraft_version_strings(self) -> None:
        result = self.run_script("matrix")
        self.assertEqual(result.returncode, 0, result.stderr)
        payload = json.loads(result.stdout)
        versions = {
            entry["version_id"]: entry["minecraft_version"]
            for entry in payload["include"]
        }
        self.assertEqual(versions["v1_21_5"], "1.21.5")
        self.assertEqual(versions["v26_2"], "26.2")
        self.assertNotIn("v26_3", versions)

    def test_matrix_only_contains_enabled_loaders(self) -> None:
        result = self.run_script("matrix")
        self.assertEqual(result.returncode, 0, result.stderr)
        payload = json.loads(result.stdout)
        rows = {(entry["version_id"], entry["loader"]) for entry in payload["include"]}
        expected = {
            ("v1_20_1", "fabric"),
            ("v1_20_2", "fabric"),
            ("v1_20_2", "neoforge"),
            ("v1_20_4", "fabric"),
            ("v1_20_4", "neoforge"),
            ("v1_20_6", "fabric"),
            ("v1_20_6", "neoforge"),
            ("v1_21_1", "fabric"),
            ("v1_21_1", "neoforge"),
            ("v1_21_3", "fabric"),
            ("v1_21_3", "neoforge"),
            ("v1_21_4", "fabric"),
            ("v1_21_4", "neoforge"),
            ("v1_21_5", "fabric"),
            ("v1_21_5", "neoforge"),
            ("v1_21_8", "fabric"),
            ("v1_21_8", "neoforge"),
            ("v1_21_10", "fabric"),
            ("v1_21_10", "neoforge"),
            ("v1_21_11", "fabric"),
            ("v1_21_11", "neoforge"),
            ("v26_1_2", "fabric"),
            ("v26_1_2", "neoforge"),
            ("v26_2", "fabric"),
            ("v26_2", "neoforge"),
        }
        self.assertEqual(rows, expected)
        self.assertIn(("v1_21_5", "fabric"), rows)
        self.assertIn(("v1_21_5", "neoforge"), rows)
        self.assertIn(("v26_2", "fabric"), rows)
        self.assertIn(("v26_2", "neoforge"), rows)

    def test_matrix_can_filter_java_version(self) -> None:
        result = self.run_script("matrix", "--java-version", "25")
        self.assertEqual(result.returncode, 0, result.stderr)
        payload = json.loads(result.stdout)
        self.assertTrue(payload["include"])
        self.assertTrue(all(row["java_version"] == 25 for row in payload["include"]))
        self.assertEqual(
            {row["version_id"] for row in payload["include"]},
            {"v26_1_2", "v26_2"},
        )

    def test_matrix_rows_include_gradle_tasks(self) -> None:
        result = self.run_script(
            "matrix", "--loader", "fabric", "--version-id", "v1_21_5"
        )
        self.assertEqual(result.returncode, 0, result.stderr)
        payload = json.loads(result.stdout)
        self.assertEqual(
            payload,
            {
                "include": [
                    {
                        "version_id": "v1_21_5",
                        "minecraft_version": "1.21.5",
                        "java_version": 21,
                        "loader": "fabric",
                        "gradle_task": ":v1_21_5:fabric:build",
                        "gametest_task": ":v1_21_5:fabric:runGameTest",
                    }
                ]
            },
        )

    def test_unreleased_versions_are_not_in_build_matrix(self) -> None:
        result = self.run_script("matrix")
        self.assertEqual(result.returncode, 0, result.stderr)
        payload = json.loads(result.stdout)
        self.assertNotIn(
            "v26_3",
            {row["version_id"] for row in payload["include"]},
            "v26_3 is not a supported build target until an official release is selected",
        )

    def test_gametest_tasks_follow_loader_conventions(self) -> None:
        result = self.run_script("matrix", "--version-id", "v1_21_5")
        self.assertEqual(result.returncode, 0, result.stderr)
        rows = {
            row["loader"]: row["gametest_task"]
            for row in json.loads(result.stdout)["include"]
        }
        self.assertEqual(rows["fabric"], ":v1_21_5:fabric:runGameTest")
        self.assertEqual(rows["neoforge"], ":v1_21_5:neoforge:runGameTestServer")

        fabric_build = (ROOT / "fabric" / "build.gradle").read_text(encoding="utf-8")
        neoforge_build = (ROOT / "neoforge" / "build.gradle").read_text(
            encoding="utf-8"
        )
        self.assertIn("fabric/gametest/config/${mod_id}", fabric_build)
        self.assertIn('config/${mod_id}-server.toml', neoforge_build)
        self.assertIn('world/serverconfig/${mod_id}-server.toml', neoforge_build)

    def test_config_contract_is_wired_for_both_loaders(self) -> None:
        common_contract = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "config"
            / "KissServerConfig.java"
        ).read_text(encoding="utf-8")
        common_data = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "config"
            / "KissConfigData.java"
        ).read_text(encoding="utf-8")
        common_defaults = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "config"
            / "KissConfigDefaults.java"
        ).read_text(encoding="utf-8")
        fabric_config = (
            ROOT
            / "fabric"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "config"
            / "KissConfig.java"
        ).read_text(encoding="utf-8")
        fabric_manager = (
            ROOT
            / "fabric"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "config"
            / "KissConfigManager.java"
        ).read_text(encoding="utf-8")
        neoforge_config = (
            ROOT
            / "neoforge"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "config"
            / "NeoForgeKissConfig.java"
        ).read_text(encoding="utf-8")

        required_fields = (
            "sneakParticleDecay",
            "heartParticleHeightOffset",
            "friendlyEntityTypes",
            "friendlyEntityTags",
        )
        for field in required_fields:
            with self.subTest(field=field):
                self.assertIn(field, common_contract)
                self.assertIn(field, common_data)
                self.assertIn(field, fabric_config)
                self.assertIn(field, fabric_manager)
                self.assertIn(field, neoforge_config)

        self.assertIn("SNEAK_PARTICLE_DECAY", common_defaults)
        self.assertIn("HEART_PARTICLE_HEIGHT_OFFSET", common_defaults)
        self.assertIn("FRIENDLY_ENTITY_TYPES", common_defaults)
        self.assertIn("FRIENDLY_ENTITY_TAGS", common_defaults)

    def test_readme_documents_modded_friendly_entity_configuration(self) -> None:
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        common_defaults = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "config"
            / "KissConfigDefaults.java"
        ).read_text(encoding="utf-8")
        kiss_utils = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "KissUtils.java"
        ).read_text(encoding="utf-8")

        self.assertIn("config/kissmod/config.yml", readme)
        self.assertIn("kissmod-server.toml", readme)
        self.assertIn("friendlyEntityTypes", readme)
        self.assertIn("enableEntityParticles: true", readme)
        self.assertIn("enableEntityParticles = true", readme)
        self.assertNotIn("touhou_little_maid:maid", readme)
        self.assertNotIn("touhou_little_maid:maid", common_defaults)
        self.assertIn(
            "BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()",
            kiss_utils,
        )
        self.assertIn("cfg.friendlyEntityTypes().contains(entityId)", kiss_utils)

    def test_automatic_particles_filter_non_visible_and_fake_players(self) -> None:
        platform_contract = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "platform"
            / "services"
            / "IPlatformHelper.java"
        ).read_text(encoding="utf-8")
        kiss_utils = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "KissUtils.java"
        ).read_text(encoding="utf-8")
        common_runtime = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "KissModCommon.java"
        ).read_text(encoding="utf-8")
        command = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "KissCommand.java"
        ).read_text(encoding="utf-8")
        fabric_platform = (
            ROOT
            / "fabric"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "platform"
            / "FabricPlatformHelper.java"
        ).read_text(encoding="utf-8")
        neoforge_platform = (
            ROOT
            / "neoforge"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "platform"
            / "NeoForgePlatformHelper.java"
        ).read_text(encoding="utf-8")

        self.assertIn("boolean isFakePlayer(ServerPlayer player)", platform_contract)
        self.assertNotIn("IPlatformHelper INSTANCE", platform_contract)
        self.assertIn("player.isSpectator()", kiss_utils)
        self.assertIn("player.isInvisible()", kiss_utils)
        self.assertIn("Services.PLATFORM.isFakePlayer(player)", kiss_utils)
        self.assertIn("KissUtils.isExcludedPlayer(player)", common_runtime)
        self.assertIn("!KissUtils.isExcludedPlayer(target)", command)
        self.assertIn("player instanceof FakePlayer", fabric_platform)
        self.assertIn("player instanceof FakePlayer", neoforge_platform)

    def test_sneak_particles_require_distinct_crouch_toggles(self) -> None:
        common = (
            ROOT
            / "common"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "KissModCommon.java"
        ).read_text(encoding="utf-8")

        toggle_guard = "if (isSneaking && !data.wasSneaking)"
        self.assertEqual(common.count(toggle_guard), 1)
        guarded_body = common.split(toggle_guard, 1)[1].split(
            "if (!isSneaking && data.wasSneaking)", 1
        )[0]
        self.assertIn("data.count", guarded_body)
        self.assertIn("getFriendlyEntitiesNearby", guarded_body)
        self.assertIn("spawnFriendlyEntityHeartParticles", guarded_body)

    def test_permissions_are_wired_for_both_loaders(self) -> None:
        readme = (ROOT / "README.md").read_text(encoding="utf-8")
        fabric_build = (ROOT / "fabric" / "build.gradle").read_text(encoding="utf-8")
        common_runtime = (
            ROOT / "common" / "src" / "main" / "java" / "com" / "kissmod" / "KissModCommon.java"
        ).read_text(encoding="utf-8")
        command = (
            ROOT / "common" / "src" / "main" / "java" / "com" / "kissmod" / "KissCommand.java"
        ).read_text(encoding="utf-8")
        neoforge_platform = (
            ROOT
            / "neoforge"
            / "src"
            / "main"
            / "java"
            / "com"
            / "kissmod"
            / "platform"
            / "NeoForgePlatformHelper.java"
        ).read_text(encoding="utf-8")

        self.assertIn("fabric-permissions-api", fabric_build)
        self.assertIn("transitive = false", fabric_build)
        self.assertIn('hasPermission(player, "command")', command)
        self.assertIn('hasPermission(player, "trigger")', common_runtime)
        self.assertIn("PermissionAPI.getPermission", neoforge_platform)
        self.assertIn("PermissionGatherEvent.Nodes", neoforge_platform)
        self.assertIn("kissmod.command", readme)
        self.assertIn("kissmod.trigger", readme)

    def test_release_builds_then_publishes_from_merged_artifacts(self) -> None:
        workflow = (ROOT / ".github" / "workflows" / "release.yml").read_text(
            encoding="utf-8"
        )
        ci_workflow = (ROOT / ".github" / "workflows" / "ci.yml").read_text(
            encoding="utf-8"
        )

        self.assertIn("python scripts/prepare_release_artifact.py", workflow)
        self.assertIn("pattern: binary-*", workflow)
        self.assertIn("python scripts/release_transaction.py publish", workflow)
        self.assertIn(
            "Publish GitHub Release, Modrinth, and CurseForge", workflow
        )
        self.assertIn("scripts/release-requirements.txt", workflow)
        self.assertIn(
            '"release-metadata.json"',
            (ROOT / ".github" / "workflows" / "ci.yml").read_text(encoding="utf-8"),
        )
        self.assertIn("needs:\n      - prepare\n      - build", workflow)
        self.assertNotIn("libs/*.jar", workflow)
        self.assertIn("max-parallel: 25", workflow)
        self.assertIn("max-parallel: 25", ci_workflow)
        self.assertIn("  preflight:\n", ci_workflow)
        self.assertIn("needs: preflight", ci_workflow)
        self.assertIn("needs.preflight.outputs.matrix", ci_workflow)
        self.assertNotIn("  build-logic:\n", ci_workflow)
        self.assertIn(
            "./gradlew -PkissActiveVersion=v1_21_5 :buildSrc:test :spotlessCheck",
            ci_workflow,
        )
        self.assertIn("'${{ matrix.gradle_task }}' -x spotlessCheck", ci_workflow)

    def test_release_dispatch_does_not_require_publication_credentials(self) -> None:
        workflow = (ROOT / ".github" / "workflows" / "release.yml").read_text(
            encoding="utf-8"
        )
        for step_name in (
            "Validate publication configuration",
            "Read-only platform preflight",
        ):
            step = workflow.split(f"- name: {step_name}", 1)[1].split("- name:", 1)[0]
            self.assertIn("if: github.event_name == 'push'", step)

    def test_release_requires_only_curseforge_author_token(self) -> None:
        workflow = (ROOT / ".github" / "workflows" / "release.yml").read_text(
            encoding="utf-8"
        )
        transaction = (
            ROOT / "scripts" / "release_support" / "transaction.py"
        ).read_text(encoding="utf-8")
        self.assertNotIn("CURSEFORGE_API_KEY", workflow)
        self.assertNotIn("CURSEFORGE_API_KEY", transaction)
        self.assertIn("Warn about manual CurseForge conflict check", workflow)

    def test_loader_builds_use_named_version_capabilities(self) -> None:
        for relative_path in (
            "common/build.gradle",
            "fabric/build.gradle",
            "neoforge/build.gradle",
        ):
            build_script = (ROOT / relative_path).read_text(encoding="utf-8")
            with self.subTest(build_script=relative_path):
                self.assertIn("capabilities", build_script)
                self.assertNotIn("minecraft_version.startsWith", build_script)
                self.assertNotIn("minecraft_version in", build_script)
                self.assertNotIn("minecraft_version ==", build_script)

    def test_legacy_neoforge_gametests_are_scanned_but_not_published(self) -> None:
        build_script = (ROOT / "neoforge" / "build.gradle").read_text(
            encoding="utf-8"
        )
        self.assertIn("java.srcDir('src/gametest-annotation/java')", build_script)
        self.assertIn("exclude 'com/kissmod/gametest/**'", build_script)
        game_test = (
            ROOT
            / "neoforge"
            / "src"
            / "gametest-annotation"
            / "java"
            / "com"
            / "kissmod"
            / "gametest"
            / "KissGameTest.java"
        ).read_text(encoding="utf-8")
        self.assertIn("@GameTestHolder", game_test)

    def test_legacy_neoforge_metadata_uses_loader_expected_filename(self) -> None:
        build_script = (ROOT / "neoforge" / "build.gradle").read_text(
            encoding="utf-8"
        )
        self.assertIn("if (legacyMetadataFileName)", build_script)
        self.assertIn("name = 'mods.toml'", build_script)
        self.assertIn("if (legacyDependencyMetadata)", build_script)

    def test_gradle_active_version_and_formatting_contract(self) -> None:
        settings = (ROOT / "settings.gradle").read_text(encoding="utf-8")
        root_build = (ROOT / "build.gradle").read_text(encoding="utf-8")
        gradle_properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")

        self.assertIn("gradleProperty('kissActiveVersion')", settings)
        self.assertIn("!requestedVersionIds.isEmpty()", settings)
        self.assertIn("rootProject.tasks.named('spotlessCheck')", root_build)
        self.assertIn("'common/src/**/*.java'", root_build)
        self.assertNotIn("apply plugin: 'com.diffplug.spotless'", root_build)
        self.assertIn("org.gradle.configuration-cache=true", gradle_properties)

    def test_archive_tasks_do_not_read_dynamic_properties_at_execution_time(self) -> None:
        common_build = (
            ROOT / "buildSrc" / "src" / "main" / "groovy" / "multiloader-common.gradle"
        ).read_text(encoding="utf-8")

        self.assertIn("def modName = project.property('mod_name').toString()", common_build)
        self.assertIn('rename { "${it}_${modName}" }', common_build)
        self.assertNotIn('rename { "${it}_${mod_name}" }', common_build)

    def test_fabric_metadata_matches_server_and_dependency_release_contract(
        self,
    ) -> None:
        metadata = (
            ROOT / "fabric" / "src" / "main" / "resources" / "fabric.mod.json"
        ).read_text(encoding="utf-8")
        self.assertIn('"environment": "server"', metadata)
        self.assertIn('"fabric-api": "*"', metadata)
        self.assertNotIn("another-mod", metadata)
        self.assertIn("jiazengp/kiss-fabric", metadata)
        properties = (ROOT / "gradle.properties").read_text(encoding="utf-8")
        license_text = (ROOT / "LICENSE").read_text(encoding="utf-8")
        self.assertIn("license=MIT", properties)
        self.assertTrue(license_text.startswith("MIT License"))

    def test_unknown_version_id_fails(self) -> None:
        result = self.run_script("matrix", "--version-id", "missing_version")
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Unknown version id", result.stderr)


if __name__ == "__main__":
    unittest.main()
