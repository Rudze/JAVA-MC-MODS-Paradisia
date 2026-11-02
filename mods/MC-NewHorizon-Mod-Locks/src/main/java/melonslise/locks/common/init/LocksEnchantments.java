package melonslise.locks.common.init;

import melonslise.locks.Locks;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/*
 * Enchantment stopped being a subclassable Java type in 1.21 - it's `final` now, and every enchantment
 * (vanilla or modded) is a DATA-DRIVEN registry entry instead: definitions live under
 * data/locks/enchantment/*.json (mirroring the 12 Java Enchantment subclasses this used to be - see git
 * history), keyed here as plain ResourceKeys so call sites can still refer to "the Shocking enchantment"
 * by a stable Java constant.
 *
 * Unlike the old DeferredRegister<Enchantment> + RegistryObject<Enchantment>.get(), a ResourceKey isn't
 * itself the data - enchantments are a *datapack* registry (populated per-world from JSON, not at mod
 * construction time), so getting the actual Holder<Enchantment> (which EnchantmentHelper's lookup/level
 * methods want) needs a RegistryAccess, resolved via #get(RegistryAccess, ResourceKey) below.
 *
 * NOT YET PORTED: the old per-enchantment isDiscoverable()/isTradeable()/isAllowedOnBooks() config gates
 * (LocksServerConfig.ENABLE_*) have no equivalent hook on this data-driven Enchantment - there's no
 * Java override to intercept anymore. Re-implementing that toggle needs a different mechanism (most
 * likely filtering these enchantments out of loot/trade/enchanting-table generation at the point those
 * are generated, gated on the same config values) and hasn't been done yet; right now every enchantment
 * here is always fully enabled regardless of its config toggle.
 */
public final class LocksEnchantments
{
	public static final ResourceKey<Enchantment> SHOCKING = key("shocking");
	public static final ResourceKey<Enchantment> STURDY = key("sturdy");
	public static final ResourceKey<Enchantment> COMPLEXITY = key("complexity");
	public static final ResourceKey<Enchantment> SILENT = key("silent");
	public static final ResourceKey<Enchantment> AUTO_PICK = key("auto_pick");
	public static final ResourceKey<Enchantment> REINFORCED = key("reinforced");
	public static final ResourceKey<Enchantment> AWARENESS = key("awareness");
	public static final ResourceKey<Enchantment> FINESSE = key("finesse");
	public static final ResourceKey<Enchantment> ATTUNEMENT = key("attunement");
	public static final ResourceKey<Enchantment> GROUNDED = key("grounded");
	public static final ResourceKey<Enchantment> QUIET_HAND = key("quiet_hand");
	public static final ResourceKey<Enchantment> LAST_CATCH = key("last_catch");

	private LocksEnchantments() {}

	private static ResourceKey<Enchantment> key(String name)
	{
		return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(Locks.ID, name));
	}

	public static Holder<Enchantment> get(RegistryAccess registries, ResourceKey<Enchantment> key)
	{
		return registries.registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
	}

	/** Shorthand for the extremely common EnchantmentHelper.getItemEnchantmentLevel(get(registries, key), stack). */
	public static int getLevel(RegistryAccess registries, ResourceKey<Enchantment> key, ItemStack stack)
	{
		return EnchantmentHelper.getItemEnchantmentLevel(get(registries, key), stack);
	}
}
