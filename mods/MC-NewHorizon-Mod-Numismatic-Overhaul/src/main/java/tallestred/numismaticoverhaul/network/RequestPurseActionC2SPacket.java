package tallestred.numismaticoverhaul.network;

import io.wispforest.owo.network.ServerAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.InventoryMenu;
import tallestred.numismaticoverhaul.cap.CurrencyHolder;
import tallestred.numismaticoverhaul.currency.CurrencyConverter;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;

public record RequestPurseActionC2SPacket(Action action, long value) {
    public static void handle(RequestPurseActionC2SPacket message, ServerAccess access) {
        final ServerPlayer player = (ServerPlayer) access.player();

        if (player.containerMenu instanceof InventoryMenu) {

            switch (message.action) {
                case STORE_ALL -> CurrencyHolder.modify(player, CurrencyHelper.getMoneyInInventory(player, true));
                case EXTRACT -> {
                    //Check if we can actually extract this much money to prevent cheeky packet forgery
                    if (CurrencyHolder.getValue(player) < message.value()) return;

                    CurrencyConverter.getAsItemStackList(message.value()).forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
                    CurrencyHolder.modify(player, -message.value());
                }
                case EXTRACT_ALL -> {
                    CurrencyConverter.getAsValidStacks(CurrencyHolder.getValue(player))
                            .forEach(stack -> player.getInventory().placeItemBackInInventory(stack));

                    CurrencyHolder.modify(player, -CurrencyHolder.getValue(player));
                }
            }
        }
    }

    public static RequestPurseActionC2SPacket storeAll() {
        return new RequestPurseActionC2SPacket(Action.STORE_ALL, 0);
    }

    public static RequestPurseActionC2SPacket extractAll() {
        return new RequestPurseActionC2SPacket(Action.EXTRACT_ALL, 0);
    }

    public static RequestPurseActionC2SPacket extract(long amount) {
        return new RequestPurseActionC2SPacket(Action.EXTRACT, amount);
    }

    public enum Action {
        STORE_ALL, EXTRACT, EXTRACT_ALL
    }
}