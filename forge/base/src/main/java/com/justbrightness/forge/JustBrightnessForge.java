package com.justbrightness.forge;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import com.justbrightness.JustBrightness;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(JustBrightness.MOD_ID)
public class JustBrightnessForge {
    public JustBrightnessForge() {
        BrightnessConfig.load(FMLPaths.CONFIGDIR.get());
        // Client-only setup lives in a separate class: a Screen-typed lambda here
        // would be resolved during mod class loading and crash a dedicated server
        // even behind this guard.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            JustBrightnessForgeClient.registerConfigScreen();
        }
        RegisterKeyMappingsEvent.BUS.addListener(event -> {
            event.register(BrightnessController.getToggleKey());
            event.register(BrightnessController.getOpenConfigKey());
        });
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> BrightnessController.handleTick());
        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> BrightnessState.applyWorldJoinDefault());
    }
}
