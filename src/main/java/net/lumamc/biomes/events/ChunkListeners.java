package net.lumamc.biomes.events;

import com.google.common.base.Preconditions;
import me.outspending.biomesapi.registry.BiomeResourceKey;
import net.lumamc.biomes.model.CachedLittleBiomes;
import net.lumamc.biomes.model.KeyedData;
import net.lumamc.biomes.model.WorldTiedChunkLocation;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.concurrent.CompletableFuture;

public class ChunkListeners implements Listener {

    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        CompletableFuture.runAsync(() -> {
            if (!KeyedData.CHUNK_BIOME.matches(chunk)) {
                return;
            }

            WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
            String biomeKeyString = Preconditions.checkNotNull(KeyedData.CHUNK_BIOME.get(chunk), "Expected to find biome key for chunk (%d, %d) in world %s".formatted(
                    chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
            ));


            BiomeResourceKey biomeKey = BiomeResourceKey.fromString(biomeKeyString);
            CachedLittleBiomes.INSTANCE.cacheChunk(worldTiedChunkLocation, biomeKey);
        });
    }

    @EventHandler
    public void onChunkUnloadEvent(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        CompletableFuture.runAsync(() -> {
            if (!KeyedData.CHUNK_BIOME.matches(chunk)) {
                return;
            }

            WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
            CachedLittleBiomes.INSTANCE.uncacheChunk(worldTiedChunkLocation);
        });
    }
}
