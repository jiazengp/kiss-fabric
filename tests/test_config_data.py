import pathlib
import subprocess
import tempfile
import textwrap
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
CONFIG_SOURCE = ROOT / "common" / "src" / "main" / "java" / "com" / "kissmod" / "config"


class ConfigDataTests(unittest.TestCase):
    def test_common_snapshot_normalizes_loader_input(self) -> None:
        harness = textwrap.dedent(
            """
            import com.kissmod.config.KissConfigData;
            import com.kissmod.config.KissConfigDefaults;
            import java.util.Arrays;

            public final class ConfigDataHarness {
              public static void main(String[] args) {
                KissConfigData config = new KissConfigData(
                    true,
                    -1,
                    null,
                    null,
                    null,
                    null,
                    100,
                    -1,
                    0,
                    999,
                    -1.0,
                    Double.NaN,
                    999.0,
                    true,
                    0.0,
                    -2.0,
                    Arrays.asList(" minecraft:villager ", "", null, "minecraft:villager"),
                    null);

                require(config.commandCooldownSeconds() == KissConfigDefaults.MIN_COMMAND_COOLDOWN_SECONDS);
                require(config.maxSneakParticles() == KissConfigDefaults.MAX_SNEAK_PARTICLES_LIMIT);
                require(config.sneakParticleDecay()
                    == KissConfigDefaults.MIN_SNEAK_PARTICLE_DECAY);
                require(config.sneakTriggerCount() == KissConfigDefaults.MIN_SNEAK_TRIGGER_COUNT);
                require(config.sneakTriggerWindowTicks()
                    == KissConfigDefaults.MAX_SNEAK_TRIGGER_WINDOW_TICKS);
                require(config.sneakPreviewRadius()
                    == KissConfigDefaults.MIN_SNEAK_PREVIEW_RADIUS);
                require(config.sneakTriggerRadius() == KissConfigDefaults.SNEAK_TRIGGER_RADIUS);
                require(config.maxViewAngle() == KissConfigDefaults.MAX_VIEW_ANGLE_LIMIT);
                require(config.entityParticleRadius() == KissConfigDefaults.MIN_ENTITY_PARTICLE_RADIUS);
                require(config.heartParticleHeightOffset()
                    == KissConfigDefaults.MIN_HEART_PARTICLE_HEIGHT_OFFSET);
                require(config.kissMessage().equals(KissConfigDefaults.KISS_MESSAGE));
                require(config.friendlyEntityTypes().equals(
                    java.util.List.of("minecraft:villager")));
                require(config.friendlyEntityTags().equals(KissConfigDefaults.FRIENDLY_ENTITY_TAGS));

                KissConfigData defaults = KissConfigData.defaults();
                require(defaults.sneakParticleDecay() == KissConfigDefaults.SNEAK_PARTICLE_DECAY);
                require(defaults.enableEntityParticles());
                require(defaults.friendlyEntityTypes().containsAll(java.util.List.of(
                    "minecraft:wolf",
                    "minecraft:camel",
                    "minecraft:sniffer",
                    "minecraft:iron_golem",
                    "minecraft:axolotl",
                    "minecraft:dolphin")));
              }

              private static void require(boolean condition) {
                if (!condition) {
                  throw new AssertionError("Configuration normalization failed");
                }
              }
            }
            """
        )

        with tempfile.TemporaryDirectory() as temp_dir:
            temp = pathlib.Path(temp_dir)
            harness_file = temp / "ConfigDataHarness.java"
            harness_file.write_text(harness, encoding="utf-8")
            sources = [
                CONFIG_SOURCE / "KissConfigDefaults.java",
                CONFIG_SOURCE / "KissServerConfig.java",
                CONFIG_SOURCE / "KissConfigData.java",
                CONFIG_SOURCE / "KissConfigs.java",
                harness_file,
            ]
            compile_result = subprocess.run(
                ["javac", "-d", str(temp), *(str(source) for source in sources)],
                cwd=ROOT,
                text=True,
                capture_output=True,
                check=False,
            )
            self.assertEqual(compile_result.returncode, 0, compile_result.stderr)

            run_result = subprocess.run(
                ["java", "-cp", str(temp), "ConfigDataHarness"],
                cwd=ROOT,
                text=True,
                capture_output=True,
                check=False,
            )
            self.assertEqual(run_result.returncode, 0, run_result.stderr)


if __name__ == "__main__":
    unittest.main()
