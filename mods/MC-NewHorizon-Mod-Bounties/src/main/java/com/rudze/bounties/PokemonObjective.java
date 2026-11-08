package com.rudze.bounties;

import com.cobblemon.mod.common.pokemon.Pokemon;
import io.ejekta.bountiful.bounty.types.IBountyObjective;
import io.ejekta.bountiful.bounty.types.Progress;
import io.ejekta.bountiful.components.BountyDataEntry;
import io.ejekta.bountiful.data.PoolEntry;
import io.ejekta.bountiful.util.ExtMiscKt;
import kotlin.Pair;
import kotlin.Unit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * A Bountiful objective counted from Cobblemon events rather than from items handed in: the
 * pool entry's {@code content} is a {@link PokemonSelector} and its {@code amount} how many times
 * the action must happen. Progress lives on the bounty item, like Bountiful's own kill objectives.
 *
 * <p>Bountiful is written in Kotlin, so the interface defaults live in {@code DefaultImpls} and
 * every method has to be spelled out here.
 */
public final class PokemonObjective implements IBountyObjective {

    private final ResourceLocation id;
    private final String audience;

    /**
     * @param audience whose Pokémon the objective is about, see {@link PokemonSelector#describe}
     */
    public PokemonObjective(String path, String audience) {
        this.id = ResourceLocation.fromNamespaceAndPath(BountiesMod.MODID, path);
        this.audience = audience;
    }

    /** Counts {@code times} actions on {@code pokemon} towards every matching bounty the player carries. */
    public void advance(ServerPlayer player, Pokemon pokemon, int times) {
        ExtMiscKt.iterateBountyStacks(player, stack -> {
            boolean changed = false;
            for (BountyDataEntry entry : stack.getObjs()) {
                if (!id.equals(entry.getLogic().getId())) {
                    continue;
                }
                PokemonSelector selector = PokemonSelector.parse(entry.getContent());
                if (selector == null || !selector.matches(pokemon)) {
                    continue;
                }
                for (int i = 0; i < times && stack.progressOf(entry) < entry.getAmount(); i++) {
                    stack.advance(entry);
                    changed = true;
                }
            }
            if (changed) {
                stack.checkForCompletionAndAlert(player);
            }
            return Unit.INSTANCE;
        });
    }

    private MutableComponent describe(BountyDataEntry entry) {
        if (entry.getName() != null) {
            return Component.literal(entry.getName());
        }
        PokemonSelector selector = PokemonSelector.parse(entry.getContent());
        return Component.translatable("nh_bounties.objective." + id.getPath(),
                selector != null ? selector.describe(audience) : Component.literal(entry.getContent()));
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public boolean isValid(PoolEntry entry, MinecraftServer server) {
        PokemonSelector selector = PokemonSelector.parse(entry.getContent());
        if (selector == null || !selector.isValid()) {
            BountiesMod.LOGGER.warn("Invalid Pokémon selector '{}' in bounty entry {}", entry.getContent(), entry.getId());
            return false;
        }
        return true;
    }

    @Override
    public List<MutableComponent> textOnBounty(BountyDataEntry entry, boolean isServer, Player player, int current) {
        Progress progress = getProgress(entry, player, current);
        return List.of(colored(describe(entry), progress.getColor())
                .append(colored(progress.getNeededText(), progress.getColor())));
    }

    @Override
    public List<Component> textOnBoardSidebar(BountyDataEntry entry, Player player) {
        return List.of(describe(entry).append(" x" + entry.getAmount()));
    }

    @Override
    public Progress getProgress(BountyDataEntry entry, Player player, int current) {
        return IBountyObjective.DefaultImpls.getProgress(this, entry, player, current);
    }

    /** Nothing to take from the inventory: the work is already done. */
    @Override
    public boolean consumeObjectives(BountyDataEntry entry, Player player, int current) {
        return IBountyObjective.DefaultImpls.consumeObjectives(this, entry, player, current);
    }

    @Override
    public int getNewCurrent(BountyDataEntry entry, Player player, int current) {
        return IBountyObjective.DefaultImpls.getNewCurrent(this, entry, player, current);
    }

    @Override
    public MutableComponent getDescription(BountyDataEntry entry) {
        return describe(entry);
    }

    @Override
    public boolean isDone(Pair<Integer, Integer> progress) {
        return IBountyObjective.DefaultImpls.isDone(this, progress);
    }

    @Override
    public ChatFormatting getColor(Pair<Integer, Integer> progress) {
        return IBountyObjective.DefaultImpls.getColor(this, progress);
    }

    @Override
    public MutableComponent colored(Component text, Pair<Integer, Integer> progress) {
        return IBountyObjective.DefaultImpls.colored(this, text, progress);
    }

    @Override
    public MutableComponent colored(Component text, ChatFormatting color) {
        return IBountyObjective.DefaultImpls.colored(this, text, color);
    }

    @Override
    public MutableComponent getNeeded(Pair<Integer, Integer> progress) {
        return IBountyObjective.DefaultImpls.getNeeded(this, progress);
    }

    @Override
    public MutableComponent getGiving(Pair<Integer, Integer> progress) {
        return IBountyObjective.DefaultImpls.getGiving(this, progress);
    }
}
