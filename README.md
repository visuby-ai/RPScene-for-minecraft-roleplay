# RP Scene

A Minecraft Forge 1.20.1 mod that gives roleplay servers persistent scene
markers and lightweight floating action text, so players who arrive later
can understand what happened without asking anyone.

## Requirements

- Java 17 (JDK)
- Minecraft Forge 1.20.1 (MDK, `47.3.0` — matches `build.gradle`)
- Forge's official mappings for 1.20.1 (fetched automatically by ForgeGradle)

## Building

This project is built on the official Forge 1.20.1 (47.4.1) MDK, so the
Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) is already
included — no setup step needed before your first build.

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

### Building from VS Code

1. Install the **Extension Pack for Java** and **Gradle for Java**
   extensions (the latter gives you a Gradle sidebar and task discovery).
2. Open the `RPScene` folder as a VS Code workspace.
3. Build with **Terminal → Run Build Task** (`Ctrl+Shift+B` /
   `Cmd+Shift+B`), which runs the `build` task in `.vscode/tasks.json`, or
   via the Gradle sidebar under `RPScene → Tasks → build → build`.
4. To play-test, run the `genVSCodeRuns` task once (also in
   `.vscode/tasks.json`) — this asks ForgeGradle to write launch
   configurations into `.vscode/launch.json` for the `client` and
   `server` runs, which then show up in the Run and Debug panel so you
   can launch/debug the mod directly from VS Code. The `runClient` and
   `runServer` tasks do the same thing without the debugger attached.

Compile errors will surface in the **Problems** panel and inline as
squiggles once the `$gradle` problem matcher picks them up from the
build task's output.

The compiled jar will be at `build/libs/rpscene-1.0.0.jar`. Drop it into
your server's or client's `mods/` folder alongside Forge 1.20.1.

> Note: this environment could not reach `maven.minecraftforge.net` to
> pre-resolve dependencies or run a verification build, so you'll want to
> run `./gradlew build` yourself the first time to let ForgeGradle
> download Forge, deobfuscate mappings, and confirm everything compiles
> against your local toolchain.

### First-time setup

```bash
./gradlew genEclipseRuns   # or genIntellijRuns / genVSCodeRuns
```

Then `./gradlew runClient` or `./gradlew runServer` to test locally.

## Commands

### Floating text above your head

`/me`, `/do`, and `/ooc` all render as floating world text above the
sender's head (never in chat), and share one vertical stack per player:
the newest line sits closest to the head, older lines shift up to make
room, so mixing all three in sequence reads top-to-bottom instead of
overlapping.

| Command | Description |
|---|---|
| `/me <action>` | In-character action text, white, prefixed `* `. Visible within `me_range` blocks, fades after `me_duration` seconds. |
| `/do <prompt>` | A GM-style follow-up cue that builds on a `/me` (e.g. "resist or not?"), gold, prefixed `» `. Same range/duration as `/me`. |
| `/ooc <message>` | Out-of-character aside, light blue, wrapped in `(( ))`. Visible within `ooc_range` blocks, fades after `ooc_duration` seconds by default. |
| `/ooc <duration> <message>` | Same, with a custom duration for just this message, e.g. `/ooc 5s brb`. |
| `/ooc remove` | Clears your own currently active OOC message(s) early. |

Example of the intended `/me` + `/do` combo:
```
/me picks up a zip tie and binds the man's hands
/do resist or not?
```

### Persistent scene markers

| Command | Description |
|---|---|
| `/scene create <message>` | Persistent scene marker at your position. |
| `/scene create <duration> <message>` | Timed scene marker, e.g. `/scene create 30m Blood stain on floor`. |
| `/scene create <type> <duration> <message>` | Typed + timed scene, e.g. `/scene create blood 30m Blood stain on floor`. Omit duration for a persistent typed scene. |
| `/scene list` | Lists scenes within 48 blocks, with short id, remaining time, and owner. |
| `/scene remove <id>` | Removes a scene you own (or any scene, if you're an op). Accepts a full UUID or the short id shown by `/scene list`. |
| `/scene tp <id>` | Teleports to a scene. Op only. |

Duration tokens: `10s`, `30s`, `1m`, `5m`, `30m`, `1h`, `12h`, `1d`, `7d`.

Scene types: `scene` 📌 · `blood` 🩸 · `footprint` 👣 · `weapon` 🔫 ·
`note` 📝 · `warning` ⚠ · `evidence` 🔍.

Both `/scene create` and `/ooc` offer tab-complete hints (type names,
duration examples) while you type.

## Interacting with scenes

Look at a nearby scene and press **F** (rebindable in Controls →
RP Scene) to open a small inspection panel showing the message, owner,
type, creation time, and remaining time.

## Configuration

Generated at `config/rpscene-common.toml` on first run:

```toml
me_duration = 10          # seconds a /me or /do action stays visible
me_range = 32              # blocks within which /me or /do is visible
ooc_duration = 15          # default seconds an /ooc message stays visible
ooc_range = 32             # blocks within which /ooc is visible
scene_render_range = 48    # blocks within which scene markers render
scene_fade_start = 0.7     # fraction of render range where fade begins
scene_inspect_range = 6    # max distance to inspect a scene with F
allow_op_remove = true     # ops can remove any scene, not just their own
```

## Architecture notes

- **Persistence**: all scenes live in one in-memory map on the server
  (`SceneManager`), flushed to the overworld's `SavedData`
  (`SceneSavedData`) on every mutation and on server stop. This keeps
  reads/writes to a single file regardless of which dimension a scene is
  in, while each `Scene` still records its own dimension for correct
  client-side filtering and `/scene tp`.
- **Sync**: on login, the server sends the player a full `SceneSyncPacket`
  snapshot (this is the "players arriving later immediately understand
  context" requirement from the spec). Creations/removals afterward are
  incremental (`SceneUpsertPacket` / `SceneRemovePacket`) broadcast to all
  clients.
- **Expiration**: checked once per second server-side rather than via
  per-scene timers or every tick, so it stays cheap with hundreds of
  scenes.
- **Rendering**: both scene markers and `/me` text are billboarded world
  text drawn in `RenderLevelStageEvent` (`AFTER_PARTICLES`), not GUI
  overlays or entities, so they occlude correctly with the world and
  don't add entity/tile-entity tick overhead.
- **`/me` command**: vanilla Minecraft already registers a `/me` command
  of its own. Brigadier merges command registrations that reuse the same
  literal/argument names regardless of type, so this mod's `/me`
  intentionally matches vanilla's exact argument name (`action`) and
  type (`MessageArgument`) to cleanly override it - our floating-text
  executor replaces vanilla's chat broadcast on the same tree node,
  rather than causing a runtime type-mismatch error.
- **Tab-complete suggestions**: `/do`'s type/duration/message are one
  combined greedy-string argument (so the three documented command forms
  all work through a single node), so per-slot Brigadier suggestions are
  implemented manually in `DoCommand.suggestArgs`, offering scene type
  names and duration examples only while the cursor is in the first one
  or two whitespace-delimited tokens. `/scene remove` and `/scene tp`
  suggest the short ids of currently known scenes.
- **`/me` stacking**: multiple simultaneously-active actions on the same
  entity render as a vertical stack rather than overlapping - each new
  action appears in the slot nearest the head, and existing ones shift
  up a line to make room. Every line still fades and expires on its own
  timer, so the stack naturally compacts as older lines expire.

## Not implemented in this v1

Per the spec's "Future Expansion" section, these are intentionally left
out: fingerprints/DNA evidence, police evidence bags, investigation
boards, per-scene ownership permissions beyond owner/op, scene editing,
crime scene tape, evidence categories, RP notebook integration, voice
chat integration.
