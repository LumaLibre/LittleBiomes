package dev.lumas.biomes.model;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.Objects;

public record SimpleBlockLocation(/* transient */ World world, int x, int y, int z) {

    public static SimpleBlockLocation of(World world, int x, int y, int z) {
        return new SimpleBlockLocation(world, x, y, z);
    }

    public Location toLocation() {
        return new Location(world, x, y, z);
    }

    public boolean explicitlyMatchesBlock(Block block) {
        return Objects.equals(block.getWorld(), world)
                && block.getX() == x
                && block.getY() == y
                && block.getZ() == z;
    }

    public boolean matchesBlock(Block block) {
        return block.getX() == x && block.getY() == y && block.getZ() == z;
    }

    public static SimpleBlockLocation fromSerialized(String serialized, World world) {
        String[] parts = serialized.split(",");
        int x = Integer.parseInt(parts[0]);
        int y = Integer.parseInt(parts[1]);
        int z = Integer.parseInt(parts[2]);
        return new SimpleBlockLocation(world, x, y, z);
    }

    public String serialize() {
        return String.format("%d,%d,%d", x, y, z);
    }
}
