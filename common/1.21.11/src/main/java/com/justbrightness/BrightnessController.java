package com.justbrightness;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BrightnessController {
    // MC 1.21.11 replaced the free-form string keybind category with a registered
    // KeyMapping.Category record; the label lang key is derived from the id as
    // key.category.<namespace>.<path> (KeyMapping.Category#label).
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(JustBrightness.MOD_ID, JustBrightness.MOD_ID));

    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.justbrightness.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            CATEGORY
    );

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.justbrightness.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            CATEGORY
    );

    public static KeyMapping getToggleKey() {
        return TOGGLE_KEY;
    }

    public static KeyMapping getOpenConfigKey() {
        return OPEN_CONFIG_KEY;
    }

    public static void handleTick() {
        while (TOGGLE_KEY.consumeClick()) {
            BrightnessState.toggle();
            if (BrightnessConfig.isToggleMessageEnabled()) {
                Minecraft.getInstance().gui.setOverlayMessage(
                        BrightnessState.isEnabled()
                                ? Component.translatable("justbrightness.message.enabled",
                                        String.format("%.1f", BrightnessConfig.getGamma()))
                                : Component.translatable("justbrightness.message.disabled"),
                        false);
            }
        }
        while (OPEN_CONFIG_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new ConfigScreen(null));
        }
    }
}
