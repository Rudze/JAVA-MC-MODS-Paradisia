package com.rudze.bounties;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.types.ElementalType;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
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

    /** e.g. "Pikachu", "Pokémon Feu chromatique", "Pokémon légendaire". */
    public MutableComponent describe() {
        MutableComponent text;
        Species s = species == null ? null : PokemonSpecies.getByName(species);
        if (s != null) {
            text = s.getTranslatedName().copy();
        } else if (species != null) {
            text = Component.literal(Character.toUpperCase(species.charAt(0)) + species.substring(1));
        } else {
            text = Component.literal("Pokémon");
        }
        List<Component> qualifiers = new ArrayList<>();
        if (type != null) {
            ElementalType t = ElementalTypes.get(type);
            qualifiers.add(t != null ? t.getDisplayName() : Component.literal(type));
        }
        if (label != null) {
            qualifiers.add(Component.literal(switch (label) {
                case "legendary" -> "légendaire";
                case "mythical" -> "fabuleux";
                case "ultra_beast" -> "Ultra-Chimère";
                case "paradox" -> "Paradoxe";
                default -> label;
            }));
        }
        if (shiny) {
            qualifiers.add(Component.literal("chromatique"));
        }
        for (Component qualifier : qualifiers) {
            text.append(" ").append(qualifier);
        }
        return text;
    }
}
