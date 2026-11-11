package com.rudze.loadingscreen;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Rebrands the loading screens: the Paradisia logo replaces Mojang Studios' (see
 * {@link com.rudze.loadingscreen.mixin.LogoTextureMixin}) and the background is black. Once the
 * game is up, the connecting and terrain loading screens show random images instead of
 * the panorama, taken from the resource packs (see {@link LoadingBackgrounds}).
 *
 * <p>The black background is Minecraft's own "monochrome logo" accessibility option, which both
 * vanilla's loading overlay and NeoForge's early loading window read. It is forced on every
 * launch; NeoForge's early window reads it from options.txt before any mod loads, so it only
 * takes effect there from the next launch (the modpack ships with it already on).
 */
@Mod(value = LoadingScreenMod.MODID, dist = Dist.CLIENT)
public final class LoadingScreenMod {

    public static final String MODID = "nh_loadingscreen";

    public LoadingScreenMod(IEventBus modEventBus) {
        // Mods are built on worker threads; options are touched on the main thread.
        modEventBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(LoadingScreenMod::forceBlackBackground));
        NeoForge.EVENT_BUS.register(new LoadingBackgrounds());
    }

    private static void forceBlackBackground() {
        var options = Minecraft.getInstance().options;
        if (!options.darkMojangStudiosBackground().get()) {
            options.darkMojangStudiosBackground().set(true);
            options.save();
        }
    }
}
