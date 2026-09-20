package com.justbrightness.forge;

import com.justbrightness.ConfigScreen;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// Loaded only from the guarded client branch to keep client-only event classes off dedicated servers.
final class JustBrightnessForgeClient {
    private JustBrightnessForgeClient() {
    }

    static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new ConfigScreen(parent)));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(JustBrightnessForgeClient::registerKeyMappings);
        MinecraftForge.EVENT_BUS.addListener(JustBrightnessForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(JustBrightnessForgeClient::onLoggingIn);
    }

    private static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(BrightnessController.getToggleKey());
        event.register(BrightnessController.getOpenConfigKey());
    }

    private static void onClientTick(TickEvent.ClientTickEvent.Post event) {
        BrightnessController.handleTick();
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        BrightnessState.applyWorldJoinDefault();
    }
}
