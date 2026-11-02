package com.rudze.treefelling.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Flood-fills connected log blocks starting from a broken log, restricted to positions
 * strictly above it (never touches logs below or at the same height as the origin).
 */
public final class TreeDetector {

    private static final BlockPos[] ORTHOGONAL_OFFSETS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    private static final BlockPos[] FULL_26_OFFSETS = build26Offsets();

    private static BlockPos[] build26Offsets() {
        List<BlockPos> offsets = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dy != 0 || dz != 0) {
                        offsets.add(new BlockPos(dx, dy, dz));
                    }
                }
            }
        }
        return offsets.toArray(new BlockPos[0]);
    }

    public record Result(List<BlockPos> logsAbove, boolean truncated) {
    }

    private TreeDetector() {
    }

    /**
     * @param origin     the position of the log block that was just broken
     * @param isLog      predicate matching blocks that count as trunk
     * @param use26      whether to use full 26-neighbour connectivity instead of the 6 orthogonal ones
     * @param maxResults search cap; once reached, the flood fill stops early
     */
    public static Result findLogsAbove(BlockGetter level, BlockPos origin, Predicate<BlockState> isLog, boolean use26, int maxResults) {
        BlockPos[] offsets = use26 ? FULL_26_OFFSETS : ORTHOGONAL_OFFSETS;

        Set<BlockPos> visited = new HashSet<>();
        List<BlockPos> found = new ArrayList<>();
        Deque<BlockPos> queue = new ArrayDeque<>();

        visited.add(origin);
        queue.add(origin);
        boolean truncated = false;

        outer:
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (BlockPos offset : offsets) {
                BlockPos next = current.offset(offset);
                if (next.getY() <= origin.getY() || !visited.add(next)) {
                    continue;
                }
                if (!isLog.test(level.getBlockState(next))) {
                    continue;
                }
                found.add(next);
                if (found.size() >= maxResults) {
                    truncated = true;
                    break outer;
                }
                queue.add(next);
            }
        }

        return new Result(found, truncated);
    }
}
