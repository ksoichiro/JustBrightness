# Just Brightness

Just Brightness is a lightweight client-side brightness toggle for Minecraft.

Press a key to apply your preferred gamma value while playing. It does not change
the brightness setting saved by Minecraft, so turning it off returns rendering to
your usual vanilla brightness setting.

## Supported Versions

| Minecraft | Mod loaders |
| --- | --- |
| 1.21.1 | Fabric, NeoForge, Forge |
| 1.21.3 | Fabric, NeoForge, Forge |
| 1.21.4 | Fabric, NeoForge, Forge |
| 1.21.5 | Fabric, NeoForge, Forge |
| 1.21.6 | Fabric, NeoForge, Forge |
| 1.21.7 | Fabric, NeoForge, Forge |
| 1.21.8 | Fabric, NeoForge, Forge |
| 1.21.9 | Fabric, NeoForge, Forge |
| 1.21.10 | Fabric, NeoForge, Forge |
| 1.21.11 | Fabric, NeoForge, Forge |
| 26.1.2 | Fabric, NeoForge, Forge |
| 26.2 | Fabric, NeoForge, Forge |
| 26.3 | Fabric, NeoForge, Forge |

It is client-side only. Install it on the client, not on a dedicated server.
Please follow the rules of every multiplayer server you join.

## Features

- Toggle enhanced brightness instantly with a configurable keybind, `B` by default
- Choose the gamma value used while enhanced brightness is enabled, from 1.0 to 32.0
- Enhanced brightness starts enabled when joining a world, and can be changed in settings
- Show or hide the temporary action-bar message displayed after a toggle
- Open the in-game settings screen from a separate, unbound keybind
- Rebind both keys in Minecraft's standard Controls screen
- No runtime dependencies beyond the selected mod loader

## Installation

1. Install the matching version of [Fabric](https://fabricmc.net/), [NeoForge](https://neoforged.net/), or [Forge](https://files.minecraftforge.net/) for your Minecraft version.
2. Download the matching Just Brightness release from CurseForge or Modrinth.
3. Place the JAR file in the instance's `mods` folder.
4. Start Minecraft.

## Usage

- Press `B` to toggle enhanced brightness. Change this key in **Options → Controls → Just Brightness**.
- Bind **Open Settings** in the same Controls category to open the configuration screen.
- Select a gamma value and choose whether it should be enabled by default when entering a world.

The configuration is stored in `config/justbrightness.toml`.

## Compatibility

Just Brightness changes the client renderer's gamma value only. It does not add
content, communicate with servers, or modify worlds. Shader packs can handle
lighting independently, so the visible result may differ by shader pack.

## Building from Source

This repository uses a Git submodule (`gradle/shared`) for shared Gradle scripts.
Initialize it after cloning:

```bash
git clone --recurse-submodules https://github.com/ksoichiro/JustBrightness.git
```

Build a target by specifying its Minecraft version and loader:

```bash
./gradlew :forge:build -Ptarget_mc_version=1.21.1
./gradlew :forge:build -Ptarget_mc_version=1.21.3
./gradlew :forge:build -Ptarget_mc_version=1.21.4
./gradlew :forge:build -Ptarget_mc_version=1.21.5
./gradlew :forge:build -Ptarget_mc_version=1.21.6
./gradlew :forge:build -Ptarget_mc_version=1.21.7
./gradlew :forge:build -Ptarget_mc_version=1.21.8
./gradlew :forge:build -Ptarget_mc_version=1.21.9
./gradlew :forge:build -Ptarget_mc_version=1.21.10
./gradlew :forge:build -Ptarget_mc_version=1.21.11
./gradlew :forge:build -Ptarget_mc_version=26.1.2
./gradlew :forge:build -Ptarget_mc_version=26.2
./gradlew :forge:build -Ptarget_mc_version=26.3
```

The JAR is written to `<loader>/<minecraft-version>/build/libs/`.

## License

[LGPL-3.0-only](COPYING.LESSER)
