package fr.rudze.furniture.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Points a {@link FurniturePartBlock} cell back at the {@link FurnitureBlock} that owns it, so
 * breaking a part can be forwarded to (and cascade the removal of) the master.
 */
public class FurniturePartBlockEntity extends BlockEntity {

    private static final String TAG_MASTER = "Master";

    @Nullable
    private BlockPos master;

    public FurniturePartBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNITURE_PART.get(), pos, state);
    }

    @Nullable
    public BlockPos getMaster() {
        return master;
    }

    public void setMaster(BlockPos master) {
        this.master = master;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (master != null) {
            tag.putLong(TAG_MASTER, master.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        master = tag.contains(TAG_MASTER) ? BlockPos.of(tag.getLong(TAG_MASTER)) : null;
    }
}
