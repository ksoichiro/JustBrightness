package com.justbrightness.forge;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.JustBrightness;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(JustBrightness.MOD_ID)
public class JustBrightnessForge {
    public JustBrightnessForge() {
        BrightnessConfig.load(FMLPaths.CONFIGDIR.get());
        // Client-only setup is isolated to avoid resolving client classes on a dedicated server.
        if (FMLEnvironment.dist == Dist.CLIENT) {
            JustBrightnessForgeClient.register();
        }
    }
}
