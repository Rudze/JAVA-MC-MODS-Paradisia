package com.rudze.quests;

import com.rudze.quests.chen.ChenCommands;
import com.rudze.quests.chen.ChenInteractionHandler;
import com.rudze.quests.data.PlayerProgress;
import com.rudze.quests.starter.StarterLockHandler;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(QuestsMod.MODID)
public final class QuestsMod {

    public static final String MODID = "nh_quests";
    public static final Logger LOGGER = LogUtils.getLogger();

    public QuestsMod(IEventBus modEventBus, ModContainer modContainer) {
        PlayerProgress.register(modEventBus);
        NeoForge.EVENT_BUS.register(new ChenCommands());
        NeoForge.EVENT_BUS.register(new ChenInteractionHandler());
        NeoForge.EVENT_BUS.register(new StarterLockHandler());
    }
}
