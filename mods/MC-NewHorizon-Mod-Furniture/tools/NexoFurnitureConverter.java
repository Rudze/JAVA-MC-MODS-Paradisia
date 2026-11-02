import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Converts a Nexo furniture yml (plus its exported pack: models + textures) directly into
 * everything nh_furniture needs: raw item model, textures, config json, block model stub,
 * blockstate, loot table, lang entry, and _index.json.
 *
 * <p>This is the "drag and drop" pipeline made concrete: point it at the yml and the pack folder,
 * it does the rest. See docs/adding-furniture.md for the full explanation and for what it does
 * NOT infer (seats, currently) that still needs a manual touch-up afterwards.
 *
 * <p>Usage:
 * {@code javac -cp gson.jar;snakeyaml.jar NexoFurnitureConverter.java}
 * {@code java -cp .;gson.jar;snakeyaml.jar NexoFurnitureConverter <items.yml> <packDir> <resourcesDir> <namespace>}
 *
 * <ul>
 *   <li>{@code items.yml} - the Nexo furniture yml (e.g. items/lanshan/lanshan_summer.yml).</li>
 *   <li>{@code packDir} - the exported pack root, containing assets/minecraft/models and
 *       assets/minecraft/textures (e.g. the folder next to the yml, usually called "pack").</li>
 *   <li>{@code resourcesDir} - the mod's src/main/resources.</li>
 *   <li>{@code namespace} - the mod's resource namespace (e.g. nh_furniture).</li>
 * </ul>
 */
public class NexoFurnitureConverter {

    record PartSpec(int dx, int dy, int dz, boolean solid, int light) {
    }

    public static void main(String[] args) throws Exception {
        Path ymlFile = Path.of(args[0]);
        Path packDir = Path.of(args[1]);
        Path resources = Path.of(args[2]);
        String namespace = args[3];

        // Two pack layouts seen so far: newer Nexo exports wrap everything in the usual resource-pack
        // "assets/minecraft/..." path; older Oraxen exports (e.g. the Autumn Field pack) put
        // "models"/"textures" directly at the pack root with no wrapper at all.
        boolean wrapped = Files.isDirectory(packDir.resolve("assets/minecraft/models"));
        Path modelsSrc = wrapped ? packDir.resolve("assets/minecraft/models") : packDir.resolve("models");
        Path texturesSrc = wrapped ? packDir.resolve("assets/minecraft/textures") : packDir.resolve("textures");

        Path configDir = mkdirs(resources.resolve(namespace + "_config"));
        Path itemModelDir = mkdirs(resources.resolve("assets/" + namespace + "/models/item"));
        Path blockModelDir = mkdirs(resources.resolve("assets/" + namespace + "/models/block"));
        Path blockstateDir = mkdirs(resources.resolve("assets/" + namespace + "/blockstates"));
        Path textureDir = mkdirs(resources.resolve("assets/" + namespace + "/textures/block"));
        Path lootDir = mkdirs(resources.resolve("data/" + namespace + "/loot_table/blocks"));
        Path langFile = mkdirs(resources.resolve("assets/" + namespace + "/lang")).resolve("en_us.json");
        Path indexFile = configDir.resolve("_index.json");
        Path dyeableTagFile = mkdirs(resources.resolve("data/minecraft/tags/item")).resolve("dyeable.json");

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

        JsonArray index = indexFile.toFile().exists()
                ? JsonParser.parseString(Files.readString(indexFile, StandardCharsets.UTF_8)).getAsJsonArray()
                : new JsonArray();
        Set<String> existingIds = new LinkedHashSet<>();
        index.forEach(e -> existingIds.add(e.getAsString()));

        JsonObject lang = langFile.toFile().exists()
                ? JsonParser.parseString(Files.readString(langFile, StandardCharsets.UTF_8)).getAsJsonObject()
                : new JsonObject();

        Set<String> dyeableIds = new LinkedHashSet<>();
        if (dyeableTagFile.toFile().exists()) {
            JsonObject existingTag = JsonParser.parseString(Files.readString(dyeableTagFile, StandardCharsets.UTF_8)).getAsJsonObject();
            existingTag.getAsJsonArray("values").forEach(e -> dyeableIds.add(e.getAsString()));
        }

        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream in = Files.newInputStream(ymlFile)) {
            root = yaml.load(in);
        }

        for (Map.Entry<String, Object> entry : root.entrySet()) {
            String nexoId = entry.getKey();
            if (!(entry.getValue() instanceof Map)) continue; // skips stray comment/metadata keys
            Map<String, Object> item = cast(entry.getValue());

            String displayName = str(item.get("itemname"), nexoId);
            Map<String, Object> pack = mapOf(item, "Pack");
            String modelPath = str(pack.get("model"), null);
            if (modelPath == null) {
                System.out.println("Skipping " + nexoId + ": no Pack.model");
                continue;
            }
            String id = modelPath.substring(modelPath.lastIndexOf('/') + 1);

            Map<String, Object> furniture = mapOf(mapOf(item, "Mechanics"), "furniture");
            Map<String, Object> limitedPlacing = mapOf(furniture, "limited_placing");
            boolean floor = bool(limitedPlacing.get("floor"));
            boolean wall = bool(limitedPlacing.get("wall"));
            boolean roof = bool(limitedPlacing.get("roof"));

            String colorStr = str(item.get("color"), "255, 255, 255");
            String[] colorParts = colorStr.split(",");
            int[] color = {
                    Integer.parseInt(colorParts[0].trim()),
                    Integer.parseInt(colorParts[1].trim()),
                    Integer.parseInt(colorParts[2].trim())
            };

            Map<String, Object> blockSounds = mapOf(furniture, "block_sounds");
            String placeSound = "minecraft:" + str(blockSounds.get("place_sound"), "block.wood.place");
            String breakSound = "minecraft:" + str(blockSounds.get("break_sound"), "block.wood.break");

            // Every Nexo yml seen prior to the Autumn Field pack explicitly set this to "0.5,0.5,0.5"
            // (no exceptions across ~70 converted pieces) - it's the net scale nh_furniture's own
            // rendering pipeline expects on top of the model's own baked "fixed" display transform.
            // Autumn Field's Oraxen yml omits the field entirely (Oraxen apparently doesn't require
            // it), which used to fall back to "1,1,1" here - literally never-before-exercised code,
            // since no prior pack ever omitted it - and rendered every piece from that pack 2x too
            // big. Default to the same "0.5,0.5,0.5" every real pack has actually used instead.
            Map<String, Object> properties = mapOf(furniture, "properties");
            String scaleStr = str(properties.get("scale"), "0.5,0.5,0.5");
            double scale = Double.parseDouble(scaleStr.split(",")[0].trim());

            if (furniture.containsKey("seats") || furniture.containsKey("seat")) {
                System.out.println("NOTE: " + id + " has a seat in the yml - sitting isn't supported, ignoring.");
            }

            // Two Nexo/Oraxen yml shapes seen so far: newer packs nest barriers/interactions under
            // "hitbox" with each offset as an "x,y,z" string; older Oraxen exports (e.g. the Autumn
            // Field pack) put barriers/interactions directly under "furniture", with each offset as
            // its own {x:, y:, z:} map instead of a string. parseOffset handles both entry shapes;
            // this just finds whichever level they're nested at.
            Map<String, Object> hitbox = mapOf(furniture, "hitbox");
            List<Object> barriers = listOf(hitbox, "barriers");
            List<Object> interactions = listOf(hitbox, "interactions");
            if (barriers.isEmpty() && interactions.isEmpty()) {
                barriers = listOf(furniture, "barriers");
                interactions = listOf(furniture, "interactions");
            }

            boolean mainSolid;
            List<PartSpec> parts = new ArrayList<>();
            if (!barriers.isEmpty()) {
                mainSolid = true;
                for (Object b : barriers) {
                    int[] offset = parseOffset(b);
                    if (offset[0] == 0 && offset[1] == 0 && offset[2] == 0) continue;
                    parts.add(new PartSpec(offset[0], offset[1], offset[2], true, 0));
                }
            } else if (!interactions.isEmpty()) {
                mainSolid = false;
            } else {
                mainSolid = true;
            }

            // Lights, two shapes: newer packs use "lights": {toggleable: bool, lights: ["dx,dy,dz
            // level", ...]} (an offset of 0,0,0 lights the main cell itself; any other offset gets
            // its own non-solid lit part, matching how desert_lantern/desert_brazier were hand-built).
            // Older Oraxen exports just have a plain "light: <level>" scalar directly under
            // "furniture", always meaning the main cell - no toggle, no offset cells.
            Map<String, Object> lightsSection = mapOf(furniture, "lights");
            boolean toggleableLight = bool(lightsSection.get("toggleable"));
            int mainLight = 0;
            if (!lightsSection.isEmpty()) {
                for (Object lightEntry : listOf(lightsSection, "lights")) {
                    String[] tokens = lightEntry.toString().trim().split("\\s+");
                    int[] offset = parseOffset(tokens[0]);
                    int level = Integer.parseInt(tokens[1].trim());
                    if (offset[0] == 0 && offset[1] == 0 && offset[2] == 0) {
                        mainLight = level;
                    } else {
                        parts.add(new PartSpec(offset[0], offset[1], offset[2], false, level));
                    }
                }
            } else if (furniture.containsKey("light")) {
                mainLight = (int) Double.parseDouble(str(furniture.get("light"), "0"));
            }

            // --- item model: raw, unmodified copy (geometry + display), textures remapped ---
            Path modelFile = modelsSrc.resolve(modelPath + ".json");
            JsonObject sourceModel = JsonParser.parseString(Files.readString(modelFile, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject sourceTextures = sourceModel.getAsJsonObject("textures");
            JsonObject newTextures = new JsonObject();
            String firstKey = null;
            for (String key : sourceTextures.keySet()) {
                String value = sourceTextures.get(key).getAsString();
                String fileName = value.substring(value.lastIndexOf('/') + 1);
                newTextures.addProperty(key, namespace + ":block/" + fileName);
                if (firstKey == null) firstKey = key;

                // copy the texture (and its .mcmeta, if animated) alongside
                Path textureSrc = texturesSrc.resolve(value + ".png");
                Path textureDst = textureDir.resolve(fileName + ".png");
                if (Files.exists(textureSrc)) Files.copy(textureSrc, textureDst, StandardCopyOption.REPLACE_EXISTING);
                Path mcmetaSrc = texturesSrc.resolve(value + ".png.mcmeta");
                if (Files.exists(mcmetaSrc)) {
                    Files.copy(mcmetaSrc, textureDir.resolve(fileName + ".png.mcmeta"), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            if (!newTextures.has("particle") && firstKey != null) {
                newTextures.addProperty("particle", newTextures.get(firstKey).getAsString());
            }
            JsonObject itemModel = new JsonObject();
            if (sourceModel.has("texture_size")) itemModel.add("texture_size", sourceModel.get("texture_size"));
            itemModel.add("textures", newTextures);
            itemModel.add("elements", sourceModel.getAsJsonArray("elements"));
            if (sourceModel.has("gui_light")) itemModel.add("gui_light", sourceModel.get("gui_light"));
            if (sourceModel.has("display")) itemModel.add("display", sourceModel.getAsJsonObject("display"));
            Files.writeString(itemModelDir.resolve(id + ".json"), gson.toJson(itemModel), StandardCharsets.UTF_8);

            // A tintindex-0 face means the artist checked "Tint" on it in Blockbench - Nexo's own
            // marker for "this can be dyed like leather armor." Detected from the model itself, no
            // extra yml field needed.
            boolean tintable = hasTintIndex(sourceModel);
            if (tintable) {
                dyeableIds.add(namespace + ":" + id);
            }

            // --- config json ---
            JsonObject config = new JsonObject();
            config.addProperty("name", displayName);
            JsonObject place = new JsonObject();
            place.addProperty("floor", floor);
            place.addProperty("wall", wall);
            place.addProperty("roof", roof);
            config.add("place", place);
            JsonObject main = new JsonObject();
            main.addProperty("solid", mainSolid);
            if (!mainSolid) {
                JsonArray box = new JsonArray();
                for (int v : new int[]{0, 0, 0, 16, 16, 16}) box.add(v);
                main.add("box", box);
            }
            config.add("main", main);
            JsonArray partsArray = new JsonArray();
            for (PartSpec p : parts) {
                JsonObject partObj = new JsonObject();
                JsonArray offset = new JsonArray();
                offset.add(p.dx()); offset.add(p.dy()); offset.add(p.dz());
                partObj.add("offset", offset);
                partObj.addProperty("solid", p.solid());
                partObj.addProperty("light", p.light());
                partsArray.add(partObj);
            }
            config.add("parts", partsArray);
            if (mainLight > 0) {
                config.addProperty("light", mainLight);
            }
            if (toggleableLight) {
                config.addProperty("toggleableLight", true);
            }
            JsonObject sounds = new JsonObject();
            sounds.addProperty("place", placeSound);
            sounds.addProperty("break", breakSound);
            config.add("sounds", sounds);
            config.addProperty("scale", scale);
            if (tintable) {
                JsonArray colorArray = new JsonArray();
                colorArray.add(color[0]); colorArray.add(color[1]); colorArray.add(color[2]);
                config.add("color", colorArray);
            }
            Files.writeString(configDir.resolve(id + ".json"), gson.toJson(config), StandardCharsets.UTF_8);

            // --- block model stub: a safe, generic small cube (never actually rendered - the
            // dynamic renderer draws the item model instead) using the item's own particle texture ---
            JsonObject stub = new JsonObject();
            JsonObject stubTextures = new JsonObject();
            String particleTex = newTextures.get("particle").getAsString();
            stubTextures.addProperty("0", particleTex);
            stubTextures.addProperty("particle", particleTex);
            stub.add("textures", stubTextures);
            JsonArray elements = new JsonArray();
            JsonObject element = new JsonObject();
            JsonArray from = new JsonArray(); from.add(4); from.add(0); from.add(4);
            JsonArray to = new JsonArray(); to.add(12); to.add(8); to.add(12);
            element.add("from", from);
            element.add("to", to);
            JsonObject faces = new JsonObject();
            for (String face : new String[]{"down", "up", "north", "south", "west", "east"}) {
                JsonObject faceObj = new JsonObject();
                faceObj.addProperty("texture", "#0");
                faces.add(face, faceObj);
            }
            element.add("faces", faces);
            elements.add(element);
            stub.add("elements", elements);
            Files.writeString(blockModelDir.resolve(id + ".json"), gson.toJson(stub), StandardCharsets.UTF_8);

            // --- blockstate: "facing" always exists (nearest of 4, grid-safe - used for parts);
            // "rotation_8" exists whenever floor/roof placement is allowed AND every part is a
            // purely vertical offset (nearest of 8, purely visual - lets floor/ceiling pieces face
            // diagonally; a *horizontal* part, like a bench's extra seats to its side, would desync
            // from the grid-locked 4-way parts rotation if it went diagonal too); "face"
            // (floor/wall/ceiling) only exists when more than one placement is allowed. Minecraft
            // requires a variant for every combination of whichever properties are registered, even
            // though this stub model is never actually drawn (RenderShape.INVISIBLE).
            int placementCount = (floor ? 1 : 0) + (wall ? 1 : 0) + (roof ? 1 : 0);
            boolean hasFaceProp = placementCount > 1;
            boolean allPartsVertical = parts.stream().allMatch(p -> p.dx() == 0 && p.dz() == 0);
            boolean hasRotation8 = (floor || roof) && allPartsVertical;
            String[] facings = {"north", "east", "south", "west"};
            int[] yRot = {0, 90, 180, 270};
            String modelRef = namespace + ":block/" + id;

            JsonObject blockstate = new JsonObject();
            JsonObject variants = new JsonObject();
            List<String> faceValues = hasFaceProp ? List.of("floor", "wall", "ceiling") : Collections.singletonList(null);
            for (String faceVal : faceValues) {
                for (int i = 0; i < facings.length; i++) {
                    int rotation8Count = hasRotation8 ? 8 : 1;
                    for (int rotation8 = 0; rotation8 < rotation8Count; rotation8++) {
                        StringBuilder key = new StringBuilder();
                        if (faceVal != null) key.append("face=").append(faceVal).append(",");
                        key.append("facing=").append(facings[i]);
                        if (hasRotation8) key.append(",rotation_8=").append(rotation8);

                        JsonObject variant = new JsonObject();
                        variant.addProperty("model", modelRef);
                        if (yRot[i] != 0) variant.addProperty("y", yRot[i]);
                        variants.add(key.toString(), variant);
                    }
                }
            }
            blockstate.add("variants", variants);
            Files.writeString(blockstateDir.resolve(id + ".json"), gson.toJson(blockstate), StandardCharsets.UTF_8);

            // --- loot table: drop self ---
            JsonObject loot = new JsonObject();
            loot.addProperty("type", "minecraft:block");
            JsonArray pools = new JsonArray();
            JsonObject pool = new JsonObject();
            pool.addProperty("bonus_rolls", 0.0);
            JsonArray conditions = new JsonArray();
            JsonObject condition = new JsonObject();
            condition.addProperty("condition", "minecraft:survives_explosion");
            conditions.add(condition);
            pool.add("conditions", conditions);
            JsonArray entries = new JsonArray();
            JsonObject lootEntry = new JsonObject();
            lootEntry.addProperty("type", "minecraft:item");
            lootEntry.addProperty("name", namespace + ":" + id);
            entries.add(lootEntry);
            pool.add("entries", entries);
            pool.addProperty("rolls", 1.0);
            pools.add(pool);
            loot.add("pools", pools);
            Files.writeString(lootDir.resolve(id + ".json"), gson.toJson(loot), StandardCharsets.UTF_8);

            // --- lang + index ---
            lang.addProperty("block." + namespace + "." + id, displayName);
            existingIds.add(id);

            System.out.println("Converted " + nexoId + " -> " + id);
        }

        JsonArray newIndex = new JsonArray();
        existingIds.forEach(newIndex::add);
        Files.writeString(indexFile, gson.toJson(newIndex), StandardCharsets.UTF_8);
        Files.writeString(langFile, gson.toJson(lang), StandardCharsets.UTF_8);

        if (!dyeableIds.isEmpty()) {
            JsonObject dyeableTag = new JsonObject();
            dyeableTag.addProperty("replace", false);
            JsonArray values = new JsonArray();
            dyeableIds.forEach(values::add);
            dyeableTag.add("values", values);
            Files.writeString(dyeableTagFile, gson.toJson(dyeableTag), StandardCharsets.UTF_8);
        }

        System.out.println("Done. " + existingIds.size() + " ids in " + indexFile + ", " + dyeableIds.size() + " dyeable.");
    }

    /** True if any face in any element has a "tintindex" - Blockbench's "Tint" checkbox. */
    private static boolean hasTintIndex(JsonObject model) {
        for (JsonElement el : model.getAsJsonArray("elements")) {
            JsonObject faces = el.getAsJsonObject().getAsJsonObject("faces");
            if (faces == null) continue;
            for (String faceKey : faces.keySet()) {
                if (faces.getAsJsonObject(faceKey).has("tintindex")) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Path mkdirs(Path path) throws IOException {
        Files.createDirectories(path);
        return path;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Object o) {
        return (Map<String, Object>) o;
    }

    private static Map<String, Object> mapOf(Map<String, Object> parent, String key) {
        Object value = parent == null ? null : parent.get(key);
        return value instanceof Map ? cast(value) : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> listOf(Map<String, Object> parent, String key) {
        Object value = parent == null ? null : parent.get(key);
        return value instanceof List ? (List<Object>) value : List.of();
    }

    private static String str(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }

    /**
     * Parses a hitbox offset in either shape seen so far: a Nexo-style "0,1,0" string, or an older
     * Oraxen-style {@code {x: 0, y: 1, z: 0}} map (as SnakeYAML parses a YAML block-list entry like
     * {@code - x: 0\n  y: 1\n  z: 0}).
     */
    private static int[] parseOffset(Object value) {
        if (value instanceof Map) {
            Map<String, Object> m = cast(value);
            return new int[]{
                    (int) Double.parseDouble(str(m.get("x"), "0")),
                    (int) Double.parseDouble(str(m.get("y"), "0")),
                    (int) Double.parseDouble(str(m.get("z"), "0"))
            };
        }
        String[] parts = value.toString().trim().split(",");
        return new int[]{
                (int) Double.parseDouble(parts[0].trim()),
                (int) Double.parseDouble(parts[1].trim()),
                (int) Double.parseDouble(parts[2].trim())
        };
    }
}
