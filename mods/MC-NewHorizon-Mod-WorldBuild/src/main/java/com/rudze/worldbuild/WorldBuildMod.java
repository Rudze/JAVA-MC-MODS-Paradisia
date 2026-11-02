package com.rudze.worldbuild;

import com.rudze.worldbuild.claim.ClaimCommands;
import com.rudze.worldbuild.claim.ClaimProtectionHandler;
import com.rudze.worldbuild.claim.ClaimSelectionHandler;
import com.rudze.worldbuild.claim.TaxBillingHandler;
import com.rudze.worldbuild.data.ModAttachments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(WorldBuildMod.MODID)
public final class WorldBuildMod {

    public static final String MODID = "nh_worldbuild";

    public static final ResourceKey<Level> WORLD_BUILD_LEVEL = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "world_build"));

    public WorldBuildMod(IEventBus modEventBus, ModContainer modContainer) {
        ModAttachments.register(modEventBus);
        NeoForge.EVENT_BUS.register(new SourceMapInstaller());
        NeoForge.EVENT_BUS.register(new SpawnWaystoneHandler());
        NeoForge.EVENT_BUS.register(new NetherPortalRedirectHandler());
        NeoForge.EVENT_BUS.register(new NaturalSpawnBlocker());
        NeoForge.EVENT_BUS.register(new ClaimSelectionHandler());
        NeoForge.EVENT_BUS.register(new ClaimProtectionHandler());
        NeoForge.EVENT_BUS.register(new ClaimCommands());
        NeoForge.EVENT_BUS.register(new TaxBillingHandler());
    }
}
