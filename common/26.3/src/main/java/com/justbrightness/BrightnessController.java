package com.justbrightness;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class BrightnessController {
    // MC 26.x dropped the GLFW keysym input path: InputConstants.Type.KEYSYM is gone and
    // key codes are SDL scancodes exposed as InputConstants constants (KEY_B), so GLFW's
    // key constants must not be used here.
    // Keybind categories are also no longer free-form translation keys but registered
    // KeyMapping.Category records; the label key is derived from the id as
    // key.category.<namespace>.<path> (KeyMapping.Category#label).
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(JustBrightness.MOD_ID, JustBrightness.MOD_ID));

    private static final KeyMapping TOGGLE_KEY = new KeyMapping(
            "key.justbrightness.toggle",
            InputConstants.Type.KEYBOARD,
            InputConstants.KEY_B,
            CATEGORY
    );

    private static final KeyMapping OPEN_CONFIG_KEY = new KeyMapping(
            "key.justbrightness.open_config",
            InputConstants.Type.KEYBOARD,
            InputConstants.UNKNOWN.getValue(),
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
            // MC 26.x removed Minecraft#setScreen(Screen); setScreenAndShow is the
            // remaining public entry point.
            Minecraft.getInstance().setScreenAndShow(new ConfigScreen(null));
        }
    }
}
