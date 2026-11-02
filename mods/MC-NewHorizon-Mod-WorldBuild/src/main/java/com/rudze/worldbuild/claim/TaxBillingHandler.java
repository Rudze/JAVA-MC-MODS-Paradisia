package com.rudze.worldbuild.claim;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;

/**
 * Bills claim owners once per real-time week (see {@link ClaimData#billDue}). Checked once a
 * minute, which is plenty for a weekly period and catches up on weeks spent offline at boot.
 */
public final class TaxBillingHandler {

    private static final int CHECK_INTERVAL_TICKS = 20 * 60;

    private int ticks;

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (++ticks < CHECK_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        MinecraftServer server = event.getServer();
        ClaimData data = ClaimData.get(server);
        for (Map.Entry<UUID, Long> billed : data.billDue(System.currentTimeMillis()).entrySet()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(billed.getKey());
            if (owner != null) {
                owner.sendSystemMessage(Component.literal("Taxe foncière : " + Coins.format(billed.getValue())
                        + " ajoutés à tes impôts (total dû : " + Coins.format(data.debt(owner.getUUID()))
                        + "). Va payer auprès du Professeur Chen !").withStyle(ChatFormatting.GOLD));
            }
        }
    }
}
