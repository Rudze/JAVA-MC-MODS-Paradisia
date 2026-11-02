package melonslise.locks.common.init;

import melonslise.locks.common.network.toclient.AddLockablePacket;
import melonslise.locks.common.network.toclient.AddLockableToChunkPacket;
import melonslise.locks.common.network.toclient.RemoveLockablePacket;
import melonslise.locks.common.network.toclient.TryPinResultPacket;
import melonslise.locks.common.network.toclient.UpdateLockablePacket;
import melonslise.locks.common.network.toserver.TryPinPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/*
 * Was a Forge SimpleChannel with numeric packet ids (LocksNetwork.MAIN.send(distributor, packet)
 * everywhere). NeoForge's payload system has no "channel object" to send through - registration
 * (this class) is fully separate from sending, which is now the static PacketDistributor.sendXxx(...)
 * methods called directly at each old MAIN.send(...) call site.
 */
public final class LocksNetwork
{
	private static final String PROTOCOL_VERSION = "2";

	private LocksNetwork() {}

	public static void register(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

		registrar.playToClient(AddLockablePacket.TYPE, AddLockablePacket.STREAM_CODEC, AddLockablePacket::handle);
		registrar.playToClient(AddLockableToChunkPacket.TYPE, AddLockableToChunkPacket.STREAM_CODEC, AddLockableToChunkPacket::handle);
		registrar.playToClient(RemoveLockablePacket.TYPE, RemoveLockablePacket.STREAM_CODEC, RemoveLockablePacket::handle);
		registrar.playToClient(UpdateLockablePacket.TYPE, UpdateLockablePacket.STREAM_CODEC, UpdateLockablePacket::handle);
		registrar.playToClient(TryPinResultPacket.TYPE, TryPinResultPacket.STREAM_CODEC, TryPinResultPacket::handle);

		registrar.playToServer(TryPinPacket.TYPE, TryPinPacket.STREAM_CODEC, TryPinPacket::handle);
	}
}
