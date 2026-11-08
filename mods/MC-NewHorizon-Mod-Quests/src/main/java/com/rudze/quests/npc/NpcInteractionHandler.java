package com.rudze.quests.npc;

import com.cobblemon.mod.common.entity.npc.NPCEntity;
import com.rudze.quests.chen.ChenDialogues;
import com.rudze.quests.community.TomDialogues;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Right-clicking a {@link TownNpc} opens our Java-built dialogue instead of Cobblemon's own NPC
 * interaction, and stale copies of them never make it back into the world.
 */
public final class NpcInteractionHandler {

    @SubscribeEvent
    public void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof NPCEntity npc)) {
            return;
        }
        TownNpc.of(npc).ifPresent(townNpc -> {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player) {
                if (townNpc == TownNpc.CHEN) {
                    ChenDialogues.open(player, npc);
                } else if (townNpc == TownNpc.TOM) {
                    TomDialogues.open(player, npc);
                }
            }
        });
    }

    @SubscribeEvent
    public void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        TownNpc.of(event.getEntity())
                .filter(townNpc -> !townNpc.isCurrent(event.getLevel().getServer(), event.getEntity()))
                .ifPresent(townNpc -> event.setCanceled(true));
    }
}
