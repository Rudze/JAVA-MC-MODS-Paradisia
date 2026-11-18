package com.rudze.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/** Builds the JSON documents of the API. Every method must run on the server thread. */
final class PlayerData {

    /** Numismatic Overhaul keeps a player's purse as a Long attachment, in bronze coins. */
    private static final ResourceLocation PURSE = ResourceLocation.fromNamespaceAndPath("numismaticoverhaul", "value");
    private static final long SILVER = 100;
    private static final long GOLD = 10_000;

    private final MinecraftServer server;

    PlayerData(MinecraftServer server) {
        this.server = server;
    }

    JsonObject health() {
        JsonObject json = new JsonObject();
        json.addProperty("status", "ok");
        json.addProperty("minecraftVersion", SharedConstants.getCurrentVersion().getName());
        json.addProperty("onlinePlayers", server.getPlayerCount());
        json.addProperty("maxPlayers", server.getMaxPlayers());
        json.addProperty("motd", server.getMotd());
        return json;
    }

    /** Everyone who has a save file, that is everyone who ever joined. */
    JsonObject listPlayers() {
        JsonArray array = new JsonArray();
        for (UUID uuid : knownPlayers()) {
            array.add(summary(uuid));
        }
        JsonObject json = new JsonObject();
        json.addProperty("count", array.size());
        json.add("players", array);
        return json;
    }

    JsonObject player(UUID uuid) {
        JsonObject json = summary(uuid);
        json.add("economy", economy(uuid));
        JsonObject counts = new JsonObject();
        JsonObject pokemon = Pokemons.of(server, uuid);
        counts.addProperty("party", pokemon.getAsJsonArray("party").size());
        counts.addProperty("pc", pokemon.getAsJsonObject("pc").get("count").getAsInt());
        json.add("pokemonCounts", counts);
        json.add("status", PlayerStatus.of(server, uuid, playerDataDir().resolve(uuid + ".dat")));
        json.add("stats", PlayerStatus.stats(server, uuid));
        json.add("pokedex", Pokemons.pokedex(uuid));
        return json;
    }

    JsonObject pokemon(UUID uuid) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("name", name(uuid));
        json.add("pokemon", Pokemons.of(server, uuid));
        return json;
    }

    /** Accepts a UUID (with or without dashes) or a player name. */
    Optional<UUID> resolve(String who) {
        UUID uuid = null;
        try {
            String dashed = who.length() == 32
                    ? who.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5") : who;
            uuid = UUID.fromString(dashed);
        } catch (IllegalArgumentException notAUuid) {
            ServerPlayer online = server.getPlayerList().getPlayerByName(who);
            if (online != null) {
                uuid = online.getUUID();
            } else if (server.getProfileCache() != null) {
                uuid = server.getProfileCache().get(who).map(GameProfile::getId).orElse(null);
            }
        }
        return uuid != null && knownPlayers().contains(uuid) ? Optional.of(uuid) : Optional.empty();
    }

    private JsonObject summary(UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        JsonObject json = new JsonObject();
        json.addProperty("uuid", uuid.toString());
        json.addProperty("name", name(uuid));
        json.addProperty("online", online != null);
        Path save = playerDataDir().resolve(uuid + ".dat");
        try {
            json.addProperty("lastSeen", online != null ? System.currentTimeMillis()
                    : Files.getLastModifiedTime(save).toMillis());
        } catch (IOException e) {
            json.add("lastSeen", null);
        }
        long playTicks = -1;
        long deaths = -1;
        if (online != null) {
            var stats = server.getPlayerList().getPlayerStats(online);
            playTicks = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
            deaths = stats.getValue(Stats.CUSTOM.get(Stats.DEATHS));
        } else {
            Path file = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(uuid + ".json");
            try {
                JsonObject custom = JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                        .getAsJsonObject("stats").getAsJsonObject("minecraft:custom");
                if (custom != null) {
                    playTicks = custom.has("minecraft:play_time") ? custom.get("minecraft:play_time").getAsLong() : 0;
                    deaths = custom.has("minecraft:deaths") ? custom.get("minecraft:deaths").getAsLong() : 0;
                }
            } catch (Exception ignored) {
                // No stats yet.
            }
        }
        json.addProperty("playTimeSeconds", playTicks < 0 ? null : playTicks / 20);
        json.addProperty("deaths", deaths < 0 ? null : deaths);
        return json;
    }

    private JsonObject economy(UUID uuid) {
        long bronze = purse(uuid);
        JsonObject json = new JsonObject();
        json.addProperty("total", bronze);
        json.addProperty("gold", bronze / GOLD);
        json.addProperty("silver", bronze % GOLD / SILVER);
        json.addProperty("bronze", bronze % SILVER);
        return json;
    }

    @SuppressWarnings("unchecked")
    private long purse(UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            AttachmentType<?> type = NeoForgeRegistries.ATTACHMENT_TYPES.get(PURSE);
            if (type != null && online.getData(type) instanceof Long value) {
                return value;
            }
            return 0;
        }
        try {
            CompoundTag tag = NbtIo.readCompressed(playerDataDir().resolve(uuid + ".dat"), NbtAccounter.unlimitedHeap());
            CompoundTag attachments = tag.getCompound("neoforge:attachments");
            return attachments.contains(PURSE.toString(), Tag.TAG_ANY_NUMERIC) ? attachments.getLong(PURSE.toString()) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    private String name(UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getGameProfile().getName();
        }
        if (server.getProfileCache() != null) {
            return server.getProfileCache().get(uuid).map(GameProfile::getName).orElse(null);
        }
        return null;
    }

    private Path playerDataDir() {
        return server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
    }

    private List<UUID> knownPlayers() {
        List<UUID> uuids = new ArrayList<>();
        try (Stream<Path> files = Files.list(playerDataDir())) {
            files.map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".dat"))
                    .forEach(name -> {
                        try {
                            uuids.add(UUID.fromString(name.substring(0, name.length() - 4)));
                        } catch (IllegalArgumentException ignored) {
                            // Not a player file.
                        }
                    });
        } catch (IOException e) {
            ApiMod.LOGGER.warn("Could not list player files", e);
        }
        return uuids;
    }
}
