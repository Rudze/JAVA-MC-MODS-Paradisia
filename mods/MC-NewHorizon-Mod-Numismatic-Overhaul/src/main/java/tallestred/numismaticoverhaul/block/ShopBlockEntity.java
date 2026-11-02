package tallestred.numismaticoverhaul.block;

import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.owo.ops.WorldOps;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.util.ImplementedInventory;
import net.minecraft.core.HolderLookup;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class ShopBlockEntity extends BlockEntity implements ImplementedInventory, WorldlyContainer, MenuProvider {

    private static final int[] SLOTS = IntStream.range(0, 27).toArray();
    private static final int[] NO_SLOTS = new int[0];
    private final NonNullList<ItemStack> INVENTORY = NonNullList.withSize(27, ItemStack.EMPTY);
    public static KeyedEndec<List<ShopOffer>> OFFERS_LIST = ShopOffer.ENDEC.listOf().keyed("offers", ArrayList::new);
    private final Merchant merchant;
    private List<ShopOffer> offers;
    private long storedCurrency;
    private UUID owner;
    public boolean allowsTransfer = false;
    private int tradeIndex;

    public ShopBlockEntity(BlockPos pos, BlockState state) {
        super(BlockInit.SHOP_BE.get(), pos, state);

        boolean inexhaustible = (state.getBlock() instanceof ShopBlock shop) && shop.inexhaustible();
        this.merchant = new ShopMerchant(this, inexhaustible);

        this.offers = new ArrayList<>();
        this.storedCurrency = 0;
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return INVENTORY;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return allowsTransfer ? SLOTS : NO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction dir) {
        return allowsTransfer;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction dir) {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.numismaticoverhaul.shop.inventory_title");
    }

    @NotNull
    public Merchant getMerchant() {
        return merchant;
    }

    public List<ShopOffer> getOffers() {
        return offers;
    }

    public long getStoredCurrency() {
        return storedCurrency;
    }

    public boolean isTransferEnabled() {
        return allowsTransfer;
    }

    public void toggleTransfer() {
        this.allowsTransfer = !this.allowsTransfer;
    }

    public void setStoredCurrency(long storedCurrency) {
        this.storedCurrency = storedCurrency;
        setChanged();
    }

    public void addCurrency(long value) {
        this.storedCurrency += value;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, INVENTORY, registries);
        tag.put(SerializationContext.attributes(RegistriesAttribute.of(this.getLevel().registryAccess())), OFFERS_LIST, offers);
        tag.putBoolean("AllowsTransfer", this.allowsTransfer);
        tag.putLong("StoredCurrency", storedCurrency);
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, INVENTORY, registries);
        this.offers = tag.get(OFFERS_LIST);
        if (tag.contains("Owner")) {
            owner = tag.getUUID("Owner");
        }
        this.allowsTransfer = tag.getBoolean("AllowsTransfer");
        this.storedCurrency = tag.getLong("StoredCurrency");
    }

    public void addOrReplaceOffer(ShopOffer offer) {

        int indexToReplace = -1;

        for (int i = 0; i < offers.size(); i++) {
            if (!ItemStack.matches(offer.getSellStack(), offers.get(i).getSellStack())) continue;
            indexToReplace = i;
            break;
        }

        if (indexToReplace == -1) {
            if (offers.size() >= 24) {
                NumismaticOverhaul.LOGGER.error("Tried adding more than 24 trades to shop at {}", this.worldPosition);
                return;
            }
            offers.add(offer);
        } else {
            offers.set(indexToReplace, offer);
        }

        this.setChanged();
    }

    public void deleteOffer(ItemStack stack) {
        if (!offers.removeIf(offer -> ItemStack.matches(stack, offer.getSellStack()))) {
            NumismaticOverhaul.LOGGER.error("Tried to delete invalid trade for {} from shop at {}", stack, this.worldPosition);
            return;
        }

        this.setChanged();
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ShopBlockEntity blockEntity) {
        blockEntity.tick();
    }

    public void tick() {
        if (level.getGameTime() % 60 == 0) tradeIndex++;
    }

    @OnlyIn(Dist.CLIENT)
    public ItemStack getItemToRender() {
        if (tradeIndex > offers.size() - 1) tradeIndex = 0;
        return offers.get(tradeIndex).getSellStack();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player player) {
        return new ShopScreenHandler(syncId, inv, this);
    }


    @Override
    public boolean stillValid(Player player) {
        return player.getUUID().equals(this.owner);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        tag.remove("Items");
        tag.remove("StoredCurrency");
        return tag;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        WorldOps.updateIfOnServer(level, worldPosition);
    }
}
