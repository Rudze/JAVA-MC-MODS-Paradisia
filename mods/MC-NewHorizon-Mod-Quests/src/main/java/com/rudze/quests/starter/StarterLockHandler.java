package com.rudze.quests.starter;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.player.GeneralPlayerData;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreManager;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes;
import com.rudze.quests.data.PlayerProgress;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Starters come from Professor Chen, not from Cobblemon's starter key: players who haven't
 * finished his welcome speech get Cobblemon's own {@code starterLocked} flag, which makes both
 * the key and the selection packet answer "you can't choose a starter".
 *
 * <p>Chen unlocks it right before opening the starter screen, and it stays unlocked afterwards so
 * a player who closed the screen by mistake can still pick with the key or by talking to him again.
 */
public final class StarterLockHandler {

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || PlayerProgress.chenIntroDone(player)) {
            return;
        }
        GeneralPlayerData data = manager().getGenericData(player);
        if (!data.getStarterSelected() && !data.getStarterLocked()) {
            data.setStarterLocked(true);
            save(player, data);
        }
    }

    public static boolean hasStarter(ServerPlayer player) {
        return manager().getGenericData(player).getStarterSelected();
    }

    public static void unlockAndOpenStarterScreen(ServerPlayer player) {
        GeneralPlayerData data = manager().getGenericData(player);
        if (data.getStarterLocked()) {
            data.setStarterLocked(false);
            save(player, data);
        }
        Cobblemon.INSTANCE.getStarterHandler().requestStarterChoice(player);
    }

    private static void save(ServerPlayer player, GeneralPlayerData data) {
        data.sendToPlayer(player);
        manager().saveSingle(data, PlayerInstancedDataStoreTypes.INSTANCE.getGENERAL());
    }

    private static PlayerInstancedDataStoreManager manager() {
        return Cobblemon.INSTANCE.getPlayerDataManager();
    }
}
