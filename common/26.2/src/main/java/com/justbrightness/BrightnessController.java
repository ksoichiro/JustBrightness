package com.justbrightness;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class BrightnessController {
    // MC 26.2 still resolves keybinds through the GLFW keysym path (InputConstants.KEY_*
    // SDL scancodes are introduced later in the 26.x line, starting with 26.3).
    // Keybind categories are already registered KeyMapping.Category records; the label key is
    // derived from the id as key.category.<namespace>.<path> (KeyMapping.Category#label).
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
                Minecraft.getInstance().gui.hud.setOverlayMessage(
                        BrightnessState.isEnabled()
                                ? Component.translatable("justbrightness.message.enabled",
                                        String.format("%.1f", BrightnessConfig.getGamma()))
                                : Component.translatable("justbrightness.message.disabled"),
                        false);
            }
        }
        while (OPEN_CONFIG_KEY.consumeClick()) {
            // MC 26.2 removed Minecraft#setScreen(Screen); setScreenAndShow is the
            // remaining public entry point.
            Minecraft.getInstance().setScreenAndShow(new ConfigScreen(null));
        }
    }
}
