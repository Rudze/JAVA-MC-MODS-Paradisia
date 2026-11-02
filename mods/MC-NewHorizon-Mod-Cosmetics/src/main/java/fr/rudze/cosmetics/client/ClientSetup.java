package fr.rudze.cosmetics.client;

import fr.rudze.cosmetics.item.ModItems;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class ClientSetup {

    private ClientSetup() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            for (String id : ModItems.cosmeticIds()) {
                var item = ModItems.cosmetic(id).get();
                AccessoriesRendererRegistry.registerRenderer(item, () -> new CosmeticAccessoryRenderer(item.getDefinition()));
            }
        });
    }

    /**
     * Pieces with a tintindex-0 face in their model (Blockbench's "Tint" checkbox, matching
     * HMCCosmetics' "dyeable: true") get colored the same way leather armor does - see
     * nh_furniture's identical technique for the full explanation.
     */
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (String id : ModItems.cosmeticIds()) {
            var item = ModItems.cosmetic(id).get();
            event.register((stack, tintIndex) -> {
                if (tintIndex != 0) {
                    return -1;
                }
                return DyedItemColor.getOrDefault(stack, item.getDefinition().color());
            }, item);
        }
    }
}
