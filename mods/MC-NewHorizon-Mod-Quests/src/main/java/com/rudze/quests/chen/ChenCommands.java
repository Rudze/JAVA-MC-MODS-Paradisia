package com.rudze.quests.chen;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rudze.worldbuild.Mayor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /chen} puts Professor Chen where the mayor stands (facing the same way), spawning him
 * the first time.
 */
public final class ChenCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("chen").requires(Mayor::isMayor)
                .executes(ChenCommands::place));
    }

    private static int place(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean moved = ProfessorChen.get(player.server).moveOrSpawnAt(player);
        ctx.getSource().sendSuccess(() -> Component.literal(moved
                ? "Le Professeur Chen a été déplacé ici."
                : "Le Professeur Chen est arrivé !").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
