package fr.rudze.furniture.block;

import fr.rudze.furniture.FurnitureMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FurnitureMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurniturePartBlockEntity>> FURNITURE_PART = BLOCK_ENTITIES.register(
            "furniture_part",
            () -> BlockEntityType.Builder.of(FurniturePartBlockEntity::new, ModBlocks.FURNITURE_PART.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FurnitureBlockEntity>> FURNITURE_DISPLAY = BLOCK_ENTITIES.register(
            "furniture_display",
            () -> BlockEntityType.Builder.of(FurnitureBlockEntity::new, ModBlocks.furnitureBlocks()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
