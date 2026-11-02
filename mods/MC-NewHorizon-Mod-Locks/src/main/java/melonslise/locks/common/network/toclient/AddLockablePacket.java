package melonslise.locks.common.network.toclient;

import melonslise.locks.Locks;
import melonslise.locks.common.init.LocksAttachments;
import melonslise.locks.common.util.Lockable;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AddLockablePacket(Lockable lockable) implements CustomPacketPayload
{
	public static final Type<AddLockablePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "add_lockable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, AddLockablePacket> STREAM_CODEC = StreamCodec.of(
		(buf, pkt) -> Lockable.toBuf(buf, pkt.lockable),
		buf -> new AddLockablePacket(Lockable.fromBuf(buf))
	);

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(AddLockablePacket pkt, IPayloadContext ctx)
	{
		ctx.enqueueWork(() ->
		{
			if(Minecraft.getInstance().level == null)
				return;
			// Skip validation on client — server already validated volume and intersection
			Minecraft.getInstance().level.getData(LocksAttachments.LOCKABLE_HANDLER.get()).addDirect(pkt.lockable);
		});
	}
}
