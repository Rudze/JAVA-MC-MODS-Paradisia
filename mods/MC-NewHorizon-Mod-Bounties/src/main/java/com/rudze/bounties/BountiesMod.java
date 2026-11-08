package com.rudze.bounties;

import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.mojang.logging.LogUtils;
import io.ejekta.bountiful.bounty.types.BountyTypeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Adds Cobblemon objectives to Bountiful's boards and caps the bounties taken per day.
 *
 * <p>Objective types use the ids {@code nh_bounties:catch}, {@code evolve}, {@code defeat},
 * {@code level_up} and {@code heal} in bounty pool JSON.
 */
@Mod(BountiesMod.MODID)
public final class BountiesMod {

    public static final String MODID = "nh_bounties";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final PokemonObjective CATCH = new PokemonObjective("catch", "Capturer");
    public static final PokemonObjective EVOLVE = new PokemonObjective("evolve", "Faire évoluer");
    public static final PokemonObjective DEFEAT = new PokemonObjective("defeat", "Vaincre (sauvage)");
    public static final PokemonObjective LEVEL_UP = new PokemonObjective("level_up", "Niveaux gagnés");
    public static final PokemonObjective HEAL = new PokemonObjective("heal", "Soigner (objet)");

    public BountiesMod(IEventBus modEventBus, ModContainer modContainer) {
        // Bountiful's registry is its own, never frozen, and must know the types on both sides.
        for (PokemonObjective objective : new PokemonObjective[]{CATCH, EVOLVE, DEFEAT, LEVEL_UP, HEAL}) {
            Registry.register(BountyTypeRegistry.INSTANCE, objective.getId(), objective);
        }
        DailyLimit.register(modEventBus);
        NeoForge.EVENT_BUS.register(new BountyCommands());
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> {
            DailyLimit.settings();
            // Loads the board slot now so a broken daily-limit mixin fails at startup, not when
            // the first player opens a board.
            try {
                Class.forName("io.ejekta.bountiful.content.gui.BoardBountySlot");
            } catch (ClassNotFoundException e) {
                LOGGER.error("Bountiful's board slot is gone; the daily bounty limit is not enforced", e);
            }
        });
        subscribeCobblemonEvents();
    }

    private static void subscribeCobblemonEvents() {
        CobblemonEvents.POKEMON_CAPTURED.subscribe(event -> CATCH.advance(event.getPlayer(), event.getPokemon(), 1));

        CobblemonEvents.EVOLUTION_COMPLETE.subscribe(event -> {
            Pokemon pokemon = event.getSourcePokemon();
            ServerPlayer owner = pokemon.getOwnerPlayer();
            if (owner != null) {
                EVOLVE.advance(owner, pokemon, 1);
            }
        });

        CobblemonEvents.LEVEL_UP_EVENT.subscribe(event -> {
            ServerPlayer owner = event.getPokemon().getOwnerPlayer();
            int gained = event.getNewLevel() - event.getOldLevel();
            if (owner != null && gained > 0) {
                LEVEL_UP.advance(owner, event.getPokemon(), gained);
            }
        });

        // Only heals from items count: Cobblemon also reports the slow passive regeneration.
        CobblemonEvents.POKEMON_HEALED.subscribe(event -> {
            ServerPlayer owner = event.getPokemon().getOwnerPlayer();
            if (owner != null && event.getAmount() > 0 && event.getSource() instanceof Item) {
                HEAL.advance(owner, event.getPokemon(), 1);
            }
        });

        // Each wild Pokémon on the losing side counts for every player on the winning side;
        // a battle ended by a capture is already counted by CATCH.
        CobblemonEvents.BATTLE_VICTORY.subscribe(event -> {
            if (event.getWasWildCapture()) {
                return;
            }
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return;
            }
            for (BattleActor winner : event.getWinners()) {
                if (winner.getType() != ActorType.PLAYER) {
                    continue;
                }
                for (UUID uuid : winner.getPlayerUUIDs()) {
                    ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                    if (player == null) {
                        continue;
                    }
                    for (BattleActor loser : event.getLosers()) {
                        if (loser.getType() != ActorType.WILD) {
                            continue;
                        }
                        for (BattlePokemon defeated : loser.getPokemonList()) {
                            DEFEAT.advance(player, defeated.getOriginalPokemon(), 1);
                        }
                    }
                }
            }
        });
    }
}
