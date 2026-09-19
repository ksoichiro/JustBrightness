# Just Brightness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship a working client-side "Just Brightness" mod for MC 1.21.1 and 26.3 (Fabric + NeoForge) that lets the player toggle a gamma override on/off with a keybind, with the target gamma and default on/off-at-world-join state configurable via a settings screen.

**Architecture:** A Mixin redirects the single `OptionInstance.get()` call inside `LightTexture.updateLightTexture` to return an above-vanilla-max gamma value while a runtime toggle is on, leaving vanilla's own brightness option untouched. Shared (cross-loader) Java code lives in `common/{version}/src/main/java`, gets recompiled into both the Fabric and NeoForge jars by each platform's own `compileJava` task (the same mechanism Just Coordinates already uses — see `commonJava`/`commonResources` configurations in this repo's build files), so there is exactly one copy of the Mixin, config, and controller code per MC version, not one per loader.

**Tech Stack:** Java 21 (MC 1.21.1) / Java 25 (MC 26.3), Fabric Loom + `net.fabricmc:fabric-loader`/`fabric-api`, NeoForge via `net.neoforged.moddev` (ModDevGradle), SpongePowered Mixin (bundled with both loaders), Gson (already on the MC classpath) for config persistence.

**Spec:** `docs/superpowers/specs/2026-09-19-just-brightness-design.md`

## Global Constraints

- Client-side only. No server-side registrations, no dedicated-server code paths that assume rendering classes exist.
- Never write to vanilla's `options.txt` / `Options` object. OFF must always fall back to whatever vanilla brightness the player has configured.
- mod_id: `justbrightness`. Root Java package: `com.justbrightness`.
- Loaders: Fabric and NeoForge only (no Forge yet).
- MC versions: 1.21.1 first, then 26.3 (Task 6). No other versions in this plan.
- No HUD indicator, no in-mod keybind-rebind UI (use vanilla Controls screen), no per-world/per-session toggle persistence — every world join re-applies the configured default on/off state.
- License: LGPL-3.0-only (matches Just Coordinates / Just Block Shapes).
- Config file name: `justbrightness.json`, written via Gson `JsonObject` with a `schema_version` field, following `HudConfig`'s pattern in Just Coordinates (`/Users/ksoichiro/src/github.com/ksoichiro/JustCoordinates/common/shared/src/main/java/com/justcoordinates/HudConfig.java`).

---

### Task 1: Bootstrap project skeleton (MC 1.21.1, Fabric + NeoForge)

**Files:**
- Modify: `/Users/ksoichiro/src/github.com/ksoichiro/JustBrightness/gradle.properties` (create)
- Create: `settings.gradle`, `build.gradle`
- Create: `props/1.21.1.properties`
- Create: `common/1.21.1/build.gradle`
- Create: `common/shared/src/main/java/com/justbrightness/JustBrightness.java`
- Create: `fabric/1.21.1/build.gradle`, `fabric/1.21.1/src/main/resources/fabric.mod.json`
- Create: `fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessFabric.java` (stub, filled out in Task 4)
- Create: `neoforge/1.21.1/build.gradle`, `neoforge/1.21.1/src/main/resources/META-INF/neoforge.mods.toml`
- Create: `neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java` (stub, filled out in Task 5)
- Create: `README.md`, `CLAUDE.md`
- Create submodule: `gradle/shared` (git submodule pointing at `minecraft-mod-gradle-scripts`)

**Interfaces:**
- Produces: `JustBrightness.MOD_ID` constant (`"justbrightness"`), consumed by every later task's Fabric/NeoForge entry classes and logger names.

- [ ] **Step 1: Add the shared Gradle scripts submodule**

```bash
cd /Users/ksoichiro/src/github.com/ksoichiro/JustBrightness
git submodule add https://github.com/ksoichiro/minecraft-mod-gradle-scripts gradle/shared
git submodule update --init
```

- [ ] **Step 2: Write `gradle.properties`**

```properties
# Gradle
org.gradle.jvmargs=-Xmx2G

# Project
mod_id=justbrightness
mod_name=Just Brightness
mod_version=0.1.0
mod_group=com.justbrightness
mod_description=Toggle a fullbright-style gamma override on demand.
mod_license=LGPL-3.0-only

# Plugin versions
fabric_loom_version=1.13.6
fabric_loom_26_version=1.15.5
moddevgradle_version=2.0.141
forgegradle_version=[7.0.17,8.0)
architectury_loom_version=1.13.467

# Default target MC version (override with -Ptarget_mc_version=...)
target_mc_version=1.21.1

# Multi-version build configuration (consumed by gradle/shared scripts)
supported_mc_versions=1.21.1,26.3
archives_name=justbrightness
release_project_name=Just Brightness
```

- [ ] **Step 3: Write `settings.gradle`** (copied from Just Coordinates, `rootProject.name` changed)

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven { url = 'https://maven.fabricmc.net/' }
        maven { url = 'https://maven.neoforged.net/releases/' }
        maven { url = 'https://maven.minecraftforge.net/' }
        maven { url = 'https://maven.architectury.dev/' }
    }

    def targetVer = settings.providers.gradleProperty('target_mc_version').getOrNull()
    plugins {
        if (targetVer.startsWith('1.')) {
            id 'fabric-loom' version settings.providers.gradleProperty('fabric_loom_version').get()
        } else {
            id 'net.fabricmc.fabric-loom' version settings.providers.gradleProperty('fabric_loom_26_version').get()
        }
    }
}

plugins {
    id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
}

rootProject.name = 'JustBrightness'

def targetMcVersion = providers.gradleProperty('target_mc_version').getOrNull()
if (targetMcVersion == null) {
    throw new GradleException("'target_mc_version' property is not defined.")
}

def propsFile = file("props/${targetMcVersion}.properties")
if (!propsFile.exists()) {
    throw new GradleException("props/${targetMcVersion}.properties not found")
}
def versionProps = new Properties()
propsFile.withInputStream { versionProps.load(it) }

gradle.beforeProject { project ->
    versionProps.each { key, value ->
        if (!project.hasProperty(key)) {
            project.ext.set(key, value)
        }
    }
}

def enabledPlatforms = versionProps.getProperty('enabled_platforms', '').split(',').collect { it.trim() }

include 'common'
project(':common').projectDir = file("common/${targetMcVersion}")

enabledPlatforms.each { platform ->
    def platformDir = file("${platform}/${targetMcVersion}")
    def baseDir = file("${platform}/base")
    def hasOwnSources = new File(platformDir, "src/main/java").exists()
    if (baseDir.exists() && !hasOwnSources) {
        include "${platform}-base"
        project(":${platform}-base").projectDir = baseDir
    }
    include platform
    project(":${platform}").projectDir = platformDir
}
```

Note: unlike Just Coordinates' `settings.gradle`, the NeoForge/Forge branch (`dev.architectury.loom` for 1.16.5, `net.minecraftforge.gradle` for Forge) is omitted — this plan only ever builds Fabric and NeoForge, and NeoForge is applied per-module via `net.neoforged.moddev` (declared directly in `neoforge/*/build.gradle`, not in `pluginManagement`), so no `pluginManagement.plugins` entry is needed for it.

- [ ] **Step 4: Write root `build.gradle`**

```groovy
plugins {
    id 'net.neoforged.moddev' version "${moddevgradle_version}" apply false
}

subprojects {
    apply plugin: 'java'

    if (project.name.endsWith('-base')) {
        layout.buildDirectory.set(file("${projectDir}/build/${project.property('target_mc_version')}"))
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(java_version as int)
        }
    }

    tasks.withType(JavaCompile).configureEach {
        options.encoding = 'UTF-8'
        options.release = java_version as int
    }

    repositories {
        maven { url = 'https://libraries.minecraft.net/' }
        mavenCentral()
    }

    afterEvaluate {
        if (!project.name.endsWith('-base') && project.name != 'common') {
            base {
                archivesName = "${mod_id}-${mod_version}+${minecraft_version}-${project.name}"
            }
        }
    }
}

if (!file('gradle/shared/multi-version-tasks.gradle').exists()) {
    throw new GradleException(
        "Shared Gradle scripts not found. Run: git submodule update --init"
    )
}
apply from: 'gradle/shared/multi-version-tasks.gradle'
apply from: 'gradle/shared/release-modrinth.gradle'
apply from: 'gradle/shared/release-curseforge.gradle'
```

- [ ] **Step 5: Write `props/1.21.1.properties`**

```properties
minecraft_version=1.21.1
java_version=21
fabric_loader_version=0.16.10
fabric_api_version=0.116.7+1.21.1
modmenu_version=11.0.4
neoforge_version=21.1.219
neoform_version=1.21.1-20240808.144430
enabled_platforms=fabric,neoforge
```

- [ ] **Step 6: Write `common/1.21.1/build.gradle`**

```groovy
plugins {
    id 'net.neoforged.moddev'
}

neoForge {
    neoFormVersion = neoform_version
}

sourceSets.main {
    java {
        srcDir rootProject.file('common/shared/src/main/java')
    }
}

configurations {
    commonJava {
        canBeConsumed = true
        canBeResolved = false
    }
    commonResources {
        canBeConsumed = true
        canBeResolved = false
    }
}

artifacts {
    commonJava sourceSets.main.java.srcDirs.first()
    commonResources sourceSets.main.resources.srcDirs.first()
}
```

- [ ] **Step 7: Write the shared mod-id constant**

`common/shared/src/main/java/com/justbrightness/JustBrightness.java`:

```java
package com.justbrightness;

public class JustBrightness {
    public static final String MOD_ID = "justbrightness";
}
```

- [ ] **Step 8: Write `fabric/1.21.1/build.gradle`**

```groovy
plugins {
    id 'fabric-loom'
}

configurations {
    commonJava {
        canBeResolved = true
    }
    commonResources {
        canBeResolved = true
    }
}

dependencies {
    minecraft "com.mojang:minecraft:${minecraft_version}"
    mappings loom.officialMojangMappings()
    modImplementation "net.fabricmc:fabric-loader:${fabric_loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${fabric_api_version}"

    compileOnly project(':common')
    compileOnly project(':fabric-base')

    if (project.hasProperty('modmenu_version')) {
        modCompileOnly "com.terraformersmc:modmenu:${modmenu_version}"
    }

    commonJava project(path: ':common', configuration: 'commonJava')
    commonResources project(path: ':common', configuration: 'commonResources')
}

sourceSets.main {
    java {
        srcDir rootProject.file('fabric/base/src/main/java')
        srcDir rootProject.file('common/shared/src/main/java')
    }
}

tasks.named('compileJava') {
    dependsOn configurations.commonJava
    source configurations.commonJava
}

tasks.named('processResources') {
    dependsOn configurations.commonResources
    from configurations.commonResources

    filesMatching('fabric.mod.json') {
        expand(
            version: mod_version,
            minecraft_version: minecraft_version,
            fabric_loader_version: fabric_loader_version,
        )
    }
}
```

- [ ] **Step 9: Write `fabric/1.21.1/src/main/resources/fabric.mod.json`**

```json
{
  "schemaVersion": 1,
  "id": "justbrightness",
  "version": "${version}",
  "name": "Just Brightness",
  "description": "Toggle a fullbright-style gamma override on demand.",
  "authors": [],
  "license": "LGPL-3.0-only",
  "environment": "client",
  "entrypoints": {
    "client": [
      "com.justbrightness.fabric.JustBrightnessFabric"
    ]
  },
  "depends": {
    "fabricloader": ">=${fabric_loader_version}",
    "fabric-api": "*",
    "minecraft": "${minecraft_version}"
  }
}
```

- [ ] **Step 10: Write a minimal Fabric entrypoint stub** (fully wired in Task 4)

`fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessFabric.java`:

```java
package com.justbrightness.fabric;

import net.fabricmc.api.ClientModInitializer;

public class JustBrightnessFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
    }
}
```

- [ ] **Step 11: Write `neoforge/1.21.1/build.gradle`**

```groovy
plugins {
    id 'net.neoforged.moddev'
}

neoForge {
    version = neoforge_version

    runs {
        client {
            client()
        }
    }

    mods {
        "${mod_id}" {
            sourceSet sourceSets.main
        }
    }
}

configurations {
    commonJava {
        canBeResolved = true
    }
    commonResources {
        canBeResolved = true
    }
}

dependencies {
    compileOnly project(':common')
    compileOnly project(':neoforge-base')

    commonJava project(path: ':common', configuration: 'commonJava')
    commonResources project(path: ':common', configuration: 'commonResources')
}

sourceSets.main {
    java {
        srcDir rootProject.file('neoforge/base/src/main/java')
        srcDir rootProject.file('common/shared/src/main/java')
    }
}

tasks.named('compileJava') {
    dependsOn configurations.commonJava
    source configurations.commonJava
}

tasks.named('processResources') {
    dependsOn configurations.commonResources
    from configurations.commonResources

    filesMatching('META-INF/neoforge.mods.toml') {
        expand(
            version: mod_version,
            minecraft_version: minecraft_version,
            neoforge_version: neoforge_version,
        )
    }
}
```

- [ ] **Step 12: Write `neoforge/1.21.1/src/main/resources/META-INF/neoforge.mods.toml`**

```toml
modLoader = "javafml"
loaderVersion = "[4,)"
license = "LGPL-3.0-only"

[[mods]]
modId = "justbrightness"
version = "${version}"
displayName = "Just Brightness"
description = "Toggle a fullbright-style gamma override on demand."

[[dependencies.justbrightness]]
modId = "neoforge"
type = "required"
versionRange = "[${neoforge_version},)"
ordering = "NONE"
side = "CLIENT"

[[dependencies.justbrightness]]
modId = "minecraft"
type = "required"
versionRange = "[${minecraft_version},)"
ordering = "NONE"
side = "CLIENT"
```

- [ ] **Step 13: Write a minimal NeoForge entrypoint stub** (fully wired in Task 5)

`neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java`:

```java
package com.justbrightness.neoforge;

import com.justbrightness.JustBrightness;
import net.neoforged.fml.common.Mod;

@Mod(JustBrightness.MOD_ID)
public class JustBrightnessNeoForge {
}
```

- [ ] **Step 14: Build both platforms and verify they produce jars**

```bash
cd /Users/ksoichiro/src/github.com/ksoichiro/JustBrightness
./gradlew :fabric:build -Ptarget_mc_version=1.21.1
./gradlew :neoforge:build -Ptarget_mc_version=1.21.1
```

Expected: both succeed; `fabric/1.21.1/build/libs/` and `neoforge/1.21.1/build/libs/` each contain a `justbrightness-0.1.0+1.21.1-*.jar`.

- [ ] **Step 15: Write `README.md` and `CLAUDE.md`, commit**

`README.md` should briefly describe the mod (one paragraph) — mirror Just Coordinates' README tone. `CLAUDE.md` should mirror Just Coordinates' `CLAUDE.md` structure (Build / Architecture sections) once the architecture below is real; for now note it targets 1.21.1 + 26.3, Fabric + NeoForge only.

```bash
git add gradle.properties settings.gradle build.gradle props/1.21.1.properties \
  common/1.21.1/build.gradle common/shared fabric/1.21.1 fabric/base \
  neoforge/1.21.1 neoforge/base README.md CLAUDE.md .gitmodules gradle/shared
git commit -m "feat: bootstrap project skeleton for MC 1.21.1 (Fabric + NeoForge)"
```

---

### Task 2: Shared brightness config and runtime state

**Files:**
- Create: `common/shared/src/main/java/com/justbrightness/BrightnessConfig.java`
- Create: `common/shared/src/main/java/com/justbrightness/BrightnessState.java`

**Interfaces:**
- Consumes: `JustBrightness.MOD_ID` (Task 1).
- Produces: `BrightnessConfig.getGamma()/setGamma(double)`, `BrightnessConfig.isDefaultEnabled()/setDefaultEnabled(boolean)`, `BrightnessConfig.load(Path)/save()`, `BrightnessConfig.MIN_GAMMA`/`MAX_GAMMA` (double constants) — consumed by the Mixin (Task 3), the config screen (Task 5), and both loader entry classes (Tasks 4-5). `BrightnessState.isEnabled()`, `setEnabled(boolean)`, `toggle()`, `getOverrideGamma()` (`Double`), `applyWorldJoinDefault()` — consumed by the Mixin (Task 3), the controller (Task 4), and both loader entry classes (Task 5).

There is no Minecraft-independent test module in this repo (Just Coordinates has none either — `common/shared` only compiles as part of a platform build), so this task is verified by building, not by a JUnit test; each step below still stays independently checkable via `javac`-level compilation in Step 3.

- [ ] **Step 1: Write `BrightnessConfig`**

```java
package com.justbrightness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BrightnessConfig {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final double MIN_GAMMA = 1.0;
    public static final double MAX_GAMMA = 32.0;
    public static final double DEFAULT_GAMMA = 16.0;

    private static final String FILE_NAME = "justbrightness.json";
    private static final Logger LOGGER = LogManager.getLogger(JustBrightness.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configFile;
    private static double gamma = DEFAULT_GAMMA;
    private static boolean defaultEnabled = false;

    private BrightnessConfig() {
    }

    public static double getGamma() {
        return gamma;
    }

    public static void setGamma(double value) {
        gamma = Math.max(MIN_GAMMA, Math.min(MAX_GAMMA, value));
    }

    public static boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public static void setDefaultEnabled(boolean value) {
        defaultEnabled = value;
    }

    public static void load(Path configDir) {
        configFile = configDir.resolve(FILE_NAME);
        if (!Files.exists(configFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                LOGGER.warn("{} is empty; using defaults", FILE_NAME);
                return;
            }
            if (json.has("gamma")) {
                setGamma(json.get("gamma").getAsDouble());
            }
            if (json.has("default_enabled")) {
                defaultEnabled = json.get("default_enabled").getAsBoolean();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to read {}; using defaults", FILE_NAME, e);
            gamma = DEFAULT_GAMMA;
            defaultEnabled = false;
        }
    }

    public static void save() {
        if (configFile == null) {
            LOGGER.warn("Config was never loaded; skipping save");
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("schema_version", CURRENT_SCHEMA_VERSION);
        json.addProperty("gamma", gamma);
        json.addProperty("default_enabled", defaultEnabled);
        try {
            Files.createDirectories(configFile.getParent());
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(json, writer);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Failed to save {}", FILE_NAME, e);
        }
    }
}
```

- [ ] **Step 2: Write `BrightnessState`**

```java
package com.justbrightness;

public final class BrightnessState {
    private static boolean enabled = false;

    private BrightnessState() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    public static void toggle() {
        enabled = !enabled;
    }

    public static Double getOverrideGamma() {
        return BrightnessConfig.getGamma();
    }

    public static void applyWorldJoinDefault() {
        enabled = BrightnessConfig.isDefaultEnabled();
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
cd /Users/ksoichiro/src/github.com/ksoichiro/JustBrightness
./gradlew :fabric:compileJava -Ptarget_mc_version=1.21.1
./gradlew :neoforge:compileJava -Ptarget_mc_version=1.21.1
```

Expected: both succeed (the new classes have no Minecraft dependency, so this mainly confirms the `commonJava` wiring from Task 1 correctly pulls new files from `common/shared`).

- [ ] **Step 4: Commit**

```bash
git add common/shared/src/main/java/com/justbrightness/BrightnessConfig.java \
  common/shared/src/main/java/com/justbrightness/BrightnessState.java
git commit -m "feat: add brightness config and runtime toggle state"
```

---

### Task 3: Gamma override Mixin (MC 1.21.1)

**Files:**
- Create: `common/1.21.1/src/main/resources/justbrightness.mixins.json`
- Create: `common/1.21.1/src/main/java/com/justbrightness/mixin/GammaOverrideMixin.java`
- Modify: `fabric/1.21.1/src/main/resources/fabric.mod.json:1-20` (add `"mixins"` array)
- Modify: `neoforge/1.21.1/src/main/resources/META-INF/neoforge.mods.toml` (add `[[mixins]]` block)

**Interfaces:**
- Consumes: `BrightnessState.isEnabled()`, `BrightnessState.getOverrideGamma()` (Task 2).
- Produces: the actual brightening effect — nothing later depends on this class directly, but Task 6 duplicates its approach for MC 26.3.

`LightTexture.updateLightTexture(float partialTick)` is the vanilla method (stable under Mojang mappings) that reads `this.minecraft.options.gamma().get()` once to compute the lightmap. This task redirects that single call. Because obfuscation/decompiled output can drift between snapshots, Step 1 verifies the exact call site exists before the Mixin is written against it — do not skip it.

- [ ] **Step 1: Confirm the injection target in the actual MC 1.21.1 source**

In your IDE (IntelliJ/VS Code with the Minecraft Development plugin), open `net.minecraft.client.renderer.LightTexture` via "Go to Class" (the project already resolves Mojang-mapped sources through `mappings loom.officialMojangMappings()` in `fabric/1.21.1/build.gradle`). Locate `updateLightTexture(float partialTick)` and count how many times `OptionInstance.get()` (i.e. any `this.minecraft.options.<option>().get()` call) appears in that method body.

Expected: exactly one such call, reading the gamma option. If there is more than one, note its position (0-indexed) among all `OptionInstance.get()` calls in the method — you will need it in Step 2's `ordinal`.

- [ ] **Step 2: Write the mixin config**

`common/1.21.1/src/main/resources/justbrightness.mixins.json`:

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.justbrightness.mixin",
  "compatibilityLevel": "JAVA_21",
  "client": [
    "GammaOverrideMixin"
  ],
  "injectors": {
    "defaultRequire": 1
  }
}
```

- [ ] **Step 3: Write the mixin class**

`common/1.21.1/src/main/java/com/justbrightness/mixin/GammaOverrideMixin.java`:

```java
package com.justbrightness.mixin;

import com.justbrightness.BrightnessState;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public class GammaOverrideMixin {

    @Redirect(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"
            )
    )
    private Object justbrightness$redirectGamma(OptionInstance<?> instance) {
        if (BrightnessState.isEnabled()) {
            return BrightnessState.getOverrideGamma();
        }
        return instance.get();
    }
}
```

If Step 1 found more than one `OptionInstance.get()` call in `updateLightTexture`, add `ordinal = <index>` inside the `@At(...)` annotation to target only the gamma read.

- [ ] **Step 4: Wire the mixin config into `fabric.mod.json`**

In `fabric/1.21.1/src/main/resources/fabric.mod.json`, add a top-level `"mixins"` array:

```json
  "mixins": [
    "justbrightness.mixins.json"
  ],
```

(insert as a sibling of `"entrypoints"` and `"depends"`, i.e. before the closing `}`).

- [ ] **Step 5: Wire the mixin config into `neoforge.mods.toml`**

In `neoforge/1.21.1/src/main/resources/META-INF/neoforge.mods.toml`, add:

```toml
[[mixins]]
config="justbrightness.mixins.json"
```

(insert directly under the `[[mods]]` block, before the `[[dependencies.justbrightness]]` entries).

- [ ] **Step 6: Build both platforms and check for Mixin application errors**

```bash
cd /Users/ksoichiro/src/github.com/ksoichiro/JustBrightness
./gradlew :fabric:build -Ptarget_mc_version=1.21.1
./gradlew :neoforge:build -Ptarget_mc_version=1.21.1
```

Expected: both succeed with no "Mixin apply failed" / "refmap" errors in the build output. If a refmap error appears, add `"refmap": "justbrightness-refmap.json"` to `justbrightness.mixins.json` under each platform's mixin config individually (Fabric and NeoForge would then need per-platform copies of the config instead of the single shared one — only do this if the shared refmap actually collides).

- [ ] **Step 7: Manual in-game verification**

```bash
./gradlew :fabric:runClient -Ptarget_mc_version=1.21.1
```

In a world, open the chat/debug and temporarily call `BrightnessState.setEnabled(true)` is not exposed yet (no keybind until Task 4) — instead, edit `BrightnessState.enabled`'s default in Step 2 of this task temporarily to `true`, relaunch, confirm the screen is uniformly bright (including unlit areas like a closed room), then revert the default back to `false` before committing.

- [ ] **Step 8: Commit**

```bash
git add common/1.21.1/src/main/resources/justbrightness.mixins.json \
  common/1.21.1/src/main/java/com/justbrightness/mixin/GammaOverrideMixin.java \
  fabric/1.21.1/src/main/resources/fabric.mod.json \
  neoforge/1.21.1/src/main/resources/META-INF/neoforge.mods.toml
git commit -m "feat: add gamma override mixin for MC 1.21.1"
```

---

### Task 4: Keybinding + tick controller

**Files:**
- Create: `common/shared/src/main/java/com/justbrightness/BrightnessController.java`
- Modify: `fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessFabric.java` (Task 1 stub)
- Modify: `neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java` (Task 1 stub)
- Create: `common/1.21.1/src/main/resources/assets/justbrightness/lang/en_us.json`
- Create: `common/1.21.1/src/main/resources/assets/justbrightness/lang/ja_jp.json`

**Interfaces:**
- Consumes: `BrightnessState.toggle()`, `BrightnessState.applyWorldJoinDefault()` (Task 2).
- Produces: `BrightnessController.getToggleKey()` / `getOpenConfigKey()` (`KeyMapping`), `BrightnessController.handleTick()` — consumed by both loader entry classes here and finished off in Task 5 (open-config keybind opens `ConfigScreen`, added in that task).

- [ ] **Step 1: Write `BrightnessController`**

```java
package com.justbrightness;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class BrightnessController {
    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.justbrightness.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.justbrightness"
    );

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.justbrightness.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.justbrightness"
    );

    public static KeyMapping getToggleKey() {
        return TOGGLE_KEY;
    }

    public static KeyMapping getOpenConfigKey() {
        return OPEN_CONFIG_KEY;
    }

    public static void handleTick() {
        while (TOGGLE_KEY.consumeClick()) {
            BrightnessState.toggle();
        }
    }
}
```

(`OPEN_CONFIG_KEY` is registered here but only consumed starting in Task 5, once `ConfigScreen` exists — `handleTick()` gains its `while (OPEN_CONFIG_KEY.consumeClick())` branch in that task.)

- [ ] **Step 2: Wire the keybind + tick handler into Fabric**

Replace `fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessFabric.java`:

```java
package com.justbrightness.fabric;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public class JustBrightnessFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BrightnessConfig.load(FabricLoader.getInstance().getConfigDir());
        KeyBindingHelper.registerKeyBinding(BrightnessController.getToggleKey());
        KeyBindingHelper.registerKeyBinding(BrightnessController.getOpenConfigKey());
        ClientTickEvents.END_CLIENT_TICK.register(client -> BrightnessController.handleTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> BrightnessState.applyWorldJoinDefault());
    }
}
```

- [ ] **Step 3: Wire the keybind + tick handler into NeoForge**

Replace `neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java`:

```java
package com.justbrightness.neoforge;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import com.justbrightness.JustBrightness;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(JustBrightness.MOD_ID)
public class JustBrightnessNeoForge {

    public JustBrightnessNeoForge(ModContainer container) {
        BrightnessConfig.load(FMLPaths.CONFIGDIR.get());
    }

    @EventBusSubscriber(modid = JustBrightness.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(BrightnessController.getToggleKey());
            event.register(BrightnessController.getOpenConfigKey());
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            BrightnessController.handleTick();
        }

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            BrightnessState.applyWorldJoinDefault();
        }
    }
}
```

(the client-only config-screen registration from the `ModContainer container` constructor parameter is added in Task 5, following Just Coordinates' `*NeoForgeClient` split — do not add a `Screen`-typed reference to this class before then.)

- [ ] **Step 4: Write lang files**

`common/1.21.1/src/main/resources/assets/justbrightness/lang/en_us.json`:

```json
{
  "key.justbrightness.toggle": "Toggle Brightness",
  "key.justbrightness.open_config": "Open Settings",
  "key.categories.justbrightness": "Just Brightness"
}
```

`common/1.21.1/src/main/resources/assets/justbrightness/lang/ja_jp.json`:

```json
{
  "key.justbrightness.toggle": "明るさを切り替え",
  "key.justbrightness.open_config": "設定を開く",
  "key.categories.justbrightness": "Just Brightness"
}
```

- [ ] **Step 5: Build and manually verify the keybind toggles brightness**

```bash
./gradlew :fabric:runClient -Ptarget_mc_version=1.21.1
```

In a world, press B: the screen should immediately brighten (including dark areas); press B again to revert. Open Controls in the options menu and confirm "Toggle Brightness" appears under the "Just Brightness" category and can be rebound.

- [ ] **Step 6: Commit**

```bash
git add common/shared/src/main/java/com/justbrightness/BrightnessController.java \
  fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessFabric.java \
  neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java \
  common/1.21.1/src/main/resources/assets/justbrightness/lang
git commit -m "feat: wire toggle keybind and world-join default state"
```

---

### Task 5: Config screen (gamma slider + default-enabled toggle)

**Files:**
- Create: `common/shared/src/main/java/com/justbrightness/ConfigScreen.java`
- Create: `neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForgeClient.java`
- Modify: `neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java:1-15` (Task 4)
- Create: `fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessModMenu.java`
- Modify: `fabric/1.21.1/src/main/resources/fabric.mod.json` (add `modmenu` entrypoint)
- Modify: `common/shared/src/main/java/com/justbrightness/BrightnessController.java:26-30` (Task 4 — add the open-config branch)
- Modify: `common/1.21.1/src/main/resources/assets/justbrightness/lang/en_us.json`, `.../ja_jp.json` (Task 4)

**Interfaces:**
- Consumes: `BrightnessConfig.getGamma()/setGamma()/isDefaultEnabled()/setDefaultEnabled()/save()`, `BrightnessConfig.MIN_GAMMA`/`MAX_GAMMA` (Task 2).
- Produces: `ConfigScreen(Screen parent)` — a vanilla `Screen` subclass, consumed by both loaders' config-screen registration.

- [ ] **Step 1: Write `ConfigScreen`**

```java
package com.justbrightness;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("justbrightness.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        double range = BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA;
        double initialProgress = (BrightnessConfig.getGamma() - BrightnessConfig.MIN_GAMMA) / range;

        addRenderableWidget(new GammaSlider(width / 2 - 100, height / 2 - 36, 200, 20, initialProgress));

        addRenderableWidget(CycleButton.onOffBuilder(BrightnessConfig.isDefaultEnabled())
                .create(width / 2 - 100, height / 2 - 12, 200, 20,
                        Component.translatable("justbrightness.config.default_enabled"),
                        (button, value) -> BrightnessConfig.setDefaultEnabled(value)));

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height / 2 + 12, 200, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        BrightnessConfig.save();
        minecraft.setScreen(parent);
    }

    private static class GammaSlider extends AbstractSliderButton {
        GammaSlider(int x, int y, int width, int height, double initialProgress) {
            super(x, y, width, height, CommonComponents.EMPTY, initialProgress);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double gamma = BrightnessConfig.MIN_GAMMA
                    + value * (BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA);
            setMessage(Component.translatable("justbrightness.config.gamma",
                    String.format("%.1f", gamma)));
        }

        @Override
        protected void applyValue() {
            double gamma = BrightnessConfig.MIN_GAMMA
                    + value * (BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA);
            BrightnessConfig.setGamma(gamma);
        }
    }
}
```

- [ ] **Step 2: Add the open-config branch to `BrightnessController.handleTick()`**

In `common/shared/src/main/java/com/justbrightness/BrightnessController.java`, add the import `net.minecraft.client.Minecraft` and change `handleTick()` to:

```java
    public static void handleTick() {
        while (TOGGLE_KEY.consumeClick()) {
            BrightnessState.toggle();
        }
        while (OPEN_CONFIG_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new ConfigScreen(null));
        }
    }
```

- [ ] **Step 3: Add NeoForge client-only config-screen registration**

`neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForgeClient.java`:

```java
package com.justbrightness.neoforge;

import com.justbrightness.ConfigScreen;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

final class JustBrightnessNeoForgeClient {
    private JustBrightnessNeoForgeClient() {
    }

    static void registerConfigScreen(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class,
                (minecraftOrContainer, parent) -> new ConfigScreen(parent));
    }
}
```

In `neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java`, add the imports `net.neoforged.api.distmarker.Dist` and `net.neoforged.fml.loading.FMLEnvironment`, and change the constructor to:

```java
    public JustBrightnessNeoForge(ModContainer container) {
        BrightnessConfig.load(FMLPaths.CONFIGDIR.get());
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            JustBrightnessNeoForgeClient.registerConfigScreen(container);
        }
    }
```

- [ ] **Step 4: Add Fabric Mod Menu integration**

`fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessModMenu.java`:

```java
package com.justbrightness.fabric;

import com.justbrightness.ConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class JustBrightnessModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return ConfigScreen::new;
    }
}
```

In `fabric/1.21.1/src/main/resources/fabric.mod.json`, add `"modmenu"` to `"entrypoints"`:

```json
  "entrypoints": {
    "client": [
      "com.justbrightness.fabric.JustBrightnessFabric"
    ],
    "modmenu": [
      "com.justbrightness.fabric.JustBrightnessModMenu"
    ]
  },
```

- [ ] **Step 5: Add config-screen translation strings**

Add to both `common/1.21.1/src/main/resources/assets/justbrightness/lang/en_us.json` and `ja_jp.json`:

English:
```json
  "justbrightness.config.title": "Just Brightness",
  "justbrightness.config.gamma": "Gamma: %s",
  "justbrightness.config.default_enabled": "Enabled by Default"
```

Japanese:
```json
  "justbrightness.config.title": "Just Brightness",
  "justbrightness.config.gamma": "ガンマ値: %s",
  "justbrightness.config.default_enabled": "デフォルトで有効"
```

(merge these keys into the existing JSON objects from Task 4, keeping valid JSON.)

- [ ] **Step 6: Build and manually verify the config screen**

```bash
./gradlew :fabric:runClient -Ptarget_mc_version=1.21.1
```

Open the mod's settings (via Mod Menu on Fabric, or the in-game mod list "Config" button on NeoForge — run `:neoforge:runClient -Ptarget_mc_version=1.21.1` to check that path too). Confirm: dragging the gamma slider changes the displayed value; toggling "Enabled by Default" persists across a game restart (check `justbrightness.json` in the run directory's `config/` folder after clicking Done); joining a new world reflects the configured default on/off state.

- [ ] **Step 7: Commit**

```bash
git add common/shared/src/main/java/com/justbrightness/ConfigScreen.java \
  common/shared/src/main/java/com/justbrightness/BrightnessController.java \
  neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForgeClient.java \
  neoforge/base/src/main/java/com/justbrightness/neoforge/JustBrightnessNeoForge.java \
  fabric/base/src/main/java/com/justbrightness/fabric/JustBrightnessModMenu.java \
  fabric/1.21.1/src/main/resources/fabric.mod.json \
  common/1.21.1/src/main/resources/assets/justbrightness/lang
git commit -m "feat: add config screen for gamma and default-enabled state"
```

---

### Task 6: Port to MC 26.3 (Fabric + NeoForge)

**Files:**
- Create: `props/26.3.properties`
- Create: `common/26.3/build.gradle`, `common/26.3/src/main/resources/justbrightness.mixins.json`
- Create: `common/26.3/src/main/java/com/justbrightness/mixin/GammaOverrideMixin.java` (may be identical to 1.21.1's, or adjusted per Step 2 below)
- Create: `common/26.3/src/main/resources/assets/justbrightness/lang/en_us.json`, `ja_jp.json` (copied from 1.21.1)
- Create: `fabric/26.3/build.gradle`, `fabric/26.3/src/main/resources/fabric.mod.json`
- Create: `neoforge/26.3/build.gradle`, `neoforge/26.3/src/main/resources/META-INF/neoforge.mods.toml`

**Interfaces:**
- Consumes: everything from Tasks 1-5 (all shared code in `common/shared` is reused as-is; only the per-version `common/{version}` Mixin needs re-verification).

- [ ] **Step 1: Write `props/26.3.properties`**

```properties
minecraft_version=26.3
java_version=25
fabric_loader_version=0.19.5
fabric_api_version=0.160.6+26.3
modmenu_version=21.0.0-beta.1
neoforge_version=26.3.0.6-beta
neoform_version=26.3-1
enabled_platforms=fabric,neoforge
```

(values copied from Just Coordinates' `props/26.3.properties`, which already ships a working 26.3 build.)

- [ ] **Step 2: Copy the version-specific build files and resources**

```bash
cd /Users/ksoichiro/src/github.com/ksoichiro/JustBrightness
mkdir -p common/26.3/src/main/resources/assets/justbrightness/lang
mkdir -p common/26.3/src/main/java/com/justbrightness/mixin
mkdir -p fabric/26.3/src/main/resources
mkdir -p neoforge/26.3/src/main/resources/META-INF

cp common/1.21.1/build.gradle common/26.3/build.gradle
cp common/1.21.1/src/main/resources/justbrightness.mixins.json common/26.3/src/main/resources/justbrightness.mixins.json
cp common/1.21.1/src/main/java/com/justbrightness/mixin/GammaOverrideMixin.java common/26.3/src/main/java/com/justbrightness/mixin/GammaOverrideMixin.java
cp common/1.21.1/src/main/resources/assets/justbrightness/lang/en_us.json common/26.3/src/main/resources/assets/justbrightness/lang/en_us.json
cp common/1.21.1/src/main/resources/assets/justbrightness/lang/ja_jp.json common/26.3/src/main/resources/assets/justbrightness/lang/ja_jp.json

cp fabric/1.21.1/build.gradle fabric/26.3/build.gradle
cp fabric/1.21.1/src/main/resources/fabric.mod.json fabric/26.3/src/main/resources/fabric.mod.json

cp neoforge/1.21.1/build.gradle neoforge/26.3/build.gradle
cp neoforge/1.21.1/src/main/resources/META-INF/neoforge.mods.toml neoforge/26.3/src/main/resources/META-INF/neoforge.mods.toml
```

- [ ] **Step 3: Re-verify the gamma injection point for 26.3**

Same as Task 3 Step 1, but against the 26.3 decompiled source: build `:fabric:compileJava -Ptarget_mc_version=26.3` once first (so the IDE resolves 26.3's Mojang mappings), then re-open `net.minecraft.client.renderer.LightTexture#updateLightTexture` and re-count `OptionInstance.get()` calls. If the method or the surrounding class was renamed/restructured for 26.3 (`LightTexture` has been a stable name historically, but 26.x is a newer major line — verify rather than assume), update `common/26.3/src/main/java/com/justbrightness/mixin/GammaOverrideMixin.java`'s `@Mixin`/`method`/`ordinal` accordingly before proceeding.

- [ ] **Step 4: Build both platforms for 26.3**

```bash
./gradlew :fabric:build -Ptarget_mc_version=26.3
./gradlew :neoforge:build -Ptarget_mc_version=26.3
```

Expected: both succeed, no Mixin application errors, matching Task 3 Step 6's check.

- [ ] **Step 5: Manual in-game verification on 26.3**

```bash
./gradlew :fabric:runClient -Ptarget_mc_version=26.3
```

Repeat Task 4 Step 5 and Task 5 Step 6's checks (toggle keybind brightens/reverts the screen including dark areas; config screen gamma slider and default-enabled toggle work; world-join applies the configured default).

- [ ] **Step 6: Commit**

```bash
git add props/26.3.properties common/26.3 fabric/26.3 neoforge/26.3
git commit -m "feat: add MC 26.3 support (Fabric + NeoForge)"
```

---

## Follow-up (not in this plan)

- Forge support for both MC versions (deferred per spec's non-goals).
- Registering the mod on Modrinth/CurseForge and wiring `gradle/shared/release-*.gradle` with real project IDs (out of scope — this plan only covers building a working mod, not publishing it).
- The family-setup bundling mechanism (modpack) — separate sub-project, to be brainstormed on its own once this mod is working.
