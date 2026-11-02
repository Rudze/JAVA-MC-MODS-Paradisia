package com.rudze.worldbuild;

import com.rudze.waystonetp.block.ModBlocks;
import com.rudze.waystonetp.block.WaystoneBlock;
import com.rudze.waystonetp.block.WaystoneVariant;
import com.rudze.waystonetp.data.WaystoneNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Once the server is up, drops a Waystone B at the main world's spawn and a Waystone A near
 * world_build's (Driftfall Island's) original spawn, each a couple of blocks off the exact
 * spawn point so players don't spawn inside the pedestal.
 *
 * <p>Gating on {@link WaystoneNetwork#hasAny} rather than re-probing the target block position:
 * once a waystone is placed it becomes part of the terrain's heightmap, so recomputing "surface
 * near spawn" on a later boot would land 2 blocks higher, on top of the previous one, and the
 * position-based check would never see it as already placed.
 *
 * <p>{@link #pruneStale} runs first specifically because {@code hasAny}'s flag lives in the
 * overworld's save data, not in world_build's own dimension folder: deleting just
 * {@code world/dimensions/nh_worldbuild/world_build/} (e.g. to fix a bad terrain generator) wipes
 * the actual waystone block but leaves the overworld-side registration behind, so without this
 * check the handler would believe a Waystone A already exists and never replace it.
 */
public final class SpawnWaystoneHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnWaystoneHandler.class);

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        WaystoneNetwork network = WaystoneNetwork.get(overworld);

        pruneStale(server, network, WaystoneVariant.A);
        pruneStale(server, network, WaystoneVariant.B);

        if (!network.hasAny(WaystoneVariant.B)) {
            BlockPos overworldSpawn = overworld.getSharedSpawnPos();
            BlockPos waystoneBPos = surfacePosNear(overworld, overworldSpawn.offset(2, 0, 0));
            placeWaystone(overworld, ModBlocks.WAYSTONE_B.get(), waystoneBPos);
        }

        ServerLevel worldBuild = server.getLevel(WorldBuildMod.WORLD_BUILD_LEVEL);
        if (worldBuild == null) {
            LOGGER.warn("[nh_worldbuild] world_build dimension isn't loaded; leave and re-enter the world once "
                    + "so NeoForge picks up the new dimension, then restart the server.");
            return;
        }

        if (!network.hasAny(WaystoneVariant.A)) {
            BlockPos sourceSpawn = SourceMapInstaller.readSourceSpawnXZ().orElse(BlockPos.ZERO);
            BlockPos waystoneAPos = surfacePosNear(worldBuild, sourceSpawn.offset(2, 0, 0));
            placeWaystone(worldBuild, ModBlocks.WAYSTONE_A.get(), waystoneAPos);
        }
    }

    private static void pruneStale(MinecraftServer server, WaystoneNetwork network, WaystoneVariant variant) {
        for (GlobalPos pos : network.positionsOf(variant)) {
            ServerLevel level = server.getLevel(pos.dimension());
            if (level == null || !isWaystoneAt(level, pos.pos())) {
                network.unregister(variant, pos);
                LOGGER.info("[nh_worldbuild] Waystone {} at {} no longer exists on disk; forgetting it so it gets re-placed.",
                        variant, pos);
            }
        }
    }

    /** Force-loads the chunk (cheap even for world_build now that it's a void generator) so a
     * dimension folder deleted outside the running game reads back as "nothing here" rather than
     * an already-loaded false positive. */
    private static boolean isWaystoneAt(ServerLevel level, BlockPos pos) {
        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return level.getBlockState(pos).getBlock() instanceof WaystoneBlock;
    }

    private static BlockPos surfacePosNear(ServerLevel level, BlockPos approx) {
        level.getChunk(approx.getX() >> 4, approx.getZ() >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, approx.getX(), approx.getZ());
        return new BlockPos(approx.getX(), y, approx.getZ());
    }

    private static void placeWaystone(ServerLevel level, Block waystoneBlock, BlockPos basePos) {
        BlockState lower = waystoneBlock.defaultBlockState();
        level.setBlock(basePos, lower, 3);
        level.setBlock(basePos.above(), lower.setValue(WaystoneBlock.HALF, DoubleBlockHalf.UPPER), 3);
        LOGGER.info("[nh_worldbuild] Placed {} at {} in {}.", waystoneBlock, basePos, level.dimension().location());
    }
}
