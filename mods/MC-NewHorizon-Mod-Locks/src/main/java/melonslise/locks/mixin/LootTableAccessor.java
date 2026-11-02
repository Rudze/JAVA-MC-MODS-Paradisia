package melonslise.locks.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * LootTable#pools has no public accessor since the 1.20.5 loot codec rewrite (it used to be mutable and
 * exposed via addPool()). Locks' loot injection (LocksForgeEvents#onLootTableLoad) needs both read and
 * write access to append an extra pool to an already-loaded vanilla/modded table.
 */
@Mixin(LootTable.class)
public interface LootTableAccessor
{
	@Accessor("pools")
	List<LootPool> locks$getPools();

	@Mutable
	@Accessor("pools")
	void locks$setPools(List<LootPool> pools);
}
