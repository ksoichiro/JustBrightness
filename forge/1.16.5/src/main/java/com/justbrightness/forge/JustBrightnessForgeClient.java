package com.justbrightness.forge;

import com.justbrightness.ConfigScreen;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;

final class JustBrightnessForgeClient {
    private JustBrightnessForgeClient() {
    }

    static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) -> new ConfigScreen(parent));
    }
}
