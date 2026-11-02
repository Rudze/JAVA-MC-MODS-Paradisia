package fr.rudze.cosmetics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.rudze.cosmetics.cosmetic.CosmeticDefinition;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a cosmetic's own model completely unmodified - same drag-and-drop principle as
 * nh_furniture's renderer: drop the raw Nexo/Blockbench model + texture, write the config, done.
 *
 * <p><b>"hat"/"balloon"</b> replicate vanilla's own {@code CustomHeadLayer.translateToHead}
 * PoseStack sequence by hand, anchored on the head bone, then draw with
 * {@code ItemDisplayContext.HEAD} so the model's own baked "head" transform (translation/rotation/
 * scale, authored by whatever exported it) applies on top. This mirrors how HMCCosmetics actually
 * displays these: it equips them into the player's *real* (spoofed) vanilla head slot and lets
 * vanilla's own rendering do the work; Accessories' slots aren't real equipment slots, so this
 * replicates that same sequence instead of using Accessories' generic {@code transformToModelPart}
 * helper - that helper rotates around a different axis and never mirrors, which is what caused
 * these to render deformed (the model's "head" transform assumes vanilla's sequence runs first).
 *
 * <p><b>"hand"</b> replicates vanilla's own {@code ItemInHandLayer.renderArmWithItem} sequence -
 * confirmed correct by comparing against holding the same item normally (not as an accessory at
 * all), which goes through unmodified vanilla rendering and looks right. An earlier version
 * dropped the extra rotate/translate after the arm anchor, assuming it was tuned for vanilla's own
 * generated-icon models; that was wrong, and the deformation reported at the time turned out to be
 * the undyed-tint alpha bug (see {@link fr.rudze.cosmetics.cosmetic.CosmeticDefinitionLoader}),
 * unrelated to this transform.
 *
 * <p><b>"back"</b> is different from all of the above: these models aren't drawn through their own
 * baked transform at all. Anchored on the body bone, drawn with {@code ItemDisplayContext.NONE}
 * (skips the model's own baked transforms entirely) with an explicit scale + XYZ offset + 180°
 * Z-axis rotation - the same technique the standalone Backpack module's own renderer uses
 * ({@code BackpackAccessoryRenderer}), copied because it's a proven, working reference for
 * exactly this "worn on the back" case. Scale/offset come from {@link CosmeticDefinition} since
 * different back cosmetics (a cloak vs. wings vs. a float) have wildly different geometry and need
 * their own tuning - see docs/adding-cosmetics.md.
 *
 * <p><b>balloon</b> ({@link CosmeticDefinition#balloon()}, only meaningful for "hand") doesn't use
 * {@link #renderHand} at all: HMCCosmetics itself doesn't hand-draw balloons either - it spawns a
 * real leashed passenger entity that floats independently above the player and lets vanilla's own
 * leash rendering draw the rope between them. A real synced entity per equipped balloon is out of
 * scope here, but the *behaviour* that matters - the balloon is not rigidly bolted to the wearer's
 * body, it hovers above them in world space and lags behind with its own inertia, connected only by
 * a sagging rope, exactly like a leashed mob - is replicated directly: {@link #renderBalloon} tracks
 * the wearer's world position once per game tick and eases a separate "balloon position" toward a
 * point above their head, storing prev/current pairs (mirroring vanilla's own entity interpolation)
 * so the balloon still renders smoothly between ticks. An earlier version anchored on the body bone
 * instead - that rotated the balloon rigidly with the player's body yaw, which looked attached rather
 * than floating, since a real leashed entity's position isn't coupled to its holder's rotation at
 * all. The rope itself reuses vanilla's leash rendering math ({@code EntityRenderer#renderLeash}/
 * {@code addVertexPair}, drawn with the same {@code RenderType.leash()}) fed the anchor-to-balloon
 * delta instead of two real entities' positions.
 *
 * @see net.minecraft.client.renderer.entity.layers.CustomHeadLayer#translateToHead
 * @see net.minecraft.client.renderer.entity.EntityRenderer#renderLeash
 */
public class CosmeticAccessoryRenderer implements AccessoryRenderer {

    private final CosmeticDefinition definition;

    public CosmeticAccessoryRenderer(CosmeticDefinition definition) {
        this.definition = definition;
    }

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
        if (!(model instanceof HumanoidModel<? extends LivingEntity> humanoidModel)) {
            return;
        }

        matrices.pushPose();
        if ("back".equals(definition.slot())) {
            renderBack(stack, reference, humanoidModel, matrices, multiBufferSource, light);
        } else if ("hand".equals(definition.slot())) {
            if (definition.balloon()) {
                renderBalloon(stack, reference, humanoidModel, matrices, multiBufferSource, light, partialTicks);
            } else {
                renderHand(stack, reference, humanoidModel, matrices, multiBufferSource, light);
            }
        } else {
            renderHead(stack, reference, humanoidModel, matrices, multiBufferSource, light);
        }
        matrices.popPose();
    }

    private void renderHead(ItemStack stack, SlotReference reference, HumanoidModel<? extends LivingEntity> model, PoseStack matrices, MultiBufferSource buffer, int light) {
        model.head.translateAndRotate(matrices);
        matrices.translate(0.0, -0.25, 0.0);
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
        matrices.scale(0.625F, -0.625F, -0.625F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.HEAD, light, OverlayTexture.NO_OVERLAY, matrices, buffer, reference.entity().level(), 0);
    }

    private void renderBack(ItemStack stack, SlotReference reference, HumanoidModel<? extends LivingEntity> model, PoseStack matrices, MultiBufferSource buffer, int light) {
        model.body.translateAndRotate(matrices);

        float scale = (float) definition.backScale();
        matrices.scale(scale, scale, scale);
        matrices.translate(definition.backOffsetX(), definition.backOffsetY(), definition.backOffsetZ());
        matrices.mulPose(Axis.ZP.rotationDegrees(180.0F));

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, matrices, buffer, reference.entity().level(), 0);
    }

    /**
     * The full {@code ItemInHandLayer.renderArmWithItem} sequence - confirmed correct by comparing
     * against holding the same item normally (not equipped as an accessory at all): that goes
     * through vanilla's own {@code PlayerItemInHandLayer} unmodified and looks right, so replicating
     * its exact sequence here (arm anchor -> rotate -90° X -> rotate 180° Y -> small per-arm
     * translate) is what these models actually need. An earlier version dropped this extra
     * rotate/translate, reasoning it was tuned for vanilla's generated-icon models rather than
     * these - that reasoning turned out wrong; the deformation reported at the time had a different
     * cause (the alpha channel on undyed pieces' default tint, fixed separately).
     */
    private void renderHand(ItemStack stack, SlotReference reference, HumanoidModel<? extends LivingEntity> model, PoseStack matrices, MultiBufferSource buffer, int light) {
        boolean rightSide = isRightSide(reference);
        HumanoidArm arm = rightSide ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        ItemDisplayContext context = rightSide ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;

        model.translateToHand(arm, matrices);
        matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
        matrices.translate((rightSide ? 1 : -1) / 16.0F, 0.125F, -0.625F);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, context, light, OverlayTexture.NO_OVERLAY, matrices, buffer, reference.entity().level(), 0);
    }

    private static boolean isRightSide(SlotReference reference) {
        return reference.slot() % 2 == 0;
    }

    /**
     * One entry per equipped balloon (keyed below): {@code prev}/{@code curr} are the balloon's own
     * world-space position at the last two game ticks, {@code lastTick} guards against updating more
     * than once per tick (render runs once per *frame*, which is more often than once per tick at
     * normal framerates). Rendering interpolates {@code prev -> curr} by {@code partialTicks}, the
     * same way vanilla interpolates any entity's own position between ticks - so the lag/inertia
     * reads as smooth motion rather than a stepped snap every 1/20s.
     */
    private record BalloonPose(Vec3 prev, Vec3 curr, int lastTick) {
    }

    // Client-thread only (rendering is single-threaded), so a plain HashMap is safe. Keyed by
    // wearer entity id + accessory slot index so two balloons on the same player track separately.
    // Entries are never evicted on unequip - harmless, since this only ever holds one small record
    // per entity+slot that has *ever* worn a balloon this session, not a per-frame allocation.
    private static final Map<Long, BalloonPose> BALLOON_POSES = new HashMap<>();

    // World-space blocks the balloon rests above the wearer's hand (not their head/body). First-pass
    // value, tuned up from an earlier, shorter one that read as "stuck to the head".
    private static final double RISE_HEIGHT = 1.6;

    // Beyond this distance from where it currently is, the balloon ignores ceilings/walls entirely
    // and beelines straight for its target instead of respecting collision - otherwise a balloon left
    // far behind (the wearer just walked out from under a low overhang it was resting against, or
    // teleported) would have to awkwardly crawl back along terrain surfaces instead of just floating
    // back up through whatever's now in the way, the way a leashed-mob's elastic snap-back would.
    private static final double ESCAPE_DISTANCE = 3.0;

    /**
     * If the wearer is under something low (a cave ceiling, an overhang), the balloon shouldn't clip
     * up into/through it - it should rest against the surface, the way an actual balloon on a string
     * would bump against a ceiling instead of passing through it. Casts from the hand anchor toward
     * the unobstructed rest position and, if something solid is in the way, rests just short of it.
     * Skipped entirely once the balloon is far off course (see {@link #ESCAPE_DISTANCE}) so it can
     * still always find its way back rather than getting stuck.
     */
    private static Vec3 clampToCeiling(LivingEntity entity, Vec3 anchorPos, Vec3 rawRest, BalloonPose existing) {
        if (existing != null && existing.curr().distanceTo(rawRest) > ESCAPE_DISTANCE) {
            return rawRest;
        }
        ClipContext clipContext = new ClipContext(anchorPos, rawRest, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        BlockHitResult hit = entity.level().clip(clipContext);
        if (hit.getType() == HitResult.Type.MISS) {
            return rawRest;
        }
        Vec3 hitPos = hit.getLocation();
        Vec3 backOffSurface = anchorPos.subtract(hitPos).normalize().scale(0.1);
        return hitPos.add(backOffSurface);
    }

    private void renderBalloon(ItemStack stack, SlotReference reference, HumanoidModel<? extends LivingEntity> model, PoseStack matrices, MultiBufferSource buffer, int light, float partialTicks) {
        LivingEntity entity = reference.entity();
        long key = ((long) entity.getId() << 8) | (reference.slot() & 0xFF);

        // The rope's anchor is the wearer's actual hand (same bone renderHand uses), not a fixed
        // shoulder/chest height, so it visibly starts from where the cosmetic is "held". Read back
        // out of the *outer* matrices (already at the entity's current camera-relative render
        // position/rotation, arm-swing and all) instead of recomputing it, so this exactly matches
        // where a real held item would sit.
        boolean rightSide = isRightSide(reference);
        matrices.pushPose();
        model.translateToHand(rightSide ? HumanoidArm.RIGHT : HumanoidArm.LEFT, matrices);
        Vector3f handRender = matrices.last().pose().transformPosition(new Vector3f());
        matrices.popPose();

        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        // handRender is camera-relative (that's the space "matrices" itself works in); convert to a
        // true world position so it can be combined with the world-space balloon position below.
        Vec3 anchorPos = new Vec3(handRender.x() + cameraPos.x, handRender.y() + cameraPos.y, handRender.z() + cameraPos.z);

        // Rest position: risen straight up from the hand (not centered above the wearer's whole
        // body/head - the balloon should read as "tied to this hand", drifting horizontally with it
        // too), plus a gentle per-tick bob so it's not perfectly still even when the wearer stands
        // still. RISE_HEIGHT is deliberately generous - a taut string barely clearing the head reads
        // as "stuck to the head"; a longer one reads as an actual balloon on a string.
        int tick = entity.tickCount;
        float bob = Mth.sin(tick * 0.1F + reference.slot() * 10.0F) * 0.1F;
        Vec3 rawRest = anchorPos.add(0.0, RISE_HEIGHT + bob, 0.0);

        BalloonPose existing = BALLOON_POSES.get(key);
        Vec3 restPosition = clampToCeiling(entity, anchorPos, rawRest, existing);

        BalloonPose pose;
        if (existing == null) {
            pose = new BalloonPose(restPosition, restPosition, tick);
        } else if (existing.lastTick() != tick) {
            // Eased toward the rest position each tick rather than snapping - this is the "lags
            // behind, has its own inertia" behaviour a real leashed mob has, instead of being
            // rigidly bolted to the wearer.
            Vec3 nextCurr = existing.curr().lerp(restPosition, 0.3);
            pose = new BalloonPose(existing.curr(), nextCurr, tick);
        } else {
            pose = existing;
        }
        BALLOON_POSES.put(key, pose);

        Vec3 balloonPos = pose.prev().lerp(pose.curr(), partialTicks);

        Vec3 anchorRender = anchorPos.subtract(cameraPos);
        Vec3 balloonRender = balloonPos.subtract(cameraPos);

        PoseStack ropeMatrices = new PoseStack();
        ropeMatrices.translate(anchorRender.x, anchorRender.y, anchorRender.z);
        renderRope(ropeMatrices, buffer,
                (float) (balloonRender.x - anchorRender.x),
                (float) (balloonRender.y - anchorRender.y),
                (float) (balloonRender.z - anchorRender.z),
                light);

        // Faces the same way the wearer's body does (vanilla's yaw convention for entities is
        // "180 - bodyYaw" - see LivingEntityRenderer#setupRotations) instead of being frozen facing
        // one fixed compass direction while the wearer turns around it. Note: no X/Y mirror here,
        // unlike that vanilla method - that mirror only makes sense inside the entity's own nested
        // Y-down bone-local space, which this fresh, plain Y-up world-space PoseStack isn't in; a
        // first attempt copied it anyway and rendered the balloon upside down.
        float bodyYaw = entity.getPreciseBodyRotation(partialTicks);
        PoseStack itemMatrices = new PoseStack();
        itemMatrices.translate(balloonRender.x, balloonRender.y, balloonRender.z);
        itemMatrices.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        itemMatrices.scale(0.7F, 0.7F, 0.7F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, itemMatrices, buffer, entity.level(), 0);
    }

    /**
     * Draws a sagging rope quad-strip from the current PoseStack origin to {@code (dx, dy, dz)},
     * using vanilla's own {@code RenderType.leash()} texture/shader. This is the same vertex math as
     * {@code EntityRenderer#renderLeash}/{@code addVertexPair} (quadratic sag, alternating shade per
     * segment), just fed two chosen offsets instead of two real entities' positions, since there's no
     * second entity here to leash to.
     */
    private static void renderRope(PoseStack matrices, MultiBufferSource buffer, float dx, float dy, float dz, int light) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.leash());
        Matrix4f pose = matrices.last().pose();
        float halfWidth = 0.0125F;
        float invLen = Mth.invSqrt(dx * dx + dz * dz) * halfWidth;
        float nx = dz * invLen;
        float nz = dx * invLen;
        for (int i = 0; i <= 16; i++) {
            addRopeVertexPair(consumer, pose, dx, dy, dz, light, nx, nz, i, false);
        }
        for (int i = 16; i >= 0; i--) {
            addRopeVertexPair(consumer, pose, dx, dy, dz, light, nx, nz, i, true);
        }
    }

    private static void addRopeVertexPair(VertexConsumer consumer, Matrix4f pose, float dx, float dy, float dz, int light, float nx, float nz, int index, boolean bottomEdge) {
        float f = index / 16.0F;
        float shade = index % 2 == (bottomEdge ? 1 : 0) ? 0.7F : 1.0F;
        float x = dx * f;
        float y = (dy > 0.0F ? dy * f * f : dy - dy * (1.0F - f) * (1.0F - f)) - (bottomEdge ? 0.025F : 0.0F);
        float z = dz * f;
        consumer.addVertex(pose, x - nx, y, z + nz).setColor(0.5F * shade, 0.4F * shade, 0.3F * shade, 1.0F).setLight(light);
        consumer.addVertex(pose, x + nx, y, z - nz).setColor(0.5F * shade, 0.4F * shade, 0.3F * shade, 1.0F).setLight(light);
    }

    @Override
    public boolean shouldRenderInFirstPerson(HumanoidArm arm, ItemStack stack, SlotReference reference) {
        if (!"hand".equals(definition.slot()) || definition.balloon()) {
            return false;
        }
        return isRightSide(reference) == (arm == HumanoidArm.RIGHT);
    }
}
