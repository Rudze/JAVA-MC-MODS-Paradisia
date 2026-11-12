package com.rudze.quests;

import com.rudze.quests.community.CommunityQuests;
import com.rudze.quests.npc.NpcCommands;
import com.rudze.quests.npc.NpcInteractionHandler;
import com.rudze.quests.data.PlayerProgress;
import com.rudze.quests.starter.StarterLockHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;

@Mod(QuestsMod.MODID)
public final class QuestsMod {

    public static final String MODID = "nh_quests";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuestsMod(IEventBus modEventBus, ModContainer modContainer) {
        PlayerProgress.register(modEventBus);
        NeoForge.EVENT_BUS.register(new NpcCommands());
        NeoForge.EVENT_BUS.register(new NpcInteractionHandler());
        NeoForge.EVENT_BUS.register(new StarterLockHandler());
        // Seeds the config file up front, so the mayor can edit it before anyone talks to Tom.
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> CommunityQuests.load());
        modEventBus.addListener(QuestsMod::addParadisiaDataPack);
    }

    /**
     * Registers datapacks/paradisia as an always-on data pack above the mods' own data, so it can
     * override other mods' recipes (Tom's Storage without ender pearls, for instance).
     */
    private static void addParadisiaDataPack(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }
        Path path = ModList.get().getModFileById(MODID).getFile().findResource("datapacks", "paradisia");
        PackLocationInfo info = new PackLocationInfo("builtin/" + MODID + "/paradisia",
                Component.literal("Paradisia"), PackSource.BUILT_IN, Optional.empty());
        Pack pack = Pack.readMetaAndCreate(info, new PathPackResources.PathResourcesSupplier(path),
                PackType.SERVER_DATA, new PackSelectionConfig(true, Pack.Position.TOP, false));
        if (pack != null) {
            event.addRepositorySource(consumer -> consumer.accept(pack));
        }
    }
}
