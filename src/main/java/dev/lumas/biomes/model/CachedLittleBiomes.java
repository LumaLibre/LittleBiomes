package dev.lumas.biomes.model;

import me.outspending.biomesapi.registry.BiomeResourceKey;
import dev.lumas.biomes.LittleBiomes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class CachedLittleBiomes {

    public static CachedLittleBiomes INSTANCE = new CachedLittleBiomes();

    private final Map<WorldTiedChunkLocation, BiomeResourceKey> cachedChunkLocations = new HashMap<>();

    public boolean isChunkCached(WorldTiedChunkLocation location) {
        return cachedChunkLocations.containsKey(location);
    }

    public boolean isChunkCached(WorldTiedChunkLocation location, BiomeResourceKey biomeKey) {
        BiomeResourceKey cachedBiomeKey = cachedChunkLocations.get(location);
        return cachedBiomeKey != null && cachedBiomeKey.equals(biomeKey);
    }

    public boolean isWithinRadiusOfCachedChunk(WorldTiedChunkLocation chunk, BiomeResourceKey biomeKey) {
        int radius = LittleBiomes.okaeriConfig().anchorBiomeRadius();

        for (var entry : cachedChunkLocations.entrySet()) {
            WorldTiedChunkLocation cachedLocation = entry.getKey();
            BiomeResourceKey cachedBiomeKey = entry.getValue();
            if (!cachedBiomeKey.equals(biomeKey) || !cachedLocation.world().equals(chunk.world())) {
                continue;
            }

            int dx = cachedLocation.chunkX() - chunk.chunkX();
            int dz = cachedLocation.chunkZ() - chunk.chunkZ();

            if (dx * dx + dz * dz <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    public void cacheChunk(WorldTiedChunkLocation location, BiomeResourceKey biomeKey) {
        cachedChunkLocations.put(location, biomeKey);
        LittleBiomes.debug("Cached new chunk, size: %d".formatted(cachedChunkLocations.size()));
    }

    public void uncacheChunk(WorldTiedChunkLocation location) {
        cachedChunkLocations.remove(location);
        LittleBiomes.debug("Uncached chunk, size: %d".formatted(cachedChunkLocations.size()));
    }

    public Set<WorldTiedChunkLocation> getCachedChunks() {
        return cachedChunkLocations.keySet();
    }
}
