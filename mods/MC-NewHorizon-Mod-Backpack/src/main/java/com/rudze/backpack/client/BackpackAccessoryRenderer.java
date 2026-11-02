package com.rudze.backpack.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.rudze.backpack.config.BackpackConfig;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Positions and renders the backpack's own 3D item model (see
 * assets/nh_backpack/models/item/backpack.json) on the wearer's back.
 *
 * <p>The transform sequence (anchor to body -> scale -> translate -> rotate 180 on Z) is copied
 * from a working reference implementation (a different backpack mod's own on-player render layer,
 * not Backpacked - Backpacked's custom "Framework" model loader turned out to use an incompatible
 * coordinate convention). That reference renders through a newer item-render API not present in
 * 1.21.1, so only the transform sequence itself is ported; the actual draw call below uses the
 * 1.21.1-appropriate {@code ItemRenderer.renderStatic}, matching {@code ItemDisplayContext.NONE} to
 * skip any of our model's own "display" entries (avoids interaction with them entirely).
 */
public final class BackpackAccessoryRenderer implements AccessoryRenderer {

    @Override
    public <M extends LivingEntity> void render(
            ItemStack stack,
            SlotReference reference,
            PoseStack matrices,
            EntityModel<M> model,
            MultiBufferSource multiBufferSource,
            int light,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        if (!(model instanceof HumanoidModel<?> humanoidModel)) {
            return;
        }

        matrices.pushPose();

        humanoidModel.body.translateAndRotate(matrices);

        float scale = BackpackConfig.RENDER_SCALE.get().floatValue();
        matrices.scale(scale, scale, scale);

        matrices.translate(
                BackpackConfig.RENDER_OFFSET_X.get(),
                BackpackConfig.RENDER_OFFSET_Y.get(),
                BackpackConfig.RENDER_OFFSET_Z.get());
        matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                matrices, multiBufferSource, reference.entity().level(), 0);

        matrices.popPose();
    }
}
