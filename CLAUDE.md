# Project: Just Brightness

Minecraft client-side mod that toggles a fullbright-style gamma override. Multi-loader (Fabric, NeoForge, and Forge where available) targeting MC 1.21.1, 26.1.2, 26.2, and 26.3.

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
- `fabric/base/`, `neoforge/base/` — Shared entry points, used only by MC 1.x
- `fabric/{version}/`, `neoforge/{version}/` — Mod metadata; own Java entry point when base is incompatible (all 26.x versions)
- `forge/{version}/` — Forge loader module. **No `forge/base` exists or is planned** — every `forge/{version}` always carries its own full entry-point sources (mirrors JustCoordinates, a sibling project using the same architecture)
- `props/{version}.properties` — Version-specific dependency versions, including `enabled_platforms` (`fabric,neoforge` or `fabric,neoforge,forge`)

`settings.gradle` includes `{platform}-base` only when `{platform}/{version}/src/main/java`
does not exist, so adding version-specific entry points automatically opts that version out
of the shared base.

### Entry-point duplication (manual sync required)

Because every 26.x version needs its own copy of the shared entry points (see API table
below), these four files exist in **four locations each** (`base` plus one per 26.x version)
and must be kept in sync by hand — there is no automated check:

- `JustBrightnessFabric.java`: `fabric/base/src/main/java/com/justbrightness/fabric/`, `fabric/26.1.2/...`, `fabric/26.2/...`, `fabric/26.3/...`
- `JustBrightnessModMenu.java`: same four `fabric/{base,26.1.2,26.2,26.3}/src/main/java/com/justbrightness/fabric/` locations
- `JustBrightnessNeoForge.java`: `neoforge/base/src/main/java/com/justbrightness/neoforge/`, `neoforge/26.1.2/...`, `neoforge/26.2/...`, `neoforge/26.3/...`
- `JustBrightnessNeoForgeClient.java`: same four `neoforge/{base,26.1.2,26.2,26.3}/src/main/java/com/justbrightness/neoforge/` locations

`JustBrightnessModMenu.java` and `JustBrightnessNeoForgeClient.java` are currently
byte-identical across all four locations (no API-forced difference). Any change to a
`*/base/` entry point's registration/wiring logic must be mirrored into every `*/26.x/` copy.

Forge has no shared `base` at all, so `JustBrightnessForge.java` and
`JustBrightnessForgeClient.java` under `forge/{version}/src/main/java/com/justbrightness/forge/`
must be kept in sync across every Forge-enabled version the same way (currently 26.1.2 and 26.2,
byte-identical to each other).

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
