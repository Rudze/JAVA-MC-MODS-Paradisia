package tallestred.numismaticoverhaul.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import tallestred.numismaticoverhaul.currency.Currency;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * The single, world-wide market for one crop (wheat, carrot, ...) - one shared stock and one shared
 * price for that commodity, not one per NPC. Originally this class only ever handled wheat
 * ({@code WheatMarket}); generalized to {@link CropConfig} so every harvestable good a merchant sells
 * follows the exact same dynamic-pricing/anti-arbitrage rules instead of copy-pasting the class per
 * crop. A merchant that sells N crops just holds N independent {@code CropMarket} instances, one per
 * {@link CropConfig} in {@link Crops#ALL} - see {@link tallestred.numismaticoverhaul.entity.WheatMerchantEntity}.
 *
 * <p>Persisted via vanilla's own {@link SavedData} mechanism (like map data or raids), attached to the
 * overworld regardless of which dimension a given merchant is standing in, keyed by commodity id so
 * each crop gets its own independent save slot and survives any individual NPC being killed.
 *
 * <p>Pricing: {@code price = basePrice * BASE_STOCK / stock}, floored at 1 bronze - at the nominal
 * {@link #BASE_STOCK} (100), price is exactly {@link CropConfig#basePrice()}. {@link #MAX_STOCK} stops
 * an endless selling spree from crashing the price to a degenerate near-zero. Every crop shares the
 * same {@link #BASE_STOCK}/{@link #MAX_STOCK} - only {@code basePrice} differs - so every commodity
 * has identical price *elasticity* (same percentage swing per unit of stock moved) and only the
 * absolute price level differs between crops; see {@link Crops} for how those base prices are set.
 *
 * <p><b>Buy and sell prices are deliberately not the same number</b> (carried over from the original
 * wheat-only design): an immediate buy-then-sell-back round trip always costs at least as much as it
 * returns, since {@link #buyPrice()} and {@link #sellPrice()} evaluate the price formula at the stock
 * level *after* that specific trade would happen (buying at {@code stock - 1}, selling at
 * {@code stock + 1}) rather than sharing one {@code currentPrice()} for both directions - the latter
 * let players mint free currency by immediately selling back what they just bought.
 */
public final class CropMarket extends SavedData {
    public static final int BASE_STOCK = 100;
    public static final int MAX_STOCK = 500;
    private static final int MAX_HISTORY = 500;

    private final CropConfig config;
    private int stock = BASE_STOCK;
    private final Deque<PricePoint> history = new ArrayDeque<>();

    private CropMarket(CropConfig config) {
        this.config = config;
    }

    public static CropMarket get(ServerLevel level, CropConfig config) {
        CropMarket market = level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(() -> new CropMarket(config), (tag, registries) -> load(config, tag, registries), DataFixTypes.LEVEL),
                "numismaticoverhaul_market_" + config.commodityId()
        );
        // Always (re-)publish, not just when history is empty: EconomyRegistry is purely in-memory and
        // starts empty on every server boot, while this SavedData can already have a non-empty history
        // loaded from disk (from a previous session) - gating on "history.isEmpty()" meant a server
        // restart with existing market data would leave the dashboard showing nothing at all for this
        // commodity until the next actual trade, even though the shop was fully working in-game.
        market.publishSnapshot();
        return market;
    }

    public int getStock() {
        return stock;
    }

    /** {@code basePrice * BASE_STOCK / stock}, floored at 1 bronze - see the class javadoc. */
    public long currentPrice() {
        return priceAt(stock);
    }

    /** What the player pays to buy 1 unit right now - the marginal price *after* stock drops by 1. */
    public long buyPrice() {
        return priceAt(stock - 1);
    }

    /** What the player receives for selling 1 unit right now - the marginal price *after* stock rises by 1. */
    public long sellPrice() {
        return priceAt(stock + 1);
    }

    private long priceAt(int hypotheticalStock) {
        long price = Math.round(config.basePrice() * (double) BASE_STOCK / Math.max(1, hypotheticalStock));
        return Math.max(Currency.BRONZE_VALUE, price);
    }

    public void onPlayerBought(long gameTime) {
        stock = Math.max(0, stock - 1);
        recordPoint(gameTime);
    }

    public void onPlayerSold(long gameTime) {
        stock = Math.min(MAX_STOCK, stock + 1);
        recordPoint(gameTime);
    }

    /** Admin-command entry point ({@code /numismaticoverhaul market reset <crop>}) - back to {@link #BASE_STOCK}, recorded as a normal history point rather than wiping the curve. */
    public void resetStock(long gameTime) {
        stock = BASE_STOCK;
        recordPoint(gameTime);
    }

    private void recordPoint(long gameTime) {
        history.addLast(new PricePoint(gameTime, System.currentTimeMillis(), currentPrice(), stock));
        while (history.size() > MAX_HISTORY) {
            history.removeFirst();
        }
        setDirty();
        publishSnapshot();
    }

    private void publishSnapshot() {
        EconomyRegistry.updateCommodity(
                config.merchantId(), config.merchantName(), config.merchantPortrait(),
                config.commodityId(), config.displayName(),
                BuiltInRegistries.ITEM.getKey(config.item()).toString(),
                stock, currentPrice(), List.copyOf(history)
        );
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Stock", stock);

        ListTag historyTag = new ListTag();
        for (PricePoint point : history) {
            CompoundTag pointTag = new CompoundTag();
            pointTag.putLong("Time", point.gameTime());
            pointTag.putLong("RealTime", point.realTime());
            pointTag.putLong("Price", point.price());
            pointTag.putInt("Stock", point.stock());
            historyTag.add(pointTag);
        }
        tag.put("History", historyTag);
        return tag;
    }

    private static CropMarket load(CropConfig config, CompoundTag tag, HolderLookup.Provider registries) {
        CropMarket market = new CropMarket(config);
        market.stock = tag.contains("Stock") ? tag.getInt("Stock") : BASE_STOCK;

        if (tag.contains("History")) {
            for (var element : tag.getList("History", CompoundTag.TAG_COMPOUND)) {
                CompoundTag pointTag = (CompoundTag) element;
                market.history.addLast(new PricePoint(
                        pointTag.getLong("Time"), pointTag.getLong("RealTime"),
                        pointTag.getLong("Price"), pointTag.getInt("Stock")
                ));
            }
        }
        return market;
    }
}
