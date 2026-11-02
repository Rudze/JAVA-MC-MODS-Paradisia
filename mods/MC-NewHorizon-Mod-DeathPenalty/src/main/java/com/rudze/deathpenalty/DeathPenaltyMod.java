package com.rudze.deathpenalty;

import com.rudze.deathpenalty.config.DeathPenaltyConfig;
import com.rudze.deathpenalty.event.DeathPenaltyServerHandler;
import com.rudze.deathpenalty.event.client.DeathPenaltyClientHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(DeathPenaltyMod.MODID)
public final class DeathPenaltyMod {

    public static final String MODID = "nh_deathpenalty";

    public DeathPenaltyMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, DeathPenaltyConfig.SPEC, "nh_deathpenalty.toml");

        DeathPenaltyAttachments.register(modEventBus);

        NeoForge.EVENT_BUS.register(new DeathPenaltyServerHandler());
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(new DeathPenaltyClientHandler());
        }
    }
}
