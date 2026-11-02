package com.rudze.quests.tax;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Coins paid to Professor Chen, waiting for the mayor to collect them. Kept as a value (Numismatic
 * raw units) rather than item stacks; it is paid back out as the fewest coins possible.
 */
public final class Treasury extends SavedData {

    private static final String DATA_NAME = "nh_quests_treasury";
    private static final SavedData.Factory<Treasury> FACTORY = new SavedData.Factory<>(Treasury::new, Treasury::load, null);

    private long balance;

    public static Treasury get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public void deposit(long amount) {
        balance += amount;
        setDirty();
    }

    public long withdrawAll() {
        long amount = balance;
        balance = 0;
        setDirty();
        return amount;
    }

    private static Treasury load(CompoundTag tag, HolderLookup.Provider provider) {
        Treasury treasury = new Treasury();
        treasury.balance = tag.getLong("Balance");
        return treasury;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putLong("Balance", balance);
        return tag;
    }
}
