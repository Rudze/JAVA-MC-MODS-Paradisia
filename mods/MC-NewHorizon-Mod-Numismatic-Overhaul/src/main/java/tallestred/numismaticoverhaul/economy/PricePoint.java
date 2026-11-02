package tallestred.numismaticoverhaul.economy;

/**
 * One sample in a {@link tallestred.numismaticoverhaul.entity.WheatMerchantEntity}'s price history -
 * recorded once per trade (not on a timer), so the curve reflects actual market activity rather than
 * idle time. {@code gameTime} is {@code Level#getGameTime()}, kept for reference but not used for the
 * dashboard's time axis. {@code realTime} is {@code System.currentTimeMillis()} at the moment the trade
 * happened - a real wall-clock timestamp, needed so the dashboard's "last 6h / 1 day / 1 week" view
 * filters mean what they say; game ticks don't map onto real elapsed time (they stop when the server
 * isn't running, and drift from real time with tick lag), so they can't drive that filtering. Points
 * loaded from a save written before this field existed get {@code realTime = 0} - old enough to never
 * match any bounded range, which is the correct behavior for data with no real timestamp.
 */
public record PricePoint(long gameTime, long realTime, long price, int stock) {
}
