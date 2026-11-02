package melonslise.locks.common.capability;

import melonslise.locks.Locks;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

/*
 * Per-level persistent storage for the lockable id counter.
 *
 * The LockableHandler is attached as a Level attachment and is not itself persisted (its own state is either
 * derivable from each chunk's LockableStorage on load, or - for the id counter - explicitly backed by this
 * SavedData instead). Persisting the counter here keeps lock ids globally unique across restarts: without it,
 * newly placed locks after a restart would reuse ids belonging to already-saved (but unloaded) locks, and on
 * reload the by-id merge would make the old lock adopt the new lock's object/position and visually vanish.
 */
public class LocksSavedData extends SavedData
{
	public static final String NAME = Locks.ID + "_lockables";
	private static final String KEY_LAST_ID = "LastId";

	private int lastId;

	public LocksSavedData() {}

	public static SavedData.Factory<LocksSavedData> factory()
	{
		return new SavedData.Factory<>(LocksSavedData::new, LocksSavedData::load, DataFixTypes.LEVEL);
	}

	public static LocksSavedData load(CompoundTag nbt, HolderLookup.Provider registries)
	{
		LocksSavedData data = new LocksSavedData();
		data.lastId = nbt.getInt(KEY_LAST_ID);
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider registries)
	{
		nbt.putInt(KEY_LAST_ID, this.lastId);
		return nbt;
	}

	public int getLastId()
	{
		return this.lastId;
	}

	public void setLastId(int id)
	{
		this.lastId = id;
	}
}
