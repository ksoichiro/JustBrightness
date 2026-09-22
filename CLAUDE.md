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

**Trap**: `OptionInstance` does not exist at all before 1.19 — at 1.18.2, `LightTexture#
updateLightTexture` reads gamma as a bare public field, `this.minecraft.options.gamma` (`Options#
gamma` is `public double gamma;`, no getter). `GammaOverrideMixin` for 1.18.2 must therefore
`@Redirect` a `@At(value = "FIELD", target = "Lnet/minecraft/client/Options;gamma:D")` returning
a primitive `double`, not a method-call redirect on `OptionInstance.get()` — an API-shape change,
not just a signature tweak. No `ordinal` is needed there (only one `options.gamma` read exists in
the method body, unlike the multiple `OptionInstance.get()` calls at 1.19.2+).

**Trap**: Fabric API's own mod id at `fabric_api_version=0.46.1+1.17` is `"fabric"`, not
`"fabric-api"` — the `"fabric-api"` id (with `"provides": ["fabric"]` for back-compat) only starts
at the version used by 1.18.2 (`0.75.1+1.18.2`) onward. `fabric/1.17.1/src/main/resources/
fabric.mod.json`'s `depends` must declare `"fabric": "*"`, not `"fabric-api": "*"` — otherwise
`:fabric:runClient` fails immediately with "Incompatible mods found! ... requires fabric-api but
it's missing" even though the artifact resolved fine at compile time (confirmed via decompiling
the actual `fabric-api-0.46.1+1.17.jar`'s `fabric.mod.json`). This is why `fabric/1.17.1` needs
its own `fabric.mod.json` rather than being a byte-identical copy of 1.18.2's.

**Trap**: `ConfigGuiHandler.ConfigGuiFactory` at 1.17.1 only has the two-arg
`BiFunction<Minecraft, Screen, Screen>` record constructor — the single-arg
`Function<Screen, Screen>` convenience constructor used at 1.18.2 doesn't exist yet (added later).
Use `new ConfigGuiHandler.ConfigGuiFactory((minecraft, parent) -> new ConfigScreen(parent))` at
1.17.1, confirmed via decompiled Forge 1.17.1-37.1.1 sources; caught as a compile error (unrelated
lambda type-mismatch messages from javac), not a silent trap.

**Trap**: `net.minecraftforge.client.ClientRegistry`/`ConfigGuiHandler` (used at 1.18.2) live under
`net.minecraftforge.fmlclient`/`net.minecraftforge.fmlclient.registry` instead at 1.17.1 — same
method/class shapes, different package, confirmed via decompiled Forge 1.17.1-37.1.1 sources. MC
1.17 also raises the minimum Java version to 16 (`java_version=16`, `compatibilityLevel:
"JAVA_16"` — safe, Mixin's `CompatibilityLevel` has had this constant since 0.8.2). On macOS
arm64, `:fabric:runClient` for 1.17.1 needs the dev-run JVM raised to 17 even though the mod
compiles for 16, because Fabric Loom injects `fabric-loom-native-support` (arm64 LWJGL natives
for pre-1.19 versions) into dev runs and that support mod itself requires Java 17+ — otherwise
the dev client aborts with "Incompatible mods found!". Fix: `tasks.withType(net.fabricmc.loom.
task.AbstractRunTask).configureEach { javaLauncher = javaToolchains.launcherFor { languageVersion
= JavaLanguageVersion.of(17) } }` in `fabric/1.17.1/build.gradle` (only affects the dev-run JVM,
not compilation). 1.18.2 doesn't need this because it already targets Java 17.

`Component.translatable(...)` and `CommonComponents.EMPTY` don't exist at 1.18.2 either (both are
1.19+ additions) — use `new TranslatableComponent(key, args...)` (`net.minecraft.network.chat.
TranslatableComponent`) and `TextComponent.EMPTY` instead. `CommonComponents.GUI_DONE` itself is
fine unchanged. This was caught by a compile error, not a silent trap, but worth listing here so
the next backport doesn't rediscover it via a failed build.

Forge's mod-registration APIs used at 1.19.2/1.20.1 also don't exist yet at 1.18.2 (confirmed via
decompiled Forge 1.18.2-40.2.21 sources, cached from a JustCoordinates build):
`RegisterKeyMappingsEvent` → use `net.minecraftforge.client.ClientRegistry.registerKeyBinding(...)`
called during `FMLClientSetupEvent` (mod bus) instead; `ConfigScreenHandler.ConfigScreenFactory`
→ use `net.minecraftforge.client.ConfigGuiHandler.ConfigGuiFactory` instead (same
`ModLoadingContext.get().registerExtensionPoint(...)` call shape, different class); and
`ClientPlayerNetworkEvent.LoggingIn` doesn't exist as a nested-class name at 1.18.2 — the
equivalent for the world-join hook is `ClientPlayerNetworkEvent.LoggedInEvent`.

**Trap (build wiring, affects 1.18.2/1.19.2/1.20.1 Forge alike, found via real-launcher testing)**:
`net.neoforged.moddev.legacyforge`'s Forge (SRG/MCP-based, needs `reobfJar`) does **not**
automatically wire the Mixin annotation processor the way Fabric Loom or ForgeGradle 7.x
(1.21.1+/26.x) do. Without it, no Mixin refmap is ever generated, `justbrightness.mixins.json`
has no `"refmap"` key, and `:forge:runClient` (dev, unobfuscated names) loads mixins fine — but a
real, reobfuscated Forge install fails with `InvalidInjectionException: ... could not find any
targets matching 'updateLightTexture' ... No refMap loaded`, because the mixin's target-method
string is never translated to the SRG name the reobfuscated jar actually uses. **A clean
`:forge:build`, and even a successful `:forge:runClient`, prove nothing about this** — it only
surfaces in a real launcher (found via a real Prism Launcher run, not automated testing).
Fix (applied to `forge/{1.18.2,1.19.2,1.20.1}/build.gradle`): add
`annotationProcessor 'net.fabricmc:sponge-mixin:0.15.4+mixin.0.8.7'` to `dependencies`, and a
`mixin { config 'justbrightness.mixins.json'; add sourceSets.main, 'justbrightness-refmap.json' }`
block (the `mixin` extension comes from the `net.neoforged.moddev.legacyforge` plugin itself,
confirmed by decompiling `moddev-gradle-*.jar`'s `MixinExtension`/`MixinCompilerArgs` classes —
there is no public documentation page for this; it had to be found by reading the plugin's own
bytecode). Also add `"refmap": "justbrightness-refmap.json"` to the shared
`common/{version}/src/main/resources/justbrightness.mixins.json` (this file is bundled by both
Fabric and Forge builds via `commonResources`). **This is safe for Fabric too**: verified by
decompiling the actually-remapped Fabric jar's `GammaOverrideMixin.class` — Fabric Loom's
`remapJar` rewrites the `@Redirect`/`@At` annotation string constants directly in the class
bytecode (e.g. `updateLightTexture` → `method_3313`, confirmed via `javap -v`), so Fabric never
needed a refmap file at all; declaring one that doesn't exist in the Fabric jar only produces a
benign, self-explanatory log warning (`Reference map ... could not be read. If this is a
development environment you can ignore this message`), not a failure — confirmed by an actual
`:fabric:runClient` run after adding the key, not assumed. 1.21.1+/26.x (ForgeGradle 7.x,
official-Mojang-mappings runtime, no reobfuscation) are believed unaffected since they never
reobfuscate to a different name set in the first place, but this was inferred by comparing
`forge/1.21.1/build.gradle`'s plugin choice against the legacyforge one, not verified via an
actual real-launcher test the way the 1.18.2 failure was — don't assume it's clean without
checking if a similar report ever comes in for those versions.

**Trap (environment, not code)**: on Apple Silicon, `:forge:runClient` for 1.18.2 crashes before
any world loads with `UnsatisfiedLinkError: ... liblwjgl.dylib ... incompatible architecture
(have 'x86_64', need 'arm64...')` — the legacyforge/moddev-resolved LWJGL 3.2.1-SNAPSHOT natives
for this version have no arm64 macOS build. This is unrelated to mod code (confirmed: `:fabric:
runClient` for the same MC version, same machine, loads fine — Fabric Loom apparently patches/
resolves different natives). `forge:runClient` smoke-testing pre-1.19 versions on Apple Silicon
therefore can't verify Forge-side Mixin application the way it does for 1.19.2+; rely on the
Fabric-side smoke test (same shared Mixin class from `:common`) plus an actual in-game check on
a compatible machine instead. Same category of failure recurs at 1.17.1 (`NoClassDefFoundError:
Could not initialize class com.mojang.blaze3d.systems.RenderSystem` during `Minecraft.main`) —
expected, not a new regression; verify via the built jar's bundled refmap + a real launcher
instead, same as 1.18.2.

### MC 1.16.5: Architectury Loom instead of legacyforge, and a third Forge API generation

1.16.5 cannot use `fabric-loom` + `net.neoforged.moddev.legacyforge` (the toolchain for
1.17.1-1.20.1) — it needs **`dev.architectury.loom`** for both platforms instead, wired via a
`targetVer == '1.16.5'` branch in `settings.gradle`'s `pluginManagement.plugins` block (checked
*before* the general `startsWith('1.')` branch). `gradle.properties`' `architectury_loom_version`
was declared but unused until this version. Root `build.gradle`'s `toolchain { ... }` block must
be skipped for `java_version < 9` (`if ((java_version as int) >= 9) { toolchain { ... } }`) since
JDK 8 doesn't support the `--release`-flag toolchain path — `options.release = java_version as
int` (unconditional) still handles compilation targeting. Verified against JustCoordinates' own
working `settings.gradle`/`build.gradle`, which already special-cases exactly this.

**Mixin needs no refmap/AP wiring under Architectury Loom** (unlike `net.neoforged.moddev.
legacyforge`, which needed the `annotationProcessor`/`mixin { add sourceSets.main, ... }` fix
documented above) — just `loom { forge { mixinConfig "justbrightness.mixins.json" } }` in
`forge/1.16.5/build.gradle` (plus the standard `[[mixins]] config="..."` entry in `mods.toml`,
which Forge itself reads in production regardless of toolchain). Confirmed by diffing ChronoDawn's
source vs. built `forge/1.20.1` mixins.json (Architectury Loom + Mixin, no `"refmap"` key
anywhere) — Architectury Loom rewrites Mixin annotation strings directly in the compiled
bytecode at remap time, same mechanism as Fabric Loom.

**Yet another `ClientRegistry` package** (third distinct one across just three Forge versions
so far): `net.minecraftforge.fml.client.registry.ClientRegistry` at 1.16.5 (vs.
`net.minecraftforge.fmlclient.registry.ClientRegistry` at 1.17.1, `net.minecraftforge.client.
ClientRegistry` at 1.18.2+). **No `ConfigGuiHandler`/`ConfigScreenHandler` class exists yet** —
config-screen registration at 1.16.5 uses `net.minecraftforge.fml.ExtensionPoint.CONFIGGUIFACTORY`
(a typed `ExtensionPoint<BiFunction<Minecraft, Screen, Screen>>` constant, no wrapper record
class): `ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
() -> (minecraft, parent) -> new ConfigScreen(parent))`. `ClientPlayerNetworkEvent.LoggedInEvent`
and `TickEvent.ClientTickEvent` are unchanged from 1.17.1/1.18.2. All confirmed via decompiled
1.16.5 Forge sources plus JustCoordinates' own real, working 1.16.5 Forge entry-point code.

**No `CycleButton` and no `Screen#addRenderableWidget` at 1.16.5** — both are later additions.
`Screen`'s widget-registration method is named `addButton` (not `addRenderableWidget`) at this
version, confirmed via JustCoordinates' own working `ConfigScreen.java` and Architectury Loom's
tiny mapping file (`method_25411` → `addButton`, no `CycleButton` entry anywhere in the mapping
table at all). `AbstractSliderButton` **does** exist, same package as 1.18.2+
(`net.minecraft.client.gui.components.AbstractSliderButton`, confirmed via mapping
`net/minecraft/class_357` → that exact class) — only the on/off toggle buttons need a manual
rewrite: a plain `Button` whose `onPress` flips a locally-tracked boolean, updates the config, and
calls `button.setMessage(...)` with a relabeled `Component` (`CommonComponents.OPTION_ON`/
`OPTION_OFF` appended to the option's translation key) instead of `CycleButton.onOffBuilder(...)`.

**Fabric API's own mod id is `"fabric"` at `fabric_api_version=0.42.0+1.16`** — same trap as
1.17.1's `0.46.1+1.17`; `fabric/1.16.5/src/main/resources/fabric.mod.json` must depend on
`"fabric": "*"`, not `"fabric-api": "*"`.

**`common/shared` is compiled per-version at that version's `java_version`, so any newer Java
syntax there breaks the oldest target.** Adding 1.16.5 (`java_version=8`) surfaced this
immediately: `BrightnessConfig.java` used Java 16's pattern-matching `instanceof` (`if (x
instanceof Number number)`), which fails with "パターンの一致は-source 8でサポートされていません" —
fixed by reverting to classic `instanceof` + cast (works on every Java version this project
targets). This is a real trap for `common/shared` specifically (not per-version `common/{ver}`
code, which only needs to compile for its own single version) — any future syntax added there
must stay compatible with the *lowest* `java_version` across all supported versions, not just
whatever version was being edited at the time.

**`:forge:runClient` for 1.16.5 crashes in pure vanilla/Forge code, unrelated to this mod** — LWJGL
itself initializes fine this time (unlike 1.17.1/1.18.2's arch-mismatch crash), but model baking
then throws `NoSuchMethodError: com.mojang.math.Transformation.func_227987_b_()` from
`net.minecraft.core.BlockMath.<clinit>` — no `com.justbrightness`/`com.justbrightness.forge`
frame anywhere in the stack trace. This looks like an Architectury Loom 1.16.5 Forge dev-run
SRG-mapping mismatch inside Forge's own patched vanilla jar, not a defect in this mod's code —
verify via a real launcher instead of chasing this further in dev.

**1.16.5-era Forge doesn't bundle night-config** — unlike every other Forge version this project
supports (1.17.1+), which ship it and only need `compileOnly` on the Forge module.
`forge/1.16.5/build.gradle` needs `modImplementation include("com.electronwill.night-config:
core:...")` / `...toml:...` (the same Fabric-style embedding used on every `fabric/*/build.gradle`)
instead — confirmed via JustCoordinates' root `build.gradle`, which applies this exact embedding
to its Architectury-Loom Forge module too.

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
