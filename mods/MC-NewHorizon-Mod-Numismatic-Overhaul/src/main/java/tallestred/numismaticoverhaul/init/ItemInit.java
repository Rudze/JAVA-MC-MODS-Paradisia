package tallestred.numismaticoverhaul.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.currency.Currency;
import tallestred.numismaticoverhaul.item.CoinItem;
import tallestred.numismaticoverhaul.item.MoneyBagItem;
import tallestred.numismaticoverhaul.item.data_components.MoneyBagComponent;

import static tallestred.numismaticoverhaul.init.CreativeTabInit.addToTab;
import static tallestred.numismaticoverhaul.init.ItemComponentInit.MONEY_BAG_COMPONENT;

public class ItemInit {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, NumismaticOverhaul.MODID);
    public static final DeferredHolder<Item, ? extends Item> BRONZE_COIN = addToTab(ITEMS.register("bronze_coin", () -> new CoinItem(Currency.BRONZE)));
    public static final DeferredHolder<Item, ? extends Item> SILVER_COIN = addToTab(ITEMS.register("silver_coin", () -> new CoinItem(Currency.SILVER)));
    public static final DeferredHolder<Item, ? extends Item> GOLD_COIN = addToTab(ITEMS.register("gold_coin", () -> new CoinItem(Currency.GOLD)));
    public static final DeferredHolder<Item, ? extends Item>  MONEY_BAG = addToTab(ITEMS.register("money_bag", () -> new MoneyBagItem(new Item.Properties().stacksTo(1).component(MONEY_BAG_COMPONENT, MoneyBagComponent.of(0)))));
    // Wheat/straw-yellow colors, matching the farmer profession's own palette.
    public static final DeferredHolder<Item, ? extends Item> WHEAT_MERCHANT_SPAWN_EGG = addToTab(ITEMS.register("wheat_merchant_spawn_egg",
            () -> new SpawnEggItem(EntityInit.WHEAT_MERCHANT.get(), 0xC9A55C, 0x5C8C3E, new Item.Properties())));
    // Wool-white/shepherd-brown, matching the shepherd profession's own palette.
    public static final DeferredHolder<Item, ? extends Item> COSMETICS_MERCHANT_SPAWN_EGG = addToTab(ITEMS.register("cosmetics_merchant_spawn_egg",
            () -> new SpawnEggItem(EntityInit.COSMETICS_MERCHANT.get(), 0xF5F5F0, 0x6B4A2E, new Item.Properties())));
}