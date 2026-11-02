package melonslise.locks.common.network.toclient;

import melonslise.locks.Locks;
import melonslise.locks.common.container.LockPickingContainer;
import melonslise.locks.common.init.LocksMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TryPinResultPacket(boolean correct, boolean reset) implements CustomPacketPayload
{
	public static final Type<TryPinResultPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Locks.ID, "try_pin_result"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TryPinResultPacket> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL, TryPinResultPacket::correct,
		ByteBufCodecs.BOOL, TryPinResultPacket::reset,
		TryPinResultPacket::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(TryPinResultPacket pkt, IPayloadContext ctx)
	{
		ctx.enqueueWork(() ->
		{
			AbstractContainerMenu container = Minecraft.getInstance().player.containerMenu;
			if(container.getType() == LocksMenuTypes.LOCK_PICKING.get())
				((LockPickingContainer) container).handlePin(pkt.correct, pkt.reset);
		});
	}
}
