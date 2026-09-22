package com.justbrightness.mixin;

import com.justbrightness.BrightnessState;
import net.minecraft.client.Options;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public class GammaOverrideMixin {
    @Redirect(
            method = "updateLightTexture",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/Options;gamma:D"
            )
    )
    private double justbrightness$redirectGamma(Options options) {
        if (BrightnessState.isEnabled()) {
            return BrightnessState.getOverrideGamma();
        }
        return options.gamma;
    }
}
