package com.rudze.waystonetp.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Carries no data of its own; its only purpose is to give the client something to hang a
 * {@code BlockEntityRenderer} off of, so the waystone can render its sky beam.
 */
public class WaystoneBlockEntity extends BlockEntity {

    public WaystoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAYSTONE.get(), pos, state);
    }
}
