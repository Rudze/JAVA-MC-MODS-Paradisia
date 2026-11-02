package fr.rudze.cosmetics.cosmetic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads {@code nh_cosmetics_config/_index.json} (a flat list of ids) and, for each id,
 * {@code nh_cosmetics_config/<id>.json} off the classpath, turning them into {@link CosmeticDefinition}s.
 *
 * <p>These live as plain classpath resources (not under {@code assets}/{@code data}) so they can be
 * read here, during mod construction, before the game's resource/data pack managers exist yet -
 * registry entries have to be created that early. Adding a new cosmetic is: drop its model json +
 * texture + one small definition json here, and add its id to {@code _index.json}. No Java changes needed.
 */
public final class CosmeticDefinitionLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(CosmeticDefinitionLoader.class);
    private static final String BASE_PATH = "/nh_cosmetics_config/";

    private CosmeticDefinitionLoader() {
    }

    public static List<CosmeticDefinition> loadAll() {
        List<CosmeticDefinition> definitions = new ArrayList<>();
        JsonArray index = readJsonArray(BASE_PATH + "_index.json");
        if (index == null) {
            LOGGER.warn("[nh_cosmetics] No cosmetic index found at {}_index.json; no cosmetic items will be registered.", BASE_PATH);
            return definitions;
        }

        for (JsonElement idElement : index) {
            String id = idElement.getAsString();
            JsonObject json = readJsonObject(BASE_PATH + id + ".json");
            if (json == null) {
                LOGGER.error("[nh_cosmetics] Cosmetic '{}' is listed in _index.json but {}{}.json is missing; skipping.", id, BASE_PATH, id);
                continue;
            }
            try {
                definitions.add(parse(id, json));
            } catch (RuntimeException e) {
                LOGGER.error("[nh_cosmetics] Failed to parse cosmetic definition '{}'; skipping.", id, e);
            }
        }
        return definitions;
    }

    // Calibrated in-game against coral_wings (a "back" cosmetic with no config override at all) so
    // that dropping in a new HMCCosmetics "back" config needs no per-item tuning by default - only
    // override backScale/backOffset for a piece whose geometry is unusual enough that this doesn't
    // fit. Started from the standalone Backpack module's own tuned Y/Z; Y sign is inverted from
    // that reference (ModelPart local space is Y-down, opposite of world Y-up, so a *positive*
    // offset here actually pushes the render down), then lowered by ~20% of a standing player's
    // height (~1.8 blocks -> ~0.36 blocks of world-space drop -> 0.36 / backScale of local offset,
    // since translate runs in the already-scaled frame) on top of the first in-game calibration.
    // Z pulled in closer to the body from that same first pass, then further to 0 and briefly
    // slightly negative (a small step past the body anchor's own depth, into the body) chasing a
    // visible gap behind coral_wings specifically - but that negative step made every "back" piece
    // (not just coral_wings) sit too far into the body once compared across the whole set
    // (lotus_backpack, shark_tail, scuba_tank, fishing_backpack, coral_wings itself), so it's back to
    // 0: flush with the body anchor's own depth, no push either way.
    private static final double DEFAULT_BACK_SCALE = 0.8;
    private static final double DEFAULT_BACK_OFFSET_X = 0.0;
    private static final double DEFAULT_BACK_OFFSET_Y = 0.30;
    private static final double DEFAULT_BACK_OFFSET_Z = 0.0;

    private static CosmeticDefinition parse(String id, JsonObject json) {
        String displayName = json.has("name") ? json.get("name").getAsString() : id;
        String slot = json.has("slot") ? json.get("slot").getAsString() : "hat";
        boolean balloon = json.has("balloon") && json.get("balloon").getAsBoolean();

        // 0xFF000000 (opaque alpha) is required here: DyedItemColor.getOrDefault returns this value
        // as-is (unlike the actually-dyed branch, which vanilla always forces opaque via
        // ARGB.opaque()) - without it, every undyed cosmetic's tinted faces render alpha=0
        // (fully transparent), which looks like broken/missing texture on almost every piece,
        // since none of them start out dyed.
        int color = 0xFFFFFFFF;
        if (json.has("color")) {
            JsonArray colorArray = json.getAsJsonArray("color");
            int r = colorArray.get(0).getAsInt();
            int g = colorArray.get(1).getAsInt();
            int b = colorArray.get(2).getAsInt();
            color = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        double backScale = json.has("backScale") ? json.get("backScale").getAsDouble() : DEFAULT_BACK_SCALE;
        double backOffsetX = DEFAULT_BACK_OFFSET_X;
        double backOffsetY = DEFAULT_BACK_OFFSET_Y;
        double backOffsetZ = DEFAULT_BACK_OFFSET_Z;
        if (json.has("backOffset")) {
            JsonArray offset = json.getAsJsonArray("backOffset");
            backOffsetX = offset.get(0).getAsDouble();
            backOffsetY = offset.get(1).getAsDouble();
            backOffsetZ = offset.get(2).getAsDouble();
        }

        return new CosmeticDefinition(id, displayName, slot, balloon, color, backScale, backOffsetX, backOffsetY, backOffsetZ);
    }

    private static JsonObject readJsonObject(String path) {
        JsonElement element = readJson(path);
        return element == null ? null : element.getAsJsonObject();
    }

    private static JsonArray readJsonArray(String path) {
        JsonElement element = readJson(path);
        return element == null ? null : element.getAsJsonArray();
    }

    private static JsonElement readJson(String path) {
        try (InputStream stream = CosmeticDefinitionLoader.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }
}
