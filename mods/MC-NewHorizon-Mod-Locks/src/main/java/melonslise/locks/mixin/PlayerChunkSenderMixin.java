package melonslise.locks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import melonslise.locks.common.capability.ILockableStorage;
import melonslise.locks.common.init.LocksAttachments;
import melonslise.locks.common.network.toclient.AddLockableToChunkPacket;
import melonslise.locks.common.util.Lockable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.PlayerChunkSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.21.1 moved chunk sending off ChunkMap#playerLoadedChunk (removed) into PlayerChunkSender's own batched
 * sender; PlayerChunkSender#sendChunk(ServerGamePacketListenerImpl, ServerLevel, LevelChunk) is now the single
 * choke point both the initial view-distance send and re-sends funnel through.
 */
@Mixin(PlayerChunkSender.class)
public class PlayerChunkSenderMixin
{
	@Inject(at = @At("TAIL"), method = "sendChunk(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;)V")
	private static void sendChunk(ServerGamePacketListenerImpl connection, ServerLevel level, LevelChunk ch, CallbackInfo ci)
	{
		ILockableStorage st = ch.getData(LocksAttachments.LOCKABLE_STORAGE.get());
		if(st == null)
			return;
		// Snapshot the lockables so iteration can't throw ConcurrentModificationException if the
		// handler/storage is mutated on another path while this player is being synced.
		for(Lockable lkb : st.snapshot())
			PacketDistributor.sendToPlayer(connection.player, new AddLockableToChunkPacket(lkb, ch));
	}
}
