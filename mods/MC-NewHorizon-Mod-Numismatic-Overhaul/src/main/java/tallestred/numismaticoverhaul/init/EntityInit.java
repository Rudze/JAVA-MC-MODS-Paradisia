package tallestred.numismaticoverhaul.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import tallestred.numismaticoverhaul.entity.CosmeticsMerchantEntity;
import tallestred.numismaticoverhaul.entity.WheatMerchantEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static tallestred.numismaticoverhaul.NumismaticOverhaul.MODID;

@EventBusSubscriber(modid = MODID)
public class EntityInit {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MODID);
    private static final List<AttributesRegister<?>> attributeSuppliers = new ArrayList<>();

    // Same base size/attributes as vanilla's own villager (createAttributes()) - only the trade
    // generation differs (see WheatMerchantEntity), not the mob itself.
    public static final DeferredHolder<EntityType<?>, EntityType<WheatMerchantEntity>> WHEAT_MERCHANT = registerEntity(
            "wheat_merchant",
            () -> EntityType.Builder.of(WheatMerchantEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10),
            Villager::createAttributes
    );

    // A separate entity type from WHEAT_MERCHANT (own registry id, own trade set) even though it
    // shares the same Villager base and wears the Shepherd profession skin - see CosmeticsMerchantEntity's
    // javadoc for why this can't just be a modification of vanilla Shepherd trades.
    public static final DeferredHolder<EntityType<?>, EntityType<CosmeticsMerchantEntity>> COSMETICS_MERCHANT = registerEntity(
            "cosmetics_merchant",
            () -> EntityType.Builder.of(CosmeticsMerchantEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10),
            Villager::createAttributes
    );

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> registerEntity(String name, Supplier<EntityType.Builder<T>> supplier) {
        return ENTITIES.register(name, () -> supplier.get().build(MODID + ":" + name));
    }

    private static <T extends LivingEntity> DeferredHolder<EntityType<?>, EntityType<T>> registerEntity(String name, Supplier<EntityType.Builder<T>> supplier,
                                                                                                        Supplier<AttributeSupplier.Builder> attributeSupplier) {
        DeferredHolder<EntityType<?>, EntityType<T>> entityTypeSupplier = registerEntity(name, supplier);
        attributeSuppliers.add(new AttributesRegister<>(entityTypeSupplier, attributeSupplier));
        return entityTypeSupplier;
    }

    @SubscribeEvent
    public static void attribs(EntityAttributeCreationEvent e) {
        attributeSuppliers.forEach(p -> e.put(p.entityTypeSupplier.get(), p.factory.get().build()));
    }

    private record AttributesRegister<E extends LivingEntity>(Supplier<EntityType<E>> entityTypeSupplier,
                                                              Supplier<AttributeSupplier.Builder> factory) {
    }
}
