package com.rudze.waystonetp;

import com.rudze.waystonetp.block.ModBlockEntities;
import com.rudze.waystonetp.block.ModBlocks;
import com.rudze.waystonetp.client.WaystoneBeamRenderer;
import com.rudze.waystonetp.config.WaystoneTPConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(WaystoneTPMod.MODID)
public final class WaystoneTPMod {

    public static final String MODID = "nh_waystonetp";

    public WaystoneTPMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, WaystoneTPConfig.SPEC, "nh_waystonetp.toml");

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(WaystoneTPMod::registerRenderers);
        }
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WAYSTONE.get(), WaystoneBeamRenderer::new);
    }
}
