package dev.lumas.biomes.model;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.util.Executors;
import dev.lumas.biomes.commands.PersonalBiomeCommand;
import dev.wyck.keys.ResourceKey;
import dev.wyck.renderer.updater.BiomeUpdater;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PersonalBiomes {

    public static final PersonalBiomes INSTANCE = new PersonalBiomes();

    private static final BiomeUpdater BIOME_UPDATER = BiomeUpdater.of(LittleBiomes.instance());

    private static final int CHUNKS_PER_REFRESH_TICK = 8;

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

    private final Map<UUID, Integer> refreshGenerations = new ConcurrentHashMap<>();

    private volatile Set<NamespacedKey> disabledWorlds = Set.of();

    private PersonalBiomes() {}

    public void load(Player player) {
        String storedKey = KeyedData.PERSONAL_BIOME.get(player);
        Boolean storedEnabled = KeyedData.PERSONAL_BIOME_ENABLED.get(player);

        ResourceKey anchorKey = null;
        if (storedKey != null) {
            try {
                anchorKey = ResourceKey.fromString(storedKey);
            } catch (RuntimeException e) {
                LittleBiomes.instance().getLogger().warning(
                        "Discarding unreadable personal biome key '%s' for %s.".formatted(storedKey, player.getName()));
                KeyedData.PERSONAL_BIOME.remove(player);
            }
        }

        boolean enabled = Boolean.TRUE.equals(storedEnabled);

        if (enabled && !player.hasPermission(PersonalBiomeCommand.TOGGLE_PERMISSION)) {
            // disable the toggle for players who have lost permission since last time
            KeyedData.PERSONAL_BIOME_ENABLED.remove(player);
            return;
        }

        State state = new State(anchorKey, enabled);
        states.put(player.getUniqueId(), state);
        LittleBiomes.debug("Loaded personal biome state for %s: %s".formatted(player.getName(), state));
    }

    public void reloadDisabledWorlds() {
        Set<NamespacedKey> resolved = new HashSet<>();
        for (String raw : LittleBiomes.okaeriConfig().personalBiomeDisabledWorlds()) {
            NamespacedKey worldKey = NamespacedKey.fromString(raw.toLowerCase(Locale.ROOT));
            if (worldKey == null) {
                LittleBiomes.instance().getLogger().warning(
                        "Ignoring unreadable world key '%s' in personalBiomeDisabledWorlds.".formatted(raw));
                continue;
            }
            resolved.add(worldKey);
        }

        this.disabledWorlds = Set.copyOf(resolved);
        LittleBiomes.debug("Personal biomes disabled in worlds: " + this.disabledWorlds);
    }

    public boolean isDisabledIn(World world) {
        Set<NamespacedKey> disabled = this.disabledWorlds;
        return !disabled.isEmpty() && disabled.contains(world.getKey());
    }

    public void unload(Player player) {
        states.remove(player.getUniqueId());
        refreshGenerations.remove(player.getUniqueId());
    }

    public State state(Player player) {
        return states.getOrDefault(player.getUniqueId(), State.EMPTY);
    }

    @Nullable
    public ResourceKey activeBiome(Player player) {
        return activeBiome(player, player.getWorld());
    }

    @Nullable
    public ResourceKey activeBiome(Player player, World world) {
        return isDisabledIn(world) ? null : state(player).active();
    }

    public boolean isActive(Player player, World world, ResourceKey biomeKey) {
        return biomeKey.equals(activeBiome(player, world));
    }

    public void setAnchor(Player player, @Nullable ResourceKey anchorKey) {
        store(player, new State(anchorKey, state(player).enabled()));
    }

    public void setEnabled(Player player, boolean enabled) {
        store(player, new State(state(player).anchorKey(), enabled));
        flush(player);
    }

    public void refresh(Player player) {
        World world = player.getWorld();
        int generation = refreshGenerations.merge(player.getUniqueId(), 1, Integer::sum);

        int playerChunkX = player.getLocation().getBlockX() >> 4;
        int playerChunkZ = player.getLocation().getBlockZ() >> 4;

        List<Long> chunkKeys = new ArrayList<>(player.getSentChunkKeys());
        chunkKeys.sort(Comparator.comparingLong(key -> {
            long dx = chunkX(key) - playerChunkX;
            long dz = chunkZ(key) - playerChunkZ;
            return dx * dx + dz * dz;
        }));

        refreshBatch(player, world, generation, chunkKeys.iterator());
    }

    private void refreshBatch(Player player, World world, int generation, Iterator<Long> chunkKeys) {
        Executors.syncPlayerDelayed(player, 1, () -> {
            if (!player.isOnline()
                    || !world.equals(player.getWorld())
                    || !Integer.valueOf(generation).equals(refreshGenerations.get(player.getUniqueId()))) {
                return;
            }

            int sent = 0;
            while (chunkKeys.hasNext() && sent < CHUNKS_PER_REFRESH_TICK) {
                long key = chunkKeys.next();
                int chunkX = chunkX(key);
                int chunkZ = chunkZ(key);
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    continue;
                }

                BIOME_UPDATER.updateChunkAsync(world.getChunkAtAsync(chunkX, chunkZ));
                sent++;
            }

            if (chunkKeys.hasNext()) {
                refreshBatch(player, world, generation, chunkKeys);
            }
        });
    }

    private static int chunkX(long chunkKey) {
        return (int) chunkKey;
    }

    private static int chunkZ(long chunkKey) {
        return (int) (chunkKey >> 32);
    }

    public void flush(Player player) {
        player.saveData();
    }

    private void store(Player player, State state) {
        states.put(player.getUniqueId(), state);

        if (state.anchorKey() == null) {
            KeyedData.PERSONAL_BIOME.remove(player);
        } else {
            KeyedData.PERSONAL_BIOME.set(player, state.anchorKey().toString());
        }
        KeyedData.PERSONAL_BIOME_ENABLED.set(player, state.enabled());
    }


    public record State(@Nullable ResourceKey anchorKey, boolean enabled) {

        private static final State EMPTY = new State(null, false);

        @Nullable
        public ResourceKey active() {
            return enabled ? anchorKey : null;
        }
    }
}
