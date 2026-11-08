package com.rudze.bounties;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Which Pokémon a bounty objective counts, written as the pool entry's {@code content}: terms
 * separated by commas, all of which must match.
 *
 * <ul>
 *   <li>{@code any}: every Pokémon</li>
 *   <li>{@code species:pikachu}: that species (Cobblemon's id, namespace optional)</li>
 *   <li>{@code type:fire}: Pokémon having that type</li>
 *   <li>{@code label:legendary}: species carrying that Cobblemon label (legendary, mythical...)</li>
 *   <li>{@code shiny}: shiny Pokémon only</li>
 * </ul>
 */
public record PokemonSelector(@Nullable String species, @Nullable String type, @Nullable String label, boolean shiny) {

    /** @return null if the content has an unknown term */
    @Nullable
    public static PokemonSelector parse(String content) {
        String species = null;
        String type = null;
        String label = null;
        boolean shiny = false;
        for (String raw : content.split(",")) {
            String term = raw.trim().toLowerCase(Locale.ROOT);
            if (term.equals("any") || term.isEmpty()) {
                continue;
            } else if (term.equals("shiny")) {
                shiny = true;
            } else if (term.startsWith("species:")) {
                species = term.substring("species:".length());
                if (species.contains(":")) {
                    species = species.substring(species.indexOf(':') + 1);
                }
            } else if (term.startsWith("type:")) {
                type = term.substring("type:".length());
            } else if (term.startsWith("label:")) {
                label = term.substring("label:".length());
            } else {
                return null;
            }
        }
        return new PokemonSelector(species, type, label, shiny);
    }

    /** Server-side check that the species and type exist, so typos never reach a board. */
    public boolean isValid() {
        return (species == null || PokemonSpecies.getByName(species) != null)
                && (type == null || ElementalTypes.get(type) != null);
    }

    public boolean matches(Pokemon pokemon) {
        if (shiny && !pokemon.getShiny()) {
            return false;
        }
        Species s = pokemon.getSpecies();
        if (species != null && !s.getName().toLowerCase(Locale.ROOT).equals(species)
                && !s.showdownId().equals(species)) {
            return false;
        }
        if (label != null && !s.getLabels().contains(label)) {
            return false;
        }
        if (type != null) {
            for (ElementalType t : pokemon.getTypes()) {
                if (t.getName().equalsIgnoreCase(type)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * What the objective is about, worded for {@code audience}: {@code any} ("des Pokémon"),
     * {@code wild} ("des Pokémon sauvages") or {@code own} ("tes Pokémon"). Translated client-side.
     */
    public MutableComponent describe(String audience) {
        if (species != null) {
            Species s = PokemonSpecies.getByName(species);
            MutableComponent name = s != null ? s.getTranslatedName().copy()
                    : Component.literal(Character.toUpperCase(species.charAt(0)) + species.substring(1));
            return shiny ? Component.translatable("nh_bounties.target.species_shiny", name) : name;
        }
        if (shiny) {
            return Component.translatable("nh_bounties.target.shiny");
        }
        if (label != null) {
            return Component.translatableWithFallback("nh_bounties.target.label." + label, label);
        }
        if (type != null) {
            ElementalType t = ElementalTypes.get(type);
            return Component.translatable("nh_bounties.target." + audience + "_type",
                    t != null ? t.getDisplayName() : Component.literal(type));
        }
        return Component.translatable("nh_bounties.target." + audience);
    }
}
