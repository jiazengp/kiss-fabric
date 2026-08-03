# Contributing to KissMod

Thank you for helping improve KissMod. This document covers the development workflow, multi-version architecture, validation requirements, and release process. User-facing commands and configuration examples remain in the [README](README.md).

## Development setup

KissMod targets Fabric and NeoForge across multiple Minecraft versions. Use the Gradle wrapper included in the repository; a separate Gradle installation is not required.

- JDK 21 is required for Minecraft 1.x targets.
- JDK 25 is required for Minecraft 26.x targets.
- Python 3 is required for manifest, release, and artifact validation scripts.

Use `./gradlew.bat` on Windows and `./gradlew` on Unix-like systems. The examples below use the Unix-like form; substitute the Windows wrapper when appropriate.

## Repository and version model

`versions.json` and each `versions/<versionId>/gradle.properties` file are the source of truth for supported Minecraft versions, Java versions, dependency coordinates, and loader availability.

- `common/` contains loader-neutral gameplay behavior, configuration contracts, and platform service interfaces.
- `fabric/` and `neoforge/` adapt loader APIs, lifecycle events, configuration formats, and compatibility boundaries.
- `versions/` contains version metadata, not copied source trees.
- `buildSrc/` creates nested projects such as `:v1_21_5:common`, `:v1_21_5:fabric`, and `:v1_21_5:neoforge`, and owns named API boundaries in `VersionCapabilities`.
- Build outputs are isolated under `build/<versionId>/<loader>/`.

A loader project exists only when its `enable_<loader>` property is `true`. Check the manifest instead of assuming every Minecraft version supports both loaders.

Validate and inspect the current matrix with:

```shell
python scripts/manifest_matrix.py validate
python scripts/manifest_matrix.py matrix
```

## Making changes

Keep loader-neutral gameplay logic in `common/`. Loader projects should contain only API adaptation, lifecycle wiring, native configuration, and version compatibility code.

When changing a cross-loader platform capability, update the common service interface, both loader implementations, and both `META-INF/services` registrations together.

When adding or changing a configuration option, update all of the following:

1. `KissServerConfig`, `KissConfigData`, and `KissConfigDefaults` in `common/`.
2. Fabric's YAML model and snapshot conversion.
3. NeoForge's TOML specification and snapshot conversion.
4. Relevant tests and user-facing configuration documentation.

Keep version-specific API differences in the compatibility source sets selected by the loader build scripts. Add an API boundary once in `buildSrc/src/main/groovy/com/kissmod/build/VersionCapabilities.groovy`; loader scripts should consume a named capability instead of comparing Minecraft version strings themselves. Avoid runtime Minecraft-version checks in gameplay code.

Compatibility source sets are intentional when an imported type, method signature, annotation, or build tool changes. Do not combine structurally different GameTest or legacy NeoGradle paths merely to reduce the file count. Small shared behavior should stay in the regular source set, with the compatibility class limited to the API call that differs.

Stonecutter and ModStitch are not part of the production build at present. Stonecutter's standard preprocessing model expects version-owned source trees, while KissMod deliberately compiles one shared `common/`, `fabric/`, and `neoforge/` tree for all manifest rows. A project-tree-only integration would add configuration cost without preprocessing the real sources. ModStitch can cover the modern loaders, but it does not currently replace the proven legacy NeoGradle path used by Minecraft 1.20.x. Re-evaluate them only as a complete source-ownership migration with the full manifest, GameTest, dev-server, and release contracts preserved; do not layer either plugin over the current build as a partial abstraction.

## Building and testing

Build each affected version and loader boundary. For example:

```shell
./gradlew :v1_21_5:fabric:build
./gradlew :v1_21_5:neoforge:build
```

For a smaller IDE import or for unqualified local tasks, set the optional `kissActiveVersion` Gradle property. Explicit version-qualified task paths always take precedence, so this setting cannot redirect a CI or release matrix task:

```shell
./gradlew -PkissActiveVersion=v1_21_5 projects
./gradlew -PkissActiveVersion=v1_21_5 check
```

Configure the same property in the IDE's Gradle VM/options when only one Minecraft version is being developed. Leave it unset for repository-wide tasks.

Configuration cache is enabled for focused builds and GameTest tasks. If a loader plugin regression needs diagnosis, temporarily disable it with `--no-configuration-cache` and report the affected version/loader task.

Run the repository checks before submitting a change:

```shell
./gradlew check
```

The Python unit-test suite covers the manifest, release, and artifact tooling:

```shell
python -m unittest discover -s tests -v
```

Do not use a local root `./gradlew build` as the normal feedback loop: it executes every enabled row in one workspace and has a long GameTest tail. Use focused version/loader tasks while developing. CI expands the manifest into independent matrix jobs and allows all 25 current rows to run concurrently; the GitHub account's runner limit may queue excess rows. Release publication starts only after the complete parallel build matrix succeeds.

CI has one preflight job for manifest validation and repository-wide formatting. The version/loader matrix starts only after preflight succeeds and excludes the already-completed Spotless task from each row.

`check` includes the repository-wide formatting validation once. Additional useful tasks are:

- `./gradlew spotlessCheck` verifies formatting without changing files.
- `./gradlew spotlessApply` applies the configured formatting rules.

Only one deployable main JAR should be produced for each enabled row. Sources, development, and other auxiliary JARs are not release artifacts.

For a server startup check, run the appropriate task and wait for Minecraft's `Done (...)` readiness marker:

```shell
./gradlew :v1_21_5:fabric:runServer
./gradlew :v1_21_5:neoforge:runServer
```

The run directories are isolated under `runs/<versionId>/<loader>/server`. Stop a verified server cleanly with `stop`.

GameTest tasks are loader-specific:

```shell
./gradlew :v1_21_5:fabric:runGameTest
./gradlew :v1_21_5:neoforge:runGameTestServer
python scripts/verify_gametest.py <path-to-latest.log>
```

The verification script ensures that at least one GameTest ran and that the run passed; a zero-test run must not be treated as success.

## Adding a Minecraft version

1. Add the ordered version ID to `versions.json`.
2. Add `versions/<versionId>/gradle.properties` with Minecraft, Java, mappings, and loader coordinates.
3. Enable only loaders that have valid coordinates and a working compatibility boundary.
4. Check `VersionCapabilities` and add or adjust a named boundary only when the new Minecraft API requires it.
5. Extend `VersionCapabilitiesTest` for every new boundary or exact-version exception.
6. Run the manifest and buildSrc tests: `python -m unittest discover -s tests -v` and `./gradlew -PkissActiveVersion=v1_21_5 :buildSrc:test`.
7. Build every enabled loader row for the new version.
8. Start every enabled development server and verify its readiness marker.

Do not duplicate shared source code under `versions/`.

## Changelog

`CHANGELOG.md` follows Keep a Changelog conventions and is the single release-note source for GitHub, Modrinth, and CurseForge.

Keep ongoing changes under `[Unreleased]`. Before tagging, create a non-empty section with an exact version and date:

```markdown
## [1.2.0] - 2026-07-18
```

Duplicate version sections, invalid dates, missing sections, and empty sections cause release planning to fail.

## Releases

Release targets come from the same manifest used by local builds and CI. An unscoped tag publishes every enabled version and loader; a scoped tag publishes only the selected subset.

Supported tag forms include:

- `v1.2.0` — all enabled Minecraft versions and loaders.
- `v1.2.0+mc26.X` — all enabled 26.x rows.
- `v1.2.0+mc1.21.1-neoforge` — NeoForge for Minecraft 1.21.1 only.
- `v1.2.0-alpha.1+mc26.X-fabric` — an Alpha Fabric release for enabled 26.x targets.

Only `alpha.N` and `beta.N` prerelease suffixes are supported. A tag without a prerelease suffix is a release version. The version before `+mc...` must exactly match the root `gradle.properties` version.

Before pushing a release tag:

1. Set the root `version` to the exact tag version without the leading `v` or release scope.
2. Move the relevant notes from `[Unreleased]` into the matching dated changelog section.
3. Run `python scripts/release_plan.py --tag <tag> --dry-run` and review every selected row.
4. Run the required builds and checks.
5. Push the tag to start the release workflow.

`workflow_dispatch` is dry-run only: it validates the plan, configuration, platform conflicts, and selected builds without publishing.

### Repository configuration

The release workflow expects these GitHub repository variables:

- `MODRINTH_PROJECT_ID`
- `CURSEFORGE_PROJECT_ID`

It also expects these repository secrets:

- `CURSEFORGE_TOKEN`, the CurseForge author Upload API token.
- `MODRINTH_TOKEN`, or the legacy-compatible `MODRINTH_AUTH_TOKEN`.

Cross-platform related-mod declarations belong in `release-metadata.json`. Dependencies are loader-scoped and use explicit Modrinth and CurseForge aliases. Fabric API is required only for Fabric release rows; NeoForge rows have no external mod dependency. Minecraft, Java, Fabric Loader, and NeoForge remain platform requirements in the JAR metadata rather than related-mod declarations.

All selected artifacts are built and validated before publication begins. The transaction creates or updates one GitHub Release for the tag and attaches every selected JAR plus `SHA256SUMS.txt`; it then publishes the matching Modrinth and CurseForge files. GitHub releases and Modrinth versions can be replaced with compensating rollback. CurseForge cannot replace, delete, or query uploaded binaries through its author token. Before pushing a release tag, manually inspect the project and delete files matching any selected external version or artifact name. If an upload response times out or fails ambiguously, inspect CurseForge manually before retrying.

If publication fails, inspect the uploaded `publication-journal-*` artifact for completed operations, rollback failures, and manual recovery instructions.

## Pull request checklist

- The manifest validation passes.
- Every affected version and loader builds successfully.
- Relevant development servers or GameTests have been exercised.
- Fabric and NeoForge behavior remains consistent where both loaders are enabled.
- User-facing changes are documented in the README and changelog.
- Generated build outputs, run directories, logs, caches, and credentials are not committed.
