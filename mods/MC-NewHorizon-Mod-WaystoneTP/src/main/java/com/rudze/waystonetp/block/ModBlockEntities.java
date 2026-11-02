package com.rudze.waystonetp.block;

import com.rudze.waystonetp.WaystoneTPMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, WaystoneTPMod.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaystoneBlockEntity>> WAYSTONE = BLOCK_ENTITIES.register(
            "waystone",
            () -> BlockEntityType.Builder.of(WaystoneBlockEntity::new, ModBlocks.WAYSTONE_A.get(), ModBlocks.WAYSTONE_B.get()).build(null));

    private ModBlockEntities() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
