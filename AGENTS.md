# KissMod — Agent Instructions

## Project truth

KissMod is a server-only Minecraft mod that provides the `/kiss` command and emits heart particles when nearby players crouch rapidly. It targets Fabric and NeoForge; Forge is not supported.

- Group: `com.kissmod`
- Mod ID: `kissmod`
- Supported Minecraft versions and loaders: `versions.json` plus each `versions/<versionId>/gradle.properties`
- Java: 21 for Minecraft 1.x targets, 25 for Minecraft 26.x targets (enforced by `scripts/manifest_matrix.py`)
- Do not describe the repository as targeting one fixed Minecraft version. The manifest is the source of truth.

## Repository layout

| Path | Responsibility |
|---|---|
| `common/` | Loader-neutral behavior, config contract/data, platform service interfaces, common mixins |
| `fabric/` | Fabric entry point, YAML config implementation, Fabric services/mixins, compatibility and GameTest source sets |
| `neoforge/` | NeoForge entry points, TOML config, NeoForge services/mixins, compatibility and GameTest source sets |
| `versions/` | Per-version dependency coordinates, Java version, and loader enablement |
| `versions.json` | Ordered list of supported version IDs |
| `release-metadata.json` | Loader-scoped environment and cross-platform mod dependency aliases |
| `buildSrc/` | Manifest loader, named version capabilities, and shared Gradle convention scripts |
| `scripts/manifest_matrix.py` | Manifest validation and CI matrix generation |
| `scripts/release_plan.py` | Strict release-tag parsing, changelog validation, and release matrix generation |
| `scripts/prepare_release_artifact.py` | Deployable JAR selection, metadata validation, renaming, and checksums |
| `scripts/release_transaction.py` | GitHub/Modrinth/CurseForge publication and compensating rollback |
| `scripts/release_support/` | Thin platform adapters and transaction coordination behind the release CLI; HTTPX owns transport and multipart handling |
| `scripts/verify_gametest.py` | Verifies that a GameTest log ran and passed at least one test |

Gradle exposes nested projects such as `:v1_21_5:common`, `:v1_21_5:fabric`, and `:v1_21_5:neoforge`. These projects reuse the source directories above; do not create copied source trees under `versions/`.

## Multi-version rules

- Read `versions.json` and the selected version properties before making version-specific assumptions.
- A loader project exists only when its `enable_<loader>` property is `true`. Never invent or invoke a disabled row (for example, NeoForge is not enabled for every version).
- Shared project metadata belongs in root `gradle.properties`; dependency coordinates and compatibility metadata belong in `versions/<versionId>/gradle.properties`.
- Any property used in resource templates must be loaded by `buildSrc/src/main/groovy/com/kissmod/build/VersionManifest.groovy` and included in `expandProps` in `buildSrc/src/main/groovy/multiloader-common.gradle`. Missing expansion values can leave invalid metadata without an obvious compile failure.
- `minecraft_version` is the dependency coordinate. When Fabric Loader reports a different normalized runtime version, use optional `fabric_minecraft_version` for `fabric.mod.json` compatibility instead of changing the dependency coordinate.
- Keep version API differences in the existing compatibility source sets selected by `fabric/build.gradle` or `neoforge/build.gradle`. Define Minecraft API boundaries once in `buildSrc/src/main/groovy/com/kissmod/build/VersionCapabilities.groovy`; loader build scripts consume named capabilities and must not maintain their own version-string lists. Do not spread version checks through gameplay code.
- Existing NeoForge boundaries are intentional: legacy Gradle/API sources for 1.20.x, the `META-INF/mods.toml` filename only for 1.20.2/1.20.4, legacy dependency metadata syntax only for 1.20.2, and separate tick, FML, GameTest, and structure-converter sources where APIs differ.
- Stonecutter and ModStitch are intentionally not production dependencies. The current shared external source trees are not processed by Stonecutter's standard version-node model, and ModStitch does not replace the legacy NeoGradle boundary. Do not add a project-tree-only or modern-only partial integration; any future migration must own the source trees and preserve the complete manifest task, GameTest, dev-server, and release contracts.
- When adding a version, update `versions.json`, add `versions/<versionId>/gradle.properties`, validate the manifest, build every enabled loader row, and start each enabled server.

## Build and verification

On Windows use `./gradlew.bat`; on Unix-like systems use `./gradlew`.

```text
python scripts/manifest_matrix.py validate
python scripts/manifest_matrix.py matrix
./gradlew.bat :v1_21_5:fabric:build
./gradlew.bat :v1_21_5:neoforge:build
./gradlew.bat build
```

- A focused change must build every affected version/loader boundary, not only the newest version.
- A full release/matrix verification must cover every row emitted by `python scripts/manifest_matrix.py matrix`.
- Deployable artifacts are isolated at `build/<versionId>/<loader>/libs/`. Confirm each enabled row produces a main JAR; do not count `-sources`, `-dev`, or other auxiliary JARs as the deliverable.
- Server startup tasks are `:<versionId>:fabric:runServer` and `:<versionId>:neoforge:runServer`. A successful dev-server check requires the Minecraft `Done (...)` readiness marker in the log, not merely a live Gradle process.
- Server run directories are isolated at `runs/<versionId>/<loader>/server`, and `runServer` writes `eula=true`; do not reuse one version's world/run directory for another.
- Stop a verified server cleanly with `stop`. Some NeoForge Gradle integrations may return a non-zero process status after stdin shutdown; judge startup from the readiness marker and inspect the log for a pre-readiness crash.
- GameTest tasks are `:<versionId>:fabric:runGameTest` and `:<versionId>:neoforge:runGameTestServer`. When validating logs in automation, run `python scripts/verify_gametest.py <log>` so a zero-test run cannot pass silently.
- `./gradlew.bat check` includes Spotless and SpotBugs. Use `spotlessApply` only when formatting changes are intended, and keep `.gradle` files CRLF as defined by `.gitattributes`.

## Release contracts

- Release tags are `v<semver>`, optionally followed by `+mc<exact-or-family.X>` and `-fabric` or `-neoforge`. Only `alpha.N` and `beta.N` prerelease suffixes are supported.
- The tag version must exactly match root `gradle.properties`, and `CHANGELOG.md` must contain a non-empty `## [<version>] - YYYY-MM-DD` section.
- A tag without a scope publishes every enabled manifest row. Scoped tags are complete releases for that version; use a new patch version rather than publishing omitted rows later under the same base version.
- GitHub repository variables are `MODRINTH_PROJECT_ID` and `CURSEFORGE_PROJECT_ID`; secrets are `CURSEFORGE_TOKEN` and either `MODRINTH_TOKEN` or the legacy `MODRINTH_AUTH_TOKEN`.
- No publication may begin until all selected builds have succeeded and every row has exactly one validated main JAR.
- GitHub and Modrinth replacements must be backed up and restored through the release transaction on failure. Never replace the transaction with independent matrix publication jobs.
- Prefer maintained libraries for protocol-level work. Platform adapters should contain only the API operations needed by the transaction; do not reimplement HTTP transport or multipart encoding.
- Keep cross-platform related-mod declarations in `release-metadata.json`. Use generic dependency types with explicit Modrinth and CurseForge aliases; do not hardcode project IDs or slugs in platform adapters.
- CurseForge author APIs cannot replace, delete, or query file binaries. Manually remove matching CurseForge versions before pushing a release tag, and manually inspect ambiguous upload failures before retrying.
- `workflow_dispatch` is dry-run only. A pushed valid tag is the only automatic publication trigger.
- `release-recovery.json` is a sanitized operation journal. Keep it free of tokens and upload it with `if: always()` for recovery diagnostics.

## Architecture contracts

### Platform services

- `common/src/main/java/com/kissmod/platform/services/` owns loader-neutral interfaces (`IPlatformHelper`, `IKissEventHelper`).
- Fabric and NeoForge implement each service under their platform source tree and register it in the matching `META-INF/services/<fully-qualified-interface>` file.
- `Services.PLATFORM` and `Services.KISS_EVENT` are the shared access points.
- For a cross-loader capability, change the common interface, both loader implementations, and both service registration files together.

### Configuration

- `KissServerConfig` is the read-only common contract; `KissConfigData` is the immutable runtime snapshot and owns shared defaults through `KissConfigDefaults`.
- Fabric reads YAML through `KissConfig`/`KissConfigManager` and publishes a snapshot with `KissConfigs.update(...)`.
- NeoForge defines TOML through `NeoForgeKissConfig`, registers its `ModConfigSpec`, and publishes a snapshot when the server is about to start.
- Runtime code reads `KissConfigs.server()` when needed. Do not cache the returned snapshot across config reloads.
- Adding a config option requires updating the common interface/data/defaults, Fabric YAML model/conversion, NeoForge TOML spec/conversion, and relevant tests or documentation.

### Entry points and server-only scope

- Fabric entry point: `fabric/src/main/java/com/kissmod/KissMod.java` (`ModInitializer`).
- NeoForge has legacy and modern entry points under `neoforge/src/legacy/` and `neoforge/src/modern/`; Gradle selects the correct one.
- There are currently no active mixins. Add loader metadata and annotation-processing dependencies only when a concrete mixin is introduced.
- Keep the mod server-side. Do not add client entry points, client-only classes, rendering code, or client run requirements.

## Coding and change discipline

- Use `Constants.LOG` (SLF4J), never `System.out`.
- Keep new code and comments in English unless editing an established Chinese-facing message or document.
- Reuse `KissUtils.DARK_RED`, `YELLOW`, `LIGHT_PURPLE`, and `GRAY` for chat formatting.
- Preserve loader-neutral gameplay behavior in `common/`; loader packages should adapt APIs, lifecycle, configuration, and event wiring.
- Do not remove an apparently redundant compatibility branch until all versions that select it have been built and started.
- Do not commit generated content from `build/`, `runs/`, Gradle caches, logs, or Python `__pycache__/`.
- Before editing, inspect the working tree and preserve unrelated user changes. Do not stage or commit unless explicitly requested.

## Key dependencies

- Fabric API and Fabric Loader for Fabric targets.
- NeoForge for NeoForge targets; 1.20.x uses the legacy NeoGradle path while newer targets use ModDevGradle.
- ConfigLib core/YAML plus SnakeYAML Engine are bundled into the Fabric artifact and compile-only in `common`.
