package com.rudze.worldbuild.claim;

import com.rudze.worldbuild.Mayor;
import com.rudze.worldbuild.WorldBuildMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Locks world_build down: only the mayor builds anywhere, and claim owners build inside their own
 * plots (full height). Everyone may still use doors, buttons and the like; only breaking,
 * placing and using items on blocks (buckets, bone meal, hoes...) are denied.
 *
 * <p>Checks only run server-side: claims aren't synced to clients, and the server's refusal
 * already resyncs any block the client predicted.
 */
public final class ClaimProtectionHandler {

    public static boolean canBuild(Player player, LevelAccessor level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel) || serverLevel.dimension() != WorldBuildMod.WORLD_BUILD_LEVEL) {
            return true;
        }
        if (Mayor.isMayor(player)) {
            return true;
        }
        return ClaimData.get(serverLevel.getServer()).claimAt(pos)
                .map(claim -> claim.owner().equals(player.getUUID()))
                .orElse(false);
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (!canBuild(event.getPlayer(), event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            deny(event.getPlayer());
        }
    }

    /** Also receives {@link BlockEvent.EntityMultiPlaceEvent} (beds, doors). Non-player placers
     * (endermen, falling blocks) are left alone; natural mob spawning is already off here. */
    @SubscribeEvent
    public void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && !canBuild(player, event.getLevel(), event.getPos())) {
            event.setCanceled(true);
            deny(player);
        }
    }

    /** LOW so the golden-shovel selection (default priority) gets to cancel the event first. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onUseItemOnBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getItemStack().isEmpty()) {
            return;
        }
        Player player = event.getEntity();
        BlockPos placedAt = event.getPos().relative(event.getHitVec().getDirection());
        if (!canBuild(player, event.getLevel(), event.getPos()) || !canBuild(player, event.getLevel(), placedAt)) {
            event.setUseItem(TriState.FALSE);
            if (event.getHand() == InteractionHand.MAIN_HAND && event.getItemStack().getItem() instanceof BucketItem) {
                deny(player);
            }
        }
    }

    /** Buckets pick up / pour fluids through a ray trace in {@code Item#use}, without ever going
     * through RightClickBlock, so they're gated on where the player stands. */
    @SubscribeEvent
    public void onUseBucket(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getItemStack().getItem() instanceof BucketItem
                && !canBuild(player, event.getLevel(), player.blockPosition())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            deny(player);
        }
    }

    @SubscribeEvent
    public void onTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof Player player && !canBuild(player, event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    private static void deny(Player player) {
        player.displayClientMessage(
                Component.literal("Ce terrain ne t'appartient pas. Demande un claim au maire !").withStyle(ChatFormatting.RED),
                true);
    }
}
