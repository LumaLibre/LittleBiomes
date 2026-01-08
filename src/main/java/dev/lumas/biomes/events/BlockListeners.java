package dev.lumas.biomes.events;

import com.google.common.base.Preconditions;
import me.outspending.biomesapi.BiomeUpdater;
import me.outspending.biomesapi.registry.BiomeResourceKey;
import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.model.CachedLittleBiomes;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.PlacedLittleBiome;
import dev.lumas.biomes.model.SimpleBlockLocation;
import dev.lumas.biomes.model.WorldTiedChunkLocation;
import org.bukkit.Chunk;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class BlockListeners implements Listener {

    private static final BiomeUpdater BIOME_UPDATER = BiomeUpdater.of();
    private static final BlockData AIR_BLOCK_DATA = Material.AIR.createBlockData();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
        if (!CachedLittleBiomes.INSTANCE.isChunkCached(worldTiedChunkLocation)) {
            return; // Chunk does not have an anchor in it
        }

        String serializedAnchor = KeyedData.ANCHOR_BLOCK.get(chunk);
        Preconditions.checkNotNull(serializedAnchor, "Expected to find anchor data for little biome in chunk (%d, %d) in world %s".formatted(
                chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
        ));

        SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, chunk.getWorld());

        if (!anchorLocation.matchesBlock(block)) {
            return; // Only proceed if the broken block is the anchor
        }

        PlacedLittleBiome placedLittleBiome = PlacedLittleBiome.fromChunk(chunk);
        placedLittleBiome.onRemove(block);
        CachedLittleBiomes.INSTANCE.uncacheChunk(worldTiedChunkLocation);

        Player player = event.getPlayer();

        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setDropItems(false);
            block.getWorld().dropItemNaturally(block.getLocation().toCenterLocation(), placedLittleBiome.anchorItemStack());
        }

        int radius = LittleBiomes.okaeriConfig().anchorBiomeRadius();
        BIOME_UPDATER.updateChunkRadius(chunk, radius);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack itemStack = event.getItemInHand();

        if (!KeyedData.ANCHOR.matches(itemStack)) {
            return; // Not a little biome anchor
        }

        Block block = event.getBlockPlaced();
        Block blowBelow = block.getRelative(BlockFace.DOWN);
        if (!blowBelow.isSolid()) {
            event.setCancelled(true);
            return;
        }

        // Check if the chunk already has a little biome
        Chunk chunk = block.getChunk();
        if (KeyedData.CHUNK_BIOME.matches(chunk)) {
            event.getPlayer().sendMessage("This chunk already has a little biome assigned.");
            event.setCancelled(true);
            return;
        }


        SimpleBlockLocation simpleBlockLocation = SimpleBlockLocation.of(block.getWorld(), block.getX(), block.getY(), block.getZ());

        String biomeResourceKeyString = Preconditions.checkNotNull(KeyedData.ANCHOR.get(itemStack), "Expected biome name in anchor item stack.");
        BiomeResourceKey biomeResourceKey = BiomeResourceKey.fromString(biomeResourceKeyString);


        PlacedLittleBiome placedLittleBiome = new PlacedLittleBiome(simpleBlockLocation, biomeResourceKey);
        placedLittleBiome.onPlace(block);
        WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(block.getChunk());
        CachedLittleBiomes.INSTANCE.cacheChunk(worldTiedChunkLocation, biomeResourceKey);

        int radius = LittleBiomes.okaeriConfig().anchorBiomeRadius();
        BIOME_UPDATER.updateChunkRadius(block.getChunk(), radius);
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Chunk chunk = event.getBlock().getChunk();
        WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
        if (!CachedLittleBiomes.INSTANCE.isChunkCached(worldTiedChunkLocation)) {
            return; // Chunk does not have an anchor in it
        }

        String serializedAnchor = Preconditions.checkNotNull(KeyedData.ANCHOR_BLOCK.get(chunk), "Expected to find anchor data for little biome in chunk (%d, %d) in world %s".formatted(chunk.getX(), chunk.getZ(), chunk.getWorld().getName()));
        SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, chunk.getWorld());
        for (Block block : event.getBlocks()) {
            if (anchorLocation.matchesBlock(block)) {
                event.setCancelled(true);
                return; // Cancel the event if the anchor block is being moved
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(EntityChangeBlockEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
        if (!CachedLittleBiomes.INSTANCE.isChunkCached(worldTiedChunkLocation)) {
            return; // Chunk does not have an anchor in it
        }

        Set<Material> checkBlockPhysMaterials = LittleBiomes.okaeriConfig().checkBlockPhysAnchorMaterials();
        if (!checkBlockPhysMaterials.contains(block.getType())) {
            return; // Not a material we care about
        }

        String serializedAnchor = Preconditions.checkNotNull(KeyedData.ANCHOR_BLOCK.get(chunk), "Expected to find anchor data for little biome in chunk (%d, %d) in world %s".formatted(block.getChunk().getX(), block.getChunk().getZ(), block.getWorld().getName()));
        SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, block.getWorld());
        if (!anchorLocation.matchesBlock(block)) {
            return; // Not the anchor block
        }


        String biomeName = Preconditions.checkNotNull(KeyedData.CHUNK_BIOME.get(chunk), "Expected biome name for little biome in chunk (%d, %d) in world %s".formatted(block.getChunk().getX(), block.getChunk().getZ(), block.getWorld().getName()));
        BiomeResourceKey biomeResourceKey = BiomeResourceKey.fromString(biomeName);
        PlacedLittleBiome placedLittleBiome = new PlacedLittleBiome(anchorLocation, biomeResourceKey);
        placedLittleBiome.onRemove(block);
        CachedLittleBiomes.INSTANCE.uncacheChunk(worldTiedChunkLocation);

        block.setBlockData(AIR_BLOCK_DATA); // Remove the anchor block
        block.getWorld().dropItemNaturally(block.getLocation().toCenterLocation(), placedLittleBiome.anchorItemStack());


        event.setCancelled(true); // Prevent physics updates on the anchor block


        int radius = LittleBiomes.okaeriConfig().anchorBiomeRadius();
        BIOME_UPDATER.updateChunkRadius(chunk, radius);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Chunk chunk = event.getPlayer().getLocation().getChunk();
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || !CachedLittleBiomes.INSTANCE.isChunkCached(WorldTiedChunkLocation.of(chunk))) {
            return;
        }

        String serializedAnchor = Preconditions.checkNotNull(KeyedData.ANCHOR_BLOCK.get(chunk), "Expected to find anchor data for little biome in chunk (%d, %d) in world %s".formatted(
                chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
        ));
        SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchor, chunk.getWorld());
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || !anchorLocation.matchesBlock(clickedBlock)) {
            return; // Not the anchor block
        }

        event.setCancelled(true); // Prevent any interaction with the anchor item unless it's a left click
    }
}
