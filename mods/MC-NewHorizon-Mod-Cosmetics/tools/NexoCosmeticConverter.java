import com.google.gson.*;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Converts a Nexo cosmetic yml + its matching HMCCosmetics yml (plus the exported pack: models +
 * textures) directly into everything nh_cosmetics needs: raw item model, textures, config json,
 * Accessories slot tag entries, and lang entries.
 *
 * <p>Nexo's own cosmetic yml only registers the item + model (no placement/slot info - that's a
 * furniture-only mechanic); the HMCCosmetics yml is what says which slot each cosmetic goes in and
 * whether it's dyeable, which is why both are needed. See docs/adding-cosmetics.md.
 *
 * <p>Usage:
 * {@code javac -cp gson.jar;snakeyaml.jar NexoCosmeticConverter.java}
 * {@code java -cp .;gson.jar;snakeyaml.jar NexoCosmeticConverter <nexoItems.yml> <hmcCosmetics.yml> <packDir> <resourcesDir> <namespace>}
 */
public class NexoCosmeticConverter {

    // HMCCosmetics slot -> Accessories slot name (see docs/adding-cosmetics.md for why). BALLOON
    // maps onto the same built-in "hand" slot as OFFHAND - it's not a distinct Accessories slot, just
    // a different render style flagged separately (see the "balloon" config field below).
    private static final Map<String, String> SLOT_MAP = Map.of(
            "HELMET", "hat",
            "OFFHAND", "hand",
            "BACKPACK", "back",
            "BALLOON", "hand"
    );

    public static void main(String[] args) throws Exception {
        Path nexoYmlFile = Path.of(args[0]);
        Path hmcYmlFile = Path.of(args[1]);
        Path packDir = Path.of(args[2]);
        Path resources = Path.of(args[3]);
        String namespace = args[4];

        // Most packs use the standard resource-pack plural folder names; at least one
        // (lanshan_cos_sunmoon_wizard) uses singular "model"/"texture" instead. Auto-detect rather
        // than requiring a flag or a manual rename before running the tool.
        Path modelsSrc = Files.isDirectory(packDir.resolve("assets/minecraft/models"))
                ? packDir.resolve("assets/minecraft/models") : packDir.resolve("assets/minecraft/model");
        Path texturesSrc = Files.isDirectory(packDir.resolve("assets/minecraft/textures"))
                ? packDir.resolve("assets/minecraft/textures") : packDir.resolve("assets/minecraft/texture");

        Path configDir = mkdirs(resources.resolve(namespace + "_config"));
        Path itemModelDir = mkdirs(resources.resolve("assets/" + namespace + "/models/item"));
        Path textureDir = mkdirs(resources.resolve("assets/" + namespace + "/textures/item"));
        Path langFile = mkdirs(resources.resolve("assets/" + namespace + "/lang")).resolve("en_us.json");
        Path indexFile = configDir.resolve("_index.json");
        Path dyeableTagFile = mkdirs(resources.resolve("data/minecraft/tags/item")).resolve("dyeable.json");
        Path accessoriesTagDir = mkdirs(resources.resolve("data/accessories/tags/item"));

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

        // slot name -> ids already in that slot's accessories:<slot> tag (merged across runs)
        Map<String, Set<String>> slotIds = new LinkedHashMap<>();
        for (String slot : Set.of("hat", "hand", "back")) {
            Path tagFile = accessoriesTagDir.resolve(slot + ".json");
            Set<String> ids = new LinkedHashSet<>();
            if (tagFile.toFile().exists()) {
                JsonObject existingTag = JsonParser.parseString(Files.readString(tagFile, StandardCharsets.UTF_8)).getAsJsonObject();
                existingTag.getAsJsonArray("values").forEach(e -> ids.add(e.getAsString()));
            }
            slotIds.put(slot, ids);
        }

        Yaml yaml = new Yaml();
        Map<String, Object> nexoRoot;
        try (InputStream in = Files.newInputStream(nexoYmlFile)) {
            nexoRoot = yaml.load(in);
        }
        Map<String, Object> hmcRoot;
        try (InputStream in = Files.newInputStream(hmcYmlFile)) {
            hmcRoot = yaml.load(in);
        }

        for (Map.Entry<String, Object> entry : hmcRoot.entrySet()) {
            String cosmeticId = entry.getKey();
            if (!(entry.getValue() instanceof Map)) continue; // skips stray comment/metadata keys
            Map<String, Object> hmcItem = cast(entry.getValue());

            Object nexoValue = nexoRoot.get(cosmeticId);
            if (!(nexoValue instanceof Map)) {
                System.out.println("Skipping " + cosmeticId + ": no matching entry in the Nexo yml");
                continue;
            }
            Map<String, Object> nexoItem = cast(nexoValue);

            String displayName = str(mapOf(hmcItem, "item").get("name"), str(nexoItem.get("itemname"), cosmeticId));
            Map<String, Object> pack = mapOf(nexoItem, "Pack");
            String modelPath = str(pack.get("model"), null);
            if (modelPath == null) {
                System.out.println("Skipping " + cosmeticId + ": no Pack.model in the Nexo yml");
                continue;
            }
            // Normally the id is the model filename (matches nh_furniture's own convention). Some
            // packs (lanshan_cos_sunmoon_wizard) export purely numeric model filenames ("1".."6")
            // instead of descriptive ones, which would make genuinely bad, collision-prone item ids
            // (nh_cosmetics:1 is meaningless and any future pack could reuse the same numbers) - fall
            // back to the yml's own cosmetic key in that case, which is always unique and descriptive.
            String filenameId = modelPath.substring(modelPath.lastIndexOf('/') + 1);
            String id = filenameId.matches("\\d+") ? cosmeticId : filenameId;

            String hmcSlot = str(hmcItem.get("slot"), "HELMET").toUpperCase(Locale.ROOT);
            String slot = SLOT_MAP.getOrDefault(hmcSlot, "hat");
            if (!SLOT_MAP.containsKey(hmcSlot)) {
                System.out.println("NOTE: " + id + " has unrecognized HMCCosmetics slot '" + hmcSlot + "', defaulting to 'hat'.");
            }

            if (hmcItem.containsKey("firstperson-item")) {
                System.out.println("NOTE: " + id + " has a 'firstperson-item' override in HMCCosmetics - not supported yet, using the main model for every perspective.");
            }

            String colorStr = str(nexoItem.get("color"), "255, 255, 255");
            String[] colorParts = colorStr.split(",");
            int[] color = {
                    Integer.parseInt(colorParts[0].trim()),
                    Integer.parseInt(colorParts[1].trim()),
                    Integer.parseInt(colorParts[2].trim())
            };

            // --- item model: raw, unmodified copy (geometry + display), textures remapped ---
            Path modelFile = modelsSrc.resolve(modelPath + ".json");
            JsonObject sourceModel = JsonParser.parseString(Files.readString(modelFile, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject sourceTextures = sourceModel.getAsJsonObject("textures");
            JsonObject newTextures = new JsonObject();
            String firstKey = null;
            for (String key : sourceTextures.keySet()) {
                String value = sourceTextures.get(key).getAsString();
                String fileName = value.substring(value.lastIndexOf('/') + 1);
                newTextures.addProperty(key, namespace + ":item/" + fileName);
                if (firstKey == null) firstKey = key;

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

            // A tintindex-0 face means the artist checked "Tint" on it in Blockbench - matches
            // HMCCosmetics' own "dyeable: true" flag, detected here from the model itself so no
            // extra config field is needed.
            boolean tintable = hasTintIndex(sourceModel);
            if (tintable) {
                dyeableIds.add(namespace + ":" + id);
            }

            // --- config json ---
            JsonObject config = new JsonObject();
            config.addProperty("name", displayName);
            config.addProperty("slot", slot);
            if (hmcSlot.equals("BALLOON")) {
                config.addProperty("balloon", true);
            }
            if (tintable) {
                JsonArray colorArray = new JsonArray();
                colorArray.add(color[0]); colorArray.add(color[1]); colorArray.add(color[2]);
                config.add("color", colorArray);
            }
            Files.writeString(configDir.resolve(id + ".json"), gson.toJson(config), StandardCharsets.UTF_8);

            slotIds.get(slot).add(namespace + ":" + id);
            lang.addProperty("item." + namespace + "." + id, displayName);
            existingIds.add(id);

            System.out.println("Converted " + cosmeticId + " -> " + id + " (slot: " + slot + ")");
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

        for (var slotEntry : slotIds.entrySet()) {
            if (slotEntry.getValue().isEmpty()) continue;
            JsonObject tag = new JsonObject();
            tag.addProperty("replace", false);
            JsonArray values = new JsonArray();
            slotEntry.getValue().forEach(values::add);
            tag.add("values", values);
            Files.writeString(accessoriesTagDir.resolve(slotEntry.getKey() + ".json"), gson.toJson(tag), StandardCharsets.UTF_8);
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

    private static String str(Object value, String fallback) {
        return value == null ? fallback : value.toString();
    }
}
