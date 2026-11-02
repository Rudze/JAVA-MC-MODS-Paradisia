package tallestred.numismaticoverhaul.cap;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.currency.CurrencyConverter;
import tallestred.numismaticoverhaul.init.DataAttachmentInit;
import tallestred.numismaticoverhaul.item.CoinItem;
import tallestred.numismaticoverhaul.network.UpdatePlayerCurrencyPacket;
import tallestred.numismaticoverhaul.network.UpdateShopScreenS2CPacket;

import java.util.ArrayList;
import java.util.List;

public class CurrencyHolder {
    public static long getValue(Player player) {
        return player.getData(DataAttachmentInit.VALUE.get());
    }

    public static void setValue(Player player, long value) {
        player.setData(DataAttachmentInit.VALUE.get(), value);
        if (player instanceof ServerPlayer)
            NumismaticOverhaul.MY_CHANNEL.serverHandle(player).send(new UpdatePlayerCurrencyPacket(value));
    }

    public static void silentModify(Player player, long value) {
        setValue(player, getValue(player) + value);
    }


    public static void modify(Player player, long value) {
        setValue(player, getValue(player) + value);

        long tempValue = value < 0 ? -value : value;

        List<ItemStack> transactionStacks = CurrencyConverter.getAsItemStackList(tempValue);
        if (transactionStacks.isEmpty()) return;

        MutableComponent message = value < 0 ? net.minecraft.network.chat.Component.literal("§c- ") : net.minecraft.network.chat.Component.literal("§a+ ");
        message.append(net.minecraft.network.chat.Component.literal("§7["));
        for (ItemStack stack : transactionStacks) {
            message.append(net.minecraft.network.chat.Component.literal("§b" + stack.getCount() + " "));
            message.append(net.minecraft.network.chat.Component.translatable("currency.numismaticoverhaul." + ((CoinItem) stack.getItem()).currency.name().toLowerCase()));
            if (transactionStacks.indexOf(stack) != transactionStacks.size() - 1)
                message.append(net.minecraft.network.chat.Component.literal(", "));
        }
        message.append(net.minecraft.network.chat.Component.literal("§7]"));

        player.displayClientMessage(message, true);
    }
}
