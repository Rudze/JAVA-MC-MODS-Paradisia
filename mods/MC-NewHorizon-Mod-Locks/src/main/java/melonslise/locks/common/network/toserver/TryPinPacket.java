package melonslise.locks.common.network.toserver;

import melonslise.locks.Locks;
import melonslise.locks.common.container.LockPickingContainer;
import melonslise.locks.common.init.LocksMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TryPinPacket(byte pin) implements CustomPacketPayload
{
	public static final Type<TryPinPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "try_pin"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TryPinPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BYTE, TryPinPacket::pin,
		TryPinPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(TryPinPacket pkt, IPayloadContext ctx)
	{
		ctx.enqueueWork(() ->
		{
			ServerPlayer sender = (ServerPlayer) ctx.player();
			AbstractContainerMenu container = sender.containerMenu;
			if(container.getType() == LocksMenuTypes.LOCK_PICKING.get())
			{
				LockPickingContainer lpc = (LockPickingContainer) container;
				if(pkt.pin >= 0 && pkt.pin < lpc.lockable.lock.getLength()
					&& lpc.lockable.lock.isLocked()
					&& lpc.isValidPick(sender.getItemInHand(lpc.hand)))
					lpc.tryPin(pkt.pin);
			}
		});
	}
}
