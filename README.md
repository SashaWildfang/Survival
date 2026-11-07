# Survival

Quality-of-life features for the **Bear Nation** survival server: homes, teleport requests, player locating, and health and scoreboard displays.

![Paper](https://img.shields.io/badge/Paper-1.21-blue) ![Java](https://img.shields.io/badge/Java-21-orange) ![Build](https://img.shields.io/badge/build-Gradle-02303A)

## Features

- **Homes**: set, list, visit and delete named homes
- **Teleport requests**: `/tpa` and `/tpahere`, with accept and deny
- **Player locator**: a particle trail that leads you to another player
- **Health display** above players
- **Survival scoreboard**
- Glow effects

## Commands

| Command | Description |
|---|---|
| `/sethome [name]` | Set a home |
| `/home [name]` | Teleport to a home |
| `/homes` | List your homes |
| `/delhome <name>` | Delete a home |
| `/tpa <player>` | Ask to teleport to a player |
| `/tpahere <player>` | Ask a player to teleport to you |
| `/tpaccept`, `/tpdeny` | Accept or deny a request |
| `/locate <player>` | Show a particle trail to a player |

## Requirements

- Paper 1.21+
- Java 21

## Building

```bash
gradle build
```

The plugin jar goes to `build/libs/`.

---

Part of the Bear Nation plugin suite.
