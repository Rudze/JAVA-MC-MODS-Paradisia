package com.rudze.loadingscreen.mixin;

import com.rudze.loadingscreen.LoadingBackgrounds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screens shown outside a world (connecting, loading terrain...) draw the panorama then blur it.
 * Ours draws one of {@link LoadingBackgrounds}' images instead, and skips the blur that follows
 * so the image stays sharp.
 */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Unique
    private boolean nh_loadingscreen$drewImage;

    @Inject(method = "renderPanorama", at = @At("HEAD"), cancellable = true)
    private void nh_loadingscreen$drawImage(GuiGraphics graphics, float partialTick, CallbackInfo ci) {
        if (LoadingBackgrounds.render(graphics)) {
            nh_loadingscreen$drewImage = true;
            ci.cancel();
        }
    }

    @Inject(method = "renderBlurredBackground", at = @At("HEAD"), cancellable = true)
    private void nh_loadingscreen$skipBlur(float partialTick, CallbackInfo ci) {
        if (nh_loadingscreen$drewImage) {
            nh_loadingscreen$drewImage = false;
            ci.cancel();
        }
    }
}
