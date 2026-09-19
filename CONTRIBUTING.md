# Contributing

## Development setup

- Minecraft Java Edition 1.21.11
- Fabric Loader 0.19.5 or newer
- Fabric API 0.141.6+1.21.11
- JDK 21

Clone the repository and use the checked-in Gradle wrapper. Do not commit `run/`, `build/`, IDE metadata, generated logs, or local configuration.

## Before opening a pull request

```bash
./gradlew build --no-daemon
git diff --check
```

Changes should preserve the existing architecture:

- `ModuleManager` is the source of registered modules.
- The ClickGUI edits real `Setting` instances; it must not duplicate combat state.
- `ModConfig` remains the source of truth for the existing combat state machine.
- `FriendManager` is the central friend source for combat and visual modules.
- HUD elements belong to `HudManager`, not to the ClickGUI.

Avoid blocking client-thread code, `Thread.sleep()`, direct generated-file edits, and unnecessary network or disk work every tick. New settings need defaults, safe deserialization, and a matching generic GUI component where applicable.

## Pull request checklist

- [ ] The change is scoped and documented.
- [ ] `./gradlew build --no-daemon` passes.
- [ ] `git diff --check` passes.
- [ ] No secrets, local configs, screenshots containing private data, or generated build output are included.
- [ ] Minecraft 1.21.11 runtime behavior was checked when the change affects client input, rendering, or interaction.
