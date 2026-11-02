package fr.rudze.furniture.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * Its main purpose is giving the client something to hang
 * {@link fr.rudze.furniture.client.FurnitureBlockEntityRenderer} off of for dynamically-rendered
 * furniture (see {@link fr.rudze.furniture.furniture.FurnitureDefinition#render()}). It also
 * remembers the dye color the placed item had (if any), so a dyed piece keeps showing that color
 * once placed instead of reverting to its undyed default - the renderer reads this back to build
 * the {@code ItemStack} it draws every frame, since a fresh stack has no memory of what was dyed.
 */
public class FurnitureBlockEntity extends BlockEntity {

    @Nullable
    private DyedItemColor dyedColor;

    public FurnitureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNITURE_DISPLAY.get(), pos, state);
    }

    public void setDyedColor(@Nullable DyedItemColor dyedColor) {
        this.dyedColor = dyedColor;
        setChanged();
    }

    @Nullable
    public DyedItemColor getDyedColor() {
        return dyedColor;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (dyedColor != null) {
            tag.putInt("DyedColor", dyedColor.rgb());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        dyedColor = tag.contains("DyedColor") ? new DyedItemColor(tag.getInt("DyedColor"), true) : null;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
