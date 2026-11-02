package fr.rudze.cosmetics.item;

import fr.rudze.cosmetics.CosmeticsMod;
import fr.rudze.cosmetics.cosmetic.CosmeticDefinition;
import fr.rudze.cosmetics.cosmetic.CosmeticDefinitionLoader;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers one {@link CosmeticItem} per {@link CosmeticDefinition} found by
 * {@link CosmeticDefinitionLoader}. Adding a new cosmetic never touches this file - it's picked up
 * automatically from the JSON definitions.
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CosmeticsMod.MODID);

    private static final List<CosmeticDefinition> DEFINITIONS = CosmeticDefinitionLoader.loadAll();
    private static final Map<String, DeferredItem<CosmeticItem>> COSMETIC_ITEMS = new LinkedHashMap<>();

    static {
        for (CosmeticDefinition definition : DEFINITIONS) {
            COSMETIC_ITEMS.put(definition.id(), ITEMS.register(
                    definition.id(), () -> new CosmeticItem(definition, new Item.Properties().stacksTo(1))));
        }
    }

    private ModItems() {
    }

    public static DeferredItem<CosmeticItem> cosmetic(String id) {
        return COSMETIC_ITEMS.get(id);
    }

    public static Collection<DeferredItem<CosmeticItem>> cosmeticItems() {
        return COSMETIC_ITEMS.values();
    }

    public static Collection<String> cosmeticIds() {
        return COSMETIC_ITEMS.keySet();
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
