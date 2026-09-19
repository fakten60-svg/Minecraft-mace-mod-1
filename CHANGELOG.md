# Changelog

## [1.0.0] - 2026-09-19

### Added

- Existing non-blocking aerial mace sequence for Minecraft 1.21.11.
- Fabric client module system with generic Boolean, Slider, Range, Mode, Keybind, Color, and Action settings.
- Animated ClickGUI with Combat, Visuals, Movement, Misc, and Client Settings panels.
- Two-handle random-delay ranges bound directly to the combat configuration.
- Right Shift GUI keybind with persistent module keybinds defaulting to `NONE`.
- Central FriendManager and Ignore Friends combat setting.
- Sprint module with Legit/Rage, omnidirectional, keep-sprint, and forward requirements.
- Search, keybind-conflict indicators, profile manager, Friends screen, HUD overlay, and HUD editor.
- Persistent GUI, module, friend, profile, and HUD configuration.
- GitHub Actions build validation and release artifact workflow.

### Verification

- Fabric Loom 1.17.21
- Minecraft 1.21.11 / Yarn 1.21.11+build.6
- Java 21
- `./gradlew build --no-daemon` passes
