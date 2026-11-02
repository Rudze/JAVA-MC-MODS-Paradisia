package com.rudze.quests.chen;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right-clicking Chen opens our Java-built dialogue instead of Cobblemon's own NPC interaction,
 * and stale copies of him (see {@link ProfessorChen}) never make it back into the world.
 */
public final class ChenInteractionHandler {

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof NPCEntity npc) || !ProfessorChen.isChen(npc)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player) {
            ChenDialogues.open(player, npc);
        }
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && ProfessorChen.isChen(event.getEntity())
                && !ProfessorChen.get(event.getLevel().getServer()).isCurrent(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
