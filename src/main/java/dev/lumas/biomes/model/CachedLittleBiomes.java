package dev.lumas.biomes.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.lumas.biomes.LittleBiomes;
import dev.wyck.keys.ResourceKey;
import dev.wyck.misc.BiomePosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CachedLittleBiomes {

    public static CachedLittleBiomes INSTANCE = new CachedLittleBiomes();

    private final Map<WorldTiedChunkLocation, CachedAnchor> cachedChunkLocations = new ConcurrentHashMap<>();

    private final LoadingCache<AnchorQuery, List<SimpleBlockLocation>> anchorsByChunk = CacheBuilder.newBuilder()
            .maximumSize(4096)
            .build(CacheLoader.from(this::findAnchorsOverlapping));

    public boolean isChunkCached(WorldTiedChunkLocation location) {
        return cachedChunkLocations.containsKey(location);
    }

    public boolean isChunkCached(WorldTiedChunkLocation location, ResourceKey biomeKey) {
        CachedAnchor cachedAnchor = cachedChunkLocations.get(location);
        return cachedAnchor != null && cachedAnchor.biomeKey().equals(biomeKey);
    }

    public boolean isChunkWithinAnchorRadius(WorldTiedChunkLocation chunk, ResourceKey biomeKey) {
        return !anchorsOverlapping(chunk, biomeKey).isEmpty();
    }

    public boolean isCellWithinAnchorRadius(WorldTiedChunkLocation chunk, ResourceKey biomeKey, BiomePosition position) {
        List<SimpleBlockLocation> anchors = anchorsOverlapping(chunk, biomeKey);
        if (anchors.isEmpty()) {
            return false;
        }

        // The centre of the cell's 4x4 footprint, in half-blocks.
        long cellX = ((long) position.blockX() << 1) + 3;
        long cellZ = ((long) position.blockZ() << 1) + 3;
        long radius = radiusInHalfBlocks();

        for (SimpleBlockLocation anchor : anchors) {
            long dx = cellX - anchorCentre(anchor.x());
            long dz = cellZ - anchorCentre(anchor.z());
            if (dx * dx + dz * dz <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    public void cacheChunk(WorldTiedChunkLocation location, ResourceKey biomeKey, SimpleBlockLocation anchor) {
        cachedChunkLocations.put(location, new CachedAnchor(biomeKey, anchor));
        anchorsByChunk.invalidateAll();
        LittleBiomes.debug("Cached new chunk, size: %d".formatted(cachedChunkLocations.size()));
    }

    public void uncacheChunk(WorldTiedChunkLocation location) {
        cachedChunkLocations.remove(location);
        anchorsByChunk.invalidateAll();
        LittleBiomes.debug("Uncached chunk, size: %d".formatted(cachedChunkLocations.size()));
    }

    /**
     * Drops the memoized per-chunk anchor lists. Needed after a config reload, since they are
     * computed against the configured radius.
     */
    public void invalidateAnchorLookups() {
        anchorsByChunk.invalidateAll();
    }

    public Set<WorldTiedChunkLocation> getCachedChunks() {
        return cachedChunkLocations.keySet();
    }

    private List<SimpleBlockLocation> anchorsOverlapping(WorldTiedChunkLocation chunk, ResourceKey biomeKey) {
        return anchorsByChunk.getUnchecked(new AnchorQuery(chunk, biomeKey));
    }

    private List<SimpleBlockLocation> findAnchorsOverlapping(AnchorQuery query) {
        WorldTiedChunkLocation chunk = query.chunk();

        long minX = (long) chunk.chunkX() << 5;
        long minZ = (long) chunk.chunkZ() << 5;
        long radius = radiusInHalfBlocks();

        List<SimpleBlockLocation> anchors = new ArrayList<>();
        for (var entry : cachedChunkLocations.entrySet()) {
            CachedAnchor cachedAnchor = entry.getValue();
            if (!cachedAnchor.biomeKey().equals(query.biomeKey()) || !entry.getKey().world().equals(chunk.world())) {
                continue;
            }

            SimpleBlockLocation anchor = cachedAnchor.anchor();
            long anchorX = anchorCentre(anchor.x());
            long anchorZ = anchorCentre(anchor.z());

            // distance from the anchor to the nearest point of this chunk
            long dx = anchorX - clamp(anchorX, minX, minX + 32);
            long dz = anchorZ - clamp(anchorZ, minZ, minZ + 32);

            if (dx * dx + dz * dz <= radius * radius) {
                anchors.add(anchor);
            }
        }
        return List.copyOf(anchors);
    }

    private static long radiusInHalfBlocks() {
        return (long) LittleBiomes.okaeriConfig().anchorBiomeRadiusBlocks() << 1;
    }

    private static long anchorCentre(int blockCoordinate) {
        return ((long) blockCoordinate << 1) + 1;
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }


    public record CachedAnchor(ResourceKey biomeKey, SimpleBlockLocation anchor) { }

    private record AnchorQuery(WorldTiedChunkLocation chunk, ResourceKey biomeKey) { }
}
