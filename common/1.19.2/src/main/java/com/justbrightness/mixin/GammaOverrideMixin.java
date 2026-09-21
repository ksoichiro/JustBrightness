package com.justbrightness.mixin;

import com.justbrightness.BrightnessState;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LightTexture.class)
public class GammaOverrideMixin {
    @Redirect(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 1
            )
    )
    private Object justbrightness$redirectGamma(OptionInstance<?> instance) {
        if (BrightnessState.isEnabled()) {
            return BrightnessState.getOverrideGamma();
        }
        return instance.get();
    }
}
