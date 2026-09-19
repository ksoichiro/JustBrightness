# Project: Just Brightness

Minecraft client-side mod that toggles a fullbright-style gamma override. Multi-loader (Fabric, NeoForge, and Forge where available) targeting MC 1.21.1, 1.21.11, 26.1.2, 26.2, and 26.3.

## Build

### Prerequisites

Shared build/release tasks live in the `gradle/shared` git submodule
([minecraft-mod-gradle-scripts](https://github.com/ksoichiro/minecraft-mod-gradle-scripts)).
Initialize it after cloning:

```
git submodule update --init
```

### Commands

Build a specific platform for a target Minecraft version:

```
./gradlew :fabric:build -Ptarget_mc_version=1.21.1
./gradlew :neoforge:build -Ptarget_mc_version=1.21.1
./gradlew :forge:build -Ptarget_mc_version=26.2
```

## Architecture

- `common/shared/` — Version-independent code with no MC API usage (`JustBrightness`, `BrightnessConfig`, `BrightnessState`). Every platform build.gradle adds this dir via `srcDir` — `compileOnly project(':common')` alone compiles but silently omits these classes from the jar
- `common/{version}/` — `ConfigScreen`, `BrightnessController` and the Mixin: version-specific MC API
- `fabric/base/` — Shared Fabric entry points, used by all 1.x versions (1.21.1 and 1.21.11)
- `neoforge/base/` — Shared NeoForge entry points, used **only by 1.21.1**. 1.21.11 needs its
  own copy despite also being a "1.x" MC version — see the note below the API table
- `fabric/{version}/`, `neoforge/{version}/` — Mod metadata; own Java entry point when base is incompatible
- `forge/{version}/` — Forge loader module. **No `forge/base` exists or is planned** — every `forge/{version}` always carries its own full entry-point sources (mirrors JustCoordinates, a sibling project using the same architecture)
- `props/{version}.properties` — Version-specific dependency versions, including `enabled_platforms` (`fabric,neoforge` or `fabric,neoforge,forge`)

`settings.gradle` includes `{platform}-base` only when `{platform}/{version}/src/main/java`
does not exist, so adding version-specific entry points automatically opts that version out
of the shared base.

### Entry-point duplication (manual sync required)

Because every 26.x version (and, for NeoForge only, 1.21.11 — see below) needs its own copy of
the shared entry points, these files exist in multiple locations and must be kept in sync by
hand — there is no automated check:

- `JustBrightnessFabric.java` / `JustBrightnessModMenu.java`: `fabric/base/src/main/java/com/justbrightness/fabric/` (shared by 1.21.1 and 1.21.11), `fabric/26.1.2/...`, `fabric/26.2/...`, `fabric/26.3/...` — **four locations**.
- `JustBrightnessNeoForge.java` / `JustBrightnessNeoForgeClient.java`: `neoforge/base/src/main/java/com/justbrightness/neoforge/` (used only by 1.21.1), `neoforge/1.21.11/...`, `neoforge/26.1.2/...`, `neoforge/26.2/...`, `neoforge/26.3/...` — **five locations**.

`JustBrightnessModMenu.java` is byte-identical across all four Fabric locations.
`JustBrightnessNeoForgeClient.java` is byte-identical across all five NeoForge locations.
`JustBrightnessNeoForge.java`'s logic is identical across the four `getDist()`-using copies
(1.21.11, 26.1.2, 26.2, 26.3), but 1.21.11's copy has a version-specific comment explaining why
`getDist()` is needed there too (see below), so it isn't byte-identical to the 26.x copies — and
all four differ from `base`'s `FMLEnvironment.dist`-using version. Any change to a `*/base/`
entry point's registration/wiring logic must be mirrored into every non-base copy.

Forge has no shared `base` at all, so `JustBrightnessForge.java` and
`JustBrightnessForgeClient.java` under `forge/{version}/src/main/java/com/justbrightness/forge/`
must be kept in sync across every Forge-enabled version the same way (currently 1.21.11, 26.1.2,
26.2, byte-identical to each other).

### MC 1.21.11 API differences

Common code (`InputConstants.Type.KEYSYM`, `LightTexture#updateLightTexture`,
`Screen#render`/`Minecraft#setScreen`, Fabric `KeyBindingHelper`) is unchanged from 1.21.1 — only
the keybind category mechanism moved to `KeyMapping.Category` (same shape as the 26.x versions;
see the table below and `common/1.21.11/BrightnessController.java`).

**Trap**: `net.neoforged.fml.loading.FMLEnvironment.dist` is gone in NeoForge `21.11.38-beta`
(the version 1.21.11 pins) — `FMLEnvironment.getDist()` is required, exactly like 26.x. This is
gated by the **NeoForge library version**, not by the MC 1.x/26.x numbering split: 1.21.1 (NeoForge
`21.1.219`) still has `.dist`, but 1.21.11 (NeoForge `21.11.38-beta`) does not. Consequently
`neoforge/1.21.11/` cannot reuse `neoforge/base` and carries its own entry-point copy (see
"Entry-point duplication" above) — don't assume every 1.x version can share `base` just because
it isn't 26.x. Fabric's entry points had no equivalent break, so `fabric/1.21.11` still reuses
`fabric/base` normally.

### MC 26.x API differences

All of 1.21.1 → 26.1.2/26.2/26.3 share the `net.fabricmc.fabric-loom`/`modImplementation`-less
build split, `FMLEnvironment.getDist()`, Fabric `KeyMappingHelper`, `LightmapRenderStateExtractor`,
`Screen#extractRenderState`, and `KeyMapping.Category` changes below. But **26.x is not
internally uniform** — `InputConstants` and `Minecraft#setScreen` each changed partway through
the line, confirmed by diffing JustCoordinates' per-version source (a sibling project already
covering these versions):

| 1.21.1 | 26.1.2 | 26.2 | 26.3 |
|--------|--------|------|------|
| `LightTexture#updateLightTexture(float)` | `LightmapRenderStateExtractor#extract(LightmapRenderState, float)` | same as 26.1.2 | same as 26.1.2 |
| `Screen#render(GuiGraphics, …)` | `Screen#extractRenderState(GuiGraphicsExtractor, …)` | same | same |
| `Minecraft#setScreen` | `Minecraft#setScreen` (still present) | `Minecraft#setScreenAndShow` (removed) | `Minecraft#setScreenAndShow` |
| `InputConstants.Type.KEYSYM` + GLFW key codes | `InputConstants.Type.KEYSYM` + GLFW key codes (unchanged) | same as 26.1.2 | `InputConstants.Type.KEYBOARD` + `InputConstants.KEY_*` (SDL) |
| category string `key.categories.<modid>` | `KeyMapping.Category` → lang key `key.category.<ns>.<path>` | same | same |
| Fabric `KeyBindingHelper` (keybinding.v1) | Fabric `KeyMappingHelper` (keymapping.v1) | same | same |
| `FMLEnvironment.dist` | `FMLEnvironment.getDist()` | same | same |
| Loom plugin `fabric-loom`, `modImplementation` | `net.fabricmc.fabric-loom`, plain `implementation` (no remap) | same | same |

The `GammaOverrideMixin` ordinal for the redirected `OptionInstance.get()` call inside
`LightmapRenderStateExtractor#extract` is the same (0=hideLightningFlash, 1=gamma,
2=darknessEffectScale) across 26.1.2/26.2/26.3 — verified by decompiling each version's vanilla
sources jar, not assumed (no equivalent in JustCoordinates to diff against, since it doesn't
touch gamma).

`Gui#setOverlayMessage` also moved during the 26.x line: it lives directly on `Gui` at 26.1.2
(same as 1.21.1) but moved onto a new `Gui#hud` (`Hud#setOverlayMessage`) field starting with
26.2 — `BrightnessController` must call the right one per version.
