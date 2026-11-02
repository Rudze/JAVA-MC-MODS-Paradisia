package fr.rudze.furniture;

import fr.rudze.furniture.block.ModBlockEntities;
import fr.rudze.furniture.block.ModBlocks;
import fr.rudze.furniture.block.ModCreativeTabs;
import fr.rudze.furniture.client.ClientSetup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(FurnitureMod.MODID)
public final class FurnitureMod {

    public static final String MODID = "nh_furniture";

    public FurnitureMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientSetup::onClientSetup);
            modEventBus.addListener(ClientSetup::registerRenderers);
            modEventBus.addListener(ClientSetup::registerItemColors);
        }
    }
}
