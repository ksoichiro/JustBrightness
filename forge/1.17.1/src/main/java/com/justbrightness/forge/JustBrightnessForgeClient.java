package com.justbrightness.forge;

import com.justbrightness.ConfigScreen;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fmlclient.ConfigGuiHandler;

final class JustBrightnessForgeClient {
    private JustBrightnessForgeClient() {
    }

    static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory((minecraft, parent) -> new ConfigScreen(parent)));
    }
}
