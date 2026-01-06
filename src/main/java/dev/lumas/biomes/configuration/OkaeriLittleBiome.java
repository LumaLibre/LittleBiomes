package dev.lumas.biomes.configuration;

import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.outspending.biomesapi.biome.BiomeHandler;
import me.outspending.biomesapi.biome.CustomBiome;
import me.outspending.biomesapi.packet.PacketHandler;
import me.outspending.biomesapi.packet.data.BlockReplacement;
import me.outspending.biomesapi.packet.data.PhonyCustomBiome;
import me.outspending.biomesapi.registry.BiomeResourceKey;
import me.outspending.biomesapi.renderer.ParticleRenderer;
import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.events.BadRegistryPrevention;
import dev.lumas.biomes.model.CachedLittleBiomes;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.WorldTiedChunkLocation;
import dev.lumas.biomes.util.TextUtil;
import me.outspending.biomesapi.wrapper.AmbientParticle;
import me.outspending.biomesapi.wrapper.BiomeSettings;
import me.outspending.biomesapi.wrapper.GrassColorModifier;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.lumas.biomes.LittleBiomes.LITTLE_BIOME_NAMESPACE;

@Getter
@Accessors(fluent = true)
public class OkaeriLittleBiome extends OkaeriConfig {

    private String name;
    private Material anchorMaterial;

    private String anchorDisplayName;
    private List<String> anchorLore;
    private String fogColor;
    private String waterColor;
    private String waterFogColor;
    private String skyColor;
    private String foliageColor;
    private String grassColor;
    private GrassColorModifier grassColorModifier;
    private PacketHandler.Priority biomePriority;
    private Map<AmbientParticle, Float> ambientParticles;
    private Map<Material, Material> blockReplacements;

    public BiomeResourceKey biomeResourceKey() {
        return BiomeResourceKey.of(LITTLE_BIOME_NAMESPACE, this.name);
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
        LittleBiomes.debug("Registered custom biome: " + this.biomeResourceKey().toString());
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
            LittleBiomes.debug("No modifications detected for biome: " + this.biomeResourceKey().toString());
            return false;
        }

        customBiome.modify();
        LittleBiomes.debug("Modified custom biome: " + this.biomeResourceKey().toString());
        return true;
    }


    public void addToPacketHandler() {
        BiomeResourceKey biomeResourceKey = this.biomeResourceKey();
        PacketHandler packetHandler = LittleBiomes.packetHandler();

        if (packetHandler.hasBiome(biomeResourceKey)) {
            LittleBiomes.debug("Packet handler already contains biome: " + biomeResourceKey);
            return;
        }


        PhonyCustomBiome phonyCustomBiome = PhonyCustomBiome.builder()
                .setCustomBiome(biomeResourceKey)
                .setConditional((player, chunkLocation) -> {
                    if (BadRegistryPrevention.shouldPrevent(biomeResourceKey, player)) {
                        return false;
                    }

                    WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(player.getWorld(), chunkLocation);
                    return CachedLittleBiomes.INSTANCE.isChunkCached(worldTiedChunkLocation, biomeResourceKey) || CachedLittleBiomes.INSTANCE.isWithinRadiusOfCachedChunk(worldTiedChunkLocation, biomeResourceKey);
                })
                .build();

        packetHandler.appendBiome(phonyCustomBiome);
        LittleBiomes.debug("Added biome to packet handler: " + this.biomeResourceKey().toString());
    }


    public ItemStack anchorItem() {
        ItemStack itemStack = new ItemStack(this.anchorMaterial);
        itemStack.editMeta(meta -> {
            meta.displayName(TextUtil.minimessage("<!i>" + this.anchorDisplayName));
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.lore(this.anchorLore.stream()
                    .map(line -> TextUtil.minimessage("<!i>" + line))
                    .toList()
            );
            KeyedData.ANCHOR.set(meta, this.biomeResourceKey().toString());
        });
        return itemStack;
    }



    public static BasicBuilder basicBuilder() {
        return new BasicBuilder();
    }

    public static class BasicBuilder {
        private String name;
        private Material anchorMaterial;
        private String anchorDisplayName;
        private List<String> anchorLore;
        private String color;
        private GrassColorModifier grassColorModifier = GrassColorModifier.NONE;
        private Map<AmbientParticle, Float> ambientParticles = new HashMap<>();
        private Map<Material, Material> blockReplacements = new HashMap<>();


        public BasicBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BasicBuilder anchorMaterial(Material anchorMaterial) {
            this.anchorMaterial = anchorMaterial;
            return this;
        }

        public BasicBuilder anchorDisplayName(String anchorDisplayName) {
            this.anchorDisplayName = anchorDisplayName;
            return this;
        }

        public BasicBuilder anchorLore(List<String> anchorLore) {
            this.anchorLore = anchorLore;
            return this;
        }

        public BasicBuilder color(String color) {
            this.color = color;
            return this;
        }

        public BasicBuilder ambientParticle(AmbientParticle particle, float density) {
            this.ambientParticles.put(particle, density);
            return this;
        }

        public BasicBuilder blockReplacement(Material from, Material to) {
            this.blockReplacements.put(from, to);
            return this;
        }


        public OkaeriLittleBiome toOkaeriConfig() {
            OkaeriLittleBiome config = new OkaeriLittleBiome();
            config.name = this.name;
            config.anchorMaterial = this.anchorMaterial;
            config.anchorDisplayName = this.anchorDisplayName;
            config.anchorLore = this.anchorLore;
            config.fogColor = this.color;
            config.waterColor = this.color;
            config.waterFogColor = this.color;
            config.skyColor = this.color;
            config.foliageColor = this.color;
            config.grassColor = this.color;
            config.grassColorModifier = this.grassColorModifier;
            config.biomePriority = PacketHandler.Priority.NORMAL;
            config.ambientParticles = this.ambientParticles;
            config.blockReplacements = this.blockReplacements;
            return config;
        }
    }

}
