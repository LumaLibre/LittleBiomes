package dev.lumas.biomes.model;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.util.Executors;
import dev.wyck.keys.ResourceKey;
import dev.wyck.renderer.updater.BiomeUpdater;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Finds anchors in chunks that are already loaded.
 */
public final class AnchorScanner {

    private static final BiomeUpdater BIOME_UPDATER = BiomeUpdater.of(LittleBiomes.instance());

    private AnchorScanner() {}

    public static void scanAround(Player player) {
        World world = player.getWorld();
        Location location = player.getLocation();
        int viewDistance = player.getViewDistance();

        int playerChunkX = location.getBlockX() >> 4;
        int playerChunkZ = location.getBlockZ() >> 4;
        for (int x = playerChunkX - viewDistance; x <= playerChunkX + viewDistance; x++) {
            for (int z = playerChunkZ - viewDistance; z <= playerChunkZ + viewDistance; z++) {
                if (world.isChunkLoaded(x, z)) {
                    scanChunk(world, x, z);
                }
            }
        }
    }

    public static void scanChunk(World world, int chunkX, int chunkZ) {
        Executors.sync(world, chunkX, chunkZ, () -> {
            if (!world.isChunkLoaded(chunkX, chunkZ)) {
                return;
            }

            Chunk chunk = world.getChunkAt(chunkX, chunkZ, false);
            if (!KeyedData.CHUNK_BIOME.matches(chunk)) {
                return;
            }

            String biomeKeyString = KeyedData.CHUNK_BIOME.get(chunk);
            String serializedAnchor = KeyedData.ANCHOR_BLOCK.get(chunk);
            if (biomeKeyString == null || serializedAnchor == null) {
                LittleBiomes.instance().getLogger().warning(
                        "Chunk (%d, %d) in world %s is tagged as a little biome but is missing its %s data; skipping.".formatted(
                                chunkX, chunkZ, world.getName(), biomeKeyString == null ? "biome key" : "anchor"
                        ));
                return;
            }

            WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
            ResourceKey biomeKey = ResourceKey.fromString(biomeKeyString);
            SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, chunk.getWorld());

            if (CachedLittleBiomes.INSTANCE.cacheChunk(worldTiedChunkLocation, biomeKey, anchorLocation)) {
                LittleBiomes.debug("Cached anchor at %d,%d in world %s from a loaded-chunk scan.".formatted(
                        chunkX, chunkZ, world.getName()
                ));
                refreshAround(chunk);
            }
        });
    }

    public static void refreshAround(Chunk chunk) {
        BIOME_UPDATER.updateChunkRadius(chunk, LittleBiomes.okaeriConfig().anchorBiomeChunkRadius());
    }
}
