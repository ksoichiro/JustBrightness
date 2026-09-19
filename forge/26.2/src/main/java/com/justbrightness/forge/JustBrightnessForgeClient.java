package com.justbrightness.forge;

import com.justbrightness.ConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

// Kept separate from the mod entry class: a Screen-typed lambda in the entry
// class is resolved during mod class loading and crashes a dedicated server
// even behind a dist guard. This class is only ever loaded from the guarded
// client branch.
final class JustBrightnessForgeClient {
    private JustBrightnessForgeClient() {
    }

    static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
    }
}
