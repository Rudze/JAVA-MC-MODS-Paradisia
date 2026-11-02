package tallestred.numismaticoverhaul.loot_stuff;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import tallestred.numismaticoverhaul.currency.CurrencyResolver;
import tallestred.numismaticoverhaul.item.MoneyBagItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class MoneyBagLootModifier extends LootModifier {
    public static final Supplier<MapCodec<MoneyBagLootModifier>> CODEC =  Suppliers.memoize(()
            -> RecordCodecBuilder.mapCodec(inst -> codecStart(inst).and(
            inst.group(
                    Codec.INT.fieldOf("min").forGetter(m -> m.min),
                    Codec.INT.fieldOf("max").forGetter(m -> m.max)
            )).apply(inst, MoneyBagLootModifier::new)
    ));

    private final int min;
    private final int max;

    public MoneyBagLootModifier(LootItemCondition[] conditionsIn, int min, int max) {
        super(conditionsIn);
        this.min = min;
        this.max = max;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (LootItemCondition condition : this.conditions) {
            if (!condition.test(context)) {
                return generatedLoot;
            }
        }
        int value = Mth.nextInt(context.getRandom(), this.min, this.max);
        if (value != 0)
            generatedLoot.add(MoneyBagItem.fromValues(CurrencyResolver.splitValues(value)));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
