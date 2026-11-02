package fr.rudze.cosmetics;

import fr.rudze.cosmetics.client.ClientSetup;
import fr.rudze.cosmetics.item.ModCreativeTabs;
import fr.rudze.cosmetics.item.ModItems;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CosmeticsMod.MODID)
public final class CosmeticsMod {

    public static final String MODID = "nh_cosmetics";

    public CosmeticsMod(IEventBus modEventBus, ModContainer modContainer) {
        ModItems.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientSetup::onClientSetup);
            modEventBus.addListener(ClientSetup::registerItemColors);
        }
    }
}
