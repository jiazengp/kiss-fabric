# Changelog

All notable changes to KissMod are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses semantic versioning for mod releases.

## [Unreleased]

### Added

- Nothing yet.

## [2.0.0-alpha.1] - 2026-07-18

### Added

- NeoForge support alongside Fabric across the manifest-enabled Minecraft 1.20.x, 1.21.x, and 26.x targets.
- Loader-native server configuration with validated, immutable runtime snapshots.
- Configurable friendly entity particles, including exact modded entity IDs and entity type tags.
- `kissmod.command` and `kissmod.trigger` permissions, with allow-all defaults on both loaders.
- Manifest-driven CI, GameTests, artifact validation, and transactional publishing to GitHub Releases, Modrinth, and CurseForge.

### Changed

- Friendly entity particles are enabled by default and restore the full vanilla friendly entity set from 1.4.2.
- Heart particles use each target's bounding-box height and appear above friendly entities rather than the triggering player.
- Rapid crouching can trigger friendly-entity particles when no other player is nearby; holding crouch still does not count as repeated input.
- Sneak particle decay, trigger timing, radii, view angle, and friendly entity matching are configurable consistently on both loaders.
- The `/kiss` player suggestions exclude the command source.
- Release metadata now uses one loader-scoped dependency model for Modrinth and CurseForge.
- CurseForge publication now needs only the author Upload API token; existing-file conflict checks and ambiguous upload verification are documented manual steps.
- Published mod metadata now consistently identifies KissMod as an MIT-licensed server-side mod.

### Fixed

- Fake players, spectators, and invisible players no longer participate in automatic kiss interactions.
- Fabric Permissions API no longer pulls an incompatible transitive Fabric API into older Minecraft targets.
- NeoForge 1.20.4 and 1.20.6 now publish the metadata filename expected by their respective FML versions, so both development and packaged runtimes discover KissMod correctly.
- NeoForge 1.21.1 now includes and executes its annotation-based GameTests instead of silently starting with zero tests.
- Repeated GameTest runs start from clean generated configs, and source JAR tasks support Gradle's configuration cache.

### Migration notes

- Fabric configuration moved from the legacy JSON file to `config/kissmod/config.yml`; NeoForge uses `kissmod-server.toml`. Existing JSON values must be migrated manually.
- The supported Minecraft and loader combinations are now defined by `versions.json`; Forge, Quilt, Minecraft 1.19, and the unpublished 26.3 target are not part of this release.

## [1.0.3] - 2025-12-10

### Added

- Support for Minecraft 1.21.11 and 1.21.9.

## [1.0.2] - 2025-10-15

### Added

- Support for Minecraft 1.21.6.

## [1.0.0] - 2025-08-01

### Added

- Initial KissMod release.
