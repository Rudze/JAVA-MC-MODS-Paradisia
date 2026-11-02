package melonslise.locks.common.init;

import java.util.stream.Stream;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;

/*
 * Was a custom Forge PacketDistributor<Stream<LevelChunk>> ("send to every player tracking any of
 * these chunks, deduplicated"). NeoForge's PacketDistributor is now a fixed set of built-in static
 * strategies (sendToPlayer, sendToPlayersTrackingChunk, ...) with no equivalent custom-distributor
 * constructor, so this is just the same dedup logic as a plain helper method instead.
 */
public final class LocksPacketDistributors
{
	private LocksPacketDistributors() {}

	public static void sendToTrackingArea(Stream<LevelChunk> chunks, CustomPacketPayload payload)
	{
		chunks
			.flatMap(chunk -> ((ServerChunkCache) chunk.getLevel().getChunkSource()).chunkMap.getPlayers(chunk.getPos(), false).stream())
			.distinct()
			.forEach(p -> p.connection.send(payload));
	}
}
