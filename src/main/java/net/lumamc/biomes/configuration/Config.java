package net.lumamc.biomes.configuration;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Getter
@Accessors(fluent = true)
public class Config extends OkaeriConfig {

    // TODO: Unused
    private Set<Material> checkBlockPhysAnchorMaterials = Set.of(Material.ANVIL);

    private int anchorBiomeRadius = 4;

    private Set<OkaeriLittleBiome> littleBiomes = Set.of(new OkaeriLittleBiome()); // TODO: defaults

    @Nullable
    public OkaeriLittleBiome getLittleBiomeByName(String name) {
        return littleBiomes.stream()
                .filter(biome -> biome.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }


}
