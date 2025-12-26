package net.lumamc.biomes.configuration;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.outspending.biomesapi.BiomeSettings;
import me.outspending.biomesapi.biome.BiomeHandler;
import me.outspending.biomesapi.biome.CustomBiome;
import me.outspending.biomesapi.packet.PacketHandler;
import me.outspending.biomesapi.packet.data.BlockReplacement;
import me.outspending.biomesapi.packet.data.PhonyCustomBiome;
import me.outspending.biomesapi.registry.BiomeResourceKey;
import me.outspending.biomesapi.renderer.AmbientParticle;
import me.outspending.biomesapi.renderer.ParticleRenderer;
import net.lumamc.biomes.PetiteBiomes;
import net.lumamc.biomes.model.CachedLittleBiomes;
import net.lumamc.biomes.model.KeyedData;
import net.lumamc.biomes.model.WorldTiedChunkLocation;
import net.lumamc.biomes.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

import static net.lumamc.biomes.PetiteBiomes.PETITE_BIOME_NAMESPACE;

@Getter
@Accessors(fluent = true)
public class OkaeriLittleBiome extends OkaeriConfig {

    // TODO: temp defaults
    private String name = "example";
    private Material anchorMaterial = Material.ANVIL;

    private String biomeName = "example";
    private String fogColor = "#6F8BEA";
    private String waterColor = "#6F8BEA";
    private String waterFogColor = "#6F8BEA";
    private String skyColor = "#6F8BEA";
    private String foliageColor = "#6F8BEA";
    private String grassColor = "#6F8BEA";
    private Map<AmbientParticle, Float> ambientParticles = Map.of(AmbientParticle.END_ROD, 0.01f);
    private Map<Material, Material> blockReplacements = Map.of(Material.DIRT, Material.BLUE_CONCRETE_POWDER);



    public BiomeResourceKey biomeResourceKey() {
        return BiomeResourceKey.of(PETITE_BIOME_NAMESPACE, this.name);
    }

    public boolean isRegistered() {
        return BiomeHandler.isBiome(this.biomeResourceKey());
    }

    public boolean register() {
        CustomBiome customBiome = CustomBiome.builder()
                .resourceKey(this.biomeResourceKey())
                .settings(BiomeSettings.defaultSettings())
                .fogColor(fogColor)
                .foliageColor(foliageColor)
                .skyColor(skyColor)
                .waterColor(waterColor)
                .waterFogColor(waterFogColor)
                .grassColor(grassColor)
                .particleRenderer(new ParticleRenderer(ambientParticles))
                .blockReplacements(
                        blockReplacements.entrySet().stream()
                                .map(entry -> BlockReplacement.of(entry.getKey(), entry.getValue()))
                                .toArray(BlockReplacement[]::new)
                )
                .build();

        customBiome.register();
        PetiteBiomes.debug("Registered custom biome: " + this.biomeResourceKey().toString());
        return true;
    }

    public boolean modify() {
        CustomBiome customBiome = CustomBiome.builder()
                .resourceKey(this.biomeResourceKey())
                .settings(BiomeSettings.defaultSettings())
                .fogColor(fogColor)
                .foliageColor(foliageColor)
                .skyColor(skyColor)
                .waterColor(waterColor)
                .waterFogColor(waterFogColor)
                .grassColor(grassColor)
                .particleRenderer(new ParticleRenderer(ambientParticles))
                .blockReplacements(
                        blockReplacements.entrySet().stream()
                                .map(entry -> BlockReplacement.of(entry.getKey(), entry.getValue()))
                                .toArray(BlockReplacement[]::new)
                )
                .build();

        CustomBiome registeredBiome = BiomeHandler.getBiome(this.biomeResourceKey());
        if (customBiome.isSimilar(registeredBiome)) {
            PetiteBiomes.debug("No modifications detected for biome: " + this.biomeResourceKey().toString());
            return false;
        }

        customBiome.modify();
        PetiteBiomes.debug("Modified custom biome: " + this.biomeResourceKey().toString());
        return true;
    }


    public void addToPacketHandler() {
        BiomeResourceKey biomeResourceKey = this.biomeResourceKey();
        PacketHandler packetHandler = PetiteBiomes.packetHandler();

        if (packetHandler.hasBiome(biomeResourceKey)) {
            PetiteBiomes.debug("Packet handler already contains biome: " + biomeResourceKey);
            return;
        }


        PhonyCustomBiome phonyCustomBiome = PhonyCustomBiome.builder()
                .setCustomBiome(biomeResourceKey)
                .setConditional((player, chunkLocation) -> {
                    WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(player.getWorld(), chunkLocation);

                    return CachedLittleBiomes.INSTANCE.isChunkCached(worldTiedChunkLocation, biomeResourceKey) || CachedLittleBiomes.INSTANCE.isWithinRadiusOfCachedChunk(worldTiedChunkLocation, biomeResourceKey);
                })
                .build();

        packetHandler.appendBiome(phonyCustomBiome);
        PetiteBiomes.debug("Added biome to packet handler: " + this.biomeResourceKey().toString());
    }


    public ItemStack anchorItem() {
        ItemStack itemStack = new ItemStack(this.anchorMaterial);
        itemStack.editMeta(meta -> {
            meta.displayName(TextUtil.minimessage("<!b>" + this.biomeName));
            meta.addEnchant(Enchantment.LURE, 5, true);
            KeyedData.ANCHOR.set(meta, this.biomeResourceKey().toString());
        });
        return itemStack;
    }



}
