package dev.lumas.biomes.events;

import com.google.common.base.Preconditions;
import dev.lumas.biomes.model.CachedLittleBiomes;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.SimpleBlockLocation;
import dev.lumas.biomes.model.WorldTiedChunkLocation;
import dev.wyck.keys.ResourceKey;
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
            String serializedAnchor = Preconditions.checkNotNull(KeyedData.ANCHOR_BLOCK.get(chunk), "Expected to find anchor data for little biome in chunk (%d, %d) in world %s".formatted(
                    chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
            ));


            ResourceKey biomeKey = ResourceKey.fromString(biomeKeyString);
            SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, chunk.getWorld());
            CachedLittleBiomes.INSTANCE.cacheChunk(worldTiedChunkLocation, biomeKey, anchorLocation);
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
