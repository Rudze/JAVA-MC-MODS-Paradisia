package melonslise.locks.common.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import melonslise.locks.Locks;
import melonslise.locks.common.capability.ILockableHandler;
import melonslise.locks.common.capability.ILockableStorage;
import melonslise.locks.common.capability.ISelection;
import melonslise.locks.common.compat.CarryOnCompat;
import melonslise.locks.common.compat.CuriosHelper;
import melonslise.locks.common.init.LocksEnchantments;
import melonslise.locks.common.config.LocksClientConfig;
import melonslise.locks.common.config.LocksServerConfig;
import melonslise.locks.common.container.KeyRingContainer;
import melonslise.locks.common.container.LockPickingContainer;
import melonslise.locks.common.init.LocksAttachments;
import melonslise.locks.common.init.LocksItemTags;
import melonslise.locks.common.init.LocksItems;
import melonslise.locks.common.network.toclient.AddLockableToChunkPacket;
import melonslise.locks.common.init.LocksTagHelper;
import melonslise.locks.common.init.LockStatsReloadListener;
import melonslise.locks.common.init.LocksSoundEvents;
import melonslise.locks.common.steel.NativeSteelPolicy;
import melonslise.locks.common.steel.NativeSteelState;
import melonslise.locks.common.steel.SteelMaterialMode;
import melonslise.locks.common.item.KeyRingItem;
import melonslise.locks.common.item.LockItem;
import melonslise.locks.common.item.LockPickItem;
import melonslise.locks.common.item.LockingItem;
import melonslise.locks.common.util.Lockable;
import melonslise.locks.common.util.LocksThreadUtil;
import melonslise.locks.common.util.LocksUtil;
import melonslise.locks.common.util.LootValueCalculator;
import melonslise.locks.common.util.ShockingHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.items.IItemHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = Locks.ID)
public final class LocksForgeEvents
{
	public static final Component LOCKED_MESSAGE = Component.translatable(Locks.ID + ".status.locked");

	private LocksForgeEvents() {}

	// No more attachCapabilitiesTo*(AttachCapabilitiesEvent<...>) here - NeoForge attachments have no
	// attach-event equivalent, LocksAttachments' holder-aware default suppliers create
	// LockableHandler/LockableStorage/Selection lazily on first getData() access instead.

	@SubscribeEvent
	public static void addReloadListeners(AddReloadListenerEvent e)
	{
		e.addListener(new LockStatsReloadListener());
	}

	@SubscribeEvent
	public static void onLevelLoad(LevelEvent.Load e)
	{
		if (e.getLevel() instanceof ServerLevel level)
		{
			// Resolve the persisted lockable id counter on the main thread before any chunk/structure
			// generation can allocate ids off-thread (C2ME), keeping ids unique across server restarts.
			level.getData(LocksAttachments.LOCKABLE_HANDLER.get()).initIds();
			if (level.dimension() == Level.OVERWORLD)
				LootValueCalculator.precomputeAll(level.getServer());
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent e)
	{
		LootValueCalculator.clearCache();
		// Drop the steel snapshot so a stale state never leaks into the next world/server session.
		NativeSteelPolicy.clear();
	}

	/**
	 * Refresh the native-steel compatibility snapshot whenever tags are (re)loaded, and log a single diagnostic
	 * summary. Only the authoritative server-data load recomputes the snapshot and logs; the client packet update
	 * refreshes the snapshot too (so the client-side creative tab reflects the server), but never logs.
	 */
	@SubscribeEvent
	public static void onTagsUpdated(TagsUpdatedEvent e)
	{
		SteelMaterialMode mode = LocksServerConfig.SPEC.isLoaded()
			? LocksServerConfig.STEEL_MATERIAL_MODE.get()
			: SteelMaterialMode.AUTO;
		NativeSteelState state = NativeSteelPolicy.refresh(e.getRegistryAccess(), mode);

		if (e.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD)
			return;

		Locks.LOGGER.info("Steel material mode: {}", mode);
		Locks.LOGGER.info("Foreign ingots: {}", state.foreignIngots());
		Locks.LOGGER.info("Foreign nuggets: {}", state.foreignNuggets());
		Locks.LOGGER.info("Foreign ores: {}", state.foreignOres());
		Locks.LOGGER.info("Locks native fallback: ingot={}, nugget={}, oreGeneration={}",
			state.nativeIngotActive(), state.nativeNuggetActive(), state.nativeOreActive());

		if (mode == SteelMaterialMode.EXTERNAL_ONLY)
		{
			List<String> missing = NativeSteelPolicy.missingForms(state);
			if (!missing.isEmpty())
				Locks.LOGGER.warn("Steel Material Mode is EXTERNAL_ONLY but no steel provider supplies: {}. "
					+ "Affected Locks recipes will be uncraftable until a mod/datapack populates the forge steel tags.", missing);
		}
	}

	private static final Gson LOOT_GSON = new Gson();

	@SubscribeEvent
	public static void onLootTableLoad(LootTableLoadEvent e)
	{
		ResourceLocation name = e.getName();
		if(!LocksServerConfig.matchesLootTablePattern(name))
			return;
		ResourceLocation injectLoc = ResourceLocation.fromNamespaceAndPath(Locks.ID, "loot_tables/inject/" + name.getPath() + ".json");
		net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if(server == null)
			return;
		ResourceManager manager = server.getResourceManager();
		Optional<Resource> resource = manager.getResource(injectLoc);
		if(resource.isEmpty())
			return;
		try(java.io.Reader reader = resource.get().openAsReader())
		{
			JsonElement json = GsonHelper.fromJson(LOOT_GSON, reader, JsonElement.class);
			if(json.isJsonObject() && json.getAsJsonObject().has("pools"))
			{
				var ops = e.getRegistries().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
				List<LootPool> extra = new ArrayList<>();
				for(JsonElement poolJson : json.getAsJsonObject().getAsJsonArray("pools"))
					LootPool.CODEC.parse(ops, poolJson)
						.resultOrPartial(err -> Locks.LOGGER.error("Failed to parse injected loot pool in {}: {}", injectLoc, err))
						.ifPresent(extra::add);
				if(!extra.isEmpty())
				{
					melonslise.locks.mixin.LootTableAccessor accessor = (melonslise.locks.mixin.LootTableAccessor) e.getTable();
					List<LootPool> pools = new ArrayList<>(accessor.locks$getPools());
					pools.addAll(extra);
					accessor.locks$setPools(pools);
				}
			}
		}
		catch(Exception ex)
		{
			Locks.LOGGER.error("Failed to inject loot table {}", injectLoc, ex);
		}
	}

	@SubscribeEvent
	public static void addVillagerTrades(VillagerTradesEvent e)
	{
		if(!LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_VILLAGER_TRADES))
			return;
		ResourceLocation wantedProfession = ResourceLocation.tryParse(LocksServerConfig.getOrDefault(LocksServerConfig.VILLAGER_PROFESSION));
		if(wantedProfession == null || !wantedProfession.equals(BuiltInRegistries.VILLAGER_PROFESSION.getKey(e.getType())))
			return;
		boolean picks = LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_VILLAGER_LOCKPICK_TRADES);
		boolean mechs = LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_VILLAGER_MECHANISM_TRADES);
		boolean locks = LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_VILLAGER_LOCK_TRADES);
		Int2ObjectMap<List<ItemListing>> levels = e.getTrades();
		List<ItemListing> trades;

		trades = levels.get(1);
		if(picks) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.WOOD_LOCK_PICK.get()), 1, 2, 16, 2, 0.05f));
		if(mechs) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.WOOD_LOCK_MECHANISM.get()), 2, 1, 12, 1, 0.2f));
		if(locks) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.WOOD_LOCK.get()), 3, 1, 12, 2, 0.2f));

		trades = levels.get(2);
		if(picks) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.IRON_LOCK_PICK.get()), 2, 2, 16, 5, 0.05f));
		if(locks) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.COPPER_LOCK.get()), 4, 1, 12, 5, 0.2f));

		trades = levels.get(3);
		if(picks) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.GOLD_LOCK_PICK.get()), 6, 2, 12, 20, 0.05f));
		if(mechs) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.IRON_LOCK_MECHANISM.get()), 5, 1, 8, 10, 0.2f));
		if(locks) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.IRON_LOCK.get()), 6, 1, 8, 10, 0.2f));

		trades = levels.get(4);
		if(picks) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.STEEL_LOCK_PICK.get()), 4, 2, 16, 20, 0.05f));

		trades = levels.get(5);
		if(picks)
		{
			trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.DIAMOND_LOCK_PICK.get()), 8, 2, 12, 30, 0.05f));
			trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.NETHERITE_LOCK_PICK.get()), 16, 1, 6, 30, 0.05f));
		}
		if(mechs) trades.add(new VillagerTrades.ItemsForEmeralds(new ItemStack(LocksItems.STEEL_LOCK_MECHANISM.get()), 8, 1, 8, 30, 0.2f));
	}

	@SubscribeEvent
	public static void addWandererTrades(WandererTradesEvent e)
	{
		if(!LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_WANDERER_TRADES))
			return;
		boolean picks = LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_WANDERER_LOCKPICK_TRADES);
		boolean locks = LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_WANDERER_LOCK_TRADES);
		boolean mechs = LocksServerConfig.getOrDefault(LocksServerConfig.ENABLE_WANDERER_MECHANISM_TRADES);
		List<ItemListing> generic = e.getGenericTrades();
		List<ItemListing> rare = e.getRareTrades();
		if(picks)
		{
			generic.add(new VillagerTrades.ItemsForEmeralds(LocksItems.GOLD_LOCK_PICK.get(), 5, 2, 6, 1));
			generic.add(new VillagerTrades.ItemsForEmeralds(LocksItems.STEEL_LOCK_PICK.get(), 3, 2, 8, 1));
		}
		if(locks)
		{
			generic.add(new VillagerTrades.EnchantedItemForEmeralds(LocksItems.STEEL_LOCK.get(), 16, 4, 1));
			rare.add(new VillagerTrades.EnchantedItemForEmeralds(LocksItems.DIAMOND_LOCK.get(), 28, 4, 1));
			rare.add(new VillagerTrades.EnchantedItemForEmeralds(LocksItems.NETHERITE_LOCK.get(), 40, 4, 1));
		}
		if(mechs)
			rare.add(new VillagerTrades.ItemsForEmeralds(LocksItems.STEEL_LOCK_MECHANISM.get(), 6, 1, 4, 1));
	}

	// Single registration trigger for ALL chunk loads (disk + freshly generated). The chunk capability NBT
	// (LockableStorage#deserializeNBT) is now pure chunk-local parsing that may run on an async worker thread
	// (C2ME); moving the parsed lockables into the world-global handler is main-thread-only work, so it is
	// deferred here. Without async chunk mods this runs inline and behaviour is unchanged.
	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load e)
	{
		if(!(e.getChunk() instanceof LevelChunk ch))
			return;
		Level level = ch.getLevel();
		if(level.isClientSide)
			return; // the client populates its handler from packets, not chunk NBT
		// `ch` IS the live LevelChunk whose load fired this event, and its LockableStorage was already populated by
		// LevelChunkMixin on the building thread. Capture both NOW and NEVER re-fetch via a blocking Level#getChunk:
		// under C2ME this handler can fire re-entrantly on the main thread while that thread is draining the chunk
		// executor (BlockableEventLoop#managedBlock), and getChunk(...,FULL,true) would re-park on the very future
		// this thread is in the middle of completing -> permanent hang.
		ILockableStorage storage = ch.getData(LocksAttachments.LOCKABLE_STORAGE.get());
		if(storage == null)
			return;
		int chX = ch.getPos().x, chZ = ch.getPos().z;
		LocksThreadUtil.runOnServerThread(level.getServer(), () ->
		{
			// Non-blocking staleness guard for the deferred (worker -> next-tick) path: the chunk may have unloaded
			// before this task runs. hasChunk == ServerChunkCache#getChunkNow != null (no future await). NEVER call
			// level.getChunk(...) here.
			if(!level.hasChunk(chX, chZ))
				return;
			ILockableHandler handler = level.getData(LocksAttachments.LOCKABLE_HANDLER.get());
			if(handler == null)
				return;
			handler.registerChunkStorage(ch, storage, false);
		});
	}

	@SubscribeEvent
	public static void onChunkUnload(ChunkEvent.Unload e)
	{
		if(!(e.getChunk() instanceof LevelChunk ch))
			return;
		Level level = ch.getLevel();
		ILockableHandler handler = level.getData(LocksAttachments.LOCKABLE_HANDLER.get());
		if(handler == null)
			return;
		ILockableStorage storage = ch.getData(LocksAttachments.LOCKABLE_STORAGE.get());
		if(storage == null)
			return;
		// Snapshot the storage now: reading the per-chunk map is thread-safe, but the handler mutation must
		// happen on the main thread, and the storage capability may be invalidated by the time a deferred task
		// runs. A lockable straddling a chunk border is only dropped from the handler once no OTHER chunk it
		// occupies is still loaded — otherwise it vanishes from rendering/sync while a neighbour stays loaded.
		List<Lockable> present = new ArrayList<>(storage.get().values());
		if(present.isEmpty())
			return;
		int chX = ch.getPos().x, chZ = ch.getPos().z;
		if(level.isClientSide)
		{
			handler.unregisterChunkStorage(chX, chZ, present); // no server thread to defer to client-side
			return;
		}
		LocksThreadUtil.runOnServerThread(level.getServer(), () -> handler.unregisterChunkStorage(chX, chZ, present));
	}

	@SubscribeEvent
	public static void onRightClick(PlayerInteractEvent.RightClickBlock e)
	{
		BlockPos pos = e.getPos();
		Level world = e.getLevel();
		Player player = e.getEntity();
		ILockableHandler handler = world.getData(LocksAttachments.LOCKABLE_HANDLER.get());
		if(handler == null)
			return;
		Int2ObjectMap<Lockable> chunkLockables = handler.getInChunk(pos);
		if(chunkLockables == null)
			return;
		List<Lockable> intersect = new ArrayList<>(4);
		for(Lockable lkb : chunkLockables.values())
			if(lkb.bb.intersects(pos))
				intersect.add(lkb);
		// Self-heal a chunk-storage <-> world-index divergence before acting on a lock (server only, and only when
		// a lockable is actually here). Door-blocking reads the per-chunk storage, but rendering and the pick
		// minigame read the world index / client mirror. registerChunkStorage is idempotent and sends no packets;
		// it makes every lockable in this chunk the canonical, handler-observed instance (so lock-state changes
		// persist and sync), then we rebuild `intersect` from the now-canonical storage so we operate on those
		// instances. Without this a door could stay blocked by a lock that never reached the index (e.g. a missed
		// sync under async chunk loading), invisible and unpickable.
		LevelChunk lockChunk = null;
		if(!world.isClientSide && !intersect.isEmpty())
		{
			lockChunk = world.getChunkAt(pos);
			ILockableStorage st = lockChunk.getData(LocksAttachments.LOCKABLE_STORAGE.get());
			if(st != null)
			{
				handler.registerChunkStorage(lockChunk, st, false);
				intersect.clear();
				for(Lockable lkb : chunkLockables.values())
					if(lkb.bb.intersects(pos))
						intersect.add(lkb);
			}
		}
		if(intersect.isEmpty())
		{
			// Adventure mode fallback: vanilla ItemStack.useOn() short-circuits when
			// !mayBuild, so LockItem.useOn() never fires. Invoke it directly on the
			// sneak-place gesture so lock placement works without changing gamemode.
			ItemStack held = e.getItemStack();
			if(e.getHand() == InteractionHand.MAIN_HAND
				&& !player.getAbilities().mayBuild
				&& player.isSecondaryUseActive()
				&& held.getItem() instanceof LockItem lockItem)
			{
				UseOnContext ctx = new UseOnContext(player, e.getHand(), e.getHitVec());
				InteractionResult result = lockItem.useOn(ctx);
				if(result != InteractionResult.PASS)
				{
					e.setCanceled(true);
					e.setCancellationResult(result);
				}
			}
			return;
		}
		// PlayerInteractEvent.RightClickBlock fires once per hand. We process only MAIN_HAND
		// and deny block interaction on OFFHAND to prevent double-firing. This is correct even
		// with shields or other offhand items — Forge fires the main-hand event independently.
		if(e.getHand() != InteractionHand.MAIN_HAND)
		{
			e.setUseBlock(TriState.FALSE);
			return;
		}
		ItemStack stack = e.getItemStack();
		Lockable locked = null;
		for(Lockable lkb : intersect)
			if(lkb.lock.isLocked()) { locked = lkb; break; }
		if(locked != null)
		{
			Lockable lkb = locked;
			e.setUseBlock(TriState.FALSE);
			e.setUseItem(TriState.FALSE);

			// Self-heal the client: push the blocking lock to the interacting player so a desynced or missing
			// client copy re-renders and the pick minigame can resolve it. Bounded to actual blocked interactions.
			if(!world.isClientSide && lockChunk != null && player instanceof ServerPlayer sp)
			{
				LevelChunk syncChunk = lockChunk;
				PacketDistributor.sendToPlayer(sp, new AddLockableToChunkPacket(lkb, syncChunk));
			}

			if(LocksTagHelper.isLockPick(stack))
			{
				// Lock pick: open lock picking minigame (or auto-pick if enchanted)
				if(!LockPickItem.canPick(world.registryAccess(), stack, lkb))
				{
					if(world.isClientSide)
						player.displayClientMessage(LockPickItem.TOO_COMPLEX_MESSAGE, true);
				}
				else
				{
					int autoPick = LocksServerConfig.ENABLE_AUTO_PICK.get() ? LocksEnchantments.getLevel(world.registryAccess(), LocksEnchantments.AUTO_PICK, lkb.stack) : 0;
					if(autoPick > 0 && !world.isClientSide && world.getRandom().nextFloat() < autoPick * 0.10f)
					{
						// Auto-Pick triggered: instant unlock without minigame
						lkb.lock.setLocked(false);
						world.playSound(null, pos, LocksSoundEvents.LOCK_OPEN.get(), SoundSource.BLOCKS, 1f, 1f);
						LocksUtil.resolveLootTables(world, lkb, player);
					}
					else if(!world.isClientSide)
					{
						player.openMenu(new LockPickingContainer.Provider(InteractionHand.MAIN_HAND, lkb), new LockPickingContainer.Writer(InteractionHand.MAIN_HAND, lkb));
					}
				}
			}
			else if(stack.getItem() == LocksItems.MASTER_KEY.get())
			{
				// Master key: toggle all lockables at position
				world.playSound(player, pos, LocksSoundEvents.LOCK_OPEN.get(), SoundSource.BLOCKS, 1f, 1f);
				if(!world.isClientSide)
					for(Lockable l : intersect)
					{
						boolean wasLocked = l.lock.isLocked();
						l.lock.setLocked(!wasLocked);
						if(wasLocked)
							LocksUtil.resolveLootTables(world, l, player);
					}
			}
			else if(LocksTagHelper.isKey(stack) && LockingItem.getOrSetId(stack) == lkb.lock.id)
			{
				// Key with matching ID: toggle matching lockables
				int id = LockingItem.getOrSetId(stack);
				world.playSound(player, pos, LocksSoundEvents.LOCK_OPEN.get(), SoundSource.BLOCKS, 1f, 1f);
				if(!world.isClientSide)
					for(Lockable l : intersect)
						if(l.lock.id == id)
						{
							boolean wasLocked = l.lock.isLocked();
							l.lock.setLocked(!wasLocked);
							if(wasLocked)
								LocksUtil.resolveLootTables(world, l, player);
						}
			}
			else if(stack.getItem() == LocksItems.KEY_RING.get() && KeyRingItem.containsId(stack, lkb.lock.id))
			{
				// Key ring with matching key: find first matching key and toggle
				IItemHandler inv = stack.getCapability(Capabilities.ItemHandler.ITEM);
				if(inv == null) return;
				for(int a = 0; a < inv.getSlots(); ++a)
				{
					int id = LockingItem.getOrSetId(inv.getStackInSlot(a));
					boolean matched = false;
					for(Lockable l : intersect)
					{
						if(l.lock.id == id)
						{
							matched = true;
							if(!world.isClientSide)
							{
								boolean wasLocked = l.lock.isLocked();
								l.lock.setLocked(!wasLocked);
								if(wasLocked)
									LocksUtil.resolveLootTables(world, l, player);
							}
						}
					}
					if(matched)
					{
						world.playSound(player, pos, LocksSoundEvents.LOCK_OPEN.get(), SoundSource.BLOCKS, 1f, 1f);
						break;
					}
				}
			}
			else
			{
				// Check Awareness enchantment: scan all locked lockables for one owned by this player
				boolean awarenessHandled = false;
				if (LocksServerConfig.ENABLE_AWARENESS.get())
				{
					for (Lockable candidate : intersect)
					{
						if (!candidate.lock.isLocked()) continue;
						if (LocksEnchantments.getLevel(world.registryAccess(), LocksEnchantments.AWARENESS, candidate.stack) <= 0) continue;
						java.util.UUID owner = LockItem.getOwner(candidate.stack);
						if (owner != null && owner.equals(player.getUUID()))
						{
							world.playSound(player, pos, LocksSoundEvents.LOCK_OPEN.get(), SoundSource.BLOCKS, 1f, 1f);
							if (!world.isClientSide)
								for (Lockable l : intersect)
								{
									java.util.UUID lOwner = LockItem.getOwner(l.stack);
									if (lOwner != null && lOwner.equals(player.getUUID())
										&& LocksEnchantments.getLevel(world.registryAccess(), LocksEnchantments.AWARENESS, l.stack) > 0)
									{
										boolean wasLocked = l.lock.isLocked();
										l.lock.setLocked(!wasLocked);
										if (wasLocked)
											LocksUtil.resolveLootTables(world, l, player);
									}
								}
							awarenessHandled = true;
							break;
						}
					}
				}
				if (!awarenessHandled)
				{
					// Check curio slots for a key ring with matching key
					ItemStack curioRing = CuriosHelper.findMatchingKeyRing(player, lkb.lock.id);
					if (!curioRing.isEmpty())
					{
						IItemHandler curioInv = curioRing.getCapability(Capabilities.ItemHandler.ITEM);
						if (curioInv == null) return;
						for (int a = 0; a < curioInv.getSlots(); ++a)
						{
							int id = LockingItem.getOrSetId(curioInv.getStackInSlot(a));
							boolean matched = false;
							for (Lockable l : intersect)
							{
								if (l.lock.id == id)
								{
									matched = true;
									if (!world.isClientSide)
									{
										boolean wasLocked = l.lock.isLocked();
										l.lock.setLocked(!wasLocked);
										if (wasLocked)
											LocksUtil.resolveLootTables(world, l, player);
									}
								}
							}
							if (matched)
							{
								world.playSound(player, pos, LocksSoundEvents.LOCK_OPEN.get(), SoundSource.BLOCKS, 1f, 1f);
								break;
							}
						}
					}
					else
					{
						// No matching item anywhere: rattle (unless Silent enchantment)
						lkb.swing(20);
						// Optional theft punishment: shock the player for interacting with a locked block without a key (off by default)
						ShockingHelper.tryShock(player, lkb.stack, Vec3.atCenterOf(pos), ShockingHelper.Trigger.UNAUTHORIZED_INTERACTION);
						if(!LocksServerConfig.ENABLE_SILENT.get() || LocksEnchantments.getLevel(world.registryAccess(), LocksEnchantments.SILENT, lkb.stack) == 0)
						{
							world.playSound(player, pos, LocksSoundEvents.LOCK_RATTLE.get(), SoundSource.BLOCKS, 1f, 1f);
							if(world.isClientSide && LocksClientConfig.DEAF_MODE.get())
								player.displayClientMessage(LOCKED_MESSAGE, true);
						}
					}
				}
			}

			player.swing(InteractionHand.MAIN_HAND);
			e.setCancellationResult(InteractionResult.SUCCESS);
			e.setCanceled(true);
			return;
		}
		else
		{
			// All lockables at this position are unlocked — handle re-locking
			boolean relocked = false;

			if(stack.getItem() == LocksItems.MASTER_KEY.get())
			{
				e.setUseBlock(TriState.FALSE);
				e.setUseItem(TriState.FALSE);
				world.playSound(player, pos, LocksSoundEvents.LOCK_CLOSE.get(), SoundSource.BLOCKS, 1f, 1f);
				if(!world.isClientSide)
					for(Lockable l : intersect)
						l.lock.setLocked(true);
				relocked = true;
			}
			else if(LocksTagHelper.isKey(stack))
			{
				int id = LockingItem.getOrSetId(stack);
				boolean hasMatch = false;
				for(Lockable l : intersect)
					if(l.lock.id == id) { hasMatch = true; break; }
				if(hasMatch)
				{
					e.setUseBlock(TriState.FALSE);
					e.setUseItem(TriState.FALSE);
					world.playSound(player, pos, LocksSoundEvents.LOCK_CLOSE.get(), SoundSource.BLOCKS, 1f, 1f);
					if(!world.isClientSide)
						for(Lockable l : intersect)
							if(l.lock.id == id)
								l.lock.setLocked(true);
					relocked = true;
				}
			}
			else if(stack.getItem() == LocksItems.KEY_RING.get())
			{
				IItemHandler inv = stack.getCapability(Capabilities.ItemHandler.ITEM);
				if(inv != null)
				{
					for(int a = 0; a < inv.getSlots(); ++a)
					{
						int id = LockingItem.getOrSetId(inv.getStackInSlot(a));
						boolean matched = false;
						for(Lockable l : intersect)
						{
							if(l.lock.id == id)
							{
								matched = true;
								if(!world.isClientSide)
									l.lock.setLocked(true);
							}
						}
						if(matched)
						{
							e.setUseBlock(TriState.FALSE);
							e.setUseItem(TriState.FALSE);
							world.playSound(player, pos, LocksSoundEvents.LOCK_CLOSE.get(), SoundSource.BLOCKS, 1f, 1f);
							relocked = true;
							break;
						}
					}
				}
			}

			if(!relocked && LocksServerConfig.ENABLE_AWARENESS.get())
			{
				for(Lockable candidate : intersect)
				{
					if(LocksEnchantments.getLevel(world.registryAccess(), LocksEnchantments.AWARENESS, candidate.stack) <= 0) continue;
					java.util.UUID owner = LockItem.getOwner(candidate.stack);
					if(owner != null && owner.equals(player.getUUID()))
					{
						e.setUseBlock(TriState.FALSE);
						e.setUseItem(TriState.FALSE);
						world.playSound(player, pos, LocksSoundEvents.LOCK_CLOSE.get(), SoundSource.BLOCKS, 1f, 1f);
						if(!world.isClientSide)
							for(Lockable l : intersect)
							{
								java.util.UUID lOwner = LockItem.getOwner(l.stack);
								if(lOwner != null && lOwner.equals(player.getUUID())
									&& LocksEnchantments.getLevel(world.registryAccess(), LocksEnchantments.AWARENESS, l.stack) > 0)
									l.lock.setLocked(true);
							}
						relocked = true;
						break;
					}
				}
			}

			if(!relocked)
			{
				for(Lockable candidate : intersect)
				{
					ItemStack curioRing = CuriosHelper.findMatchingKeyRing(player, candidate.lock.id);
					if(!curioRing.isEmpty())
					{
						IItemHandler curioInv = curioRing.getCapability(Capabilities.ItemHandler.ITEM);
						if(curioInv != null)
						{
							for(int a = 0; a < curioInv.getSlots(); ++a)
							{
								int id = LockingItem.getOrSetId(curioInv.getStackInSlot(a));
								boolean matched = false;
								for(Lockable l : intersect)
								{
									if(l.lock.id == id)
									{
										matched = true;
										if(!world.isClientSide)
											l.lock.setLocked(true);
									}
								}
								if(matched)
								{
									e.setUseBlock(TriState.FALSE);
									e.setUseItem(TriState.FALSE);
									world.playSound(player, pos, LocksSoundEvents.LOCK_CLOSE.get(), SoundSource.BLOCKS, 1f, 1f);
									relocked = true;
									break;
								}
							}
						}
						if(relocked) break;
					}
				}
			}

			if(relocked)
			{
				player.swing(InteractionHand.MAIN_HAND);
				e.setCancellationResult(InteractionResult.SUCCESS);
				e.setCanceled(true);
				return;
			}
		}
		if(LocksServerConfig.ALLOW_REMOVING_LOCKS.get() && player.isShiftKeyDown() && stack.isEmpty())
		{
			List<Lockable> match = new ArrayList<>(4);
			for(Lockable lkb : intersect)
				if(!lkb.lock.isLocked())
					match.add(lkb);
			if(match.isEmpty())
				return;
			e.setUseBlock(TriState.FALSE);
			world.playSound(player, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
			player.swing(InteractionHand.MAIN_HAND);
			if(!world.isClientSide)
				for(Lockable lkb : match)
				{
					world.addFreshEntity(new ItemEntity(world, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, lkb.stack));
					handler.remove(lkb.id);
				}
		}
	}

	@SubscribeEvent
	public static void onEmptyHandRightClick(PlayerInteractEvent.RightClickItem e)
	{
		if (e.getHand() != InteractionHand.MAIN_HAND)
			return;
		Player player = e.getEntity();
		if (!player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty())
			return;
		if (!player.isShiftKeyDown())
			return;
		if (player.level().isClientSide)
			return;
		ItemStack curioRing = CuriosHelper.findAnyKeyRing(player);
		if (curioRing.isEmpty())
			return;
		player.openMenu(new KeyRingContainer.Provider(curioRing), new KeyRingContainer.CurioWriter());
		e.setCanceled(true);
		e.setCancellationResult(InteractionResult.SUCCESS);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Pre e)
	{
		Player player = e.getEntity();
		ISelection select = player.getData(LocksAttachments.SELECTION.get());
		if (select == null || select.get() == null)
			return;
		for (ItemStack stack : player.getHandSlots())
			if(LocksTagHelper.isLock(stack))
				return;
		select.set(null);
	}

	public static boolean canBreakLockable(Player player, BlockPos pos)
	{
		return !LocksServerConfig.PROTECT_LOCKABLES.get() || player.isCreative() || !LocksUtil.locked(player.level(), pos);
	}

	@SubscribeEvent
	public static void onBlockBreaking(PlayerEvent.BreakSpeed e)
	{
		Optional<BlockPos> optPos = e.getPosition();
		if(optPos.isEmpty())
			return;
		if(!canBreakLockable(e.getEntity(), optPos.get()))
			e.setCanceled(true);
	}

	@SubscribeEvent
	public static void onBlockBreak(BlockEvent.BreakEvent e)
	{
		// Carry On posts a BreakEvent as its pickup gate. When our Carry On compat has already authorized
		// carrying this locked block, let it through instead of vetoing the pickup as an illegal break.
		if(CarryOnCompat.isAuthorizedPickup(e.getPos()))
			return;
		if(canBreakLockable(e.getPlayer(), e.getPos()))
			return;
		e.setCanceled(true);
		// Optional theft punishment: shock the player for trying to break a protected locked block (off by default).
		// canBreakLockable already exempts creative players, so this only fires in survival.
		if(ShockingHelper.isTriggerEnabled(ShockingHelper.Trigger.BLOCK_BREAK_ATTEMPT))
		{
			Lockable lkb = findLockedAt(e.getPlayer().level(), e.getPos());
			if(lkb != null)
				ShockingHelper.tryShock(e.getPlayer(), lkb.stack, Vec3.atCenterOf(e.getPos()), ShockingHelper.Trigger.BLOCK_BREAK_ATTEMPT);
		}
	}

	private static Lockable findLockedAt(Level world, BlockPos pos)
	{
		ILockableHandler handler = world.getData(LocksAttachments.LOCKABLE_HANDLER.get());
		if(handler == null)
			return null;
		Int2ObjectMap<Lockable> chunkLockables = handler.getInChunk(pos);
		if(chunkLockables == null)
			return null;
		for(Lockable lkb : chunkLockables.values())
			if(lkb.lock.isLocked() && lkb.bb.intersects(pos))
				return lkb;
		return null;
	}
}
