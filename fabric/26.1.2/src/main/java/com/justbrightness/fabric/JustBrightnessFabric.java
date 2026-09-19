package com.justbrightness.fabric;

import com.justbrightness.BrightnessConfig;
import com.justbrightness.BrightnessController;
import com.justbrightness.BrightnessState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public class JustBrightnessFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        BrightnessConfig.load(FabricLoader.getInstance().getConfigDir());
        // Fabric API for MC 26.x dropped fabric-key-binding-api-v1 (KeyBindingHelper) in
        // favour of fabric-key-mapping-api-v1 (KeyMappingHelper).
        KeyMappingHelper.registerKeyMapping(BrightnessController.getToggleKey());
        KeyMappingHelper.registerKeyMapping(BrightnessController.getOpenConfigKey());
        ClientTickEvents.END_CLIENT_TICK.register(client -> BrightnessController.handleTick());
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> BrightnessState.applyWorldJoinDefault());
    }
}
