package com.rudze.bounties.mixin;

import com.rudze.bounties.DailyLimit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Enforces {@link DailyLimit} where a bounty leaves the board. Both a plain click and a shift-click
 * ask the slot first, so this single check covers them. The client still predicts the pickup;
 * the server's refusal puts the bounty back.
 */
@Mixin(targets = "io.ejekta.bountiful.content.gui.BoardBountySlot")
public abstract class BoardBountySlotMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void nh_bounties$dailyLimit(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (player instanceof ServerPlayer serverPlayer && !DailyLimit.allow(serverPlayer)) {
            cir.setReturnValue(false);
        }
    }
}
