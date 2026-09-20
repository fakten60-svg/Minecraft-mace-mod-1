# Changelog

## [Unreleased]

## [1.2.0] - 2026-09-20

### Added

- Notification system (`de.aerialmace.notification`): INFO/SUCCESS/WARNING/ERROR toasts at the bottom right, animated FPS-independently with a capped queue. Wired to real events: module toggles (via the new event bus), profile save/load/rename/delete, cloud config actions, config load problems and keybind conflicts.
- Typed event bus (`de.aerialmace.event`): small synchronous client-thread pub/sub used to decouple modules from observers (`ModuleToggleEvent`).
- Debug overlay (**F10** or the "Debug Overlay" client setting): FPS, current screen, GUI scale, config dirty state, combat state machine phase, active modules plus real client/MC/Fabric versions from the loader metadata. Toggling it also enables debug logging.
- Central `ClientLogger` with a debug gate; config/friend/HUD write failures and unreadable configs are now logged instead of silently swallowed, and the user sees a notification where it matters.
- Config format versioning (`configVersion` in `aerialmace-gui.json` and `aerialmace.json`) with a separated migration hook; files from newer client versions are reported instead of silently misread.
- Rotating config backups (`aerialmace-gui.json.bak.*`, max 3) plus session backups for the combat config and single `.bak` files for HUD layout and friends.
- Profile rename (**R + name** in the profile manager) alongside create/save/load/delete.
- Setting descriptions shown as tooltips in the ClickGUI (all MaceSwitch and Client Settings settings have real, short descriptions).
- Unit test suite (52 tests): random delays, config normalization and cloud sanitizing, range/keybind/mode settings, event bus and notifications.
- Standalone website in [`website/`](website/): home, features, download, documentation, changelog and FAQ with full SEO setup (canonical URLs, Open Graph, structured data, sitemap, robots.txt).

### Changed

- `Module.addSetting` now returns the setting so registrations can chain `.describe(...)`.
- Keybind assignment warns via notification when the key is already bound to another module (the first-bound module still wins when polled).
- The Fabric API dependency uses the correct `net.fabricmc.fabric-api:fabric-api` coordinates.

### Fixed

- `ModeSetting` no longer crashes when constructed with an empty options list.

## [1.1.1] - 2026-09-20

### Changed

- The built mod jar is now named `gugugaga-client-<version>.jar` instead of `aerial-mace-automation-<version>.jar`. The mod id, package and config paths stay unchanged.

## [1.1.0] - 2026-09-20

### Added

- Cloud-config hardening on the Supabase side: upload throttle (max 5 uploads per author per minute) and an automatic retention trigger that keeps only the newest 500 rows, so the shared table stays bounded (~16 MB) and fits the free tier forever.
- Client-side abuse protection for cloud configs: upload name/author are sanitized and length-limited, every displayed entry string is truncated to 48 characters, and the download parser has a defense-in-depth guard against deeply nested hostile JSON.
- New module-settings screenshot (`docs/screenshots/module.svg`) and a refreshed HUD-editor screenshot matching the current element set.

### Fixed

- A corrupt `aerialmace-gui.json` no longer throws while loading: unknown keys are ignored, wrong types are skipped, and one broken entry cannot abort the whole load.
- Sliders, random-delay ranges and the color picker now receive GUI-scaled mouse coordinates while dragging, so a GUI scale other than 1 no longer writes wrong combat values.
- Boolean toggles rendered ON while being OFF (initial animation state) and stayed stale when the value changed outside the GUI.
- "Reset Keybinds" now resets the real GUI Keybind setting to RIGHT_SHIFT instead of only a cached state field.
- Panel positions survive a window resize (they are written back into the in-memory store on save).
- Typing while a keybind is being recorded no longer leaks into the module search.
- Mode dropdowns only consume clicks inside their own list; clicks elsewhere no longer select an option.
- HUD element scale is applied when rendering (it was persisted but ignored).
- Saving a config profile now persists the live state first, so profiles are never stale.
- The deprecated `HudRenderCallback` was replaced with the current `HudElementRegistry` API.
- Random-delay range bars no longer break on out-of-range config values: the stored bounds are clamped into the GUI range (and written back), so the two handles can never slide off the track or end up in a reversed range.
- The "Target Height" and "Target Tolerance" sliders no longer overwrite each other: the height slider reads the tolerance live instead of using a value captured while the GUI was built.
- The ClickGUI now mirrors the real combat values after a config load, profile switch, or applied cloud config, instead of showing stale numbers (`Module.refreshFromSource`).
- An applied cloud config or profile can no longer leave the master switch mirrored wrongly (the module's ON/OFF now follows `ModConfig.enabled`).
- Config values are no longer overwritten while the GUI is being constructed: settings write to their backing config only on real changes (previously every setting wrote its default on construction).
- "Reset Module Settings" restores the documented defaults (initial 100-120, equip 70-80, attack 67-90 ms, target height 3.0 +/- 0.2) instead of the values that happened to be stored at the last launch.
- The GUI keybind can no longer be cleared to `NONE`, which used to make the ClickGUI unreachable.
- The color picker no longer draws inverted gradients for a frame while unfolding, and picks up colors that changed outside the picker (theme preset, reset, config load).
- Verified the 1.21.11 `PlayerScreenHandler` layout (armor 5-8, hotbar 36-44, offhand 45) and the `clickSlot` sync-id guard; the chestplate equip and hotbar switch use the correct slots.
- Removed an unused local in `InventoryUtils.equipHeldItemAsChestplate`.

### Added

- `Max Horizontal Distance` slider, so both reach limits from the spec are configurable in the GUI.
- Mouse buttons can be assigned as module/GUI keybinds while the keybind row is recording.
- Tag-triggered release workflow (`.github/workflows/release.yml`) that publishes the built jar as a GitHub Release.

### Changed

- README slimmed down to a compact overview (short feature list, condensed configuration and control tables, cloud-config section reduced to the essentials).
- Renamed the user-facing client to **Gugugaga Client** (watermark, ClickGUI/HUD editor window titles, overlay messages and the mod display name). The mod id, package and config file paths stay unchanged so existing configs keep working; the name now comes from a single `ModConfig.CLIENT_NAME` constant.
- "Reset GUI Layout" also resets HUD positions; "Reset Everything" requires a confirmation click and no longer deletes the friend list.
- Module keybinds take precedence over the reserved F6-F9 editor shortcuts.
- HUD gained a Speed element plus per-element settings (scale, color, label, decimals, unit) and a full editor (visibility, scale, color, rename, reset).
- Placeholder modules (ESP, Fullbright, HUD, Speed, Step, AutoGG, NoRotate) no longer expose settings without an effect.
- Removed unused code: `ModuleManager.getByName`, `HudManager.get`, `Setting.isVisible/setVisible`, `Animation.easeOutBack/isAnimating/isClosed`, `CategoryPanel.getWidth`, unused `ThemeManager.Theme` enum and `TargetSelector.isPlayerTracked`.

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
