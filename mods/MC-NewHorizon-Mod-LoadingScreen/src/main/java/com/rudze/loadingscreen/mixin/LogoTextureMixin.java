package com.rudze.loadingscreen.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import com.rudze.loadingscreen.LoadingScreenMod;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Vanilla loads the Mojang Studios logo straight from its own jar, out of reach of resource
 * packs, so the texture itself is swapped. Both vanilla's loading overlay and NeoForge's take
 * their logo from this texture.
 *
 * <p>Our image keeps the vanilla layout: the top half is drawn on the left and the bottom half on
 * the right, as one 4:1 strip.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.LoadingOverlay$LogoTexture")
public abstract class LogoTextureMixin {

    private static final String LOGO = "/assets/" + LoadingScreenMod.MODID + "/textures/gui/logo.png";

    @Inject(method = "getTextureImage", at = @At("HEAD"), cancellable = true)
    private void nh_loadingscreen$paradisiaLogo(ResourceManager resourceManager, CallbackInfoReturnable<SimpleTexture.TextureImage> cir) {
        try (InputStream in = LoadingScreenMod.class.getResourceAsStream(LOGO)) {
            if (in == null) {
                cir.setReturnValue(new SimpleTexture.TextureImage(new FileNotFoundException(LOGO)));
                return;
            }
            cir.setReturnValue(new SimpleTexture.TextureImage(new TextureMetadataSection(true, true), NativeImage.read(in)));
        } catch (IOException e) {
            cir.setReturnValue(new SimpleTexture.TextureImage(e));
        }
    }
}
