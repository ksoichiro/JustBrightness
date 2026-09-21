package com.justbrightness.forge;

import com.justbrightness.ConfigScreen;
import net.minecraftforge.client.ConfigGuiHandler;
import net.minecraftforge.fml.ModLoadingContext;

final class JustBrightnessForgeClient {
    private JustBrightnessForgeClient() {
    }

    static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigGuiHandler.ConfigGuiFactory.class,
                () -> new ConfigGuiHandler.ConfigGuiFactory(parent -> new ConfigScreen(parent)));
    }
}
