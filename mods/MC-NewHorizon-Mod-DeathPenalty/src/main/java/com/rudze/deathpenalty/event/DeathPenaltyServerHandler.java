package com.rudze.deathpenalty.event;

import com.rudze.deathpenalty.DeathPenaltyAttachments;
import com.rudze.deathpenalty.config.DeathPenaltyConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Sets and enforces the respawn delay server-side, independent of the client-side button-greying
 * UX in DeathPenaltyClientHandler. The client gate can be bypassed (e.g. via a "quit to title"
 * confirmation dialog that lets the game respawn the player without going through our disabled
 * button), so the actual penalty - reduced stats, and a temporary incapacitating debuff covering
 * whatever time was skipped - is applied here regardless of how the respawn was triggered.
 * The cooldown itself is stored in a disk-persisted attachment (see DeathPenaltyAttachments), so
 * it survives disconnecting and reconnecting, or even a full server restart.
 */
public final class DeathPenaltyServerHandler {

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!DeathPenaltyConfig.ENABLED.get()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.isCreative()) {
            return;
        }
        long penaltyEnd = System.currentTimeMillis() + DeathPenaltyConfig.RESPAWN_DELAY_SECONDS.get() * 1000L;
        player.setData(DeathPenaltyAttachments.RESPAWN_PENALTY_END, penaltyEnd);
    }

    @SubscribeEvent
    public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!DeathPenaltyConfig.ENABLED.get()) {
            return;
        }
        if (event.isEndConquered()) {
            return;
        }
        Player player = event.getEntity();
        if (player.level().isClientSide()) {
            return;
        }

        float health = (float) (DeathPenaltyConfig.RESPAWN_HEARTS.get() * 2.0);
        player.setHealth(Math.min(health, player.getMaxHealth()));

        FoodData foodData = player.getFoodData();
        foodData.setFoodLevel(DeathPenaltyConfig.RESPAWN_HUNGER_ICONS.get() * 2);
        foodData.setSaturation((float) DeathPenaltyConfig.RESPAWN_SATURATION.get().doubleValue());

        long penaltyEnd = player.getData(DeathPenaltyAttachments.RESPAWN_PENALTY_END);
        long remainingMillis = penaltyEnd - System.currentTimeMillis();
        if (remainingMillis > 0) {
            applyEarlyRespawnLockout(player, remainingMillis);
        }
    }

    private void applyEarlyRespawnLockout(LivingEntity player, long remainingMillis) {
        int durationTicks = (int) Math.min(Integer.MAX_VALUE, remainingMillis / 50L + 1L);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, durationTicks, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 5, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, durationTicks, 3, false, false));
    }
}
