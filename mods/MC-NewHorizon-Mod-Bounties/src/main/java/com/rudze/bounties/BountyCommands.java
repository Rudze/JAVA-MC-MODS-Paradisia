package com.rudze.bounties;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.rudze.worldbuild.Mayor;
import io.ejekta.bountiful.content.BountifulContent;
import io.ejekta.bountiful.content.item.DecreeItem;
import io.ejekta.bountiful.decree.DecreeSpawnRank;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.Map;

/**
 * Lets the mayor, who isn't a server operator, set up bounty boards: {@code /bounty board} hands
 * over a board and {@code /bounty decree <theme>} the decree that picks its bounties.
 */
public final class BountyCommands {

    /** Theme name typed in the command, to the decree id in {@code bounty_decrees/paradisia}. */
    private static final Map<String, String> THEMES = Map.of(
            "dresseur", "nh_dresseur",
            "recolte", "nh_recolte",
            "mineur", "nh_mineur",
            "nature", "nh_nature");

    private static final DynamicCommandExceptionType UNKNOWN_THEME = new DynamicCommandExceptionType(
            theme -> Component.literal("Thème inconnu : " + theme));

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bounty").requires(Mayor::isMayor)
                .then(Commands.literal("board").executes(BountyCommands::board))
                .then(Commands.literal("decree")
                        .then(Commands.argument("theme", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(THEMES.keySet(), builder))
                                .executes(BountyCommands::decree))));
    }

    private static int board(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        give(ctx.getSource().getPlayerOrException(), new ItemStack(BountifulContent.INSTANCE.getBOARD_ITEM()), "un tableau de quêtes");
        return 1;
    }

    private static int decree(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String theme = StringArgumentType.getString(ctx, "theme");
        String id = THEMES.get(theme);
        if (id == null) {
            throw UNKNOWN_THEME.create(theme);
        }
        give(ctx.getSource().getPlayerOrException(),
                DecreeItem.Companion.create(List.of(id), 1, DecreeSpawnRank.CONSTANT), "le décret « " + theme + " »");
        return 1;
    }

    private static void give(ServerPlayer player, ItemStack stack, String what) {
        player.getInventory().placeItemBackInInventory(stack);
        player.sendSystemMessage(Component.literal("Tu as reçu " + what + ".").withStyle(ChatFormatting.GREEN));
    }
}
