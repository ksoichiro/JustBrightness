package com.justbrightness;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private GammaSlider gammaSlider;
    private CycleButton<Boolean> defaultEnabledButton;
    private CycleButton<Boolean> toggleMessageButton;

    public ConfigScreen(Screen parent) {
        super(new TranslatableComponent("justbrightness.config.title"));
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
                        new TranslatableComponent("justbrightness.config.default_enabled"),
                        (button, value) -> BrightnessConfig.setDefaultEnabled(value)));

        toggleMessageButton = addRenderableWidget(CycleButton.onOffBuilder(BrightnessConfig.isToggleMessageEnabled())
                .create(width / 2 - 100, height / 2 + 12, 200, 20,
                        new TranslatableComponent("justbrightness.config.show_toggle_message"),
                        (button, value) -> BrightnessConfig.setToggleMessageEnabled(value)));

        addRenderableWidget(new Button(width / 2 - 100, height / 2 + 36, 200, 20,
                new TranslatableComponent("justbrightness.config.reset"), button -> resetToDefaults()));

        addRenderableWidget(new Button(width / 2 - 100, height / 2 + 60, 200, 20,
                CommonComponents.GUI_DONE, button -> onClose()));
    }

    private void resetToDefaults() {
        double range = BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA;
        double defaultProgress = (BrightnessConfig.DEFAULT_GAMMA - BrightnessConfig.MIN_GAMMA) / range;
        gammaSlider.setProgress(defaultProgress);
        defaultEnabledButton.setValue(BrightnessConfig.DEFAULT_ENABLED);
        BrightnessConfig.setDefaultEnabled(BrightnessConfig.DEFAULT_ENABLED);
        toggleMessageButton.setValue(BrightnessConfig.DEFAULT_SHOW_TOGGLE_MESSAGE);
        BrightnessConfig.setToggleMessageEnabled(BrightnessConfig.DEFAULT_SHOW_TOGGLE_MESSAGE);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        drawCenteredString(poseStack, font, title, width / 2, height / 2 - 60, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        BrightnessConfig.save();
        minecraft.setScreen(parent);
    }

    private static class GammaSlider extends AbstractSliderButton {
        GammaSlider(int x, int y, int width, int height, double initialProgress) {
            super(x, y, width, height, TextComponent.EMPTY, initialProgress);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double gamma = BrightnessConfig.MIN_GAMMA
                    + value * (BrightnessConfig.MAX_GAMMA - BrightnessConfig.MIN_GAMMA);
            setMessage(new TranslatableComponent("justbrightness.config.gamma",
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
