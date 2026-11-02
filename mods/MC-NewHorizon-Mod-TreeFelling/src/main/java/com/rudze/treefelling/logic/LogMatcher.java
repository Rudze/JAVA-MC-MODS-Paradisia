package com.rudze.treefelling.logic;

import com.rudze.treefelling.config.TreeFellingConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides whether a block counts as part of a tree trunk, based on the minecraft:logs tag
 * plus this module's extra/excluded block lists. Reads config live so it always reflects
 * the current (possibly reloaded) config values.
 */
public final class LogMatcher {

    private LogMatcher() {
    }

    public static boolean isLog(BlockState state) {
        Block block = state.getBlock();
        ResourceLocation id = blockId(block);
        if (isExcluded(id)) {
            return false;
        }
        return state.is(BlockTags.LOGS) || isExtra(id);
    }

    public static boolean isLeaves(BlockState state) {
        return state.is(BlockTags.LEAVES);
    }

    private static ResourceLocation blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private static boolean isExcluded(ResourceLocation id) {
        return toIdSet(TreeFellingConfig.EXCLUDED_LOG_BLOCKS.get()).contains(id.toString());
    }

    private static boolean isExtra(ResourceLocation id) {
        return toIdSet(TreeFellingConfig.EXTRA_LOG_BLOCKS.get()).contains(id.toString());
    }

    private static Set<String> toIdSet(List<? extends String> list) {
        return new HashSet<>(list);
    }
}
