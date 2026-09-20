package com.justbrightness.forge;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import com.justbrightness.JustBrightness;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(JustBrightness.MOD_ID)
public class JustBrightnessForge {
    public JustBrightnessForge() {
        BrightnessConfig.load(FMLPaths.CONFIGDIR.get());
        if (FMLEnvironment.dist == Dist.CLIENT) JustBrightnessForgeClient.registerConfigScreen();
    }

    @Mod.EventBusSubscriber(modid = JustBrightness.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(BrightnessController.getToggleKey());
            event.register(BrightnessController.getOpenConfigKey());
        }
        @SubscribeEvent public static void onClientTick(TickEvent.ClientTickEvent.Post event) {
            BrightnessController.handleTick();
        }
        @SubscribeEvent public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
            BrightnessState.applyWorldJoinDefault();
        }
    }
}
