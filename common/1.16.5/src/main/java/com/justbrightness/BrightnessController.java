package com.justbrightness;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.TranslatableComponent;
import org.lwjgl.glfw.GLFW;

public class BrightnessController {
    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.justbrightness.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.justbrightness"
    );

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.justbrightness.open_config",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.justbrightness"
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
                                ? new TranslatableComponent("justbrightness.message.enabled",
                                        String.format("%.1f", BrightnessConfig.getGamma()))
                                : new TranslatableComponent("justbrightness.message.disabled"),
                        false);
            }
        }
        while (OPEN_CONFIG_KEY.consumeClick()) {
            Minecraft.getInstance().setScreen(new ConfigScreen(null));
        }
    }
}
