package com.rudze.quests.community;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.rudze.quests.QuestsMod;
import net.neoforged.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The server-wide quests Tom hands out, read from {@code config/nh_quests/community_quests.json}
 * (seeded from the copy bundled in the jar). Quests run in file order: the first one not marked
 * {@code "completed": true} is the current one, so the mayor moves the town on by flipping that
 * flag. The file is re-read on every conversation, so no restart is needed.
 */
public final class CommunityQuests {

    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve(QuestsMod.MODID).resolve("community_quests.json");
    private static final String DEFAULT = "/" + QuestsMod.MODID + "/community_quests.json";
    private static final Gson GSON = new Gson();

    /** One quest as written in the JSON; every page is a list of up to two lines. */
    public static final class Quest {
        public String id = "";
        public String title = "";
        public boolean completed;
        public List<List<String>> story = List.of();
        public List<List<String>> objective = List.of();
        /** One line recalling the achievement, shown once the quest is completed. */
        @Nullable
        public String completedText;
    }

    private static final class QuestFile {
        List<Quest> quests = List.of();
    }

    private CommunityQuests() {
    }

    /** @return the quests in order, or empty if the file can't be read (the error is logged) */
    public static Optional<List<Quest>> load() {
        try {
            if (Files.notExists(FILE)) {
                Files.createDirectories(FILE.getParent());
                try (InputStream in = CommunityQuests.class.getResourceAsStream(DEFAULT)) {
                    if (in == null) {
                        throw new IOException(DEFAULT + " is missing from the jar");
                    }
                    Files.copy(in, FILE);
                }
            }
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                QuestFile file = GSON.fromJson(reader, QuestFile.class);
                return Optional.of(file == null || file.quests == null ? List.of() : file.quests);
            }
        } catch (IOException | JsonParseException e) {
            QuestsMod.LOGGER.error("Couldn't read the community quests from {}", FILE, e);
            return Optional.empty();
        }
    }

    @Nullable
    public static Quest current(List<Quest> quests) {
        return quests.stream().filter(q -> !q.completed).findFirst().orElse(null);
    }

    public static List<Quest> completed(List<Quest> quests) {
        return quests.stream().filter(q -> q.completed).toList();
    }
}
