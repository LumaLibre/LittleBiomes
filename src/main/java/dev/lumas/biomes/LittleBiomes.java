package dev.lumas.biomes;

import com.google.common.base.Preconditions;
import dev.lumas.biomes.configuration.serdes.EnumTransformers;
import dev.lumas.biomes.enums.SimpleParticleData;
import dev.lumas.biomes.model.WorldGuardHook;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;
import lombok.experimental.Accessors;
import dev.lumas.biomes.commands.CommandManager;
import dev.lumas.biomes.configuration.Config;
import dev.lumas.biomes.events.BlockListeners;
import dev.lumas.biomes.events.ChunkListeners;
import dev.lumas.biomes.events.BadRegistryPrevention;
import dev.lumas.biomes.model.CachedLittleBiomes;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.SimpleBlockLocation;
import dev.lumas.biomes.model.WorldTiedChunkLocation;
import dev.lumas.biomes.util.Executors;
import me.outspending.biomesapi.keys.ResourceKey;
import me.outspending.biomesapi.renderer.packet.PacketHandler;
import me.outspending.biomesapi.wrapper.environment.GrassColorModifier;
import me.outspending.biomesapi.wrapper.environment.particle.WrappedParticleTypes;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Accessors(fluent = true)
public final class LittleBiomes extends JavaPlugin {

    public static final String LITTLE_BIOME_NAMESPACE = "littlebiomes";

    @Getter
    private static LittleBiomes instance;
    @Getter
    private static PacketHandler packetHandler;
    @Getter
    private static Config okaeriConfig;
    @Getter
    private static WorldGuardHook worldGuardHook;

    @Override
    public void onLoad() {
        instance = this;
        okaeriConfig = loadConfig(Config.class, "config.yml");
        packetHandler = PacketHandler.of(this, PacketHandler.Injector.PROTOCOLLIB, PacketHandler.Priority.HIGH);
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardHook = new WorldGuardHook();
            worldGuardHook.register();
            getLogger().info("WorldGuard detected, WorldGuardHook enabled.");
        }
    }


    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new BlockListeners(), this);
        getServer().getPluginManager().registerEvents(new ChunkListeners(), this);
        getServer().getPluginManager().registerEvents(new BadRegistryPrevention(), this);
        getCommand("littlebiomes").setExecutor(new CommandManager());


        Executors.delayedGlobalSync(1, () -> {
            okaeriConfig.littleBiomes().forEach(okaeriLittleBiome -> {
                try {
                    okaeriLittleBiome.register();
                    okaeriLittleBiome.addToPacketHandler();
                } catch (Exception e) {
                    getLogger().severe("Failed to register little biome: " + okaeriLittleBiome.name());
                    e.printStackTrace();
                }
            });
            packetHandler.register();
        });

        this.loadExistingChunks();
        this.anchorParticlesTask();
    }

    @Override
    public void onDisable() {
        packetHandler.unregister();
    }



    public <T extends OkaeriConfig> T loadConfig(Class<T> configClass, String fileName) {
        Path bindFile = this.getDataPath().resolve(fileName);
        return ConfigManager.create(configClass, it -> {
            it.withConfigurer(new YamlBukkitConfigurer(), registry -> {
                registry.register(new StandardSerdes());
                registry.register(EnumTransformers.lowercase(Material.class));
                registry.register(EnumTransformers.lowercase(Particle.class));
                registry.register(EnumTransformers.lowercase(WrappedParticleTypes.class));
                registry.register(EnumTransformers.lowercase(GrassColorModifier.class));
                registry.register(EnumTransformers.lowercase(PacketHandler.Priority.class));
                registry.register(EnumTransformers.lowercase(SimpleParticleData.class));
            });
            it.withRemoveOrphans(false);
            it.withBindFile(bindFile);

            it.saveDefaults();
            it.load(true);
        });
    }



    // TODO: temporarily here
    private void anchorParticlesTask() {
        Executors.runRepeatingAsync(1, TimeUnit.SECONDS, task -> {
            for (WorldTiedChunkLocation worldTiedChunkLocation : CachedLittleBiomes.INSTANCE.getCachedChunks()) {
                worldTiedChunkLocation.toBukkitChunk().thenAccept(chunk -> {
                    Executors.sync(chunk, () -> {
                        if (!chunk.isLoaded()) {
                            Executors.sync(chunk, () -> {
                                CachedLittleBiomes.INSTANCE.uncacheChunk(worldTiedChunkLocation);
                                debug("Uncached chunk at %s in world %s because it was unloaded?".formatted(
                                        worldTiedChunkLocation.chunkX() + "," + worldTiedChunkLocation.chunkZ(),
                                        worldTiedChunkLocation.world().getName()
                                ));
                            });
                            return;
                        }

                        @Nullable String serializedAnchorLocation = KeyedData.ANCHOR_BLOCK.get(chunk);
                        if (serializedAnchorLocation == null) {
                            return; // Could have been removed.
                        }

                        SimpleBlockLocation anchorLocation = SimpleBlockLocation.fromSerialized(serializedAnchorLocation, chunk.getWorld());
                        Location location = anchorLocation.toLocation().toCenterLocation();
                        Particle particle = okaeriConfig.anchorParticle();
                        if (particle != null) {
                            location.getWorld().spawnParticle(particle, location, 3, 0.3, 0.3, 0.3, 0.01);
                        }
                    });
                });
            }
        });
    }

    private void loadExistingChunks() {
        List<Chunk> chunks = new ArrayList<>();
        for (Player player : getServer().getOnlinePlayers()) {
            int viewDistance = player.getViewDistance();
            Location playerLocation = player.getLocation();
            World world = player.getWorld();

            int playerChunkX = playerLocation.getBlockX() >> 4;
            int playerChunkZ = playerLocation.getBlockZ() >> 4;
            for (int x = playerChunkX - viewDistance; x <= playerChunkX + viewDistance; x++) {
                for (int z = playerChunkZ - viewDistance; z <= playerChunkZ + viewDistance; z++) {
                    chunks.add(world.getChunkAt(x, z, false));
                }
            }
        }

        for (Chunk chunk : chunks) {
            if (!KeyedData.CHUNK_BIOME.matches(chunk)) {
                continue;
            }

            WorldTiedChunkLocation worldTiedChunkLocation = WorldTiedChunkLocation.of(chunk);
            String biomeKeyString = Preconditions.checkNotNull(KeyedData.CHUNK_BIOME.get(chunk), "Expected to find biome key for chunk (%d, %d) in world %s".formatted(
                    chunk.getX(), chunk.getZ(), chunk.getWorld().getName()
            ));

            ResourceKey biomeKey = ResourceKey.fromString(biomeKeyString);
            CachedLittleBiomes.INSTANCE.cacheChunk(worldTiedChunkLocation, biomeKey);
            debug("Cached chunk at %s in world %s on startup.".formatted(
                    worldTiedChunkLocation.chunkX() + "," + worldTiedChunkLocation.chunkZ(),
                    worldTiedChunkLocation.world().getName()
            ));
        }
    }


    public static void debug(String message) {
        if (okaeriConfig.debug()) {
            instance.getLogger().info("[DEBUG] " + message);
        }
    }
}