package com.rudze.pokemononly;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A Pokémon-only world: see {@link MobFilter} for which mobs are allowed, and
 * {@link SpecialPokemonFilter} for the Pokémon that never spawn on their own.
 */
@Mod(PokemonOnlyMod.MODID)
public final class PokemonOnlyMod {

    public static final String MODID = "nh_pokemononly";

    public PokemonOnlyMod() {
        NeoForge.EVENT_BUS.register(new MobFilter());
        SpecialPokemonFilter.register();
    }
}
