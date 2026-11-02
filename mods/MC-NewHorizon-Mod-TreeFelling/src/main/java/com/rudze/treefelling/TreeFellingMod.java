package com.rudze.treefelling;

import com.rudze.treefelling.config.TreeFellingConfig;
import com.rudze.treefelling.event.TreeFellingEventHandler;
import com.rudze.treefelling.util.TickScheduler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@Mod(TreeFellingMod.MODID)
public final class TreeFellingMod {

    public static final String MODID = "nh_treefelling";

    public TreeFellingMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, TreeFellingConfig.SPEC, "nh_treefelling.toml");

        NeoForge.EVENT_BUS.register(new TreeFellingEventHandler());
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, TickScheduler.INSTANCE::onServerTick);
    }
}
