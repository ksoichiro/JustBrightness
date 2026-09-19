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

- `common/shared/` — Version-independent code (e.g. `JustBrightness.MOD_ID`). Every platform build.gradle adds this dir via `srcDir` — `compileOnly project(':common')` alone compiles but silently omits these classes from the jar
- `common/{version}/` — Version-specific MC API usage
- `fabric/base/` — Shared Fabric entry point
- `fabric/{version}/` — Fabric mod metadata
- `neoforge/base/` — Shared NeoForge entry point
- `neoforge/{version}/` — NeoForge mod metadata
- `props/{version}.properties` — Version-specific dependency versions
