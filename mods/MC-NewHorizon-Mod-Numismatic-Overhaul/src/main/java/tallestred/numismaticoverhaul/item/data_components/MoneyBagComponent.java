package tallestred.numismaticoverhaul.item.data_components;

import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.item.ItemStack;
import tallestred.numismaticoverhaul.currency.CurrencyResolver;

import static tallestred.numismaticoverhaul.init.ItemComponentInit.MONEY_BAG_COMPONENT;

public record MoneyBagComponent(long bronze, long silver, long gold) {

    public static final StructEndec<MoneyBagComponent> ENDEC = StructEndecBuilder.of(
            StructEndec.LONG.fieldOf("bronze", MoneyBagComponent::bronze),
            StructEndec.LONG.fieldOf("silver", MoneyBagComponent::silver),
            StructEndec.LONG.fieldOf("gold", MoneyBagComponent::gold),
            MoneyBagComponent::new
    );

    public static MoneyBagComponent of(long[] values) {
        return MoneyBagComponent.of(
                values[0],
                values[1],
                values[2]
        );
    }

    public static MoneyBagComponent of(long value) {
        var money = CurrencyResolver.splitValues(value);
        return new MoneyBagComponent(money[0], money[1], money[2]);
    }

    public static MoneyBagComponent of(long bronze, long silver, long gold) {
        return new MoneyBagComponent(bronze, silver, gold);
    }

    public static MoneyBagComponent combine(long[] money, long[] money2) {
        return new MoneyBagComponent(
                money[0] + money2[0],
                money[1] + money2[1],
                money[2] + money2[2]
        );
    }

    public static MoneyBagComponent combine(ItemStack stack1, ItemStack stack2) {
        var values1 = stack1.getOrDefault(MONEY_BAG_COMPONENT, of(0));
        var values2 = stack2.getOrDefault(MONEY_BAG_COMPONENT, of(0));
        return new MoneyBagComponent(
                values1.bronze + values2.bronze,
                values1.silver + values2.silver,
                values1.gold + values2.gold
        );
    }

    public long value() {
        return CurrencyResolver.combineValues(bronze, silver, gold);
    }
}