package com.rudze.worldbuild.claim;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Every world_build claim plus each player's outstanding land tax. Stored on the overworld (like
 * the waystone network) so wiping world_build's own folder doesn't silently forgive debts.
 *
 * <p>Debts are in Numismatic Overhaul's raw value unit (see {@link Coins}) and can go negative,
 * which simply means the player paid ahead.
 */
public final class ClaimData extends SavedData {

    private static final String DATA_NAME = "nh_worldbuild_claims";

    /** One gold coin per claimed surface block per week. */
    public static final long TAX_PER_BLOCK_PER_WEEK = Coins.GOLD;
    public static final long BILLING_PERIOD_MS = 7L * 24 * 60 * 60 * 1000;

    public static final SavedData.Factory<ClaimData> FACTORY =
            new SavedData.Factory<>(ClaimData::new, ClaimData::load, null);

    private final Map<Integer, Claim> claims = new LinkedHashMap<>();
    private final Map<UUID, Long> debts = new HashMap<>();
    private int nextId = 1;

    public static ClaimData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public List<Claim> claims() {
        return List.copyOf(claims.values());
    }

    public Optional<Claim> claimAt(BlockPos pos) {
        return claims.values().stream().filter(c -> c.contains(pos)).findFirst();
    }

    @Nullable
    public Claim firstOverlap(Claim candidate) {
        return claims.values().stream().filter(c -> c.overlaps(candidate)).findFirst().orElse(null);
    }

    /** @return the stored claim, or empty if it would overlap an existing one */
    public Optional<Claim> add(UUID owner, String ownerName, BlockPos a, BlockPos b, long now) {
        Claim claim = Claim.of(nextId, owner, ownerName, a, b, now);
        if (firstOverlap(claim) != null) {
            return Optional.empty();
        }
        claims.put(claim.id(), claim);
        nextId++;
        setDirty();
        return Optional.of(claim);
    }

    @Nullable
    public Claim remove(int id) {
        Claim removed = claims.remove(id);
        if (removed != null) {
            setDirty();
        }
        return removed;
    }

    public long debt(UUID player) {
        return debts.getOrDefault(player, 0L);
    }

    public void pay(UUID player, long amount) {
        if (amount == 0) {
            return;
        }
        debts.put(player, debt(player) - amount);
        setDirty();
    }

    public long weeklyTax(UUID player) {
        return claims.values().stream().filter(c -> c.owner().equals(player)).mapToLong(Claim::weeklyTax).sum();
    }

    /**
     * Adds a week of tax to each owner for every full billing period elapsed since a claim was
     * last billed. Loops per claim so a server that was offline for several weeks catches up.
     *
     * @return the amount newly billed per owner, for notifying them
     */
    public Map<UUID, Long> billDue(long now) {
        Map<UUID, Long> billed = new HashMap<>();
        for (Claim claim : List.copyOf(claims.values())) {
            long lastBilled = claim.lastBilledAt();
            long weeks = 0;
            while (now - lastBilled >= BILLING_PERIOD_MS) {
                lastBilled += BILLING_PERIOD_MS;
                weeks++;
            }
            if (weeks == 0) {
                continue;
            }
            long amount = weeks * claim.weeklyTax();
            debts.merge(claim.owner(), amount, Long::sum);
            billed.merge(claim.owner(), amount, Long::sum);
            claims.put(claim.id(), claim.withLastBilledAt(lastBilled));
        }
        if (!billed.isEmpty()) {
            setDirty();
        }
        return billed;
    }

    private static ClaimData load(CompoundTag tag, HolderLookup.Provider provider) {
        ClaimData data = new ClaimData();
        data.nextId = Math.max(1, tag.getInt("NextId"));
        ListTag claimList = tag.getList("Claims", Tag.TAG_COMPOUND);
        for (int i = 0; i < claimList.size(); i++) {
            Claim claim = Claim.load(claimList.getCompound(i));
            data.claims.put(claim.id(), claim);
        }
        ListTag debtList = tag.getList("Debts", Tag.TAG_COMPOUND);
        for (int i = 0; i < debtList.size(); i++) {
            CompoundTag entry = debtList.getCompound(i);
            data.debts.put(entry.getUUID("Player"), entry.getLong("Amount"));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putInt("NextId", nextId);
        ListTag claimList = new ListTag();
        for (Claim claim : claims.values()) {
            claimList.add(claim.save());
        }
        tag.put("Claims", claimList);
        ListTag debtList = new ListTag();
        for (Map.Entry<UUID, Long> entry : new ArrayList<>(debts.entrySet())) {
            CompoundTag debt = new CompoundTag();
            debt.putUUID("Player", entry.getKey());
            debt.putLong("Amount", entry.getValue());
            debtList.add(debt);
        }
        tag.put("Debts", debtList);
        return tag;
    }
}
