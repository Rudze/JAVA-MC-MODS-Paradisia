package com.rudze.pokemononly;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;

/**
 * Legendary, Mythical, Ultra Beast and Paradox Pokémon never appear on their own: Cobblemon's
 * spawner (wild spawns, fishing, Poké Snacks) is refused them. Spawning one on purpose, with a
 * command or from code, doesn't go through the spawner and still works.
 */
public final class SpecialPokemonFilter {

    private SpecialPokemonFilter() {
    }

    public static void register() {
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event -> {
            if (isSpecial(event.getEntity().getPokemon())) {
                event.cancel();
            }
        });
    }

    private static boolean isSpecial(Pokemon pokemon) {
        return pokemon.isLegendary() || pokemon.isMythical() || pokemon.isUltraBeast()
                || pokemon.getSpecies().getLabels().contains("paradox");
    }
}
