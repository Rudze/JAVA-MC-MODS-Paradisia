package com.rudze.waystonetp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.rudze.waystonetp.block.WaystoneBlock;
import com.rudze.waystonetp.block.WaystoneBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/**
 * Reuses vanilla's beacon beam renderer to shoot a plain white column of light into the sky
 * from the lower half of every waystone, so it stays easy to spot from far away.
 */
public class WaystoneBeamRenderer implements BlockEntityRenderer<WaystoneBlockEntity> {

    private static final int WHITE = 0xFFFFFF;
    private static final int BEAM_HEIGHT = 1024;

    public WaystoneBeamRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WaystoneBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.getBlockState().getValue(WaystoneBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        BeaconRenderer.renderBeaconBeam(
                poseStack, bufferSource, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
                level.getGameTime(), 0, BEAM_HEIGHT, WHITE, 0.2F, 0.25F);
    }

    @Override
    public boolean shouldRenderOffScreen(WaystoneBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(WaystoneBlockEntity blockEntity) {
        return AABB.INFINITE;
    }
}
