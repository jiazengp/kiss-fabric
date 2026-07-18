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
  - touhou_little_maid:maid
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
  "minecraft:wolf",
  "touhou_little_maid:maid"
]
friendlyEntityTags = []
```

Touhou Little Maid registers its maid entity as `touhou_little_maid:maid`, so adding that ID to `friendlyEntityTypes` makes the maid eligible for KissMod's friendly-entity particle logic when Touhou Little Maid is present on the server.

## Contributing

Development setup, multi-version build instructions, testing requirements, and the release process are documented in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](https://github.com/jiazengp/kiss-fabric/blob/main/LICENSE)
