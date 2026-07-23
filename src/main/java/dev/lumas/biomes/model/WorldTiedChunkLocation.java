package dev.lumas.biomes.model;

import dev.wyck.misc.ChunkLocation;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public record WorldTiedChunkLocation(World world, int chunkX, int chunkZ) {

    public static WorldTiedChunkLocation of(World world, int chunkX, int chunkZ) {
        return new WorldTiedChunkLocation(world, chunkX, chunkZ);
    }

    public static WorldTiedChunkLocation of(World world, ChunkLocation chunkLocation) {
        return new WorldTiedChunkLocation(world, chunkLocation.x(), chunkLocation.z());
    }

    public static WorldTiedChunkLocation of(Chunk chunk) {
        return new WorldTiedChunkLocation(chunk.getWorld(), chunk.getX(), chunk.getZ());
    }

    public CompletableFuture<Chunk> toBukkitChunk() {
        return world.getChunkAtAsync(chunkX, chunkZ);
    }

    public Location toLocation() {
        return new Location(world, chunkX << 4, 0, chunkZ << 4);
    }
}
