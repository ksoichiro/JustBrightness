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
