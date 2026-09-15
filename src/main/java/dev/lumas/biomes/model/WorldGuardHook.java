package dev.lumas.biomes.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.World;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WorldGuardHook {

    private static final String FLAG_NAME = "little-biome";
    private static final String CHUNK_PROBE_ID = "littlebiomes_chunk_probe";

    @Getter
    private StringFlag littleBiomeFlag;

    private final LoadingCache<WorldTiedChunkLocation, Optional<String>> regionBiomeNames = CacheBuilder.newBuilder()
            .maximumSize(4096)
            .expireAfterWrite(10, TimeUnit.SECONDS)
            .build(CacheLoader.from(chunk -> Optional.ofNullable(queryLittleBiomeName(chunk))));

    public void register() {
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        try {
            this.littleBiomeFlag = new StringFlag("little-biome");
            registry.register(this.littleBiomeFlag);
        } catch (FlagConflictException | IllegalStateException e) {
            this.littleBiomeFlag = (StringFlag) registry.get(FLAG_NAME);
        }
        if (this.littleBiomeFlag == null) {
            throw new IllegalStateException("Failed to register or retrieve WorldGuard flag: " + FLAG_NAME);
        }
    }


    @Nullable
    public String getWorldGuardRegionLittleBiomeName(WorldTiedChunkLocation worldTiedChunkLocation) {
        return regionBiomeNames.getUnchecked(worldTiedChunkLocation).orElse(null);
    }


    @Nullable
    private String queryLittleBiomeName(WorldTiedChunkLocation worldTiedChunkLocation) {
        World world = worldTiedChunkLocation.world();
        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = regionContainer.get(BukkitAdapter.adapt(world));
        if (regionManager == null) {
            return null;
        }

        int minX = worldTiedChunkLocation.chunkX() << 4;
        int minZ = worldTiedChunkLocation.chunkZ() << 4;
        ProtectedRegion chunkColumn = new ProtectedCuboidRegion(
                CHUNK_PROBE_ID,
                true,
                BlockVector3.at(minX, world.getMinHeight(), minZ),
                BlockVector3.at(minX + 15, world.getMaxHeight(), minZ + 15)
        );

        // size() excludes __global__, which queryValue falls back to, so don't short-circuit on an empty set
        ApplicableRegionSet regionSet = regionManager.getApplicableRegions(chunkColumn);
        return regionSet.queryValue(null, this.littleBiomeFlag);
    }

}
