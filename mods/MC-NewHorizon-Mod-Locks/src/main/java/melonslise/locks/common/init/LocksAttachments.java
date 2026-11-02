package melonslise.locks.common.init;

import melonslise.locks.Locks;
import melonslise.locks.common.capability.ILockableStorage;
import melonslise.locks.common.capability.LockableHandler;
import melonslise.locks.common.capability.LockableStorage;
import melonslise.locks.common.capability.Selection;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/*
 * Replaces the old Forge capability system (LocksCapabilities: AttachCapabilitiesEvent on Level/LevelChunk/
 * Entity) with NeoForge's Data Attachments. Unlike Forge capabilities, an AttachmentType's default-value
 * supplier can be handed the holder it's being created for (AttachmentType.builder(Function<IAttachmentHolder,
 * T>)), so LockableHandler/LockableStorage keep the exact same "one instance per Level/LevelChunk, holding a
 * reference back to it" shape they had as capabilities - only the attach/access mechanism changed.
 *
 * LOCKABLE_HANDLER is intentionally NOT serialized here: the one piece of its state that needs to survive a
 * restart (the id counter) is persisted separately via LocksSavedData, and the loaded-lockable index is
 * rebuilt from each chunk's own LOCKABLE_STORAGE as chunks load - there's nothing left that needs an
 * attachment-level NBT round-trip.
 */
public final class LocksAttachments
{
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Locks.ID);

	public static final DeferredHolder<AttachmentType<?>, AttachmentType<LockableHandler>> LOCKABLE_HANDLER = ATTACHMENT_TYPES.register(
		"lockable_handler", () -> AttachmentType.builder(holder -> new LockableHandler((Level) holder)).build());

	public static final DeferredHolder<AttachmentType<?>, AttachmentType<LockableStorage>> LOCKABLE_STORAGE = ATTACHMENT_TYPES.register(
		"lockable_storage", () -> AttachmentType.builder(holder -> new LockableStorage((LevelChunk) holder))
			.serialize(new IAttachmentSerializer<ListTag, LockableStorage>()
			{
				@Override
				public LockableStorage read(IAttachmentHolder holder, ListTag nbt, HolderLookup.Provider registries)
				{
					LockableStorage storage = new LockableStorage((LevelChunk) holder);
					storage.deserializeNBT(registries, nbt);
					return storage;
				}

				@Override
				public ListTag write(LockableStorage storage, HolderLookup.Provider registries)
				{
					return storage.serializeNBT(registries);
				}
			})
			.build());

	public static final DeferredHolder<AttachmentType<?>, AttachmentType<Selection>> SELECTION = ATTACHMENT_TYPES.register(
		"selection", () -> AttachmentType.builder(holder -> new Selection()).build());

	private LocksAttachments() {}

	public static void register(IEventBus bus)
	{
		ATTACHMENT_TYPES.register(bus);
	}
}
