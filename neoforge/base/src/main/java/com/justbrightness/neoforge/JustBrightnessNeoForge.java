package com.justbrightness.neoforge;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import com.justbrightness.JustBrightness;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(JustBrightness.MOD_ID)
public class JustBrightnessNeoForge {

    public JustBrightnessNeoForge(ModContainer container) {
        BrightnessConfig.load(FMLPaths.CONFIGDIR.get());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            JustBrightnessNeoForgeClient.registerConfigScreen(container);
        }
    }

    @EventBusSubscriber(modid = JustBrightness.MOD_ID, value = Dist.CLIENT)
    public static class ClientEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(BrightnessController.getToggleKey());
            event.register(BrightnessController.getOpenConfigKey());
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            BrightnessController.handleTick();
        }

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            BrightnessState.applyWorldJoinDefault();
        }
    }
}
