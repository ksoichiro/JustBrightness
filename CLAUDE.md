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
- `forge/{version}/src/main/resources/pack.mcmeta` — **Forge-only requirement.** Forge treats a mod's resources as a vanilla resource pack and requires `pack.mcmeta`; without it Forge logs "failed to load a valid ResourcePackInfo" for the mod's resources and silently drops them, including lang files (so translation keys render raw, e.g. `key.category.justbrightness.justbrightness` in the settings screen). Fabric and NeoForge don't need this file — don't assume it's optional everywhere because it's absent from `fabric/`/`neoforge/`.
  **Trap**: a plain `"pack_format": N` is only accepted by `PackFormat`'s codec (`net/minecraft/server/packs/metadata/pack/PackFormat.java`) when `N` is at or below the current game version's legacy-format cutoff (confirmed 64 for 1.21.11) — above that, Minecraft rejects the pack with "declares support for version newer than 64, but is missing mandatory fields min_format and max_format" (a real in-game error, not a build-time one, so a clean build proves nothing). JustCoordinates' own `pack.mcmeta` files use the plain `pack_format`-only form and are **untested by that project against this validation path** — don't copy them as-is for values this high. Use `min_format`/`max_format` instead (verified against Mojang's own `trade_rebalance` datapack `pack.mcmeta`, extracted from `client.jar`, which uses this exact form): `{"pack": {"description": "...", "min_format": N, "max_format": N}}` with `min_format == max_format == N` for a single supported format (1.21.11→75, 26.1.2→76, 26.2→88).
  **Further trap**: `min_format`/`max_format` alone still wasn't enough for 1.21.11 — a mod's `pack.mcmeta` is validated *twice*, once as `PackType.CLIENT_RESOURCES` (legacy cutoff 64) and once as `PackType.SERVER_DATA` (legacy cutoff 81, a **different, higher** cutoff than the client one — confirmed via `PackFormat.lastPreMinorVersion(PackType)`), against the same file. With `min_format=max_format=75`: the CLIENT_RESOURCES check (75>64) requires `supported_formats` to be **absent** ("is deprecated ... remove it" if present), while the SERVER_DATA check (75≤81) requires `supported_formats` to be **present** (plus a `pack_format` field) — i.e. **the two checks want opposite things and no single file satisfies both**. Both errors are logged every run (confirmed via `./gradlew :forge:runClient`) but appear to be non-fatal — the game keeps running, worlds load — so this is currently left as a **known, unresolved cosmetic issue** rather than chased further; re-derive per-version behavior from `net/minecraft/server/packs/metadata/pack/PackFormat.java` in that version's decompiled vanilla sources rather than assuming these exact numbers carry over.
  As of 2026-09-20, only 1.21.11's `pack.mcmeta` has been confirmed in-game (via `runClient`) to hit this dual-validation conflict and needed `supported_formats` added; `26.1.2`/`26.2` currently ship `min_format`/`max_format` only (no `supported_formats`), **unconfirmed by an actual `runClient` run** — don't assume they're clean of this issue just because their build succeeded (a clean build proves nothing here, same as above).
- `forge/{version}/build.gradle`'s `jar { manifest { attributes('MixinConfigs': ...) } }` **plus** `minecraft.runs.configureEach { args "--mixin.config=${mod_id}.mixins.json" }` — **both are required, for different reasons, confirmed against Forge's own `MinecraftForge/MDKExamples` repo (`mixins-only/fg7`, targets 1.21.11)**:
  - The manifest attribute is read by `org.spongepowered:mixin:0.8.7`'s `MixinPlatformAgentDefault.prepare()` via `IContainerHandle.getAttribute("MixinConfigs")` — this is what makes the mixin work in a **packaged, installed jar** (a real `META-INF/MANIFEST.MF` exists there). `mods.toml`'s `[[mixins]]` table has **no effect on Forge** (only NeoForge parses it) — confirmed by exhaustively grepping `fmlcore`/`fmlloader`/`javafmllanguage` for any "mixins" string and finding none.
  - The `args "--mixin.config=..."` program argument is what makes it work in **`:forge:runClient` / dev-run testing** — dev runs load the mod from a raw output directory (`forge/{version}/build/sourcesSets/main/`, not a jar), so there is no manifest for the agent above to read; Mixin needs the config path as an explicit ModLauncher program argument instead (this is what the now-unsupported MixinGradle plugin used to inject automatically). **Without this, dev-run testing shows zero mixin-related log output at all — no error, no success — because Mixin's `MixinEnvironment` prepares zero configs; confirmed by running with `-Dmixin.debug.verbose=true` and seeing "Preparing mixins for MixinEnvironment[DEFAULT]" followed by nothing.** This was the actual root cause of gamma silently not changing on Forge across three separate testing rounds: the mod loads fine, the toggle keybind and its action-bar message work, `GammaOverrideMixin` just never gets a chance to run.
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

Common code (`InputConstants.Type.KEYSYM`, `Screen#render`/`Minecraft#setScreen`, Fabric
`KeyBindingHelper`) is unchanged from 1.21.1 — the keybind category mechanism moved to
`KeyMapping.Category` (same shape as the 26.x versions; see the table below and
`common/1.21.11/BrightnessController.java`).

**Trap**: `LightTexture#updateLightTexture` gained a new `options.hideLightningFlash().get()`
call (inside the `endFlashState != null` branch) between 1.21.1 and 1.21.11, which shifts the
`OptionInstance.get()` ordinal that `GammaOverrideMixin` redirects: it's ordinal 1
(`darknessEffectScale` is 0) at 1.21.1, but ordinal **2** at 1.21.11
(0=`hideLightningFlash`, 1=`darknessEffectScale`, 2=`gamma`). This was initially missed because
the ordinal was "verified" by grepping decompiled source only for the option names already
expected (`darknessEffectScale`/`gamma`) instead of reading the whole method — the redirect
silently compiled and ran against the wrong option (`darknessEffectScale` instead of `gamma`),
so gamma never visibly changed in-game despite a clean build. When verifying a `@Redirect`
ordinal for a new version, read the **entire target method** (or disassemble it with `javap -c`)
rather than grepping for the specific calls you expect to find.

**Trap**: `net.neoforged.fml.loading.FMLEnvironment.dist` is gone in NeoForge `21.11.38-beta`
(the version 1.21.11 pins) — `FMLEnvironment.getDist()` is required, exactly like 26.x. This is
gated by the **NeoForge library version**, not by the MC 1.x/26.x numbering split: 1.21.1 (NeoForge
`21.1.219`) still has `.dist`, but 1.21.11 (NeoForge `21.11.38-beta`) does not. Consequently
`neoforge/1.21.11/` cannot reuse `neoforge/base` and carries its own entry-point copy (see
"Entry-point duplication" above) — don't assume every 1.x version can share `base` just because
it isn't 26.x. Fabric's entry points had no equivalent break, so `fabric/1.21.11` still reuses
`fabric/base` normally.

### Pre-1.20 API differences (1.19.2 and earlier)

`GuiGraphics` doesn't exist before 1.20 — `ConfigScreen#render` and friends must use the
`PoseStack`-based `Screen`/`AbstractSliderButton`/`CycleButton` APIs instead (no verbatim copy
from `common/1.20.1/ConfigScreen.java` is possible), and `Button` has no `.builder(...)` static
factory yet, only the direct `Button(int, int, int, int, Component, OnPress)` constructor.
NeoForge also doesn't exist before 1.20.1, so every pre-1.20 version is Fabric+Forge only, same
as `common/1.20.1`/`forge/1.20.1`.

The `GammaOverrideMixin` `@Redirect` on `LightTexture#updateLightTexture`'s `OptionInstance.get()`
calls keeps the same ordinal at 1.19.2 as at 1.20.1 (0=`darknessEffectScale`, 1=`gamma` — there is
no `hideLightningFlash` call at all this far back; that third option only appears starting at
1.21.11, see above) — confirmed by decompiling 1.19.2's vanilla sources jar, not assumed from the
1.20.1 copy.

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

**Trap**: `common/{version}/justbrightness.mixins.json`'s `compatibilityLevel` cannot just track
`java_version` from `props/{version}.properties`. 26.1.2/26.2/26.3 all set `java_version=25`, but
Forge (unlike Fabric Loom / NeoForge's moddev, which apparently bundle a newer/patched Mixin
build) pins the plain upstream `org.spongepowered:mixin:0.8.7`, whose `CompatibilityLevel` enum
only goes up to `JAVA_21` — no `JAVA_25` entry exists at all. Declaring `"compatibilityLevel":
"JAVA_25"` (copied from `java_version`, seemed like the obvious value) makes Forge fail at
startup with `MixinInitialisationError: ... specifies compatibility level JAVA_25 which is not
recognised`, while Fabric/NeoForge start up fine with the exact same file. `common/26.1.2` and
`common/26.2` now declare `"JAVA_21"` instead (verified fine for Fabric/NeoForge too, and fixes
the Forge crash) — `common/26.3` still has `forge/26.3` out of scope so it's untouched, but if
Forge support is ever added there this same trap applies.
