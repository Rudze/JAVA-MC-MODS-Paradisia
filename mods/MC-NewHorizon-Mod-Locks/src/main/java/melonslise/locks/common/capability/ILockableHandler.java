package melonslise.locks.common.capability;

import java.util.List;
import java.util.Observer;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import melonslise.locks.common.util.Lockable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;

public interface ILockableHandler extends Observer
{
	int nextId();

	void initIds();

	Int2ObjectMap<Lockable> getLoaded();

	List<Lockable> snapshotLoaded();

	Int2ObjectMap<Lockable> getInChunk(BlockPos pos);

	boolean add(Lockable lkb);

	void addDirect(Lockable lkb);

	boolean remove(int id);

	void registerChunkStorage(LevelChunk chunk, ILockableStorage storage, boolean sync);

	void unregisterChunkStorage(int chX, int chZ, List<Lockable> present);

	void markDirty(Lockable lkb);
}
