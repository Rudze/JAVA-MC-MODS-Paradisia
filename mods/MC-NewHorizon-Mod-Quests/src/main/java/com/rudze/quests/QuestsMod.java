package com.rudze.quests;

import com.rudze.quests.community.CommunityQuests;
import com.rudze.quests.npc.NpcCommands;
import com.rudze.quests.npc.NpcInteractionHandler;
import com.rudze.quests.data.PlayerProgress;
import com.rudze.quests.starter.StarterLockHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

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
    }
}
