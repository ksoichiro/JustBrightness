# Just Brightness - CurseForge Description

**A lightweight client-side brightness toggle for Minecraft on Fabric, NeoForge, and Forge.**

## Make dark places easier to see

Just Brightness lets you enable or disable an enhanced gamma value with one key
press. Use it while exploring caves, building at night, or whenever you want a
brighter view, then switch back to your normal vanilla brightness just as quickly.

The mod changes the gamma value used by the client renderer. It never writes to
Minecraft's saved brightness setting, so disabling it restores your own vanilla
brightness setting.

## Features

- Press `B` by default to toggle enhanced brightness
- Set the gamma used while enabled, from 1.0 to 32.0
- Enhanced brightness and the toggle message are enabled by default
- Choose whether enhanced brightness starts automatically when entering a world
- Show or hide the temporary action-bar message after a toggle
- Rebind the toggle key in the standard **Options → Controls** screen
- Bind a separate **Open Settings** key to configure the mod in game
- Client-side only, with no runtime dependencies beyond the selected mod loader

Settings are stored in `config/justbrightness.toml`.

## Installation

1. Install Fabric, NeoForge, or Forge for your Minecraft version.
2. Download the matching Just Brightness JAR for your mod loader and Minecraft version.
3. Put the JAR in your Minecraft instance's `mods` folder.
4. Launch the game.

## Use on servers

Just Brightness is installed on the client only and does not need to be installed
on a server. Brightness-related client modifications may be restricted by a
server's rules. Check those rules before using it in multiplayer.

## Compatibility

Just Brightness changes only the client renderer's gamma value. It does not
modify worlds or send data to external services. Shader packs can control
lighting independently, so the visible result may vary with the shader pack.

## Requirements

- Minecraft Java Edition 1.16.5, 1.17.1, 1.18.2, 1.19.2, 1.20.1, 1.21.1, 1.21.3 through 1.21.11,
  26.1.2, 26.2, or 26.3
- Fabric or Forge for 1.16.5 through 1.20.1; Fabric, NeoForge, or Forge for 1.21.1 and later
  (NeoForge did not exist before 1.20.1)

## Support and source

- [Report an issue](https://github.com/ksoichiro/JustBrightness/issues)
- [Source code](https://github.com/ksoichiro/JustBrightness)

## License and modpacks

Just Brightness is licensed under [LGPL-3.0-only](https://www.gnu.org/licenses/lgpl-3.0.html).
It may be included in modpacks under the terms of that license.
