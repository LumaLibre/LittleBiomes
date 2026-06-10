package dev.lumas.biomes.configuration;

import dev.lumas.biomes.enums.SimpleParticleData;
import dev.lumas.biomes.model.WorldGuardHook;
import eu.okaeri.configs.OkaeriConfig;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.outspending.biomesapi.biome.CustomBiome;
import me.outspending.biomesapi.biome.RegisteredBiomes;
import me.outspending.biomesapi.keys.ResourceKey;
import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.events.BadRegistryPrevention;
import dev.lumas.biomes.model.CachedLittleBiomes;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.WorldTiedChunkLocation;
import dev.lumas.biomes.util.TextUtil;
import me.outspending.biomesapi.renderer.packet.PacketHandler;
import me.outspending.biomesapi.renderer.packet.data.BlockReplacement;
import me.outspending.biomesapi.renderer.packet.data.PhonyCustomBiome;
import me.outspending.biomesapi.wrapper.BiomeSettings;
import me.outspending.biomesapi.wrapper.environment.GrassColorModifier;
import me.outspending.biomesapi.wrapper.environment.attribute.IntColorSupplier;
import me.outspending.biomesapi.wrapper.environment.attribute.WrappedEnvironmentAttribute;
import me.outspending.biomesapi.wrapper.environment.attribute.WrappedEnvironmentAttributeMap;
import me.outspending.biomesapi.wrapper.environment.attribute.WrappedEnvironmentAttributeSupplier;
import me.outspending.biomesapi.wrapper.environment.attribute.WrappedEnvironmentAttributes;
import me.outspending.biomesapi.wrapper.environment.particle.ParticleCatalog;
import me.outspending.biomesapi.wrapper.environment.particle.ParticleData;
import me.outspending.biomesapi.wrapper.environment.particle.WrappedParticleTypes;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.lumas.biomes.LittleBiomes.LITTLE_BIOME_NAMESPACE;

@Getter
@Accessors(fluent = true)
// TODO: Add support for complex particle types
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
    private Map<WrappedParticleTypes, Float> ambientParticles;
    private Map<SimpleParticleData, String> ambientParticleData;
    private Map<Material, Material> blockReplacements;
    private Map<String, Object> environmentAttributes;


    public ResourceKey ResourceKey() {
        return ResourceKey.of(LITTLE_BIOME_NAMESPACE, this.name);
    }

    public boolean isRegistered() {
        return RegisteredBiomes.isRegistered(this.ResourceKey());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public CustomBiome customBiome() {
        ParticleCatalog particleCatalog = createParticleCatalog();

        List<WrappedEnvironmentAttribute<?, ?>> wrappedEnvironmentAttributes = new ArrayList<>();
        for (var entry : (this.environmentAttributes != null ? this.environmentAttributes.entrySet() : new HashMap<String, Object>().entrySet())) {
            WrappedEnvironmentAttributeSupplier supplier = WrappedEnvironmentAttributes.byId(entry.getKey());
            if (supplier == null) {
                LittleBiomes.debug("Unknown environment attribute: " + entry.getKey());
                continue;
            }

            Object value = entry.getValue();

            // IntColorSupplier accepts hex strings; convert before unboxing.
            if (supplier instanceof IntColorSupplier && value instanceof String hex) {
                value = IntColorSupplier.parseHex(hex);
            } else {
                value = coerceNumber(value, supplier);
            }

            WrappedEnvironmentAttribute attr = supplier.unbox(value);
            wrappedEnvironmentAttributes.add(attr);
        }

        return CustomBiome.builder()
                .resourceKey(this.ResourceKey())
                .settings(BiomeSettings.defaultSettings())
                .fogColor(fogColor)
                .foliageColor(foliageColor)
                .dryFoliageColor(foliageColor)
                .skyColor(skyColor)
                .waterColor(waterColor)
                .waterFogColor(waterFogColor)
                .grassColor(grassColor)
                .particleCatalog(particleCatalog)
                .blockReplacements(
                        blockReplacements.entrySet().stream()
                                .map(entry -> BlockReplacement.of(entry.getKey(), entry.getValue()))
                                .toArray(BlockReplacement[]::new)
                )
                .setAttributes(WrappedEnvironmentAttributeMap.of(
                        wrappedEnvironmentAttributes.toArray(new WrappedEnvironmentAttribute[0])))
                .build();
    }


    public void register() {
        CustomBiome customBiome = customBiome();

        customBiome.register();
        LittleBiomes.debug("Registered custom biome: " + this.ResourceKey().toString());
    }


    public void modify() {
        CustomBiome customBiome = customBiome();

        CustomBiome registeredBiome = RegisteredBiomes.get(this.ResourceKey());
        if (registeredBiome == null || customBiome.isSimilar(registeredBiome)) {
            LittleBiomes.debug("No modifications detected for biome: " + this.ResourceKey().toString());
            return;
        }

        customBiome.modify();
        LittleBiomes.debug("Modified custom biome: " + this.ResourceKey().toString());
    }


    public void addToPacketHandler() {
        ResourceKey ResourceKey = this.ResourceKey();
        PacketHandler packetHandler = LittleBiomes.packetHandler();

        if (packetHandler.hasBiome(ResourceKey)) {
            LittleBiomes.debug("Packet handler already contains biome: " + ResourceKey);
            return;
        }


        PhonyCustomBiome phonyCustomBiome = PhonyCustomBiome.builder()
                .setCustomBiome(ResourceKey)
                .setConditional((player, chunkLocation) -> {
                    if (BadRegistryPrevention.shouldPrevent(ResourceKey, player)) {
                        return false;
                    }

                    WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(player.getWorld(), chunkLocation);
                    if (CachedLittleBiomes.INSTANCE.isChunkCached(worldTiedChunkLocation, ResourceKey) || CachedLittleBiomes.INSTANCE.isWithinRadiusOfCachedChunk(worldTiedChunkLocation, ResourceKey)) {
                        return true;
                    }

                    WorldGuardHook worldGuardHook = LittleBiomes.worldGuardHook();
                    if (worldGuardHook == null) {
                        return false;
                    }

                    String worldguardRegionLittleBiomeName = worldGuardHook.getWorldGuardRegionLittleBiomeName(worldTiedChunkLocation);
                    return ResourceKey.key().value().equalsIgnoreCase(worldguardRegionLittleBiomeName);
                })
                .build();

        packetHandler.appendBiome(phonyCustomBiome);
        LittleBiomes.debug("Added biome to packet handler: " + this.ResourceKey().toString());
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
            KeyedData.ANCHOR.set(meta, this.ResourceKey().toString());
        });
        return itemStack;
    }


    private ParticleCatalog createParticleCatalog() {
        ParticleCatalog.Builder particleCatalog = ParticleCatalog.builder();
        for (var entry : ambientParticles.entrySet()) {
            WrappedParticleTypes wrappedType = entry.getKey();
            float probability = entry.getValue();
            if (wrappedType.isSimple()) {
                particleCatalog.addSimple(wrappedType, probability);
            } else {
                SimpleParticleData simpleParticleData = SimpleParticleData.fromParticleData(wrappedType.getParticleDataClass());
                String context = ambientParticleData.get(simpleParticleData);
                ParticleData converted = simpleParticleData.create(context);

                particleCatalog.addComplex(wrappedType, probability, converted);
            }
        }
        return particleCatalog.build();
    }


    /**
     * Coerces a YAML-parsed number to the type expected by the supplier.
     * SnakeYAML parses 0.5 as Double, but Float attributes need Float — this bridges that gap.
     * Returns the value unchanged if it's not a Number or already matches.
     */
    private static Object coerceNumber(Object value, WrappedEnvironmentAttributeSupplier<?, ?> supplier) {
        if (!(value instanceof Number n)) return value;
        Object def = supplier.get().getAttribute().defaultValue();
        Class<?> expected = def.getClass();
        if (expected.isInstance(value)) return value;
        if (expected == Integer.class) return n.intValue();
        if (expected == Float.class) return n.floatValue();
        if (expected == Double.class) return n.doubleValue();
        if (expected == Long.class) return n.longValue();
        return value;
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
        private Map<WrappedParticleTypes, Float> ambientParticles = new HashMap<>();
        private Map<SimpleParticleData, String> ambientParticleData = new HashMap<>();
        private Map<Material, Material> blockReplacements = new HashMap<>();
        private Map<String, Object> environmentAttributes = new HashMap<>();


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

        public BasicBuilder ambientParticle(WrappedParticleTypes particle, float probability) {
            this.ambientParticles.put(particle, probability);
            return this;
        }

        public BasicBuilder ambientParticleData(SimpleParticleData particleData, String context) {
            this.ambientParticleData.put(particleData, context);
            return this;
        }

        public BasicBuilder blockReplacement(Material from, Material to) {
            this.blockReplacements.put(from, to);
            return this;
        }

        public BasicBuilder environmentAttribute(WrappedEnvironmentAttributeSupplier<?, ?> attribute, Object value) {
            this.environmentAttributes.put(attribute.get().getAttribute().key(), value);
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
            config.ambientParticleData = this.ambientParticleData;
            config.blockReplacements = this.blockReplacements;
            config.environmentAttributes = this.environmentAttributes;
            return config;
        }
    }

}
