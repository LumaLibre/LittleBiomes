package dev.lumas.biomes.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.lumas.biomes.LittleBiomes;
import dev.wyck.keys.ResourceKey;
import dev.wyck.misc.BiomePosition;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CachedLittleBiomes {

    public static CachedLittleBiomes INSTANCE = new CachedLittleBiomes();

    private static final int FIRST_CELL_CENTRE = 3;
    private static final int LAST_CELL_CENTRE = 27;

    private final Map<WorldTiedChunkLocation, CachedAnchor> cachedChunkLocations = new ConcurrentHashMap<>();

    private final LoadingCache<AnchorQuery, ChunkCoverage> coverageByChunk = CacheBuilder.newBuilder()
            .maximumSize(4096)
            .build(CacheLoader.from(this::computeCoverage));

    private static final ThreadLocal<RenderingChunk> RENDERING = ThreadLocal.withInitial(RenderingChunk::new);

    public boolean isChunkCached(WorldTiedChunkLocation location) {
        return cachedChunkLocations.containsKey(location);
    }

    public boolean isChunkCached(WorldTiedChunkLocation location, ResourceKey biomeKey) {
        CachedAnchor cachedAnchor = cachedChunkLocations.get(location);
        return cachedAnchor != null && cachedAnchor.biomeKey().equals(biomeKey);
    }

    public boolean chunkMatches(WorldTiedChunkLocation chunk, ResourceKey biomeKey) {
        ChunkCoverage coverage = coverageOf(chunk, biomeKey);
        boolean regionMatch = matchesWorldGuardRegion(chunk, biomeKey);
        RENDERING.get().remember(chunk, biomeKey, coverage, regionMatch);
        return regionMatch || !coverage.anchors().isEmpty();
    }

    public boolean cellMatches(WorldTiedChunkLocation chunk, ResourceKey biomeKey, BiomePosition position) {
        RenderedChunkState state = RENDERING.get().recall(chunk, biomeKey);
        if (state == null) {
            state = new RenderedChunkState(coverageOf(chunk, biomeKey), matchesWorldGuardRegion(chunk, biomeKey)); // no chunk gate ran on this thread
        }

        ChunkCoverage coverage = state.coverage();
        if (state.regionMatch() || coverage.fullyCovered()) {
            return true;
        }
        List<SimpleBlockLocation> anchors = coverage.anchors();
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
        CachedAnchor cachedAnchor = new CachedAnchor(biomeKey, anchor);
        if (cachedAnchor.equals(cachedChunkLocations.put(location, cachedAnchor))) {
            return; // re-reported on chunk load; the lookups are still good
        }

        coverageByChunk.invalidateAll();
        LittleBiomes.debug("Cached new chunk, size: %d".formatted(cachedChunkLocations.size()));
    }

    public void uncacheChunk(WorldTiedChunkLocation location) {
        if (cachedChunkLocations.remove(location) == null) {
            return;
        }

        coverageByChunk.invalidateAll();
        LittleBiomes.debug("Uncached chunk, size: %d".formatted(cachedChunkLocations.size()));
    }

    /**
     * Drops the memoized per-chunk coverage. Needed after a config reload, since it is computed
     * against the configured radius.
     */
    public void invalidateAnchorLookups() {
        coverageByChunk.invalidateAll();
    }

    public Set<WorldTiedChunkLocation> getCachedChunks() {
        return cachedChunkLocations.keySet();
    }

    private ChunkCoverage coverageOf(WorldTiedChunkLocation chunk, ResourceKey biomeKey) {
        return coverageByChunk.getUnchecked(new AnchorQuery(chunk, biomeKey));
    }

    private ChunkCoverage computeCoverage(AnchorQuery query) {
        WorldTiedChunkLocation chunk = query.chunk();

        long minX = (long) chunk.chunkX() << 5;
        long minZ = (long) chunk.chunkZ() << 5;
        long radius = radiusInHalfBlocks();
        long radiusSquared = radius * radius;

        List<SimpleBlockLocation> anchors = new ArrayList<>();
        boolean fullyCovered = false;
        for (var entry : cachedChunkLocations.entrySet()) {
            CachedAnchor cachedAnchor = entry.getValue();
            if (!cachedAnchor.biomeKey().equals(query.biomeKey()) || !entry.getKey().world().equals(chunk.world())) {
                continue;
            }

            SimpleBlockLocation anchor = cachedAnchor.anchor();
            long anchorX = anchorCentre(anchor.x());
            long anchorZ = anchorCentre(anchor.z());

            // distance from the anchor to the nearest point of this chunk
            long nearX = anchorX - clamp(anchorX, minX, minX + 32);
            long nearZ = anchorZ - clamp(anchorZ, minZ, minZ + 32);
            if (nearX * nearX + nearZ * nearZ > radiusSquared) {
                continue;
            }
            anchors.add(anchor);

            long farX = furthestCellCentreDistance(anchorX, minX);
            long farZ = furthestCellCentreDistance(anchorZ, minZ);
            if (farX * farX + farZ * farZ <= radiusSquared) {
                fullyCovered = true;
            }
        }
        return new ChunkCoverage(List.copyOf(anchors), fullyCovered);
    }

    private static long furthestCellCentreDistance(long anchorCoordinate, long chunkMin) {
        long low = Math.abs(anchorCoordinate - (chunkMin + FIRST_CELL_CENTRE));
        long high = Math.abs(anchorCoordinate - (chunkMin + LAST_CELL_CENTRE));
        return Math.max(low, high);
    }

    private static boolean matchesWorldGuardRegion(WorldTiedChunkLocation chunk, ResourceKey biomeKey) {
        WorldGuardHook worldGuardHook = LittleBiomes.worldGuardHook();
        if (worldGuardHook == null) {
            return false;
        }

        return biomeKey.key().value().equalsIgnoreCase(worldGuardHook.getWorldGuardRegionLittleBiomeName(chunk));
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


    /**
     * One thread's view of the chunk it is rendering. Written by the chunk-level gate, read by
     * every cell of that chunk, and reset as soon as the gate runs for a different chunk.
     */
    private static final class RenderingChunk {

        private @Nullable WorldTiedChunkLocation chunk;
        private final Map<ResourceKey, RenderedChunkState> stateByBiome = new HashMap<>();

        void remember(WorldTiedChunkLocation chunk, ResourceKey biomeKey, ChunkCoverage coverage, boolean regionMatch) {
            if (!chunk.equals(this.chunk)) {
                this.chunk = chunk;
                this.stateByBiome.clear();
            }
            this.stateByBiome.put(biomeKey, new RenderedChunkState(coverage, regionMatch));
        }

        @Nullable
        RenderedChunkState recall(WorldTiedChunkLocation chunk, ResourceKey biomeKey) {
            return chunk.equals(this.chunk) ? this.stateByBiome.get(biomeKey) : null;
        }
    }


    public record ChunkCoverage(List<SimpleBlockLocation> anchors, boolean fullyCovered) { }

    private record RenderedChunkState(ChunkCoverage coverage, boolean regionMatch) { }

    public record CachedAnchor(ResourceKey biomeKey, SimpleBlockLocation anchor) { }

    private record AnchorQuery(WorldTiedChunkLocation chunk, ResourceKey biomeKey) { }
}
