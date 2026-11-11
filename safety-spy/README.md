# SafetySpy

Safety spy mode for server staff. A spy is hidden on **packet level** via
[PacketEvents](https://github.com/retrooper/packetevents) – not only via the
Bukkit API – so the player is invisible in every place a client could see them.

## Requirements

- Paper 1.21+ (Java 21)
- PacketEvents 2.14+ plugin installed on the server

## What gets hidden (all configurable in `config.yml`)

| Option | Effect |
|---|---|
| `hide.entity` | Spawn, movement, metadata, equipment, animations, effects, passengers, damage … |
| `hide.tablist` / `hide.keep-profile` | Tab list entry (removed completely, or only unlisted) |
| `hide.server-list` | Online count + player sample in the multiplayer list |
| `hide.tab-complete` | Spy name in command suggestions |
| `hide.locator-bar` | Waypoint on the 1.21.6+ locator bar |
| `hide.join-quit-messages` / `hide.advancement-messages` | Broadcast messages |
| `hide.list-command` / `hide.player-commands` | `/list`, `/msg <spy>`, `/tp <spy>` … behave as if the spy is offline |
| `chat.mode` | `BLOCK`, `SEERS` or `PUBLIC` chat for spies |
| `fake-messages.*` | Fake quit / join message when toggling |
| `safety.*` | Invulnerable, no mob targeting, no pickup, no pressure plates, no sculk, no collision, ignored for sleep, flight |
| `persistence.remember-state` | Spy mode survives reconnects and restarts (hidden already at login) |

## Commands & permissions

| Command | Permission | Default |
|---|---|---|
| `/spy` | `safetyspy.use` | op |
| `/spy <player>` | `safetyspy.others` | op |
| `/spy list` | `safetyspy.list` | op |
| `/spy reload` | `safetyspy.reload` | op |
| – see spies | `safetyspy.see` (configurable) | op |
| – join in spy mode | `safetyspy.autojoin` | false |

Aliases: `/safetyspy`, `/sspy`.

## Build

```bash
./gradlew :safety-spy:build
# -> safety-spy/build/libs/SafetySpy-1.0.0.jar
```

## How it works

- `VisibilityRegistry` holds spies, entity ids and viewers with see permission (thread-safe for netty).
- `SpyPacketListener` cancels / rewrites every outgoing packet that would reveal a spy.
- On toggle, `SpyManager` lets the server re-send the spy to each affected viewer
  (`hidePlayer` + `showPlayer`); the packet filter then decides what arrives.
- Permission changes of viewers are picked up every `visibility.refresh-interval-ticks`.
