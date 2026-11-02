package tallestred.numismaticoverhaul.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.config.NOConfig;
import tallestred.numismaticoverhaul.init.DataAttachmentInit;
import tallestred.numismaticoverhaul.network.UpdatePlayerCurrencyPacket;
import tallestred.numismaticoverhaul.villagers.data.NumismaticVillagerTradesRegistry;
import tallestred.numismaticoverhaul.villagers.data.VillagerTradesResourceListener;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

import java.util.HashMap;

@EventBusSubscriber(modid = NumismaticOverhaul.MODID)
public class CommonForgeEvents {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() == VillagerProfession.NONE) return;
        for (int i = 1; i <= event.getTrades().size(); i++) {
            NumismaticVillagerTradesRegistry.registerFabricVillagerTrades(event.getType(), i, event.getTrades().get(i));
        }

//        for(int i = 1; i<= event.getTrades().size(); i++) {
//            if(event.getTrades().get(i).stream().filter(trade -> trade instanceof NumOTrade).count() == 0) {
//                continue;
//            }
//            for(int j = event.getTrades().get(i).size() - 1; j >=0; j--){
//                VillagerTrades.ItemListing trade = event.getTrades().get(i).get(j);
//                if(!(trade instanceof NumOTrade)){
//                    event.getTrades().get(i).remove(j);
//                }
//            }
//        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onWanderingTrades(WandererTradesEvent event) {
        for (int i = 0; i < event.getGenericTrades().size(); i++) {
            NumismaticVillagerTradesRegistry.registerWanderingTraderTrade(i, event.getGenericTrades().get(i));
        }
        for (int i = 0; i < event.getRareTrades().size(); i++) {
            NumismaticVillagerTradesRegistry.registerWanderingTraderTrade(i, event.getRareTrades().get(i));
        }
    }

    @SubscribeEvent
    public static void playerJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() != null && event.getEntity() instanceof ServerPlayer player && !event.getLevel().isClientSide()) {
            //if (player.hasData(DataAttachmentInit.VALUE.get()))
            //   NumismaticOverhaul.MY_CHANNEL.serverHandle(player).send(new UpdatePlayerCurrencyPacket(player.getExistingDataOrNull(DataAttachmentInit.VALUE.get())));
        }
    }

    @SubscribeEvent
    public static void reloadListener(AddReloadListenerEvent event) {
        event.addListener(new VillagerTradesResourceListener());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void doCommonVillagerTrades(ServerStartedEvent event) {
        if (!NOConfig.INSTANCE.enableVillagerTrading.get()) return;
        final Tuple<HashMap<VillagerProfession, Int2ObjectOpenHashMap<VillagerTrades.ItemListing[]>>, Int2ObjectOpenHashMap<VillagerTrades.ItemListing[]>> registry = NumismaticVillagerTradesRegistry.getRegistryForLoading();
        VillagerTrades.TRADES.putAll(registry.getA());
        NumismaticVillagerTradesRegistry.wrapModVillagers();
    }
}
