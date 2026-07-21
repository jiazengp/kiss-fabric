# KissMod

A tiny server-side mod: sneak near another player a few times and hearts pop up overhead — a small, friendly gesture.

![Cover](https://cdn.modrinth.com/data/cached_images/229f853e39181c31096cbc8252a28a401b63e409.png)

## Command

- `/kiss <player>` — send a kiss to another player. Each player can use it once every 10 seconds by default. The kissed player gets a heads-up and a little burst of hearts above their head.

Two permissions are exposed by default, both granted to all players:

- `kissmod.command` for `/kiss`
- `kissmod.trigger` for the sneak-triggered hearts

Override either with your permissions mod if you want to change who can use them.

## Configuration

KissMod is server-side — edit the server config and restart to apply changes.

- Fabric: `config/kissmod/config.yml`
- NeoForge: `kissmod-server.toml` in the loader's server config location

The config keeps your existing values across upgrades and fills new fields with defaults. Upgrading from KissMod v1? Your old `kissmod.json` (Fabric) or `kissmod-common.toml` (NeoForge) is imported the first time, then removed once the new file is written. A current config with custom values is never overwritten.

### Hearts for friendly mobs

Hearts trigger the same way as with players: sneak rapidly nearby. By default, this also works for vanilla friendly mobs like villagers, cows, and wolves. The difference is that the hearts appear over the mob's head, not the player's.

You can turn this off, or customize the target list with any entity type IDs or tags you like. Modded mobs are also supported as long as the mod is on the server — no extra integration needed.

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

If you'd like to build or release KissMod, see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](https://github.com/jiazengp/kiss-fabric/blob/main/LICENSE)