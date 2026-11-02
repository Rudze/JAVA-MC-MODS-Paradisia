package com.rudze.backpack;

import com.rudze.backpack.client.BackpackAccessoryRenderer;
import com.rudze.backpack.client.BackpackColorHandler;
import com.rudze.backpack.config.BackpackConfig;
import com.rudze.backpack.item.BackpackAccessory;
import com.rudze.backpack.item.BackpackItems;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(BackpackMod.MODID)
public final class BackpackMod {

    public static final String MODID = "nh_backpack";

    public BackpackMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, BackpackConfig.SPEC, "nh_backpack.toml");

        BackpackItems.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(BackpackColorHandler::register);
        }

        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        AccessoriesAPI.registerAccessory(BackpackItems.BACKPACK.get(), new BackpackAccessory());

        if (FMLEnvironment.dist == Dist.CLIENT) {
            AccessoriesRendererRegistry.registerRenderer(BackpackItems.BACKPACK.get(), BackpackAccessoryRenderer::new);
        }
    }
}
