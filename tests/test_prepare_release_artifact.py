import pathlib
import tempfile
import unittest
import zipfile

import sys


ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from prepare_release_artifact import prepare, select_deployable_jar  # noqa: E402


class PrepareReleaseArtifactTests(unittest.TestCase):
    def create_jar(self, path: pathlib.Path, metadata: str) -> None:
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr(metadata, "metadata")

    def test_selects_main_jar_and_writes_checksum(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            source = root / "libs"
            output = root / "out"
            source.mkdir()
            self.create_jar(source / "kissmod.jar", "fabric.mod.json")
            self.create_jar(source / "kissmod-sources.jar", "fabric.mod.json")

            result = prepare(
                source, output, "kissmod-1.2.0+mc1.21.1-fabric.jar", "fabric"
            )

            self.assertTrue(pathlib.Path(result["artifact"]).is_file())
            self.assertTrue(pathlib.Path(result["checksum"]).is_file())
            self.assertEqual(len(result["sha256"]), 64)

    def test_rejects_multiple_main_jars(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            self.create_jar(root / "one.jar", "fabric.mod.json")
            self.create_jar(root / "two.jar", "fabric.mod.json")
            with self.assertRaisesRegex(ValueError, "Expected exactly one"):
                select_deployable_jar(root)

    def test_rejects_wrong_loader_metadata(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = pathlib.Path(directory)
            source = root / "libs"
            source.mkdir()
            self.create_jar(source / "kissmod.jar", "fabric.mod.json")
            with self.assertRaisesRegex(ValueError, "neoforge.mods.toml"):
                prepare(source, root / "out", "kissmod.jar", "neoforge")


if __name__ == "__main__":
    unittest.main()
