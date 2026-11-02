package com.rudze.worldbuild;

import com.rudze.waystonetp.block.WaystoneVariant;
import com.rudze.waystonetp.data.WaystoneNetwork;
import com.rudze.worldbuild.data.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;

import java.util.Set;

/**
 * Vanilla nether portals only ever remember two sides: any non-Nether dimension in, and the
 * overworld out (see {@code NetherPortalBlock.getPortalDestination}'s hardcoded ternary). For a
 * third dimension like world_build that means a portal built there works one-way: it drops you in
 * the shared Nether fine, but any portal back out always lands you in the overworld instead of
 * back in world_build, which reads as "the portal doesn't work" once you can't get back.
 *
 * <p>This remembers, per entity, which non-Nether dimension they most recently entered the Nether
 * from, and redirects the return trip there instead of following vanilla's overworld default.
 */
public final class NetherPortalRedirectHandler {

    @SubscribeEvent
    public void onTravel(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel currentLevel)) {
            return;
        }

        ResourceKey<Level> from = currentLevel.dimension();
        ResourceKey<Level> to = event.getDimension();

        if (to == Level.NETHER && from != Level.NETHER) {
            entity.setData(ModAttachments.LAST_NON_NETHER_DIMENSION, from);
            return;
        }

        if (to == Level.OVERWORLD && from == Level.NETHER && entity instanceof ServerPlayer player) {
            ResourceKey<Level> lastNonNether = entity.getData(ModAttachments.LAST_NON_NETHER_DIMENSION);
            if (lastNonNether == WorldBuildMod.WORLD_BUILD_LEVEL) {
                ServerLevel worldBuild = currentLevel.getServer().getLevel(WorldBuildMod.WORLD_BUILD_LEVEL);
                if (worldBuild != null) {
                    event.setCanceled(true);
                    BlockPos landing = landingSpot(worldBuild);
                    player.teleportTo(worldBuild, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5,
                            Set.of(), player.getYRot(), player.getXRot());
                }
            }
        }
    }

    /** Lands the player next to Waystone A if it's still there, otherwise at world_build's spawn
     * surface - either way somewhere safe rather than wherever vanilla's portal-scaling math
     * would have guessed. */
    private static BlockPos landingSpot(ServerLevel worldBuild) {
        WaystoneNetwork network = WaystoneNetwork.get(worldBuild);
        for (GlobalPos pos : network.positionsOf(WaystoneVariant.A)) {
            if (pos.dimension() == WorldBuildMod.WORLD_BUILD_LEVEL) {
                return pos.pos().offset(2, 0, 0);
            }
        }
        BlockPos spawn = worldBuild.getSharedSpawnPos();
        worldBuild.getChunk(spawn.getX() >> 4, spawn.getZ() >> 4);
        int y = worldBuild.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.getX(), spawn.getZ());
        return new BlockPos(spawn.getX(), y, spawn.getZ());
    }
}
