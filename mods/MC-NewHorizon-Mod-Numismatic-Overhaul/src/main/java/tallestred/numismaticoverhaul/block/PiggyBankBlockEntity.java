package tallestred.numismaticoverhaul.block;

import io.wispforest.owo.util.ImplementedInventory;
import net.minecraft.core.HolderLookup;
import tallestred.numismaticoverhaul.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class PiggyBankBlockEntity extends BlockEntity implements MenuProvider {

    private final NonNullList<ItemStack> inventory = NonNullList.withSize(3, ItemStack.EMPTY);

    public PiggyBankBlockEntity(BlockPos pos, BlockState state) {
        super(BlockInit.PIGGY_BANK_BE.get(), pos, state);
    }

    @Override
    protected void loadAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
        this.inventory.clear();
        ContainerHelper.loadAllItems(nbt, this.inventory, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag nbt, HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
        ContainerHelper.saveAllItems(nbt, this.inventory, registries);
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    public NonNullList<ItemStack> inventory() {
        return this.inventory;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new PiggyBankScreenHandler(
                syncId,
                player.getInventory(),
                ContainerLevelAccess.create(this.level, this.worldPosition),
                (ImplementedInventory) () -> PiggyBankBlockEntity.this.inventory
        );
    }
}
