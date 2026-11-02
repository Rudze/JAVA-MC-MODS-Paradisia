package com.rudze.worldbuild;

import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Cancels ambient/natural mob spawning in world_build only, so the imported Driftfall Island
 * terrain stays hostile/passive-mob-free without touching the copied real-world biomes.
 *
 * <p>Only {@link MobSpawnType#NATURAL} is blocked here: spawn eggs, mob spawners, commands, and
 * mods that summon entities directly go through other spawn types and are left alone.
 */
public final class NaturalSpawnBlocker {

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL
                && event.getLevel().getLevel().dimension() == WorldBuildMod.WORLD_BUILD_LEVEL) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
