package com.rudze.bounties;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.ejekta.bountiful.content.BountifulContent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Caps how many bounties each player may take from boards per day, read from
 * {@code config/nh_bounties.json} (re-read every minute at most, so edits apply without a restart).
 *
 * <p>Bountiful already counts every bounty taken in a player statistic, so this only remembers
 * that statistic's value when the player's day started: taken today = now - baseline.
 */
public final class DailyLimit {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve(BountiesMod.MODID + ".json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long RELOAD_EVERY_MS = 60_000;

    /** Contents of the JSON file; the field names are what the mayor edits. */
    static final class Settings {
        String _aide = "maxBountiesPerDay : nombre de quêtes qu'un joueur peut prendre sur les tableaux par jour (0 = illimité). "
                + "timezone : fuseau horaire du changement de jour (minuit).";
        int maxBountiesPerDay = 3;
        String timezone = "Europe/Paris";
    }

    private record Day(long epochDay, int baseline) {
        static final Codec<Day> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("epochDay").forGetter(Day::epochDay),
                Codec.INT.fieldOf("baseline").forGetter(Day::baseline)
        ).apply(i, Day::new));
    }

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, BountiesMod.MODID);

    private static final DeferredHolder<AttachmentType<?>, AttachmentType<Day>> DAY =
            ATTACHMENT_TYPES.register("bounty_day",
                    () -> AttachmentType.builder(() -> new Day(-1, 0)).serialize(Day.CODEC).copyOnDeath().build());

    private static Settings settings = new Settings();
    private static long loadedAt;

    private DailyLimit() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }

    /**
     * @return whether the player may take one more bounty today; tells them why not otherwise
     */
    public static boolean allow(ServerPlayer player) {
        Settings s = settings();
        if (s.maxBountiesPerDay <= 0) {
            return true;
        }
        int taken = takenToday(player, s);
        if (taken < s.maxBountiesPerDay) {
            return true;
        }
        player.displayClientMessage(Component.literal("Tu as déjà pris " + s.maxBountiesPerDay
                + " quêtes aujourd'hui. Reviens demain !").withStyle(ChatFormatting.RED), true);
        return false;
    }

    private static int takenToday(ServerPlayer player, Settings s) {
        int total = player.getStats().getValue(BountifulContent.CustomStats.INSTANCE.getBOUNTIES_TAKEN());
        long today = LocalDate.now(zone(s)).toEpochDay();
        Day day = player.getData(DAY);
        if (day.epochDay() != today) {
            day = new Day(today, total);
            player.setData(DAY, day);
        }
        return total - day.baseline();
    }

    private static ZoneId zone(Settings s) {
        try {
            return ZoneId.of(s.timezone);
        } catch (DateTimeException e) {
            return ZoneId.of("Europe/Paris");
        }
    }

    /** Loads the file, writing the defaults first if it doesn't exist yet. */
    static synchronized Settings settings() {
        long now = System.currentTimeMillis();
        if (now - loadedAt < RELOAD_EVERY_MS) {
            return settings;
        }
        loadedAt = now;
        try {
            if (Files.notExists(FILE)) {
                try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                    GSON.toJson(new Settings(), writer);
                }
            }
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                Settings loaded = GSON.fromJson(reader, Settings.class);
                settings = loaded != null ? loaded : new Settings();
            }
        } catch (IOException | JsonParseException e) {
            BountiesMod.LOGGER.error("Couldn't read {}, keeping the previous settings", FILE, e);
        }
        return settings;
    }
}
