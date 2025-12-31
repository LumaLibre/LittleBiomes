package dev.lumas.biomes.model;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.outspending.biomesapi.registry.BiomeResourceKey;
import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.configuration.OkaeriLittleBiome;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

@Getter
@Builder
@Accessors(fluent = true)
public class PlacedLittleBiome {

    private final SimpleBlockLocation anchor;
    private final BiomeResourceKey biomeKey;


    public PlacedLittleBiome(SimpleBlockLocation anchor, BiomeResourceKey biomeKey) {
        this.anchor = anchor;
        this.biomeKey = biomeKey;
    }

    public String name() {
        return biomeKey.key().value();
    }

    public void onPlace(Block block) {
        Chunk chunk = block.getChunk();
        SimpleBlockLocation simpleBlockLocation = SimpleBlockLocation.of(chunk.getWorld(), block.getX(), block.getY(), block.getZ());

        System.out.println(chunk.getPersistentDataContainer().getKeys());

        Preconditions.checkState(!KeyedData.CHUNK_BIOME.matches(chunk),
                "Chunk at (%d, %d) in world %s already has a little biome assigned.".formatted(
                        chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
                ));

        KeyedData.ANCHOR_BLOCK.set(chunk, simpleBlockLocation.serialize());
        KeyedData.CHUNK_BIOME.set(chunk, this.biomeKey.toString());

        LittleBiomes.debug("Placed little biome %s at chunk (%d, %d) in world %s with anchor at %s".formatted(
                this.biomeKey,
                chunk.getX(),
                chunk.getZ(),
                chunk.getWorld().getName(),
                simpleBlockLocation.serialize()
        ));
    }


    public void onRemove(Block block) {
        Chunk chunk = block.getChunk();

        KeyedData.ANCHOR_BLOCK.remove(chunk);
        KeyedData.CHUNK_BIOME.remove(chunk);

        LittleBiomes.debug("Removed little biome from chunk (%d, %d) in world %s".formatted(
                chunk.getX(),
                chunk.getZ(),
                chunk.getWorld().getName()
        ));
    }

    public ItemStack anchorItemStack() {
        OkaeriLittleBiome littleBiomeConfig = LittleBiomes.okaeriConfig().getLittleBiomeByName(this.name());
        Preconditions.checkNotNull(littleBiomeConfig, "No little biome config found for biome key: " + this.biomeKey);

        return littleBiomeConfig.anchorItem();
    }


    public static PlacedLittleBiome fromChunk(Chunk chunk) {
        String serializedAnchor = KeyedData.ANCHOR_BLOCK.get(chunk);

        Preconditions.checkNotNull(serializedAnchor, "Expected to find anchor data for little biome in chunk (%d, %d) in world %s".formatted(
                chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
        ));

        SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, chunk.getWorld());
        String biomeKeyString = KeyedData.CHUNK_BIOME.get(chunk);

        Preconditions.checkNotNull(biomeKeyString, "Expected to find biome key data for little biome in chunk (%d, %d) in world %s".formatted(
                chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
        ));

        BiomeResourceKey biomeKey = BiomeResourceKey.fromString(biomeKeyString);

        return new PlacedLittleBiome(anchorLocation, biomeKey);
    }


}
