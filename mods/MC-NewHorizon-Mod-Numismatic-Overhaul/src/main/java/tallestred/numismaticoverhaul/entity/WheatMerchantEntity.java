package tallestred.numismaticoverhaul.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.Nullable;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.currency.CurrencyHelper;
import tallestred.numismaticoverhaul.economy.CropConfig;
import tallestred.numismaticoverhaul.economy.CropMarket;
import tallestred.numismaticoverhaul.economy.Crops;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A standalone farmer-villager NPC that trades against every crop in {@link Crops#ALL} - each crop
 * backed by its own single, world-wide {@link CropMarket} - buying from and selling to players at that
 * crop's own live price. A *different* {@link EntityType} from vanilla's own villager rather than a
 * flag on one - that sidesteps needing to distinguish "is this a market villager" on every random
 * village villager, and lets this NPC's {@link #updateTrades()} completely replace vanilla's
 * profession-trade-table generation without touching normal villagers at all.
 *
 * <p>Everything else (model, walking/look-at-player AI, job-site/bed memories, breeding, gossip) is
 * inherited from vanilla {@link Villager} untouched - only the trade generation and post-trade
 * bookkeeping are overridden, so this still behaves and looks like a real villager, just one that
 * always sells/buys its whole crop lineup at live prices instead of a random profession trade table.
 *
 * <p>This entity holds no economy state of its own (see {@link CropMarket} for why) - it's purely a
 * trading front-end: {@link #updateTrades()} reads each crop market's current price, {@link #notifyTrade}
 * figures out which crop and direction a completed trade was, tells that crop's market, and lets it
 * recompute.
 *
 * <p>All offers are single-use ({@code maxUses = 1}): after every individual trade, that crop's market
 * stock moves by exactly 1, and {@link #updateTrades()} rebuilds every offer from the new prices - then
 * the refreshed offers are pushed to the trading player immediately (mirroring
 * {@code ShopMerchant#notifyTrade}) so the open trade screen reflects the new price on the very next
 * trade instead of letting several trades go through at one stale price.
 *
 * <p><b>Never a baby.</b> {@code Villager#mobInteract} checks {@code isBaby()} *before* it ever looks
 * at offers, and short-circuits straight to {@code InteractionResult.sidedSuccess(isClientSide)} -
 * {@code CONSUME} on the server - without ever calling {@code startTrading}. {@link AgeableMob}'s own
 * {@code finalizeSpawn} has a ~5% chance of setting a freshly spawned entity's age negative (the
 * "spawn as a baby" roll every animal/villager gets); confirmed via logging that this entity was
 * indeed hitting that branch, which reads exactly like "the shop closes itself instantly" from the
 * player's side - no trade screen ever opens, no error anywhere, just a quiet "unhappy villager"
 * sound. {@link #isBaby()} is overridden to unconditionally report false so this can never happen,
 * regardless of what the underlying age field says.
 */
public class WheatMerchantEntity extends Villager {

    // This NPC's RP identity - shared between its own display name and the dashboard's
    // merchant-selector catalog (via every crop's CropConfig, see Crops), so both sides agree on who
    // this character is without duplicating the name/id in two places.
    public static final String MERCHANT_ID = "wheat_merchant";
    public static final String MERCHANT_NAME = "Thomas Miller";
    public static final String MERCHANT_PORTRAIT = "portraits/thomas_miller.png";

    // Built once - used by notifyTrade to figure out which crop a completed trade belongs to.
    private static final Map<Item, CropConfig> CROPS_BY_ITEM =
            Crops.ALL.stream().collect(Collectors.toMap(CropConfig::item, config -> config));

    public WheatMerchantEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
        forceFarmerProfession();
        applyIdentity();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        forceFarmerProfession();
        applyIdentity();
        this.setAge(0);
        return data;
    }

    private void applyIdentity() {
        this.setCustomName(Component.literal(MERCHANT_NAME));
        this.setCustomNameVisible(true);
    }

    @Override
    public boolean isBaby() {
        return false;
    }

    // Fixed at 1 forever - see getVillagerData() below. No profession-tier progression for this NPC.
    private static final int FIXED_LEVEL = 1;

    /**
     * Proactively re-asserting FARMER once per tick (before vs. after {@code super.tick()}) was tried
     * and, per logging, did NOT stop {@code customServerAiStep}'s NONE-check from firing anyway - so
     * instead of continuing to chase exactly which tick/path resets the underlying stored profession,
     * this intercepts at the *read* instead: every single caller of {@link #getVillagerData()} -
     * vanilla's own NONE-check included - is unconditionally handed a FARMER-profession copy,
     * regardless of what's actually stored in the synced entity data. This can't be defeated by a
     * timing/ordering issue the way the previous tick-based fix could, since it doesn't matter *when*
     * or *why* the underlying data drifted - every read is corrected right at the source.
     *
     * <p>Also pins the level to {@link #FIXED_LEVEL}, the same way: this NPC doesn't use vanilla's
     * profession-tier trade tables at all, so "leveling up" never did anything to its actual offers -
     * it only ever showed a novice/apprentice/etc. progress bar in the trade screen. Removed by making
     * every read report a fixed level, same trick as profession.
     */
    @Override
    public VillagerData getVillagerData() {
        VillagerData data = super.getVillagerData();
        if (data.getProfession() != VillagerProfession.FARMER) data = data.setProfession(VillagerProfession.FARMER);
        if (data.getLevel() != FIXED_LEVEL) data = data.setLevel(FIXED_LEVEL);
        return data;
    }

    /** No level-up progress bar in the trade screen - there's nothing to progress toward anymore. */
    @Override
    public boolean showProgressBar() {
        return false;
    }

    /** Always reads as 0 XP toward a next level that will never come, on top of {@link #showProgressBar()} being off. */
    @Override
    public int getVillagerXp() {
        return 0;
    }

    /** Kept as a second layer so the underlying stored data (NBT, sync packets) also reads FARMER/level 1, not just {@link #getVillagerData()}'s return value. */
    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            forceFarmerProfession();
        }
        super.tick();
    }

    private void forceFarmerProfession() {
        VillagerData current = super.getVillagerData();
        if (current.getProfession() != VillagerProfession.FARMER || current.getLevel() != FIXED_LEVEL) {
            this.setVillagerData(current.setProfession(VillagerProfession.FARMER).setLevel(FIXED_LEVEL));
        }
    }
    // (was logging every correction here while chasing the trade-screen-closing bug - removed once the
    // fix above made it harmless; it still fires basically every tick, which would otherwise flood the
    // log forever for no benefit now that we know it's inert.)

    /** Debug: full stack trace + state so we can see exactly what's calling this and why, if it's still happening. */
    @Override
    protected void stopTrading() {
        NumismaticOverhaul.LOGGER.warn(
                "WheatMerchantEntity#stopTrading() called (storedProfession={}, isBaby={}, tick={})",
                super.getVillagerData().getProfession(), this.isBaby(), this.tickCount,
                new Throwable("stack trace for diagnostics, not a real error")
        );
        super.stopTrading();
    }

    /** Debug: full state at every interaction attempt. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.level().isClientSide) {
            NumismaticOverhaul.LOGGER.info(
                    "WheatMerchantEntity interact: hand={}, isTrading={}, isBaby={}, storedProfession={}, offers.isEmpty={}",
                    hand, this.isTrading(), this.isBaby(), super.getVillagerData().getProfession(), this.getOffers().isEmpty()
            );
        }
        InteractionResult result = super.mobInteract(player, hand);
        if (!this.level().isClientSide) {
            NumismaticOverhaul.LOGGER.info("WheatMerchantEntity interact result: {}", result);
        }
        return result;
    }

    @Override
    protected void updateTrades() {
        try {
            MerchantOffers newOffers = new MerchantOffers();
            for (CropConfig crop : Crops.ALL) {
                CropMarket market = CropMarket.get((ServerLevel) this.level(), crop);
                int stock = market.getStock();

                // Both offers always exist per crop - out-of-stock is shown the same way vanilla shows
                // any exhausted trade (greyed out, red X in the slot), via MerchantOffer#setToOutOfStock(),
                // rather than the offer disappearing from the list entirely.
                //
                // Buy and sell deliberately use different prices (CropMarket#buyPrice/#sellPrice, not a
                // single shared currentPrice()) - see CropMarket's class javadoc for why using the same
                // number for both let players mint free currency by immediately selling back what they
                // just bought.
                ItemCost cost = CurrencyHelper.getClosestTradeItem(market.buyPrice());
                ItemStack cropStack = new ItemStack(crop.item());
                cropStack.set(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal("Stock: " + stock).withStyle(ChatFormatting.GRAY)
                )));
                MerchantOffer sellOffer = new MerchantOffer(cost, cropStack, 1, 1, 0.0F);
                if (stock <= 0) sellOffer.setToOutOfStock();
                newOffers.add(sellOffer);

                ItemCost cropCost = new ItemCost(crop.item(), 1);
                ItemStack payment = CurrencyHelper.getClosest(market.sellPrice());
                MerchantOffer buyOffer = new MerchantOffer(cropCost, payment, 1, 1, 0.0F);
                if (stock >= CropMarket.MAX_STOCK) buyOffer.setToOutOfStock();
                newOffers.add(buyOffer);
            }
            this.offers = newOffers;
        } catch (Exception e) {
            // Never leave `this.offers` null/empty on a failure - an empty offer list is exactly what
            // makes vanilla treat this villager as having "nothing to trade" and silently refuse to
            // open the screen at all (no error shown to the player), which is indistinguishable from
            // this bug report without this log line.
            NumismaticOverhaul.LOGGER.error("Failed to update WheatMerchantEntity trades", e);
            if (this.offers == null) this.offers = new MerchantOffers();
        }
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        super.notifyTrade(offer);

        // The result item tells us the crop when the player bought one (result = crop); otherwise the
        // player sold a crop, and the cost item (what they paid with) is that crop instead.
        CropConfig traded = CROPS_BY_ITEM.get(offer.getResult().getItem());
        boolean playerBoughtCrop = traded != null;
        if (!playerBoughtCrop) {
            traded = CROPS_BY_ITEM.get(offer.getCostA().getItem());
        }
        if (traded == null) {
            NumismaticOverhaul.LOGGER.warn("WheatMerchantEntity completed a trade that matches no known crop: {}", offer);
            return;
        }

        CropMarket market = CropMarket.get((ServerLevel) this.level(), traded);
        if (playerBoughtCrop) {
            market.onPlayerBought(this.level().getGameTime());
        } else {
            market.onPlayerSold(this.level().getGameTime());
        }

        updateTrades();

        if (this.getTradingPlayer() instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundMerchantOffersPacket(
                    serverPlayer.containerMenu.containerId, this.getOffers(), 0, 0, false, false
            ));
        }
    }
}
