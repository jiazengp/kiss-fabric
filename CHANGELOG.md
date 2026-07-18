# Changelog

All notable changes to KissMod are documented in this file.

Release notes for the legacy `v1` branch remain available in
[GitHub Releases](https://github.com/jiazengp/kiss-fabric/releases).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project uses semantic versioning for mod releases.

## [Unreleased]

### Added

- Nothing yet.

## [2.0.0-alpha.1] - 2026-07-19

### Added

- Friendly entity types can be customized, including entities added by other mods and entity type tags.
- `kissmod.command` and `kissmod.trigger` permissions, both enabled by default.

### Changed

- Heart particles appear above friendly entities at a height appropriate for each entity.
- Rapid crouching can trigger friendly-entity particles when no other player is nearby; holding crouch still does not count as repeated input.
- Sneak particle decay, trigger timing, radii, view angle, and friendly entity matching are configurable.
- The `/kiss` player suggestions no longer include the player using the command.

### Fixed

- Fake players, spectators, and invisible players no longer participate in automatic kiss interactions.

### Migration notes

- Fabric configuration moved from the legacy JSON file to `config/kissmod/config.yml`. Existing JSON values must be migrated manually.
