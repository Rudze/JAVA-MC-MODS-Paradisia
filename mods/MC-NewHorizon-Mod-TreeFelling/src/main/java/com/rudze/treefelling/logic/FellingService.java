package com.rudze.treefelling.logic;

import com.rudze.treefelling.config.TreeFellingConfig;
import com.rudze.treefelling.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Core Tree Felling behaviour: axe check, anti-cheat leaves check, break-time multiplier
 * and the actual cascading break of the logs above the one the player targeted.
 */
public final class FellingService {

    private FellingService() {
    }

    public static boolean isHoldingAxe(Player player) {
        return player.getMainHandItem().is(ItemTags.AXES);
    }

    public static boolean hasNearbyLeaves(LevelReader level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    cursor.setWithOffset(center, x, y, z);
                    if (LogMatcher.isLeaves(level.getBlockState(cursor))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static BlockPos topOf(BlockPos origin, List<BlockPos> logsAbove) {
        BlockPos top = origin;
        for (BlockPos pos : logsAbove) {
            if (pos.getY() > top.getY()) {
                top = pos;
            }
        }
        return top;
    }

    /** Multiplier applied to the vanilla break speed of the targeted log, based on total tree size. */
    public static double computeTimeMultiplier(int treeSize) {
        double raw = TreeFellingConfig.BASE_TIME_MULTIPLIER.get()
                + (treeSize - 1) * TreeFellingConfig.PER_LOG_TIME_MULTIPLIER.get();
        return Math.min(TreeFellingConfig.MAX_TIME_MULTIPLIER.get(), Math.max(1.0, raw));
    }

    /**
     * Fells the given logs (all strictly above the log the player just broke), respecting
     * the configured fall mode. Must be called server-side only, after the originally
     * targeted log has already been let through to break via normal game code.
     */
    public static void fellTree(ServerLevel level, Player player, List<BlockPos> logsAbove) {
        if (logsAbove.isEmpty()) {
            return;
        }
        if (TreeFellingConfig.FALL_MODE.get() == TreeFellingConfig.FallMode.INSTANT) {
            for (BlockPos pos : logsAbove) {
                if (!breakOne(level, player, pos)) {
                    break;
                }
            }
        } else {
            FellingJob job = new FellingJob();
            int delayStep = TreeFellingConfig.PROGRESSIVE_DELAY_TICKS.get();
            int delay = 0;
            for (BlockPos pos : logsAbove) {
                delay += delayStep;
                int scheduledDelay = delay;
                TickScheduler.INSTANCE.schedule(scheduledDelay, () -> {
                    if (job.aborted) {
                        return;
                    }
                    if (!player.isAlive() || player.isRemoved() || player.level() != level) {
                        job.aborted = true;
                        return;
                    }
                    if (!breakOne(level, player, pos)) {
                        job.aborted = true;
                    }
                });
            }
        }
    }

    /** @return true if the caller may continue felling further logs (axe still usable). */
    private static boolean breakOne(ServerLevel level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!LogMatcher.isLog(state)) {
            return true;
        }
        if (TreeFellingConfig.REQUIRE_AXE.get() && !isHoldingAxe(player)) {
            return false;
        }

        level.destroyBlock(pos, true, player);

        if (player.isCreative()) {
            return true;
        }

        ItemStack axeStack = player.getMainHandItem();
        if (axeStack.isEmpty() || !axeStack.isDamageableItem()) {
            return false;
        }
        axeStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        return !player.getMainHandItem().isEmpty();
    }

    private static final class FellingJob {
        private boolean aborted = false;
    }
}
