# KissMod

## Description

A lightweight server-side mod that spawns heart particles above the player who rapidly crouches near others — a simple gesture of friendliness.

![Cover](https://cdn.modrinth.com/data/cached_images/229f853e39181c31096cbc8252a28a401b63e409.png)

## Command

- `/kiss <player>`: Allowing you to kiss other players with this command (The command can be used once every 10 seconds per player by default. The kissed player will receive a notification and heart particle effects will appear above their head.
  )

KissMod exposes `kissmod.command` for `/kiss` and `kissmod.trigger` for sneak-triggered particles. Both permissions allow all players by default and can be overridden by a compatible permissions provider.

## Configuration

KissMod is server-side. Edit the server config and restart the server to apply changes.

- Fabric config path: `config/kissmod/config.yml`
- NeoForge server config: `kissmod-server.toml` in the loader's server config location

Existing values are preserved when new configuration fields are added; missing fields are written
with their current defaults. On the first upgrade from KissMod v1, Fabric imports
`config/kissmod.json` when the new YAML file does not exist, while NeoForge imports
`config/kissmod-common.toml` when the new server configuration still contains only defaults. Legacy
files are removed only after the new configuration has been written successfully. Renamed settings
such as `commandCooldown`, `sneakTimeWindow`, `maxViewAngleDegree`, and `mobTriggerRadius` are
mapped to their current equivalents. A current NeoForge configuration with custom values always
takes precedence over the legacy file.

### Friendly entity particles

Friendly entity particles are enabled by default for the vanilla passive and friendly entities listed in the generated config. You can disable the feature or replace the list with exact entity type IDs and entity type tags.

KissMod matches `friendlyEntityTypes` against the entity registry ID, so modded entities work without a KissMod-side integration as long as that mod is installed on the server.

#### Fabric YAML example

```yaml
sneakParticleDecay: 2
sneakTriggerCount: 3
sneakTriggerWindowTicks: 40
sneakPreviewRadius: 6.0
enableEntityParticles: true
entityParticleRadius: 8.0
heartParticleHeightOffset: 0.35
friendlyEntityTypes:
  - minecraft:villager
  - minecraft:cow
  - minecraft:sheep
  - minecraft:pig
  - minecraft:chicken
  - minecraft:horse
  - minecraft:donkey
  - minecraft:mule
  - minecraft:llama
  - minecraft:cat
  - minecraft:wolf
friendlyEntityTags: []
```

#### NeoForge TOML example

```toml
[sneak]
sneakParticleDecay = 2
sneakTriggerCount = 3
sneakTriggerWindowTicks = 40
sneakPreviewRadius = 6.0

[entityParticles]
enableEntityParticles = true
entityParticleRadius = 8.0
heartParticleHeightOffset = 0.35
friendlyEntityTypes = [
  "minecraft:villager",
  "minecraft:cow",
  "minecraft:sheep",
  "minecraft:pig",
  "minecraft:chicken",
  "minecraft:horse",
  "minecraft:donkey",
  "minecraft:mule",
  "minecraft:llama",
  "minecraft:cat",
  "minecraft:wolf"
]
friendlyEntityTags = []
```

## Contributing

Development setup, multi-version build instructions, testing requirements, and the release process are documented in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](https://github.com/jiazengp/kiss-fabric/blob/main/LICENSE)
