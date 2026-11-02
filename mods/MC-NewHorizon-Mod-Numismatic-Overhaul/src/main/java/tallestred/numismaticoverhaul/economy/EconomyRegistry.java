package tallestred.numismaticoverhaul.economy;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory, thread-safe snapshot of every merchant's markets that's been touched this server session,
 * kept up to date so {@link EconomyWebServer} can read it from its own HTTP handler threads without
 * ever touching the actual world/entities - Minecraft's world state isn't safe to read off the server
 * thread, so {@link CropMarket} pushes a fresh, fully-detached snapshot here on every trade instead of
 * the web server reaching into the level to look one up on request.
 *
 * <p>Keyed by merchant id ("wheat_merchant"), one entry per named NPC shopkeeper shown on the
 * dashboard's merchant-selector bar - not by entity instance or by commodity. A merchant can sell
 * several crops (each its own {@link CropMarket}), so each merchant entry holds a map of its own,
 * keyed by commodity id, that individual {@code CropMarket#publishSnapshot} calls update one commodity
 * at a time without clobbering the merchant's other crops. Merchant ids that exist on the dashboard's
 * static catalog but never call {@link #updateCommodity} (no shop implemented yet) simply have no
 * entry here - the dashboard renders those as locked/"coming soon" buttons.
 */
public final class EconomyRegistry {
    private static final Map<String, MerchantEntry> MERCHANTS = new ConcurrentHashMap<>();

    private EconomyRegistry() {
    }

    public static void updateCommodity(
            String merchantId, String merchantName, String merchantPortrait,
            String commodityId, String displayName, String item,
            int stock, long price, List<PricePoint> history
    ) {
        MerchantEntry entry = MERCHANTS.computeIfAbsent(
                merchantId, id -> new MerchantEntry(id, merchantName, merchantPortrait, new ConcurrentHashMap<>())
        );
        entry.commodities.put(commodityId, new CommoditySnapshot(commodityId, displayName, item, stock, price, List.copyOf(history)));
    }

    public static Collection<Snapshot> snapshotAll() {
        return MERCHANTS.values().stream().map(MerchantEntry::toSnapshot).toList();
    }

    private record MerchantEntry(String id, String name, String portrait, Map<String, CommoditySnapshot> commodities) {
        Snapshot toSnapshot() {
            return new Snapshot(id, name, portrait, List.copyOf(commodities.values()));
        }
    }

    public record CommoditySnapshot(String id, String name, String item, int stock, long price, List<PricePoint> history) {
    }

    public record Snapshot(String id, String name, String portrait, List<CommoditySnapshot> commodities) {
    }
}
