package fr.rudze.furniture.furniture;

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
 * Reads {@code nh_furniture_config/_index.json} (a flat list of ids) and, for each id,
 * {@code nh_furniture_config/<id>.json} off the classpath, turning them into {@link FurnitureDefinition}s.
 *
 * <p>These live as plain classpath resources (not under {@code assets}/{@code data}) so they can be
 * read here, during mod construction, before the game's resource/data pack managers exist yet -
 * registry entries have to be created that early. Adding a new furniture piece is: drop its model
 * json + texture (adjusted per the conversion notes) + one small definition json here, and add its
 * id to {@code _index.json}. No Java changes needed.
 */
public final class FurnitureDefinitionLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FurnitureDefinitionLoader.class);
    private static final String BASE_PATH = "/nh_furniture_config/";

    private FurnitureDefinitionLoader() {
    }

    public static List<FurnitureDefinition> loadAll() {
        List<FurnitureDefinition> definitions = new ArrayList<>();
        JsonArray index = readJsonArray(BASE_PATH + "_index.json");
        if (index == null) {
            LOGGER.warn("[nh_furniture] No furniture index found at {}_index.json; no furniture blocks will be registered.", BASE_PATH);
            return definitions;
        }

        for (JsonElement idElement : index) {
            String id = idElement.getAsString();
            JsonObject json = readJsonObject(BASE_PATH + id + ".json");
            if (json == null) {
                LOGGER.error("[nh_furniture] Furniture '{}' is listed in _index.json but {}{}.json is missing; skipping.", id, BASE_PATH, id);
                continue;
            }
            try {
                definitions.add(parse(id, json));
            } catch (RuntimeException e) {
                LOGGER.error("[nh_furniture] Failed to parse furniture definition '{}'; skipping.", id, e);
            }
        }
        return definitions;
    }

    private static FurnitureDefinition parse(String id, JsonObject json) {
        JsonObject place = json.getAsJsonObject("place");
        boolean placeFloor = place != null && place.has("floor") && place.get("floor").getAsBoolean();
        boolean placeWall = place != null && place.has("wall") && place.get("wall").getAsBoolean();
        boolean placeRoof = place != null && place.has("roof") && place.get("roof").getAsBoolean();

        JsonObject main = json.getAsJsonObject("main");
        FurnitureDefinition.MainShape mainShape = parseMainShape(main);

        List<FurnitureDefinition.Part> parts = new ArrayList<>();
        if (json.has("parts")) {
            for (JsonElement partElement : json.getAsJsonArray("parts")) {
                JsonObject part = partElement.getAsJsonObject();
                JsonArray offset = part.getAsJsonArray("offset");
                boolean solid = part.has("solid") && part.get("solid").getAsBoolean();
                int light = part.has("light") ? part.get("light").getAsInt() : 0;
                parts.add(new FurnitureDefinition.Part(
                        offset.get(0).getAsInt(), offset.get(1).getAsInt(), offset.get(2).getAsInt(), solid, light));
            }
        }

        int mainLight = json.has("light") ? json.get("light").getAsInt() : 0;
        boolean toggleableLight = json.has("toggleableLight") && json.get("toggleableLight").getAsBoolean();

        JsonObject sounds = json.getAsJsonObject("sounds");
        String placeSound = sounds != null && sounds.has("place") ? sounds.get("place").getAsString() : "minecraft:block.wood.place";
        String breakSound = sounds != null && sounds.has("break") ? sounds.get("break").getAsString() : "minecraft:block.wood.break";

        String displayName = json.has("name") ? json.get("name").getAsString() : id;

        double scale = json.has("scale") ? json.get("scale").getAsDouble() : 1.0;

        // 0xFF000000 (opaque alpha) is required here: DyedItemColor.getOrDefault returns this value
        // as-is (unlike the actually-dyed branch, which vanilla always forces opaque via
        // ARGB.opaque()) - without it, every undyed dyeable piece's tinted faces render alpha=0
        // (fully transparent), looking like broken/missing texture.
        int color = 0xFFFFFFFF;
        if (json.has("color")) {
            JsonArray colorArray = json.getAsJsonArray("color");
            int r = colorArray.get(0).getAsInt();
            int g = colorArray.get(1).getAsInt();
            int b = colorArray.get(2).getAsInt();
            color = 0xFF000000 | (r << 16) | (g << 8) | b;
        }

        return new FurnitureDefinition(id, displayName, placeFloor, placeWall, placeRoof,
                mainShape, parts, mainLight, toggleableLight, placeSound, breakSound, scale, color);
    }

    private static FurnitureDefinition.MainShape parseMainShape(JsonObject main) {
        if (main == null) {
            return FurnitureDefinition.MainShape.solidCube();
        }
        boolean solid = main.has("solid") && main.get("solid").getAsBoolean();
        if (main.has("box")) {
            JsonArray box = main.getAsJsonArray("box");
            double[] values = new double[6];
            for (int i = 0; i < 6; i++) {
                values[i] = box.get(i).getAsDouble();
            }
            return new FurnitureDefinition.MainShape(solid, values);
        }
        return solid ? FurnitureDefinition.MainShape.solidCube() : FurnitureDefinition.MainShape.empty();
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
        try (InputStream stream = FurnitureDefinitionLoader.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }
}
