package fr.rudze.furniture.client;

import fr.rudze.furniture.block.FurnitureBlock;
import fr.rudze.furniture.block.ModBlockEntities;
import fr.rudze.furniture.block.ModBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Furniture textures have transparent edges (leaves, flame cutouts); the default solid render
 * layer ignores alpha entirely and paints those pixels opaque, showing up as a beige/white halo
 * instead of see-through. Switching every furniture block to the cutout layer fixes that.
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.FURNITURE_PART.get(), RenderType.cutout());
            for (String id : ModBlocks.furnitureIds()) {
                ItemBlockRenderTypes.setRenderLayer(ModBlocks.furniture(id).get(), RenderType.cutout());
            }
        });
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.FURNITURE_DISPLAY.get(), FurnitureBlockEntityRenderer::new);
    }

    /**
     * Pieces with a tintindex-0 face in their model (Blockbench's "Tint" checkbox, matching Nexo's
     * own dye-able items) get colored the same way leather armor does: this reads the item's
     * {@code DYED_COLOR} data component if it's been dyed, falling back to the definition's
     * undyed color otherwise. Registering it on every furniture item is harmless for the ones with
     * no tinted face - nothing ever asks for tintIndex 0 on those, so it's simply never called.
     */
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (String id : ModBlocks.furnitureIds()) {
            event.register((stack, tintIndex) -> {
                if (tintIndex != 0) {
                    return -1;
                }
                int defaultColor = 0xFFFFFF;
                if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof FurnitureBlock furnitureBlock) {
                    defaultColor = furnitureBlock.getDefinition().color();
                }
                return DyedItemColor.getOrDefault(stack, defaultColor);
            }, ModBlocks.furniture(id).get());
        }
    }
}
