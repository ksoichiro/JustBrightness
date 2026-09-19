# Just Brightness

A lightweight client-side mod that lets you toggle a fullbright-style gamma override on demand — supports Fabric and NeoForge.

## Supported Versions

| Minecraft | Fabric | NeoForge |
|-----------|--------|----------|
| 1.21.1    | Yes    | Yes      |

## Build

Build a specific platform for a target Minecraft version using `-Ptarget_mc_version`:

```
./gradlew :fabric:build -Ptarget_mc_version=1.21.1
./gradlew :neoforge:build -Ptarget_mc_version=1.21.1
```

The default `target_mc_version` is `1.21.1` (defined in `gradle.properties`), so the following also works:

```
./gradlew :fabric:build
```

Build outputs are located in `<platform>/<mc_version>/build/libs/` (e.g. `fabric/1.21.1/build/libs/`).

## License

LGPL-3.0-only
