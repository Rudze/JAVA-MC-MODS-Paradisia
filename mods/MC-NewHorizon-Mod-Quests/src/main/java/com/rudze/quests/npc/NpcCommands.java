package com.rudze.quests.npc;

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
 * {@code /chen} and {@code /tom} put that NPC where the mayor stands (facing the same way),
 * spawning them the first time.
 */
public final class NpcCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("chen").requires(Mayor::isMayor)
                .executes(ctx -> place(ctx, TownNpc.CHEN)));
        event.getDispatcher().register(Commands.literal("tom").requires(Mayor::isMayor)
                .executes(ctx -> place(ctx, TownNpc.TOM)));
    }

    private static int place(CommandContext<CommandSourceStack> ctx, TownNpc npc) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean moved = npc.moveOrSpawnAt(player);
        ctx.getSource().sendSuccess(() -> Component.literal(moved
                ? npc.name() + " a été déplacé ici."
                : npc.name() + " est arrivé !").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
