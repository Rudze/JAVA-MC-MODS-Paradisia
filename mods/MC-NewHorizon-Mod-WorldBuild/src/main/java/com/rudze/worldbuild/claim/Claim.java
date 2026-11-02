package com.rudze.worldbuild.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * A world_build plot owned by one player: a full column from bedrock to build limit, so only the
 * X/Z rectangle is stored. Bounds are inclusive.
 *
 * @param lastBilledAt epoch millis of the end of the last billed week; the next week's tax is due
 *                     once {@link ClaimData#BILLING_PERIOD_MS} has passed since then.
 */
public record Claim(int id, UUID owner, String ownerName, int minX, int minZ, int maxX, int maxZ, long lastBilledAt) {

    public static Claim of(int id, UUID owner, String ownerName, BlockPos a, BlockPos b, long now) {
        return new Claim(id, owner, ownerName,
                Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()),
                now);
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX && pos.getX() <= maxX && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public boolean overlaps(Claim other) {
        return minX <= other.maxX && maxX >= other.minX && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    /** Surface area in blocks, i.e. what the weekly tax is charged on. */
    public long area() {
        return (long) (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    public long weeklyTax() {
        return area() * ClaimData.TAX_PER_BLOCK_PER_WEEK;
    }

    public Claim withLastBilledAt(long time) {
        return new Claim(id, owner, ownerName, minX, minZ, maxX, maxZ, time);
    }

    public String describeBounds() {
        return "(" + minX + ", " + minZ + ") -> (" + maxX + ", " + maxZ + ")";
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Id", id);
        tag.putUUID("Owner", owner);
        tag.putString("OwnerName", ownerName);
        tag.putInt("MinX", minX);
        tag.putInt("MinZ", minZ);
        tag.putInt("MaxX", maxX);
        tag.putInt("MaxZ", maxZ);
        tag.putLong("LastBilledAt", lastBilledAt);
        return tag;
    }

    static Claim load(CompoundTag tag) {
        return new Claim(tag.getInt("Id"), tag.getUUID("Owner"), tag.getString("OwnerName"),
                tag.getInt("MinX"), tag.getInt("MinZ"), tag.getInt("MaxX"), tag.getInt("MaxZ"),
                tag.getLong("LastBilledAt"));
    }
}
