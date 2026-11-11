# 4netlib

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
| `hide.tablist` / `hide.profile` | Tab list entry (profile removed completely, or only unlisted) |
| `hide.from-other-spies` | Spies don't see each other either |
| `hide.server-list` | Online count + player sample in the multiplayer list |
| `hide.tab-complete` | Spy name in command suggestions |
| `hide.locator-bar` | Waypoint on the 1.21.6+ locator bar |
| `hide.join-quit-messages` / `hide.advancement-messages` | Broadcast messages |
| `hide.list-command` / `hide.player-commands` | `/list`, `/msg <spy>`, `/tp <spy>` … behave as if the spy is offline |
| `chat.mode` | `BLOCK`, `SEERS` or `PUBLIC` chat for spies |
| `fake-messages.*` | Fake quit / join message when toggling |
| `safety.*` | Invulnerable, no mob targeting, no pickup, no pressure plates, no sculk, no collision, ignored for sleep, flight |
| `persistence.remember-state` | Spy mode survives reconnects and restarts (hidden already at login) |

## Commands & access

`/spy` exists **only for players listed in `access.players`** (name or UUID).
For everyone else – OPs included – it is removed from the command tree,
tab-complete and `/help`, and answers like an unknown command. The console can
always use it. The list is empty by default.

| Command | Effect |
|---|---|
| `/spy` | Toggle spy mode for yourself |
| `/spy <player>` | Toggle spy mode for another player |
| `/spy list` | Online spies (only those you can see) |
| `/spy reload` | Reload `config.yml` (incl. access list) |

Aliases: `/4netlib`, `/sspy`.

| Permission | Effect | Default |
|---|---|---|
| `4netlib.autojoin` | Join in spy mode (only for listed players) | false |
| `4netlib.see` | See spies – only if set as `visibility.see-permission` | false |

**Spies are hidden from everyone – including OPs.** OP status never grants
seeing a spy. All `hide.*` and `safety.*` options are enabled by default.

## Build

```bash
./gradlew dist
```

Produces `build/dist/`:

```
4netlib-1.0.0.jar        -> plugins/
modules/
  4netlib-safety.jar     -> plugins/4netlib/modules/
  4netlib-chat.jar       -> plugins/4netlib/modules/
  4netlib-actionbar.jar  -> plugins/4netlib/modules/
```

Drop the core jar into `plugins/` and the module jars into
`plugins/4netlib/modules/`. A module jar that is not present just disables that
feature; its config section is then ignored.

## Modules

Optional features ship as separate jars loaded from `plugins/4netlib/modules/`
at startup (and re-scanned on `/spy reload`, so modules can be added or removed
without a full restart). The core jar always provides the hiding engine, the
command and the connection handling.

| Module jar | Feature | Config |
|---|---|---|
| `4netlib-safety.jar` | Invulnerable, no mob target, no pickup, no pressure plates, no sculk | `safety.*` |
| `4netlib-chat.jar` | Spy chat handling (BLOCK / SEERS / PUBLIC) | `chat.mode` |
| `4netlib-actionbar.jar` | Action-bar reminder for spies | `action-bar.*` |

### Writing your own module

A module is any jar in the modules folder that contains a `module.yml` and a
class implementing `net.f02k.safetyspy.api.SpyModule`:

```yaml
# module.yml
name: Example
main: com.example.ExampleModule
```

```java
public final class ExampleModule implements SpyModule {
    @Override
    public void onEnable(ModuleContext ctx) {
        // ctx exposes: plugin(), logger(), spyManager(), registry(),
        //              config(), messages()
        // Registrations are cleaned up automatically on disable:
        ctx.registerListener(new MyListener(ctx));
        ctx.runTimer(new MyTask(ctx), 20L, 20L);
        ctx.registerPacketListener(new MyPacketListener(ctx), PacketListenerPriority.NORMAL);
    }
}
```

Compile it against the core jar (`compileOnly`) and drop the result into
`plugins/4netlib/modules/`.

## How it works

- `VisibilityRegistry` holds spies, entity ids and viewers with see permission (thread-safe for netty).
- `SpyPacketListener` cancels / rewrites every outgoing packet that would reveal a spy.
- On toggle, `SpyManager` lets the server re-send the spy to each affected viewer
  (`hidePlayer` + `showPlayer`); the packet filter then decides what arrives.
- Permission changes of viewers are picked up every `visibility.refresh-interval-ticks`.
