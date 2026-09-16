# Changelog

All notable changes to KissMod are documented in this file.

Release notes for the legacy `v1` branch remain available in
[GitHub Releases](https://github.com/jiazengp/kiss-fabric/releases).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses semantic versioning for mod releases.

## [2.0.0-alpha.3] - 2026-09-16

### Added

- Minecraft 26.3 is now supported for Fabric and NeoForge servers.

### Fixed

- The sneak-trigger burst honors `maxSneakParticles` instead of emitting one heart above the configured cap, and is skipped entirely when the cap is `0`.
- The `/kiss` cooldown is no longer reset by disconnecting and reconnecting; expired entries are now pruned by elapsed time instead of by online status.

## [2.0.0-alpha.2] - 2026-07-19

### Added

- Fabric and NeoForge servers can now import supported settings from their v1 config files automatically.

### Changed

- Existing config values are kept; new fields are filled with their defaults.

## [2.0.0-alpha.1] - 2026-07-19

### Added

- Friendly entity types can be customized, including entities added by other mods and entity type tags.
- `kissmod.command` and `kissmod.trigger` permissions, both enabled by default.

### Changed

- Hearts appear over friendly entities at a height suited to each entity.
- Rapid crouching can now trigger hearts over friendly entities even when no other player is nearby. Holding crouch still doesn't count.
- Sneak particle decay, trigger timing, radii, view angle, and friendly entity matching are configurable.
- `/kiss` suggestions no longer include yourself.

### Fixed

- Fake players, spectators, and invisible players no longer participate in automatic kiss interactions.

### Migration notes

- Fabric config moved from the old JSON file to `config/kissmod/config.yml`. Existing JSON values need to be migrated manually.
