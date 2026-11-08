package com.rudze.quests.npc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.rudze.worldbuild.Mayor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /npc spawn <name>} puts that NPC where the mayor stands (facing the same way), spawning
 * them the first time.
 */
public final class NpcCommands {

    private static final DynamicCommandExceptionType UNKNOWN_NPC = new DynamicCommandExceptionType(
            key -> Component.literal("PNJ inconnu : " + key));

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("npc").requires(Mayor::isMayor)
                .then(Commands.literal("spawn")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        TownNpc.ALL.stream().map(TownNpc::key), builder))
                                .executes(NpcCommands::spawn))));
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String key = StringArgumentType.getString(ctx, "name");
        TownNpc npc = TownNpc.byKey(key).orElseThrow(() -> UNKNOWN_NPC.create(key));
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        boolean moved = npc.moveOrSpawnAt(player);
        ctx.getSource().sendSuccess(() -> Component.literal(moved
                ? npc.name() + " a été déplacé ici."
                : npc.name() + " est arrivé !").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }
}
