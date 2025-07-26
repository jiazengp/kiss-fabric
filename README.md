## Description

A lightweight server-side mod that spawns heart particles above the player who rapidly crouches near others — a simple gesture of friendliness.

![Cover](https://cdn.modrinth.com/data/cached_images/229f853e39181c31096cbc8252a28a401b63e409.png)

## Command

- `/kiss <player>`: Allowing you to kiss other players with this command (The command can be used once every 10 seconds per player by default. The kissed player will receive a notification and heart particle effects will appear above their head.
  )

## Default Config

`kissmod.json`

```jsonc
{
  // Maximum number of heart particles that can appear when sneaking (1–20)
  "maxSneakParticles": 6,

  // Vertical position offset for particles (in blocks)
  // 1.6 = Eye level for standing players
  "sneakParticleHeight": 1.6,

  // Message shown when receiving a kiss
  // %s = Automatically replaced by the kisser's username
  "kissMessage": "%s kissed you!",

  // Message shown to the sender after kissing someone
  // %s = Automatically replaced by the receiver's username
  "kissPromptMessage": "you kissed %s",

  // Cooldown between /kiss commands (in seconds)
  // Prevents command spamming (0 = no cooldown)
  "commandCooldown": 10,

  // Enable or disable the /kiss command entirely
  "enableKissCommand": true,

  // Error message shown when trying to kiss yourself
  "selfKissErrorMessage": "You can't kiss yourself!",

  // Error message shown when using /kiss before cooldown ends
  // %d = Automatically replaced by the number of seconds remaining
  "cooldownErrorMessage": "You must wait %d seconds before kissing again!",

  // Maximum view angle (in degrees) to detect nearby players for kiss or sneak particle effects
  // A smaller angle means only players in front are considered visible
  "maxViewAngleDegree": 90.0
}
```

## License

[MIT](https://github.com/jiazengp/kiss-fabric/blob/master/LICENSE)