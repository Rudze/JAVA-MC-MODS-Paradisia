package com.rudze.pokemononly;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Keeps the world to Pokémon. Mobs allowed everywhere: anything from Cobblemon (Pokémon, NPCs)
 * and villagers. Nether and End mobs are allowed in their own dimensions.
 *
 * <p>Natural spawns of other mobs are refused before the mob is even created, since the spawner
 * keeps retrying when the mob caps never fill up. Any other mob is dropped when it joins a level, which covers natural spawns, mobs placed at
 * chunk generation and mobs already saved in the world. The exception is mobs spawned on purpose
 * (spawn eggs, spawners, commands...): they get a tag when they spawn, so they are kept, even
 * after being saved and loaded again.
 */
public final class MobFilter {

    private static final String KEEP_TAG = PokemonOnlyMod.MODID + ".keep";
    private static final Set<MobSpawnType> DELIBERATE = EnumSet.of(
            MobSpawnType.SPAWN_EGG, MobSpawnType.DISPENSER, MobSpawnType.SPAWNER, MobSpawnType.TRIAL_SPAWNER,
            MobSpawnType.COMMAND, MobSpawnType.BREEDING, MobSpawnType.BUCKET, MobSpawnType.CONVERSION);

    @SubscribeEvent
    public void onSpawnPlacementCheck(MobSpawnEvent.SpawnPlacementCheck event) {
        if (!DELIBERATE.contains(event.getSpawnType())
                && !isAllowed(event.getEntityType(), event.getLevel().getLevel().dimension())) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (isAllowed(mob.getType(), mob.level().dimension())) {
            return;
        }
        if (DELIBERATE.contains(event.getSpawnType())) {
            mob.addTag(KEEP_TAG);
        } else {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!event.getLevel().isClientSide() && entity instanceof Mob
                && !entity.getTags().contains(KEEP_TAG) && !isAllowed(entity.getType(), event.getLevel().dimension())) {
            event.setCanceled(true);
        }
    }

    private static boolean isAllowed(EntityType<?> type, ResourceKey<Level> dimension) {
        return type == EntityType.VILLAGER
                || BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals("cobblemon")
                || dimension == Level.NETHER
                || dimension == Level.END;
    }
}
