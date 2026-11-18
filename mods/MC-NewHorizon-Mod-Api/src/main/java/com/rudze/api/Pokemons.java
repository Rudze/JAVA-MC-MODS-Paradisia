package com.rudze.api;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokedex.PokedexManager;
import com.cobblemon.mod.common.api.pokedex.SpeciesDexRecord;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.player.PlayerInstancedDataStoreTypes;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCBox;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.PokemonStats;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Cobblemon party and PC of a player, online or not. Must run on the server thread. */
final class Pokemons {

    private static final List<Stats> STATS = List.of(Stats.HP, Stats.ATTACK, Stats.DEFENCE,
            Stats.SPECIAL_ATTACK, Stats.SPECIAL_DEFENCE, Stats.SPEED);

    private Pokemons() {
    }

    static JsonObject of(MinecraftServer server, UUID uuid) {
        var storage = Cobblemon.INSTANCE.getStorage();
        PlayerPartyStore party = storage.getParty(uuid, server.registryAccess());
        PCStore pc = storage.getPC(uuid, server.registryAccess());

        JsonArray partyJson = new JsonArray();
        List<Pokemon> slots = party.toGappyList();
        for (int slot = 0; slot < slots.size(); slot++) {
            if (slots.get(slot) != null) {
                JsonObject json = pokemon(slots.get(slot));
                json.addProperty("slot", slot);
                partyJson.add(json);
            }
        }

        JsonArray boxes = new JsonArray();
        int count = 0;
        List<PCBox> pcBoxes = pc.getBoxes();
        for (int index = 0; index < pcBoxes.size(); index++) {
            PCBox box = pcBoxes.get(index);
            JsonArray content = new JsonArray();
            for (Map.Entry<Integer, Pokemon> entry : box.getNonEmptySlots().entrySet()) {
                JsonObject json = pokemon(entry.getValue());
                json.addProperty("slot", entry.getKey());
                content.add(json);
                count++;
            }
            JsonObject boxJson = new JsonObject();
            boxJson.addProperty("index", index);
            boxJson.addProperty("name", box.getName());
            boxJson.add("pokemon", content);
            boxes.add(boxJson);
        }
        JsonObject pcJson = new JsonObject();
        pcJson.addProperty("count", count);
        pcJson.add("boxes", boxes);

        JsonObject json = new JsonObject();
        json.add("party", partyJson);
        json.add("pc", pcJson);
        return json;
    }

    /** How many species the player has seen and caught. */
    static JsonObject pokedex(UUID uuid) {
        int seen = 0;
        int caught = 0;
        var data = Cobblemon.INSTANCE.getPlayerDataManager().get(uuid, PlayerInstancedDataStoreTypes.INSTANCE.getPOKEDEX());
        if (data instanceof PokedexManager dex) {
            for (SpeciesDexRecord record : dex.getSpeciesRecords().values()) {
                PokedexEntryProgress knowledge = record.getKnowledge();
                if (knowledge == PokedexEntryProgress.OWNED) {
                    caught++;
                    seen++;
                } else if (knowledge == PokedexEntryProgress.SEEN) {
                    seen++;
                }
            }
        }
        JsonObject json = new JsonObject();
        json.addProperty("seen", seen);
        json.addProperty("caught", caught);
        json.addProperty("total", PokemonSpecies.getImplemented().size());
        return json;
    }

    private static JsonObject pokemon(Pokemon pokemon) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", pokemon.getUuid().toString());
        json.addProperty("species", pokemon.getSpecies().getName());
        json.addProperty("speciesId", pokemon.getSpecies().getResourceIdentifier().toString());
        json.addProperty("dexNumber", pokemon.getSpecies().getNationalPokedexNumber());
        json.addProperty("form", pokemon.getForm().getName());
        json.addProperty("nickname", pokemon.getNickname() == null ? null : pokemon.getNickname().getString());
        json.addProperty("level", pokemon.getLevel());
        json.addProperty("experience", pokemon.getExperience());
        json.addProperty("shiny", pokemon.getShiny());
        json.addProperty("gender", pokemon.getGender().name().toLowerCase());
        json.addProperty("nature", pokemon.getNature().getName().getPath());
        json.addProperty("ability", pokemon.getAbility().getName());
        json.addProperty("friendship", pokemon.getFriendship());
        json.addProperty("ball", pokemon.getCaughtBall().getName().toString());
        ItemStack held = pokemon.heldItem();
        json.addProperty("heldItem", held.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(held.getItem()).toString());
        json.addProperty("originalTrainer", pokemon.getOriginalTrainerName());

        JsonObject hp = new JsonObject();
        hp.addProperty("current", pokemon.getCurrentHealth());
        hp.addProperty("max", pokemon.getMaxHealth());
        hp.addProperty("fainted", pokemon.isFainted());
        json.add("health", hp);
        json.add("ivs", stats(pokemon.getIvs()));
        json.add("evs", stats(pokemon.getEvs()));

        JsonArray moves = new JsonArray();
        for (Move move : pokemon.getMoveSet().getMoves()) {
            JsonObject moveJson = new JsonObject();
            moveJson.addProperty("name", move.getName());
            moveJson.addProperty("pp", move.getCurrentPp());
            moveJson.addProperty("maxPp", move.getMaxPp());
            moves.add(moveJson);
        }
        json.add("moves", moves);

        JsonArray aspects = new JsonArray();
        pokemon.getAspects().forEach(aspects::add);
        json.add("aspects", aspects);
        return json;
    }

    private static JsonObject stats(PokemonStats values) {
        JsonObject json = new JsonObject();
        for (Stats stat : STATS) {
            json.addProperty(stat.getShowdownId(), values.getOrDefault(stat));
        }
        return json;
    }
}
