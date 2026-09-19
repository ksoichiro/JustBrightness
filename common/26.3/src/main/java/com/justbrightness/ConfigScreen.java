package com.justbrightness;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private GammaSlider gammaSlider;
    private CycleButton<Boolean> defaultEnabledButton;

    public ConfigScreen(Screen parent) {
        super(Component.translatable("justbrightness.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        double range = BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA;
        double initialProgress = (BrightnessConfig.getGamma() - BrightnessConfig.MIN_GAMMA) / range;

        gammaSlider = addRenderableWidget(
                new GammaSlider(width / 2 - 100, height / 2 - 36, 200, 20, initialProgress));

        defaultEnabledButton = addRenderableWidget(CycleButton.onOffBuilder(BrightnessConfig.isDefaultEnabled())
                .create(width / 2 - 100, height / 2 - 12, 200, 20,
                        Component.translatable("justbrightness.config.default_enabled"),
                        (button, value) -> BrightnessConfig.setDefaultEnabled(value)));

        addRenderableWidget(Button.builder(Component.translatable("justbrightness.config.reset"),
                        button -> resetToDefaults())
                .bounds(width / 2 - 100, height / 2 + 12, 200, 20)
                .build());

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width / 2 - 100, height / 2 + 36, 200, 20)
                .build());
    }

    private void resetToDefaults() {
        double range = BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA;
        double defaultProgress = (BrightnessConfig.DEFAULT_GAMMA - BrightnessConfig.MIN_GAMMA) / range;
        gammaSlider.setProgress(defaultProgress);
        defaultEnabledButton.setValue(false);
        BrightnessConfig.setDefaultEnabled(false);
    }

    // MC 26.x screens no longer draw immediately via GuiGraphics#render; they collect a
    // deferred render state through GuiGraphicsExtractor instead.
    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.centeredText(font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        BrightnessConfig.save();
        // MC 26.x removed Minecraft#setScreen(Screen); setScreenAndShow is the
        // remaining public entry point.
        minecraft.setScreenAndShow(parent);
    }

    private static class GammaSlider extends AbstractSliderButton {
        GammaSlider(int x, int y, int width, int height, double initialProgress) {
            super(x, y, width, height, CommonComponents.EMPTY, initialProgress);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double gamma = BrightnessConfig.MIN_GAMMA
                    + value * (BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA);
            setMessage(Component.translatable("justbrightness.config.gamma",
                    String.format("%.1f", gamma)));
        }

        @Override
        protected void applyValue() {
            double gamma = BrightnessConfig.MIN_GAMMA
                    + value * (BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA);
            BrightnessConfig.setGamma(gamma);
        }

        void setProgress(double progress) {
            this.value = progress;
            updateMessage();
            applyValue();
        }
    }
}
