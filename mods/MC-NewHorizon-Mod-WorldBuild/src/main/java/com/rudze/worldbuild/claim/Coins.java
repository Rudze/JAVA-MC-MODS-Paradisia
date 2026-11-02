package com.rudze.worldbuild.claim;

/**
 * Formats amounts expressed in Numismatic Overhaul's raw value unit (1 bronze), mirroring its
 * {@code Currency} constants so this mod doesn't need a hard dependency on the economy mod.
 */
public final class Coins {

    public static final long GOLD = 10_000;
    public static final long SILVER = 100;

    private Coins() {
    }

    /** e.g. {@code 2 or, 5 argent, 3 bronze}; zero parts are skipped, and 0 reads "0 or". */
    public static String format(long raw) {
        long abs = Math.abs(raw);
        long gold = abs / GOLD;
        long silver = (abs % GOLD) / SILVER;
        long bronze = abs % SILVER;

        StringBuilder sb = new StringBuilder();
        append(sb, gold, "or");
        append(sb, silver, "argent");
        append(sb, bronze, "bronze");
        if (sb.isEmpty()) {
            sb.append("0 or");
        }
        return raw < 0 ? "-" + sb : sb.toString();
    }

    private static void append(StringBuilder sb, long count, String name) {
        if (count == 0) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(", ");
        }
        sb.append(count).append(' ').append(name);
    }
}
