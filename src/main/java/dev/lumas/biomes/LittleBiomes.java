package dev.lumas.biomes;

import dev.lumas.biomes.configuration.serdes.EnumTransformers;
import dev.lumas.biomes.enums.SimpleParticleData;
import dev.lumas.biomes.model.WorldGuardHook;
import dev.wyck.environment.GrassColorModifier;
import dev.wyck.environment.particle.ParticleTypes;
import dev.wyck.renderer.packet.PacketHandler;
import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.serdes.standard.StandardSerdes;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import lombok.Getter;
import lombok.experimental.Accessors;
import dev.lumas.biomes.commands.CommandManager;
import dev.lumas.biomes.commands.PersonalBiomeCommand;
import dev.lumas.biomes.configuration.Config;
import dev.lumas.biomes.events.BlockListeners;
import dev.lumas.biomes.events.ChunkListeners;
import dev.lumas.biomes.events.BadRegistryPrevention;
import dev.lumas.biomes.events.PersonalAnchorMenuListeners;
import dev.lumas.biomes.events.PlayerListeners;
import dev.lumas.biomes.model.AnchorScanner;
import dev.lumas.biomes.model.CachedLittleBiomes;
import dev.lumas.biomes.model.KeyedData;
import dev.lumas.biomes.model.PersonalBiomes;
import dev.lumas.biomes.model.SimpleBlockLocation;
import dev.lumas.biomes.model.WorldTiedChunkLocation;
import dev.lumas.biomes.util.Executors;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.nio.file.Path;
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
        packetHandler = PacketHandler.of(this, PacketHandler.Injector.PROTOCOLLIB, PacketHandler.Priority.HIGHEST);
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
        getServer().getPluginManager().registerEvents(new PlayerListeners(), this);
        getServer().getPluginManager().registerEvents(new PersonalAnchorMenuListeners(), this);
        getCommand("littlebiomes").setExecutor(new CommandManager());

        PersonalBiomeCommand personalBiomeCommand = new PersonalBiomeCommand();
        getCommand("pbiome").setExecutor(personalBiomeCommand);
        getCommand("pbiome").setTabCompleter(personalBiomeCommand);

        PersonalBiomes.INSTANCE.reloadDisabledWorlds();
        // Covers a mid-session plugin reload, where nobody is going to fire a join event for us.
        getServer().getOnlinePlayers().forEach(PersonalBiomes.INSTANCE::load);


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

        try {
            this.loadExistingChunks();
        } catch (Throwable t) {
            getLogger().severe("Failed to scan loaded chunks for existing anchors; they will be picked up as chunks reload.");
            t.printStackTrace();
        }
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
                registry.register(EnumTransformers.lowercase(ParticleTypes.class));
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
                if (!worldTiedChunkLocation.world().isChunkLoaded(worldTiedChunkLocation.chunkX(), worldTiedChunkLocation.chunkZ())) {
                    continue;
                }

                worldTiedChunkLocation.toBukkitChunk().thenAccept(chunk -> {
                    Executors.sync(chunk, () -> {
                        if (!chunk.isLoaded()) {
                            return;
                        }

                        @Nullable String serializedAnchorLocation = KeyedData.ANCHOR_BLOCK.get(chunk);
                        if (serializedAnchorLocation == null) {
                            CachedLittleBiomes.INSTANCE.uncacheChunk(worldTiedChunkLocation);
                            debug("Uncached chunk at %s in world %s because its anchor is gone, probably.".formatted(
                                    worldTiedChunkLocation.chunkX() + "," + worldTiedChunkLocation.chunkZ(),
                                    worldTiedChunkLocation.world().getName()
                            ));
                            return;
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
        for (Player player : getServer().getOnlinePlayers()) {
            AnchorScanner.scanAround(player);
        }
    }


    public static void debug(String message) {
        if (okaeriConfig.debug()) {
            instance.getLogger().info("[DEBUG] " + message);
        }
    }
}