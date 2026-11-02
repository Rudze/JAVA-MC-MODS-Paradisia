package com.rudze.treefelling.event;

import com.rudze.treefelling.config.TreeFellingConfig;
import com.rudze.treefelling.logic.FellingService;
import com.rudze.treefelling.logic.LogMatcher;
import com.rudze.treefelling.logic.TreeDetector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Wires vanilla/NeoForge events to {@link FellingService}. All the actual gameplay logic
 * (tree detection, drops, durability, scheduling) lives in {@code logic}; this class only
 * validates preconditions and dispatches.
 */
public final class TreeFellingEventHandler {

    @SubscribeEvent
    public void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!TreeFellingConfig.ENABLED.get()) {
            return;
        }
        Player player = event.getEntity();
        if (player.isCreative()) {
            return;
        }
        BlockState state = event.getState();
        if (!LogMatcher.isLog(state)) {
            return;
        }
        if (TreeFellingConfig.REQUIRE_AXE.get() && !FellingService.isHoldingAxe(player)) {
            return;
        }
        BlockPos pos = event.getPosition().orElse(null);
        if (pos == null) {
            return;
        }

        TreeDetector.Result result = TreeDetector.findLogsAbove(
                player.level(), pos, LogMatcher::isLog,
                TreeFellingConfig.USE_26_DIRECTIONS.get(), TreeFellingConfig.MAX_TREE_SIZE.get());

        if (TreeFellingConfig.REQUIRE_LEAVES_NEARBY.get()) {
            BlockPos top = FellingService.topOf(pos, result.logsAbove());
            if (!FellingService.hasNearbyLeaves(player.level(), top, TreeFellingConfig.LEAVES_SEARCH_RADIUS.get())) {
                return;
            }
        }

        int treeSize = result.logsAbove().size() + 1;
        double multiplier = FellingService.computeTimeMultiplier(treeSize);
        if (multiplier > 1.0) {
            event.setNewSpeed((float) (event.getNewSpeed() / multiplier));
        }
    }

    @SubscribeEvent
    public void onBreak(BlockEvent.BreakEvent event) {
        if (!TreeFellingConfig.ENABLED.get()) {
            return;
        }
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        if (!LogMatcher.isLog(state)) {
            return;
        }
        if (TreeFellingConfig.REQUIRE_AXE.get() && !FellingService.isHoldingAxe(player)) {
            return;
        }

        BlockPos origin = event.getPos();
        TreeDetector.Result result = TreeDetector.findLogsAbove(
                level, origin, LogMatcher::isLog,
                TreeFellingConfig.USE_26_DIRECTIONS.get(), TreeFellingConfig.MAX_TREE_SIZE.get());

        if (result.logsAbove().isEmpty()) {
            return;
        }

        if (TreeFellingConfig.REQUIRE_LEAVES_NEARBY.get()) {
            BlockPos top = FellingService.topOf(origin, result.logsAbove());
            if (!FellingService.hasNearbyLeaves(level, top, TreeFellingConfig.LEAVES_SEARCH_RADIUS.get())) {
                return;
            }
        }

        FellingService.fellTree(level, player, result.logsAbove());
    }
}
