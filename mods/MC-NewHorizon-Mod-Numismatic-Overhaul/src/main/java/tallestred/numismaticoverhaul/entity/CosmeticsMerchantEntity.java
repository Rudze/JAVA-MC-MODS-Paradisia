package tallestred.numismaticoverhaul.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.currency.Currency;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;

/**
 * A dedicated NPC (not a modification of vanilla's own Shepherd trades - a whole separate
 * {@link EntityType}, same reasoning as {@link WheatMerchantEntity}) that sells cosmetic accessories
 * ({@link CosmeticItems}) from the Cosmetics mod at fixed, deliberately steep prices - these are vanity
 * purchases, not a supply/demand commodity, so unlike {@code CropMarket} there's no live stock/price
 * curve here: {@link #updateTrades()} builds the same fixed-price offers every time, and vanilla's own
 * villager restock behavior (sleeping at a bed) refills {@code maxUses} over time exactly like any other
 * trade - no custom bookkeeping needed.
 *
 * <p>Wears the Shepherd profession skin purely for the wool/textile-merchant look (matches the
 * dashboard portrait already prepared under the {@code shepherd_merchant} catalog id) - it does not
 * touch or share trades with actual vanilla Shepherd villagers in the world, since this is a fully
 * separate entity type with its own hardcoded offers.
 *
 * <p>Carries over the same defensive overrides {@link WheatMerchantEntity} needed (never a baby, profession/level
 * pinned via {@link #getVillagerData()}) since both extend vanilla {@link Villager} and are subject to
 * the exact same underlying quirks (see {@link WheatMerchantEntity}'s javadoc for the full story).
 */
public class CosmeticsMerchantEntity extends Villager {

    public static final String MERCHANT_ID = "shepherd_merchant";
    public static final String MERCHANT_NAME = "Julien Berger";
    public static final String MERCHANT_PORTRAIT = "portraits/shepherd_merchant.png";

    private static final int FIXED_LEVEL = 1;

    public CosmeticsMerchantEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        forceShepherdProfession();
        applyIdentity();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        forceShepherdProfession();
        applyIdentity();
        this.setAge(0);
        return data;
    }

    private void applyIdentity() {
        this.setCustomName(Component.literal(MERCHANT_NAME));
        this.setCustomNameVisible(true);
    }

    // See WheatMerchantEntity's javadoc: AgeableMob#finalizeSpawn has a ~5% chance of spawning any
    // villager as a baby, which makes mobInteract short-circuit before ever offering a trade.
    @Override
    public boolean isBaby() {
        return false;
    }

    // Same read-side pinning trick as WheatMerchantEntity - proactive tick-based correction wasn't
    // reliable, so every read of getVillagerData() (vanilla's own included) is unconditionally handed
    // a SHEPHERD/level-1 copy regardless of what's actually stored.
    @Override
    public VillagerData getVillagerData() {
        VillagerData data = super.getVillagerData();
        if (data.getProfession() != VillagerProfession.SHEPHERD) data = data.setProfession(VillagerProfession.SHEPHERD);
        if (data.getLevel() != FIXED_LEVEL) data = data.setLevel(FIXED_LEVEL);
        return data;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            forceShepherdProfession();
        }
        super.tick();
    }

    private void forceShepherdProfession() {
        VillagerData current = super.getVillagerData();
        if (current.getProfession() != VillagerProfession.SHEPHERD || current.getLevel() != FIXED_LEVEL) {
            this.setVillagerData(current.setProfession(VillagerProfession.SHEPHERD).setLevel(FIXED_LEVEL));
        }
    }

    @Override
    protected void updateTrades() {
        try {
            MerchantOffers newOffers = new MerchantOffers();
            addIfPresent(newOffers, "nh_cosmetics:diving_goggles", 250, 3);
            addIfPresent(newOffers, "nh_cosmetics:scuba_tank", 400, 3);
            addIfPresent(newOffers, "nh_cosmetics:toy_bubble_gun", 150, 3);
            if (newOffers.isEmpty()) {
                NumismaticOverhaul.LOGGER.warn("CosmeticsMerchantEntity found none of its cosmetic items - is nh_cosmetics installed?");
            }
            this.offers = newOffers;
        } catch (Exception e) {
            NumismaticOverhaul.LOGGER.error("Failed to update CosmeticsMerchantEntity trades", e);
            if (this.offers == null) this.offers = new MerchantOffers();
        }
    }

    // nh_cosmetics is a soft dependency, not a compile-time one - looked up by registry name at
    // runtime and simply skipped (not crashed) if that mod isn't installed, same reasoning as
    // Crops#cropIfPresent for Farmer's Delight.
    private static void addIfPresent(MerchantOffers offers, String itemId, long priceSilver, int maxUses) {
        Item item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(itemId)).orElse(null);
        if (item == null) return;
        ItemCost cost = CurrencyHelper.getClosestTradeItem(Currency.SILVER.getRawValue(priceSilver));
        offers.add(new MerchantOffer(cost, new ItemStack(item), maxUses, 10, 0.0F));
    }
}
