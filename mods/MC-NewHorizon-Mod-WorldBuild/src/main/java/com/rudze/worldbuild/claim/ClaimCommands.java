package com.rudze.worldbuild.claim;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.rudze.worldbuild.Mayor;
import com.rudze.worldbuild.WorldBuildMod;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@code /claim <joueur>} turns the mayor's golden-shovel selection into a claim;
 * {@code /claim liste} and {@code /claim retirer <id>} manage them (mayor only);
 * {@code /claim info} tells anyone who owns the ground they stand on and what they owe.
 */
public final class ClaimCommands {

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("claim")
                .then(Commands.literal("info").executes(ClaimCommands::info))
                .then(Commands.literal("liste").requires(Mayor::isMayor).executes(ClaimCommands::list))
                .then(Commands.literal("retirer").requires(Mayor::isMayor)
                        .then(Commands.argument("id", IntegerArgumentType.integer(1)).executes(ClaimCommands::remove)))
                .then(Commands.argument("joueur", GameProfileArgument.gameProfile()).requires(Mayor::isMayor)
                        .executes(ClaimCommands::create)));
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer mayor = ctx.getSource().getPlayerOrException();
        Collection<GameProfile> profiles = GameProfileArgument.getGameProfiles(ctx, "joueur");
        if (profiles.size() != 1) {
            ctx.getSource().sendFailure(Component.literal("Indique un seul joueur."));
            return 0;
        }
        GameProfile owner = profiles.iterator().next();

        ClaimSelectionHandler.Selection selection = ClaimSelectionHandler.get(mayor);
        if (selection == null || !selection.isComplete()) {
            ctx.getSource().sendFailure(Component.literal(
                    "Sélectionne d'abord deux coins avec une pelle en or (clic droit) dans le monde de build."));
            return 0;
        }
        if (mayor.level().dimension() != WorldBuildMod.WORLD_BUILD_LEVEL) {
            ctx.getSource().sendFailure(Component.literal("Les claims se créent depuis le monde de build."));
            return 0;
        }

        ClaimData data = ClaimData.get(ctx.getSource().getServer());
        Optional<Claim> created = data.add(owner.getId(), owner.getName(),
                selection.first(), selection.second(), System.currentTimeMillis());
        if (created.isEmpty()) {
            Claim overlap = data.firstOverlap(Claim.of(0, owner.getId(), "", selection.first(), selection.second(), 0));
            ctx.getSource().sendFailure(Component.literal("Cette zone chevauche le claim #" + overlap.id()
                    + " de " + overlap.ownerName() + " " + overlap.describeBounds() + "."));
            return 0;
        }
        ClaimSelectionHandler.clear(mayor);

        Claim claim = created.get();
        ctx.getSource().sendSuccess(() -> Component.literal("Claim #" + claim.id() + " attribué à " + owner.getName()
                + " : " + claim.describeBounds() + ", " + claim.area() + " blocs, "
                + Coins.format(claim.weeklyTax()) + " / semaine.").withStyle(ChatFormatting.GREEN), true);

        ServerPlayer ownerPlayer = ctx.getSource().getServer().getPlayerList().getPlayer(owner.getId());
        if (ownerPlayer != null && ownerPlayer != mayor) {
            ownerPlayer.sendSystemMessage(Component.literal("Le maire t'a attribué un terrain " + claim.describeBounds()
                    + " dans le monde de build (" + claim.area() + " blocs). Taxe : "
                    + Coins.format(claim.weeklyTax()) + " par semaine, à payer au Professeur Chen.")
                    .withStyle(ChatFormatting.GREEN));
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        ClaimData data = ClaimData.get(ctx.getSource().getServer());
        List<Claim> claims = data.claims();
        if (claims.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.literal("Aucun claim."), false);
            return 0;
        }
        for (Claim claim : claims) {
            ctx.getSource().sendSuccess(() -> Component.literal("#" + claim.id() + " " + claim.ownerName() + " "
                    + claim.describeBounds() + " - " + claim.area() + " blocs - doit "
                    + Coins.format(data.debt(claim.owner()))), false);
        }
        return claims.size();
    }

    private static int remove(CommandContext<CommandSourceStack> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Claim removed = ClaimData.get(ctx.getSource().getServer()).remove(id);
        if (removed == null) {
            ctx.getSource().sendFailure(Component.literal("Aucun claim #" + id + "."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Claim #" + id + " de " + removed.ownerName()
                + " supprimé. Ses impôts déjà dus restent à payer.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ClaimData data = ClaimData.get(ctx.getSource().getServer());

        if (player.level().dimension() == WorldBuildMod.WORLD_BUILD_LEVEL) {
            Optional<Claim> here = data.claimAt(player.blockPosition());
            ctx.getSource().sendSuccess(() -> Component.literal(here
                    .map(c -> "Ici : claim #" + c.id() + " de " + c.ownerName() + " " + c.describeBounds() + ".")
                    .orElse("Ici : terrain de la mairie (non attribué).")), false);
        }
        long weekly = data.weeklyTax(player.getUUID());
        long debt = data.debt(player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("Ta taxe : " + Coins.format(weekly) + " / semaine. "
                + (debt > 0 ? "Tu dois " + Coins.format(debt) + "." : debt < 0
                ? "Tu as " + Coins.format(-debt) + " d'avance." : "Tu ne dois rien.")), false);
        return 1;
    }
}
