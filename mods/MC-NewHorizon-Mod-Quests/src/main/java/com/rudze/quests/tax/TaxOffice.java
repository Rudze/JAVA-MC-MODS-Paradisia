package com.rudze.quests.tax;

import com.rudze.worldbuild.claim.ClaimData;
import com.rudze.worldbuild.claim.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import tallestred.numismaticoverhaul.currency.CurrencyConverter;

/**
 * Professor Chen's tax desk: players pay their world_build land tax (billed by nh_worldbuild) by
 * dropping coins in a chest, and the mayor collects everything paid so far.
 */
public final class TaxOffice {

    private TaxOffice() {
    }

    public static void openPayment(ServerPlayer player) {
        long owed = ClaimData.get(player.server).debt(player.getUUID());
        Component title = Component.literal(owed > 0 ? "Impôts - dû : " + Coins.format(owed) : "Impôts - rien à payer");
        player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new TaxMenu(id, inventory, owed), title));
    }

    /** Called by {@link TaxMenu} when the chest is closed with coins inside. */
    static void pay(ServerPlayer player, long amount) {
        ClaimData claims = ClaimData.get(player.server);
        claims.pay(player.getUUID(), amount);
        Treasury.get(player.server).deposit(amount);

        long remaining = claims.debt(player.getUUID());
        String status = remaining > 0 ? "Il te reste " + Coins.format(remaining) + " à payer."
                : remaining < 0 ? "Tu as " + Coins.format(-remaining) + " d'avance."
                : "Tu es à jour, merci !";
        player.sendSystemMessage(Component.literal("Tu as payé " + Coins.format(amount) + " d'impôts. " + status)
                .withStyle(ChatFormatting.GREEN));
    }

    public static void collect(ServerPlayer mayor) {
        long amount = Treasury.get(mayor.server).withdrawAll();
        if (amount <= 0) {
            mayor.sendSystemMessage(Component.literal("Les caisses sont vides : personne n'a encore payé.")
                    .withStyle(ChatFormatting.YELLOW));
            return;
        }
        for (ItemStack coins : CurrencyConverter.getAsValidStacks(amount)) {
            mayor.getInventory().placeItemBackInInventory(coins);
        }
        mayor.sendSystemMessage(Component.literal("Tu as récupéré " + Coins.format(amount) + " d'impôts.")
                .withStyle(ChatFormatting.GOLD));
    }
}
