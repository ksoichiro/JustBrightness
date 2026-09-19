package com.justbrightness.mixin;

import com.justbrightness.BrightnessState;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// LightTexture#updateLightTexture(float) reads two OptionInstance.get() values in order:
// ordinal 0 is minecraft.options.darknessEffectScale().get(), ordinal 1 is
// minecraft.options.gamma().get(). Verified against decompiled 1.21.11 (Mojang mappings)
// source; same ordinals as 1.21.1.
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
