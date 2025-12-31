package dev.lumas.biomes.model;

import me.outspending.biomesapi.packet.data.ChunkLocation;
import org.bukkit.Chunk;
import org.bukkit.World;

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

    public Chunk toBukkitChunk() {
        return world.getChunkAt(chunkX, chunkZ);
    }
}
