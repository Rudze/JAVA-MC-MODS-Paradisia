package fr.rudze.cosmetics.item;

import fr.rudze.cosmetics.cosmetic.CosmeticDefinition;
import io.wispforest.accessories.api.AccessoryItem;

/**
 * Generic cosmetic item: one instance per {@link CosmeticDefinition}, all sharing this same class.
 * Extending {@link AccessoryItem} auto-registers it with Accessories the moment it's constructed
 * (i.e. during normal {@code DeferredRegister} item creation - no separate setup-event call needed).
 * Which slot it's actually equippable into is decided by the {@code accessories:<slot>} item tag
 * (see the generated {@code data/accessories/tags/item/<slot>.json}), not by anything here - this
 * class has no behavior of its own beyond carrying its {@link CosmeticDefinition} for the renderer.
 */
public class CosmeticItem extends AccessoryItem {

    private final CosmeticDefinition definition;

    public CosmeticItem(CosmeticDefinition definition, Properties properties) {
        super(properties);
        this.definition = definition;
    }

    public CosmeticDefinition getDefinition() {
        return definition;
    }
}
