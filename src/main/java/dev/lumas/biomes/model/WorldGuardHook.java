package dev.lumas.biomes.model;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class WorldGuardHook {

    private static final String FLAG_NAME = "little-biome";

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
        Location worldEditLocation = BukkitAdapter.adapt(worldTiedChunkLocation.toLocation());
        RegionContainer regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery regionQuery = regionContainer.createQuery();
        ApplicableRegionSet regionSet = regionQuery.getApplicableRegions(worldEditLocation);

        return regionSet.queryValue(null, this.littleBiomeFlag);
    }

}
