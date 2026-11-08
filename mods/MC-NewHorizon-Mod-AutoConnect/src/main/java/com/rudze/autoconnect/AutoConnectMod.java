package com.rudze.autoconnect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Turns the client into a launcher for the Paradisia server: no title screen, no server list.
 * The game joins the server as soon as it starts, and leaving the server closes the game.
 * It also switches the game to French the first time it runs.
 */
@Mod(value = AutoConnectMod.MODID, dist = Dist.CLIENT)
public final class AutoConnectMod {

    public static final String MODID = "nh_autoconnect";
    private static final String FRENCH = "fr_fr";
    /** Exists once the game has been switched to French, so players can switch back for good. */
    private static final Path FRENCH_SET = FMLPaths.CONFIGDIR.get().resolve(MODID + "_langue_fr");

    public AutoConnectMod() {
        NeoForge.EVENT_BUS.register(new ScreenRedirects());
        switchToFrenchOnce();
    }

    /**
     * Mods are built after the options are read but before the language is picked from them, so
     * changing the option here takes effect on this very launch.
     */
    private static void switchToFrenchOnce() {
        if (Files.exists(FRENCH_SET)) {
            return;
        }
        Options options = Minecraft.getInstance().options;
        if (!FRENCH.equals(options.languageCode)) {
            options.languageCode = FRENCH;
            options.save();
        }
        try {
            Files.createFile(FRENCH_SET);
        } catch (IOException e) {
            // Not fatal: the game is in French, it'll just be set again next launch.
        }
    }
}
