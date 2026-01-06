package dev.lumas.biomes.configuration;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.outspending.biomesapi.wrapper.AmbientParticle;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@Getter
@Accessors(fluent = true)
public class Config extends OkaeriConfig {

    @Comment("Enable debug logging.")
    private boolean debug = true;

    @Comment("Materials that should be checked for BlockPhysicsEvent.")
    private Set<Material> checkBlockPhysAnchorMaterials = Set.of(Material.ANVIL);

    @Comment("Radius (in chunks) around an anchor to apply little biome effects.")
    private int anchorBiomeRadius = 4;

    @Comment("Particle effect to display around little biome anchors.")
    private Particle anchorParticle = Particle.WITCH;

    @Comment({
            "Defined little biomes. There's lots to configure!",
            "Use: /littlebiomes reload to reload the config after making changes.",
            "Modified biomes will require a relog to see changes."
    })
    private Set<OkaeriLittleBiome> littleBiomes = Set.of(
            OkaeriLittleBiome.basicBuilder()
                    .name("basic_blue")
                    .anchorMaterial(Material.ANVIL)
                    .anchorDisplayName("<blue><b>Basic Blue Biome Anchor")
                    .anchorLore(List.of("<gray>A simple little biome that is blue everywhere."))
                    .color("#6F8BEA")
                    .ambientParticle(AmbientParticle.END_ROD, 0.01f)
                    .blockReplacement(Material.BIRCH_LEAVES, Material.ACACIA_LEAVES)
                    .toOkaeriConfig(),
            OkaeriLittleBiome.basicBuilder()
                    .name("basic_green")
                    .anchorMaterial(Material.ANVIL)
                    .anchorDisplayName("<green><b>Basic Green Biome Anchor")
                    .anchorLore(List.of("<gray>A simple little biome that is green everywhere."))
                    .color("#6FEA8B")
                    .ambientParticle(AmbientParticle.HAPPY_VILLAGER, 0.01f)
                    .blockReplacement(Material.OAK_LEAVES, Material.JUNGLE_LEAVES)
                    .blockReplacement(Material.ICE, Material.GREEN_WOOL)
                    .toOkaeriConfig()
    );



    @Nullable
    public OkaeriLittleBiome getLittleBiomeByName(String name) {
        return littleBiomes.stream()
                .filter(biome -> biome.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
