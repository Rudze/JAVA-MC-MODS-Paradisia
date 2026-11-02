package tallestred.numismaticoverhaul.economy;

import net.minecraft.world.item.Item;

/**
 * Static, design-time description of one tradable crop: which merchant sells it (identity is
 * duplicated onto every crop rather than looked up elsewhere, so {@link CropMarket} never needs to
 * know about {@link tallestred.numismaticoverhaul.entity.WheatMerchantEntity} - a future second
 * merchant selling a different crop lineup is just a different set of {@code CropConfig}s), what item
 * it trades, and its {@link #basePrice()} in raw currency units at {@link CropMarket#BASE_STOCK}. See
 * {@link Crops} for the actual balancing numbers and the reasoning behind each price.
 */
public record CropConfig(
        String merchantId,
        String merchantName,
        String merchantPortrait,
        String commodityId,
        Item item,
        String displayName,
        long basePrice
) {
}
