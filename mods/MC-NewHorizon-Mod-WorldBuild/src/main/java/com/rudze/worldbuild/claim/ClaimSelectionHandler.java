package com.rudze.worldbuild.claim;

import com.rudze.worldbuild.Mayor;
import com.rudze.worldbuild.WorldBuildMod;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Lets the mayor mark a claim's two opposite corners by right-clicking blocks in world_build with
 * a golden shovel; {@code /claim <player>} then turns the selection into a claim. Clicks
 * alternate between the first and second corner.
 *
 * <p>Selections are only kept in memory: they are meant to be used right away.
 */
public final class ClaimSelectionHandler {

    public record Selection(@Nullable BlockPos first, @Nullable BlockPos second) {
        public boolean isComplete() {
            return first != null && second != null;
        }
    }

    private static final Map<UUID, Selection> SELECTIONS = new HashMap<>();

    @Nullable
    public static Selection get(Player player) {
        return SELECTIONS.get(player.getUUID());
    }

    public static void clear(Player player) {
        SELECTIONS.remove(player.getUUID());
    }

    /** Runs on both sides so the client doesn't predict a dirt path under the shovel. */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND
                || !event.getItemStack().is(Items.GOLDEN_SHOVEL)
                || event.getLevel().dimension() != WorldBuildMod.WORLD_BUILD_LEVEL
                || !Mayor.isMayor(player)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockPos pos = event.getPos();
        Selection current = SELECTIONS.get(player.getUUID());
        Selection next = current == null || current.isComplete() || current.first() == null
                ? new Selection(pos, null)
                : new Selection(current.first(), pos);
        SELECTIONS.put(player.getUUID(), next);

        if (!next.isComplete()) {
            player.sendSystemMessage(Component.literal("Coin 1 : " + pos.getX() + ", " + pos.getZ()
                    + ". Clic droit sur le coin opposé.").withStyle(ChatFormatting.GOLD));
            return;
        }
        Claim preview = Claim.of(0, player.getUUID(), "", next.first(), next.second(), 0);
        player.sendSystemMessage(Component.literal("Coin 2 : " + pos.getX() + ", " + pos.getZ()
                + ". Zone " + preview.describeBounds() + " = " + preview.area() + " blocs, taxe "
                + Coins.format(preview.weeklyTax()) + " / semaine.").withStyle(ChatFormatting.GOLD));
        player.sendSystemMessage(Component.literal("Tape /claim <joueur> pour l'attribuer.").withStyle(ChatFormatting.YELLOW));
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }
}
