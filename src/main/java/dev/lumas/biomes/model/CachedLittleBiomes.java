package dev.lumas.biomes.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import dev.lumas.biomes.LittleBiomes;
import dev.wyck.keys.ResourceKey;
import dev.wyck.misc.BiomePosition;
import org.bukkit.World;
import org.bukkit.entity.Player;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class CachedLittleBiomes {

    public static CachedLittleBiomes INSTANCE = new CachedLittleBiomes();

    private static final int FIRST_CELL_CENTRE = 3;
    private static final int LAST_CELL_CENTRE = 27;

    private final Map<WorldTiedChunkLocation, CachedAnchor> cachedChunkLocations = new ConcurrentHashMap<>();

    private final LoadingCache<AnchorQuery, ChunkCoverage> coverageByChunk = CacheBuilder.newBuilder()
            .maximumSize(4096)
            .build(CacheLoader.from(this::computeCoverage));

    private static final long MEMO_TTL_NANOS = TimeUnit.SECONDS.toNanos(1);
    private final AtomicLong generation = new AtomicLong();

    private static final ThreadLocal<RenderingChunk> RENDERING = ThreadLocal.withInitial(RenderingChunk::new);

    public boolean isChunkCached(WorldTiedChunkLocation location) {
        return cachedChunkLocations.containsKey(location);
    }

    public boolean isChunkCached(WorldTiedChunkLocation location, ResourceKey biomeKey) {
        CachedAnchor cachedAnchor = cachedChunkLocations.get(location);
        return cachedAnchor != null && cachedAnchor.biomeKey().equals(biomeKey);
    }

    public boolean chunkMatches(Player player, int chunkX, int chunkZ, ResourceKey biomeKey) {
        World world = player.getWorld();
        RenderedChunkState state = stateFor(player, world, chunkX, chunkZ, biomeKey);
        if (state.regionMatch() || !state.coverage().anchors().isEmpty()) {
            return true;
        }

        if (!state.personalActive()) {
            return false;
        }
        return !state.foreignRegion()
                && !state.foreignCoverage(this, world, chunkX, chunkZ, biomeKey).fullyCovered();
    }

    public boolean chunkFullyMatches(Player player, int chunkX, int chunkZ, ResourceKey biomeKey) {
        World world = player.getWorld();
        RenderedChunkState state = stateFor(player, world, chunkX, chunkZ, biomeKey);

        if (state.regionMatch() || state.coverage().fullyCovered()) {
            return true;
        }

        return state.personalActive()
                && !state.foreignRegion()
                && state.foreignCoverage(this, world, chunkX, chunkZ, biomeKey).anchors().isEmpty();
    }

    private RenderedChunkState stateFor(Player player, World world, int chunkX, int chunkZ, ResourceKey biomeKey) {
        long currentGeneration = this.generation.get();
        long now = System.nanoTime();

        RenderingChunk memo = RENDERING.get();
        RenderedChunkState state = memo.recall(player, world, chunkX, chunkZ, biomeKey, currentGeneration, now);
        if (state == null) {
            WorldTiedChunkLocation chunk = WorldTiedChunkLocation.of(world, chunkX, chunkZ);
            state = new RenderedChunkState(
                    coverageOf(chunk, biomeKey, false),
                    regionBiomeName(chunk),
                    biomeKey,
                    PersonalBiomes.INSTANCE.isActive(player, world, biomeKey));
            memo.remember(player, world, chunkX, chunkZ, biomeKey, state, currentGeneration, now);
        }
        return state;
    }

    public boolean cellMatches(Player player, int chunkX, int chunkZ, ResourceKey biomeKey, BiomePosition position) {
        World world = player.getWorld();
        RenderedChunkState state = stateFor(player, world, chunkX, chunkZ, biomeKey);
        
        ChunkCoverage coverage = state.coverage();
        if (state.regionMatch() || coverage.fullyCovered() || covers(coverage.anchors(), position)) {
            return true;
        }

        if (!state.personalActive() || state.foreignRegion()) {
            return false;
        }

        ChunkCoverage foreign = state.foreignCoverage(this, world, chunkX, chunkZ, biomeKey);
        return !foreign.fullyCovered() && !covers(foreign.anchors(), position);
    }

    private static boolean covers(List<SimpleBlockLocation> anchors, BiomePosition position) {
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

    public boolean cacheChunk(WorldTiedChunkLocation location, ResourceKey biomeKey, SimpleBlockLocation anchor) {
        CachedAnchor cachedAnchor = new CachedAnchor(biomeKey, anchor);
        if (cachedAnchor.equals(cachedChunkLocations.put(location, cachedAnchor))) {
            return false; // re-reported on chunk load; the lookups are still good
        }

        invalidateAnchorLookups();
        LittleBiomes.debug("Cached new chunk, size: %d".formatted(cachedChunkLocations.size()));
        return true;
    }

    public void uncacheChunk(WorldTiedChunkLocation location) {
        if (cachedChunkLocations.remove(location) == null) {
            return;
        }

        invalidateAnchorLookups();
        LittleBiomes.debug("Uncached chunk, size: %d".formatted(cachedChunkLocations.size()));
    }

    /**
     * Drops the memoized per-chunk coverage. Needed after a config reload, since it is computed
     * against the configured radius.
     */
    public void invalidateAnchorLookups() {
        coverageByChunk.invalidateAll();
        generation.incrementAndGet();
    }

    public Set<WorldTiedChunkLocation> getCachedChunks() {
        return cachedChunkLocations.keySet();
    }

    private ChunkCoverage coverageOf(WorldTiedChunkLocation chunk, ResourceKey biomeKey, boolean foreign) {
        if (cachedChunkLocations.isEmpty()) {
            return ChunkCoverage.EMPTY;
        }
        return coverageByChunk.getUnchecked(new AnchorQuery(chunk, biomeKey, foreign));
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
            if (cachedAnchor.biomeKey().equals(query.biomeKey()) == query.foreign()
                    || !entry.getKey().world().equals(chunk.world())) {
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

    @Nullable
    private static String regionBiomeName(WorldTiedChunkLocation chunk) {
        WorldGuardHook worldGuardHook = LittleBiomes.worldGuardHook();
        if (worldGuardHook == null) {
            return null;
        }

        return worldGuardHook.getWorldGuardRegionLittleBiomeName(chunk);
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


    private static final class RenderingChunk {

        private static final int MAX_BIOMES = 16;

        private final ResourceKey[] keys = new ResourceKey[MAX_BIOMES];
        private final RenderedChunkState[] states = new RenderedChunkState[MAX_BIOMES];
        private int size;

        private @Nullable Player player;
        private @Nullable World world;
        private int chunkX;
        private int chunkZ;
        private long generation = Long.MIN_VALUE;
        private long expiresAtNanos;

        @Nullable
        RenderedChunkState recall(Player player, World world, int chunkX, int chunkZ, ResourceKey biomeKey, long generation, long now) {
            if (!holds(player, world, chunkX, chunkZ, generation, now)) {
                return null;
            }
            for (int i = 0; i < this.size; i++) {
                if (this.keys[i] == biomeKey) {
                    return this.states[i];
                }
            }
            return null;
        }

        void remember(Player player, World world, int chunkX, int chunkZ, ResourceKey biomeKey, RenderedChunkState state, long generation, long now) {
            if (!holds(player, world, chunkX, chunkZ, generation, now)) {
                this.player = player;
                this.world = world;
                this.chunkX = chunkX;
                this.chunkZ = chunkZ;
                this.generation = generation;
                this.expiresAtNanos = now + MEMO_TTL_NANOS;
                this.size = 0;
            }
            for (int i = 0; i < this.size; i++) {
                if (this.keys[i] == biomeKey) {
                    this.states[i] = state;
                    return;
                }
            }
            if (this.size < MAX_BIOMES) {
                this.keys[this.size] = biomeKey;
                this.states[this.size] = state;
                this.size++;
            }
        }

        private boolean holds(Player player, World world, int chunkX, int chunkZ, long generation, long now) {
            return this.generation == generation
                    && this.chunkX == chunkX
                    && this.chunkZ == chunkZ
                    && now - this.expiresAtNanos < 0
                    && this.world == world
                    && this.player == player;
        }
    }


    private static final class RenderedChunkState {

        private final ChunkCoverage coverage;
        private final boolean regionMatch;
        private final boolean foreignRegion;
        private final boolean personalActive;

        private @Nullable ChunkCoverage foreignCoverage;

        RenderedChunkState(ChunkCoverage coverage, @Nullable String regionBiomeName, ResourceKey biomeKey, boolean personalActive) {
            this.coverage = coverage;
            this.regionMatch = biomeKey.key().value().equalsIgnoreCase(regionBiomeName);
            this.foreignRegion = regionBiomeName != null && !this.regionMatch;
            this.personalActive = personalActive;
        }

        boolean personalActive() {
            return this.personalActive;
        }

        ChunkCoverage coverage() {
            return this.coverage;
        }

        boolean regionMatch() {
            return this.regionMatch;
        }

        boolean foreignRegion() {
            return this.foreignRegion;
        }

        ChunkCoverage foreignCoverage(CachedLittleBiomes owner, World world, int chunkX, int chunkZ, ResourceKey biomeKey) {
            if (this.foreignCoverage == null) {
                this.foreignCoverage = owner.coverageOf(WorldTiedChunkLocation.of(world, chunkX, chunkZ), biomeKey, true);
            }
            return this.foreignCoverage;
        }
    }


    public record ChunkCoverage(List<SimpleBlockLocation> anchors, boolean fullyCovered) {

        private static final ChunkCoverage EMPTY = new ChunkCoverage(List.of(), false);
    }

    public record CachedAnchor(ResourceKey biomeKey, SimpleBlockLocation anchor) { }

    private record AnchorQuery(WorldTiedChunkLocation chunk, ResourceKey biomeKey, boolean foreign) { }
}
