package com.rudze.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/** Health, food, armour and statistics of a player. Must run on the server thread. */
final class PlayerStatus {

    /** Armour pieces, by name in the JSON and by their slot number in the player's save file. */
    private static final Map<String, EquipmentSlot> ARMOR = Map.of(
            "head", EquipmentSlot.HEAD, "chest", EquipmentSlot.CHEST,
            "legs", EquipmentSlot.LEGS, "feet", EquipmentSlot.FEET);

    private PlayerStatus() {
    }

    /** Live values for an online player, the last saved ones otherwise. */
    static JsonObject of(MinecraftServer server, UUID uuid, Path saveFile) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        return player != null ? online(player) : offline(server, saveFile);
    }

    private static JsonObject online(ServerPlayer player) {
        JsonObject json = new JsonObject();
        json.addProperty("health", player.getHealth());
        json.addProperty("maxHealth", player.getMaxHealth());
        json.addProperty("armor", player.getArmorValue());
        json.addProperty("food", player.getFoodData().getFoodLevel());
        json.addProperty("saturation", player.getFoodData().getSaturationLevel());
        json.addProperty("xpLevel", player.experienceLevel);
        json.addProperty("dimension", player.level().dimension().location().toString());
        json.addProperty("gameMode", player.gameMode.getGameModeForPlayer().getName());
        JsonObject armor = new JsonObject();
        ARMOR.forEach((name, slot) -> armor.add(name, item(player.getItemBySlot(slot))));
        json.add("armorItems", armor);
        return json;
    }

    private static JsonObject offline(MinecraftServer server, Path saveFile) {
        JsonObject json = new JsonObject();
        CompoundTag tag;
        try {
            tag = NbtIo.readCompressed(saveFile, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            return json;
        }
        json.addProperty("health", tag.getFloat("Health"));
        json.addProperty("maxHealth", maxHealth(tag));
        json.addProperty("food", tag.getInt("foodLevel"));
        json.addProperty("saturation", tag.getFloat("foodSaturationLevel"));
        json.addProperty("xpLevel", tag.getInt("XpLevel"));
        json.addProperty("dimension", tag.getString("Dimension"));
        json.addProperty("gameMode", GameType.byId(tag.getInt("playerGameType")).getName());

        // Armour is stored in the inventory list, in slots 100 (feet) to 103 (head).
        int points = 0;
        JsonObject armor = new JsonObject();
        ListTag inventory = tag.getList("Inventory", Tag.TAG_COMPOUND);
        for (Map.Entry<String, EquipmentSlot> entry : ARMOR.entrySet()) {
            int slot = 100 + entry.getValue().getIndex();
            ItemStack stack = ItemStack.EMPTY;
            for (int i = 0; i < inventory.size(); i++) {
                CompoundTag item = inventory.getCompound(i);
                if ((item.getByte("Slot") & 0xFF) == slot) {
                    stack = ItemStack.parseOptional(server.registryAccess(), item);
                }
            }
            if (stack.getItem() instanceof ArmorItem armorItem) {
                points += armorItem.getDefense();
            }
            armor.add(entry.getKey(), item(stack));
        }
        json.addProperty("armor", points);
        json.add("armorItems", armor);
        return json;
    }

    private static double maxHealth(CompoundTag tag) {
        ListTag attributes = tag.getList("attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag attribute = attributes.getCompound(i);
            if (attribute.getString("id").equals("minecraft:generic.max_health")) {
                return attribute.getDouble("base");
            }
        }
        return 20;
    }

    private static JsonElement item(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        json.addProperty("name", stack.getHoverName().getString());
        return json;
    }

    /** A selection of the vanilla statistics, read from the player's stats file. */
    static JsonObject stats(MinecraftServer server, UUID uuid) {
        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            // Writes the live counters to the file, so online and offline players are read the same way.
            server.getPlayerList().getPlayerStats(player).save();
        }
        JsonObject all = new JsonObject();
        try {
            Path file = server.getWorldPath(LevelResource.PLAYER_STATS_DIR).resolve(uuid + ".json");
            all = JsonParser.parseString(Files.readString(file)).getAsJsonObject().getAsJsonObject("stats");
        } catch (Exception ignored) {
            // No stats yet: everything stays at 0.
        }
        JsonObject custom = all.has("minecraft:custom") ? all.getAsJsonObject("minecraft:custom") : new JsonObject();

        long distanceCm = 0;
        for (String key : custom.keySet()) {
            if (key.endsWith("_one_cm")) {
                distanceCm += custom.get(key).getAsLong();
            }
        }
        JsonObject json = new JsonObject();
        json.addProperty("playTimeSeconds", get(custom, "play_time") / 20);
        json.addProperty("timeSinceDeathSeconds", get(custom, "time_since_death") / 20);
        json.addProperty("deaths", get(custom, "deaths"));
        json.addProperty("mobKills", get(custom, "mob_kills"));
        json.addProperty("playerKills", get(custom, "player_kills"));
        json.addProperty("damageDealt", get(custom, "damage_dealt") / 10);
        json.addProperty("damageTaken", get(custom, "damage_taken") / 10);
        json.addProperty("distanceKm", Math.round(distanceCm / 1000.0) / 100.0);
        json.addProperty("jumps", get(custom, "jump"));
        json.addProperty("fishCaught", get(custom, "fish_caught"));
        json.addProperty("villagerTrades", get(custom, "traded_with_villager"));
        json.addProperty("timesSlept", get(custom, "sleep_in_bed"));
        json.addProperty("sessions", get(custom, "leave_game"));
        json.addProperty("blocksMined", sum(all, "minecraft:mined"));
        json.addProperty("itemsCrafted", sum(all, "minecraft:crafted"));
        json.addProperty("itemsUsed", sum(all, "minecraft:used"));
        return json;
    }

    private static long get(JsonObject custom, String key) {
        JsonElement value = custom.get("minecraft:" + key);
        return value == null ? 0 : value.getAsLong();
    }

    private static long sum(JsonObject all, String category) {
        if (!all.has(category)) {
            return 0;
        }
        long total = 0;
        for (Map.Entry<String, JsonElement> entry : all.getAsJsonObject(category).entrySet()) {
            total += entry.getValue().getAsLong();
        }
        return total;
    }
}
