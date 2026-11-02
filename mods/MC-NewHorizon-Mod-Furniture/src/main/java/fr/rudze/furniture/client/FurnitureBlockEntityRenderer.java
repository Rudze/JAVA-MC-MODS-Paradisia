package fr.rudze.furniture.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.rudze.furniture.block.FurnitureBlock;
import fr.rudze.furniture.block.FurnitureBlockEntity;
import fr.rudze.furniture.furniture.FurnitureDefinition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

/**
 * Draws the furniture item's own model completely unmodified (no per-item geometry math, ever -
 * that's the whole point of "drag and drop": drop the raw Nexo/Blockbench model + texture, write
 * the config, done).
 *
 * <p>This mirrors how Nexo (and vanilla item frames) actually render these items: via
 * {@code ItemDisplayContext.FIXED}, which makes Minecraft apply the model's own baked
 * "display.fixed" translation/rotation/scale automatically. That block isn't a GUI-only setting -
 * it's the model author's actual furniture placement transform, so there's nothing left for us to
 * guess (no bounding-box math, no reading the model's scale by hand). We only add what Nexo adds
 * on top: the yml's own "properties.scale" multiplier, and rotation/positioning for the block
 * face the player placed it on - the 8-direction yaw for floor/ceiling pieces, the exact wall
 * normal for wall pieces (see {@link FurnitureBlock}).
 */
public class FurnitureBlockEntityRenderer implements BlockEntityRenderer<FurnitureBlockEntity> {

    public FurnitureBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FurnitureBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof FurnitureBlock furnitureBlock)) {
            return;
        }
        FurnitureDefinition definition = furnitureBlock.getDefinition();
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        ItemStack stack = new ItemStack(furnitureBlock.asItem());
        if (stack.isEmpty()) {
            return;
        }
        DyedItemColor dyedColor = blockEntity.getDyedColor();
        if (dyedColor != null) {
            stack.set(DataComponents.DYED_COLOR, dyedColor);
        }

        AttachFace face = furnitureBlock.getResolvedFace(state);
        Direction facing = furnitureBlock.getResolvedFacing(state);
        float scale = (float) definition.scale();
        // Wall pieces, and pieces with a horizontal `parts` cell (see FurnitureBlock), only ever
        // use the grid-safe 4-way `facing`; the rest get the finer 8-direction yaw so diagonal
        // placement actually looks diagonal.
        float yaw = face != AttachFace.WALL && furnitureBlock.hasDiagonalRotation()
                ? furnitureBlock.getResolvedRotation8(state) * 45.0F
                : yRotationFor(facing);

        poseStack.pushPose();
        poseStack.translate(0.5, baseY(face), 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        // FIXED's native orientation lies the model flat (as an item frame would on a floor/ceiling);
        // standing it up on FLOOR needs an extra tilt on top of that.
        if (face == AttachFace.FLOOR) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        } else if (face == AttachFace.WALL) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        } else if (face == AttachFace.CEILING) {
            poseStack.mulPose(Axis.XP.rotationDegrees(180));
        }
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, level, (int) blockEntity.getBlockPos().asLong());
        poseStack.popPose();
    }

    private static double baseY(AttachFace face) {
        return switch (face) {
            case FLOOR -> 0.0;
            case CEILING -> 1.0;
            case WALL -> 0.5;
        };
    }

    private static float yRotationFor(Direction facing) {
        return switch (facing) {
            case EAST -> 90F;
            case SOUTH -> 180F;
            case WEST -> 270F;
            default -> 0F;
        };
    }
}
