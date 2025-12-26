package net.lumamc.biomes.commands.subcommand;

import com.google.common.base.Preconditions;
import net.lumamc.biomes.commands.Subcommand;
import net.lumamc.biomes.model.CachedLittleBiomes;
import net.lumamc.biomes.model.KeyedData;
import net.lumamc.biomes.model.SimpleBlockLocation;
import net.lumamc.biomes.model.WorldTiedChunkLocation;
import net.lumamc.biomes.util.TextUtil;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class NearestAnchorCommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, String label, List<String> args) {
        int radius = !args.isEmpty() ? Integer.parseInt(args.getFirst()) : 200;

        Player player = (Player) sender;
        WorldTiedChunkLocation nearest = CachedLittleBiomes.INSTANCE.getCachedChunks().stream()
                .filter(worldTiedChunkLocation ->
                    Objects.equals(worldTiedChunkLocation.world(), player.getWorld()) && worldTiedChunkLocation.toBukkitChunk().getBlock(8, 0, 8).getLocation().distanceSquared(player.getLocation()) < radius * radius
                )
                .sorted((worldTiedChunkLocation1, worldTiedChunkLocation2) -> {
                    Chunk chunk1 = worldTiedChunkLocation1.toBukkitChunk();
                    Chunk chunk2 = worldTiedChunkLocation2.toBukkitChunk();

                    double distance1 = chunk1.getBlock(8, 0, 8).getLocation().distanceSquared(player.getLocation());
                    double distance2 = chunk2.getBlock(8, 0, 8).getLocation().distanceSquared(player.getLocation());

                    return Double.compare(distance1, distance2);
                })
                .limit(1)
                .findFirst()
                .orElse(null);

        if (nearest != null) {
            Chunk chunk = nearest.toBukkitChunk();
            String serializedAnchorLocation = Preconditions.checkNotNull(KeyedData.ANCHOR_BLOCK.get(chunk),"Expected to find anchor data for little biome in chunk (%d, %d) in world %s".formatted(
                    chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
            ));

            SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchorLocation, chunk.getWorld());

            TextUtil.msg(sender, "Nearest anchor found at: <green><click:copy_to_clipboard:'%d %d %d'><hover:show_text:'Click to copy coordinates'>[%d, %d, %d]</hover></click></green>".formatted(
                    anchorLocation.x(), anchorLocation.y(), anchorLocation.z(),
                    anchorLocation.x(), anchorLocation.y(), anchorLocation.z()
            ));
        } else {
            TextUtil.msg(sender, "No anchors found within a radius of %d blocks.".formatted(radius));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, List<String> args) {
        return List.of("<radius>");
    }

    @Override
    public Options options() {
        return Options.builder()
                .label("nearestanchor")
                .permission("petitebiomes.command.nearestanchor")
                .playerOnly(true)
                .usage("/<command> nearestanchor <radius>")
                .build();
    }
}
