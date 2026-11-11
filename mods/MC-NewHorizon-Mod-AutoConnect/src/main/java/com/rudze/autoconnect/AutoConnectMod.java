package com.rudze.autoconnect;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Turns the client into a launcher for the Paradisia server: no title screen, no server list.
 * The game joins the server as soon as it starts, and leaving the server closes the game.
 * It also switches the game to French the first time it runs, and always turns on the
 * server's own resource pack.
 */
@Mod(value = AutoConnectMod.MODID, dist = Dist.CLIENT)
public final class AutoConnectMod {

    public static final String MODID = "nh_autoconnect";
    private static final String FRENCH = "fr_fr";
    /** Exists once the game has been switched to French, so players can switch back for good. */
    private static final Path FRENCH_SET = FMLPaths.CONFIGDIR.get().resolve(MODID + "_langue_fr");

    public AutoConnectMod(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(new ScreenRedirects());
        modEventBus.addListener(AutoConnectMod::addServerPack);
        switchToFrenchOnce();
    }

    /**
     * Adds {@code resourcepacks/paradisia} from this jar as a required pack on top of the others,
     * so every player has it without picking it from the pack screen.
     */
    private static void addServerPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        Path path = ModList.get().getModFileById(MODID).getFile().findResource("resourcepacks", "paradisia");
        PackLocationInfo info = new PackLocationInfo("builtin/" + MODID + "/paradisia",
                Component.literal("Paradisia"), PackSource.BUILT_IN, Optional.empty());
        Pack pack = Pack.readMetaAndCreate(info, new PathPackResources.PathResourcesSupplier(path),
                PackType.CLIENT_RESOURCES, new PackSelectionConfig(true, Pack.Position.TOP, false));
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
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
