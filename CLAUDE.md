# Project: Just Brightness

Minecraft client-side mod that toggles a fullbright-style gamma override. Multi-loader (Fabric, NeoForge) targeting MC 1.21.1 and 26.3 only.

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
```

## Architecture

- `common/shared/` — Version-independent code with no MC API usage (`JustBrightness`, `BrightnessConfig`, `BrightnessState`). Every platform build.gradle adds this dir via `srcDir` — `compileOnly project(':common')` alone compiles but silently omits these classes from the jar
- `common/{version}/` — `ConfigScreen`, `BrightnessController` and the Mixin: version-specific MC API
- `fabric/base/`, `neoforge/base/` — Shared entry points, used only by MC 1.x
- `fabric/{version}/`, `neoforge/{version}/` — Mod metadata; own Java entry point when base is incompatible (26.x)
- `props/{version}.properties` — Version-specific dependency versions

`settings.gradle` includes `{platform}-base` only when `{platform}/{version}/src/main/java`
does not exist, so adding version-specific entry points automatically opts that version out
of the shared base.

### Entry-point duplication (manual sync required)

Because 26.3 needs its own copies of the shared entry points (see API table below), these
four files exist in two locations each and must be kept in sync by hand — there is no
automated check:

- `JustBrightnessFabric.java`: `fabric/base/src/main/java/com/justbrightness/fabric/` and `fabric/26.3/src/main/java/com/justbrightness/fabric/`
- `JustBrightnessModMenu.java`: `fabric/base/src/main/java/com/justbrightness/fabric/` and `fabric/26.3/src/main/java/com/justbrightness/fabric/`
- `JustBrightnessNeoForge.java`: `neoforge/base/src/main/java/com/justbrightness/neoforge/` and `neoforge/26.3/src/main/java/com/justbrightness/neoforge/`
- `JustBrightnessNeoForgeClient.java`: `neoforge/base/src/main/java/com/justbrightness/neoforge/` and `neoforge/26.3/src/main/java/com/justbrightness/neoforge/`

`JustBrightnessModMenu.java` and `JustBrightnessNeoForgeClient.java` are currently
byte-identical between their two locations (no API-forced difference). Any change to a
`*/base/` entry point's registration/wiring logic must be mirrored into the corresponding
`*/26.3/` file.

### MC 26.x API differences

| 1.21.1 | 26.3 |
|--------|------|
| `LightTexture#updateLightTexture(float)` | `LightmapRenderStateExtractor#extract(LightmapRenderState, float)` |
| `Screen#render(GuiGraphics, …)` | `Screen#extractRenderState(GuiGraphicsExtractor, …)` |
| `Minecraft#setScreen` | `Minecraft#setScreenAndShow` |
| `InputConstants.Type.KEYSYM` + GLFW key codes | `InputConstants.Type.KEYBOARD` + `InputConstants.KEY_*` (SDL) |
| category string `key.categories.<modid>` | `KeyMapping.Category` → lang key `key.category.<ns>.<path>` |
| Fabric `KeyBindingHelper` (keybinding.v1) | Fabric `KeyMappingHelper` (keymapping.v1) |
| `FMLEnvironment.dist` | `FMLEnvironment.getDist()` |
| Loom plugin `fabric-loom`, `modImplementation` | `net.fabricmc.fabric-loom`, plain `implementation` (no remap) |
