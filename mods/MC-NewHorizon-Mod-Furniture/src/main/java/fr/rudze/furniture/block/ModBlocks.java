package fr.rudze.furniture.block;

import fr.rudze.furniture.FurnitureMod;
import fr.rudze.furniture.furniture.FurnitureDefinition;
import fr.rudze.furniture.furniture.FurnitureDefinitionLoader;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers the shared {@link FurniturePartBlock} plus one {@link FurnitureBlock} per
 * {@link FurnitureDefinition} found by {@link FurnitureDefinitionLoader}. Adding a new furniture
 * piece never touches this file - it's picked up automatically from the JSON definitions.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FurnitureMod.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FurnitureMod.MODID);

    public static final DeferredBlock<FurniturePartBlock> FURNITURE_PART = BLOCKS.registerBlock(
            "furniture_part", FurniturePartBlock::new, partProperties());

    private static final List<FurnitureDefinition> DEFINITIONS = FurnitureDefinitionLoader.loadAll();
    private static final Map<String, DeferredBlock<FurnitureBlock>> FURNITURE_BLOCKS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<BlockItem>> FURNITURE_ITEMS = new LinkedHashMap<>();

    static {
        for (FurnitureDefinition definition : DEFINITIONS) {
            DeferredBlock<FurnitureBlock> block = BLOCKS.registerBlock(
                    definition.id(), props -> new FurnitureBlock(definition, props), furnitureProperties());
            FURNITURE_BLOCKS.put(definition.id(), block);
            FURNITURE_ITEMS.put(definition.id(), ITEMS.registerSimpleBlockItem(block));
        }
    }

    private ModBlocks() {
    }

    public static DeferredBlock<FurnitureBlock> furniture(String id) {
        return FURNITURE_BLOCKS.get(id);
    }

    public static Collection<DeferredItem<BlockItem>> furnitureItems() {
        return FURNITURE_ITEMS.values();
    }

    public static Collection<String> furnitureIds() {
        return FURNITURE_BLOCKS.keySet();
    }

    /** Every registered furniture block, as plain {@link Block}s - for {@code BlockEntityType.Builder.of(...)}. */
    public static Block[] furnitureBlocks() {
        return FURNITURE_BLOCKS.values().stream()
                .map(DeferredBlock::get)
                .toArray(Block[]::new);
    }

    private static BlockBehaviour.Properties partProperties() {
        return BlockBehaviour.Properties.of()
                .noOcclusion()
                .noLootTable()
                .strength(0.5F)
                .sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties furnitureProperties() {
        return BlockBehaviour.Properties.of()
                .noOcclusion()
                .strength(0.5F);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
