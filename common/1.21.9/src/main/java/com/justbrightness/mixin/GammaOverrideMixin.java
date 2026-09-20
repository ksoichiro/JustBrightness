package com.justbrightness.mixin;

import com.justbrightness.BrightnessState;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// LightTexture#updateLightTexture(float) reads three OptionInstance.get() values in order:
// ordinal 0 is minecraft.options.hideLightningFlash().get() (new in 1.21.11 vs 1.21.1, inside
// the endFlashState != null branch), ordinal 1 is minecraft.options.darknessEffectScale().get(),
// ordinal 2 is minecraft.options.gamma().get(). Verified against decompiled 1.21.11 (Mojang
// mappings) source and its javap bytecode disassembly — the hideLightningFlash() addition
// shifts gamma() from ordinal 1 (in 1.21.1) to ordinal 2 here.
@Mixin(LightTexture.class)
public class GammaOverrideMixin {

    @Redirect(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
                    ordinal = 2
            )
    )
    private Object justbrightness$redirectGamma(OptionInstance<?> instance) {
        if (BrightnessState.isEnabled()) {
            return BrightnessState.getOverrideGamma();
        }
        return instance.get();
    }
}
