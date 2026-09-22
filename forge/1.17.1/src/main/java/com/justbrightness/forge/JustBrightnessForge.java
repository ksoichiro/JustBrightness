package com.justbrightness.forge;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import com.justbrightness.JustBrightness;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fmlclient.registry.ClientRegistry;

@Mod(JustBrightness.MOD_ID)
public class JustBrightnessForge {
    public JustBrightnessForge() {
        BrightnessConfig.load(FMLPaths.CONFIGDIR.get());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            JustBrightnessForgeClient.registerConfigScreen();
        }
    }

    @Mod.EventBusSubscriber(modid = JustBrightness.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            ClientRegistry.registerKeyBinding(BrightnessController.getToggleKey());
            ClientRegistry.registerKeyBinding(BrightnessController.getOpenConfigKey());
        }
    }

    @Mod.EventBusSubscriber(modid = JustBrightness.MOD_ID, value = Dist.CLIENT)
    public static class ForgeClientEvents {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END) {
                BrightnessController.handleTick();
            }
        }

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggedInEvent event) {
            BrightnessState.applyWorldJoinDefault();
        }
    }
}
