package melonslise.locks.common.util;

import java.util.Random;
import java.util.stream.Stream;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.util.RandomSource;

import melonslise.locks.common.config.LocksConfig;
import melonslise.locks.common.init.LocksAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;

public final class LocksUtil
{
	// ItemStack#ATTRIBUTE_MODIFIER_FORMAT was removed; this reproduces its exact "#.##" pattern for tooltips.
	public static final java.text.DecimalFormat TOOLTIP_NUMBER_FORMAT = new java.text.DecimalFormat("#.##");

	private LocksUtil() {}

	/**
	 * Resolves the currently-active HolderLookup.Provider for item (de)serialization from code with no
	 * Level in scope (e.g. capability factories). Prefers the client's synced registries when running on the
	 * client (covers both a remote dedicated server and the client side of an integrated server), falling
	 * back to the dedicated/integrated server's registries otherwise, and RegistryAccess.EMPTY only if neither
	 * is currently available (e.g. between disconnect and the next connection).
	 */
	public static HolderLookup.Provider currentRegistries()
	{
		if (FMLEnvironment.dist.isClient())
		{
			net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
			if (level != null)
				return level.registryAccess();
		}
		net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		return server != null ? server.registryAccess() : RegistryAccess.EMPTY;
	}

	public static void shuffle(byte[] array, Random rng)
	{
		for (int a = array.length - 1; a > 0; --a)
		{
			int index = rng.nextInt(a + 1);
			byte temp = array[index];
			array[index] = array[a];
			array[a] = temp;
		}
	}

	public static boolean chance(RandomSource rng, double ch)
	{
		return ch == 1d || ch != 0d && rng.nextDouble() <= ch;
	}

	public static BlockPos transform(int x, int y, int z, StructurePlaceSettings settings)
	{
		switch(settings.getMirror())
		{
		case LEFT_RIGHT:
			z = -z + 1;
			break;
		case FRONT_BACK:
			x = -x + 1;
			break;
		default:
			break;
		}
		int x1 = settings.getRotationPivot().getX();
		int z1 = settings.getRotationPivot().getZ();
		switch(settings.getRotation())
		{
		case COUNTERCLOCKWISE_90:
			return new BlockPos(x1 - z1 + z, y, x1 + z1 - x + 1);
		case CLOCKWISE_90:
			return new BlockPos(x1 + z1 - z + 1, y, z1 - x1 + x);
		case CLOCKWISE_180:
			return new BlockPos(x1 + x1 - x + 1, y, z1 + z1 - z + 1);
		default:
			return new BlockPos(x, y, z);
		}
	}

	public static AttachFace faceFromDir(Direction dir)
	{
		return dir == Direction.UP ? AttachFace.CEILING : dir == Direction.DOWN ? AttachFace.FLOOR : AttachFace.WALL;
	}

	public static AABB rotateY(AABB bb)
	{
		return new AABB(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX);
	}

	public static AABB rotateX(AABB bb)
	{
		return new AABB(bb.minX, bb.minZ, bb.minY, bb.maxX, bb.maxZ, bb.maxY);
	}

	public static boolean intersectsInclusive(AABB bb1, AABB bb2)
	{
		return bb1.minX <= bb2.maxX && bb1.maxX >= bb2.minX && bb1.minY <= bb2.maxY && bb1.maxY >= bb2.minY && bb1.minZ <= bb2.maxZ && bb1.maxZ >= bb2.minZ;
	}

	public static Vec3 sideCenter(AABB bb, Direction side)
	{
		Vec3i dir = side.getNormal();
		return new Vec3((bb.minX + bb.maxX + (bb.maxX - bb.minX) * dir.getX()) * 0.5d, (bb.minY + bb.maxY + (bb.maxY - bb.minY) * dir.getY()) * 0.5d, (bb.minZ + bb.maxZ + (bb.maxZ - bb.minZ) * dir.getZ()) * 0.5d);
	}

	public static Stream<Lockable> intersecting(Level world, BlockPos pos)
	{
		Int2ObjectMap<Lockable> chunk = world.getData(LocksAttachments.LOCKABLE_HANDLER.get()).getInChunk(pos);
		return chunk != null ? chunk.values().stream().filter(lkb -> lkb.bb.intersects(pos)) : Stream.<Lockable>empty();
	}

	public static boolean locked(Level world, BlockPos pos)
	{
		return intersecting(world, pos).anyMatch(LocksPredicates.LOCKED);
	}

	public static void resolveLootTables(Level level, Lockable lockable, Player player)
	{
		if(level.isClientSide)
			return;
		for(BlockPos pos : lockable.bb.getContainedPos())
		{
			BlockEntity be = level.getBlockEntity(pos);
			if(be == null)
				continue;
			// Skip Lootr block entities — Lootr manages its own per-player loot generation
			if(be.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("lootr"))
				continue;
			if(be instanceof RandomizableContainerBlockEntity container && container.lootTable != null)
				container.unpackLootTable(player);
		}
	}

	// Single source of truth for "what lock goes on this chest", shared by worldgen
	// (LockChestsFeature) and the Respawning Structures compat hook. Returns a ready-to-register
	// Lockable for the chest at pos, or null if it shouldn't be locked (not a valid lootable chest,
	// failed the generation chance, the right half of a double chest, or no lock tier matched).
	// access is used for block/block-entity lookups; level provides the lockable handler + server.
	public static Lockable createChestLockable(LevelAccessor access, Level level, BlockPos pos, RandomSource rng)
	{
		BlockState state = access.getBlockState(pos);
		if(!state.hasProperty(ChestBlock.TYPE))
			return null;
		boolean lootr = ModList.get().isLoaded("lootr");
		// The right half of a double chest is covered by the left half's lockable, so skip it
		// (unless Lootr is present, where each half is an independent single chest)
		if(state.getValue(ChestBlock.TYPE) == ChestType.RIGHT && !lootr)
			return null;
		BlockEntity be = access.getBlockEntity(pos);
		if(!(be instanceof RandomizableContainerBlockEntity container) || container.lootTable == null)
			return null;

		if(!chance(rng, LocksConfig.GENERATION_CHANCE.get()))
			return null;

		ItemStack stack = ItemStack.EMPTY;
		boolean usedLootScaling = false;
		if(LocksConfig.LOOT_SCALED_LOCKS.get())
		{
			double lootValue = LootValueCalculator.getLootValue(level.getServer(), container.lootTable.location());
			if(!Double.isNaN(lootValue))
			{
				stack = LocksConfig.getLockForLootValue(level.registryAccess(), lootValue, rng);
				usedLootScaling = true;
			}
		}
		if(!usedLootScaling)
			stack = LocksConfig.getRandomLock(level.registryAccess(), rng);

		// When loot-scaled locks are enabled and the loot value is below all tier thresholds, skip this chest
		if(stack.isEmpty())
			return null;

		BlockPos pos1 = state.getValue(ChestBlock.TYPE) == ChestType.SINGLE || lootr ? pos : pos.relative(ChestBlock.getConnectedDirection(state));
		return new Lockable(new Cuboid6i(pos, pos1), Lock.from(stack), Transform.fromDirection(state.getValue(ChestBlock.FACING), Direction.NORTH), stack, level);
	}
}
