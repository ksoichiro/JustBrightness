# Just Brightness — Design

## Background

Just Coordinates now supports MC 26.3, but a companion brightness mod the
family also uses does not yet support it. The family asked for a "Just
Brightness" mod: same fast-follow philosophy as Just Coordinates, adjusting
gamma to brighten the screen with an in-game keybind toggle.

## Goals

- Ship a new client-side mod, "Just Brightness" (`justbrightness`), that
  brightens the screen on demand and can be toggled instantly during play.
- Reuse Just Coordinates' proven multi-loader/multi-version build setup so
  future MC releases can be supported quickly.
- Keep scope minimal (YAGNI): no HUD indicator, no in-mod keybind UI, no
  per-dimension behavior — these can be added later if actually needed.

## Non-goals

- Full parity with Just Coordinates' version range (1.16.5+) or all three
  loaders at launch.
- A bundling/distribution mechanism (modpack, all-in-one mod) for the family
  to install multiple Just-series mods together — that is a separate
  sub-project to be brainstormed independently.

## Approach: gamma override via Mixin

Vanilla's brightness slider is UI-capped (~0–1.0 internally) and does not
produce a true "fullbright" effect in dark areas (caves, etc.) even at
maximum. Just Brightness will use a Mixin to intercept the code path that
reads the gamma value at render time, and return a value far above the
vanilla UI's max (e.g. 1000) while the toggle is ON. While OFF, the
intercepted call falls through to the vanilla value unchanged.

This is the same technique used by common "Fullbright" mods. It satisfies
the requirement that the mod behaves independently of vanilla's brightness
setting (`options.txt` is never written by this mod; OFF always restores
the vanilla-configured brightness).

## Project structure

New repository: `JustBrightness` under
`/Users/ksoichiro/src/github.com/ksoichiro/`, following the same layout as
Just Coordinates / Just Block Shapes (bootstrapped from
`MultiVersionModTemplate` where practical):

- `gradle/shared` submodule (`minecraft-mod-gradle-scripts`) for build/release
  tasks, reused as-is.
- Loaders: Fabric, NeoForge at launch. Forge deferred (structure should not
  preclude adding it later, matching the existing per-loader directory
  pattern).
- MC versions: starting from a recent range only (e.g. 1.21.1+ and the 26.x
  series), not the full historical range Just Coordinates covers.
- `common/shared/` — version-independent code: toggle state, config model
  (target gamma, default ON/OFF state).
- `common/{version}/` — version-specific Mixin (gamma read-site injection)
  and ConfigScreen.
- `fabric/`, `neoforge/` — per-loader entry points, mod metadata, keybinding
  registration.

## Feature design

### Config screen

Reachable from the vanilla mod config menu, same pattern as Just
Coordinates:

- **Target gamma value** — the brightness applied while the toggle is ON.
- **Default ON/OFF state on world join** — chosen by the user; applied every
  time a world is entered (no cross-session "remember last toggle").

### Keybind

- One keybind, registered through the standard MC keybind system so it
  shows up in vanilla's Controls screen for rebinding (no custom rebind UI
  in the mod's own config screen).
- Pressing it toggles brightness ON/OFF instantly during play.

### State management

- Toggle state is not persisted across sessions/worlds; each world join
  re-applies the configured default ON/OFF state.
- No HUD indicator for ON state (deliberately deferred; most comparable
  mods skip this too).

## Testing

- Per-version build verification, matching Just Coordinates' pattern:
  `./gradlew :fabric:build -Ptarget_mc_version=...`,
  `./gradlew :neoforge:build -Ptarget_mc_version=...`.
- Manual in-game verification per supported version: keybind toggle,
  config screen gamma value change, default ON/OFF state on world join.
- Because the Mixin target depends on MC's internal rendering code, each
  newly supported version needs individual verification (same caveat as
  Just Coordinates' per-version Mixin work).

## Open follow-up (separate sub-project)

A "bundle" of Just-series mods for easier family setup (likely a
CurseForge/Modrinth modpack, so each mod's own download count is preserved)
is a distinct piece of work, to be brainstormed on its own once this mod
exists. It should be designed to accommodate future Just-series
multiplayer-oriented mods being added later.
