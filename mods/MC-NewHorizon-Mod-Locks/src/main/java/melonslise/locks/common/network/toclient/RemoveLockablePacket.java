package melonslise.locks.common.network.toclient;

import melonslise.locks.Locks;
import melonslise.locks.common.init.LocksAttachments;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RemoveLockablePacket(int id) implements CustomPacketPayload
{
	public static final Type<RemoveLockablePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "remove_lockable"));

	public static final StreamCodec<RegistryFriendlyByteBuf, RemoveLockablePacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.INT, RemoveLockablePacket::id,
		RemoveLockablePacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(RemoveLockablePacket pkt, IPayloadContext ctx)
	{
		ctx.enqueueWork(() ->
		{
			if(Minecraft.getInstance().level == null)
				return;
			Minecraft.getInstance().level.getData(LocksAttachments.LOCKABLE_HANDLER.get()).remove(pkt.id);
		});
	}
}
