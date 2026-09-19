package com.justbrightness.mixin;

import com.justbrightness.BrightnessState;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// MC 26.x replaced LightTexture#updateLightTexture(float) with
// LightmapRenderStateExtractor#extract(LightmapRenderState, float), which writes the gamma
// value into LightmapRenderState#brightness for the lightmap shader UBO.
// extract() calls OptionInstance.get() three times, in bytecode order:
//   ordinal 0 = options.hideLightningFlash()
//   ordinal 1 = options.gamma()
//   ordinal 2 = options.darknessEffectScale()
// Verified against decompiled 26.1.2 (Mojang mappings) source; same ordinals as 26.3.
@Mixin(LightmapRenderStateExtractor.class)
public class GammaOverrideMixin {

    @Redirect(
            method = "extract",
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
