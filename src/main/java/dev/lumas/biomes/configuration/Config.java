package dev.lumas.biomes.configuration;

import dev.lumas.biomes.enums.SimpleParticleData;
import dev.wyck.environment.attribute.EnvironmentAttributes;
import dev.wyck.environment.particle.ParticleTypes;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.experimental.Accessors;
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


    @Comment("Cooldown to break an anchor in seconds. Set to <= 0 to disable.")
    private int anchorBreakCooldown = 3;

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
                    .ambientParticle(ParticleTypes.END_ROD, 0.01f)
                    .blockReplacement(Material.BIRCH_LEAVES, Material.ACACIA_LEAVES)
                    .environmentAttribute(EnvironmentAttributes.BLOCK_LIGHT_TINT, "#FF10F0")
                    .toOkaeriConfig(),
            OkaeriLittleBiome.basicBuilder()
                    .name("basic_green")
                    .anchorMaterial(Material.ANVIL)
                    .anchorDisplayName("<green><b>Basic Green Biome Anchor")
                    .anchorLore(List.of("<gray>A simple little biome that is green everywhere."))
                    .color("#6FEA8B")
                    .ambientParticle(ParticleTypes.HAPPY_VILLAGER, 0.01f)
                    .blockReplacement(Material.OAK_LEAVES, Material.JUNGLE_LEAVES)
                    .blockReplacement(Material.ICE, Material.GREEN_WOOL)
                    .toOkaeriConfig(),
            OkaeriLittleBiome.basicBuilder()
                    .name("basic_purple")
                    .anchorMaterial(Material.ANVIL)
                    .anchorDisplayName("<b><#C7B2F1>Basic Purple Biome Anchor")
                    .anchorLore(List.of(
                            "<gray>A simple biome that's purple",
                            "<gray>everywhere.",
                            "",
                            "<gray>Place to change all nearby",
                            "<gray>chunks to this biome!"
                    ))
                    .color("#C7B2F1")
                    .ambientParticle(ParticleTypes.DUST, 0.01f)
                    .ambientParticleData(SimpleParticleData.DUST, "#C7B2F1")
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
