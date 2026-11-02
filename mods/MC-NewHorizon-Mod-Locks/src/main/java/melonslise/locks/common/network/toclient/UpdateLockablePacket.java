package melonslise.locks.common.network.toclient;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import melonslise.locks.Locks;
import melonslise.locks.common.init.LocksAttachments;
import melonslise.locks.common.util.Lockable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateLockablePacket(int id, boolean locked) implements CustomPacketPayload
{
	public static final Type<UpdateLockablePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "update_lockable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, UpdateLockablePacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, UpdateLockablePacket::id,
		ByteBufCodecs.BOOL, UpdateLockablePacket::locked,
		UpdateLockablePacket::new
	);

	public UpdateLockablePacket(Lockable lkb)
	{
		this(lkb.id, lkb.lock.isLocked());
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(UpdateLockablePacket pkt, IPayloadContext ctx)
	{
		ctx.enqueueWork(() ->
		{
			if(Minecraft.getInstance().level == null)
				return;
			Int2ObjectMap<Lockable> lkbs = Minecraft.getInstance().level.getData(LocksAttachments.LOCKABLE_HANDLER.get()).getLoaded();
			Lockable lkb = lkbs.get(pkt.id);
			if(lkb == lkbs.defaultReturnValue())
				return;
			lkb.lock.setLocked(pkt.locked);
		});
	}
}
