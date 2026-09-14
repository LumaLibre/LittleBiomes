package dev.lumas.biomes.events;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.model.AnchorScanner;
import dev.lumas.biomes.model.CachedLittleBiomes;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.SimpleBlockLocation;
import dev.lumas.biomes.model.WorldTiedChunkLocation;
import dev.wyck.keys.ResourceKey;
import org.bukkit.Chunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class ChunkListeners implements Listener {

    @EventHandler
    public void onChunkLoadEvent(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        if (!KeyedData.CHUNK_BIOME.matches(chunk)) {
            return;
        }

        String biomeKeyString = KeyedData.CHUNK_BIOME.get(chunk);
        String serializedAnchor = KeyedData.ANCHOR_BLOCK.get(chunk);
        if (biomeKeyString == null || serializedAnchor == null) {
            LittleBiomes.instance().getLogger().warning(
                    "Chunk (%d, %d) in world %s is tagged as a little biome but is missing its %s data; skipping.".formatted(
                            chunk.getX(), chunk.getZ(), chunk.getWorld().getName(),
                            biomeKeyString == null ? "biome key" : "anchor"
                    ));
            return;
        }

        WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
        ResourceKey biomeKey = ResourceKey.fromString(biomeKeyString);
        SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, chunk.getWorld());
        if (CachedLittleBiomes.INSTANCE.cacheChunk(worldTiedChunkLocation, biomeKey, anchorLocation)) {
            AnchorScanner.refreshAround(chunk, biomeKey);
        }
    }
}
