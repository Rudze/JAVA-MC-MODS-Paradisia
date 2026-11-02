package tallestred.numismaticoverhaul.init;

import com.mojang.serialization.Codec;
import io.wispforest.owo.serialization.CodecUtils;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tallestred.numismaticoverhaul.NumismaticOverhaul;
import tallestred.numismaticoverhaul.item.data_components.MoneyBagComponent;

public class ItemComponentInit {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NumismaticOverhaul.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<MoneyBagComponent>> MONEY_BAG_COMPONENT = DATA_COMPONENTS.register("value", () -> DataComponentType.<MoneyBagComponent>builder()
            .persistent(CodecUtils.toCodec(MoneyBagComponent.ENDEC))
            .networkSynchronized(CodecUtils.toPacketCodec(MoneyBagComponent.ENDEC))
            .build());
}
