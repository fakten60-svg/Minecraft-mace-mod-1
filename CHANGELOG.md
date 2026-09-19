# Changelog

## [1.0.0] - 2026-09-19

### Added

- Additional configurable random-delay ranges for target lock settling and post-attack cooldown.
- New non-blocking `TARGET_LOCK_DELAY` and `POST_ATTACK_DELAY` state-machine phases; both default to 0 ms for backward-compatible timing.

- Existing non-blocking aerial mace sequence for Minecraft 1.21.11.
- Fabric client module system with generic Boolean, Slider, Range, Mode, Keybind, Color, and Action settings.
- Animated ClickGUI with Combat, Visuals, Movement, Misc, and Client Settings panels.
- Two-handle random-delay ranges bound directly to the combat configuration.
- Right Shift GUI keybind with persistent module keybinds defaulting to `NONE`.
- Central FriendManager and Ignore Friends combat setting.
- Sprint module with Legit/Rage, omnidirectional, keep-sprint, and forward requirements.
- Search, keybind-conflict indicators, profile manager, Friends screen, HUD overlay, and HUD editor.
- Persistent GUI, module, friend, profile, and HUD configuration.
- GitHub Actions build validation for pushes and pull requests.
- Cloud config sharing for client configs only; releases stay manual and independent.

### Verification

- Fabric Loom 1.17.21
- Minecraft 1.21.11 / Yarn 1.21.11+build.6
- Java 21
- `./gradlew build --no-daemon` passes
