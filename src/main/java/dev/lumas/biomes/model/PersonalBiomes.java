package dev.lumas.biomes.model;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.commands.PersonalBiomeCommand;
import dev.wyck.keys.ResourceKey;
import dev.wyck.renderer.updater.BiomeUpdater;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PersonalBiomes {

    public static final PersonalBiomes INSTANCE = new PersonalBiomes();

    private static final BiomeUpdater BIOME_UPDATER = BiomeUpdater.of(LittleBiomes.instance());

    private final Map<UUID, State> states = new ConcurrentHashMap<>();

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
        BIOME_UPDATER.updateChunksForPlayer(player);
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
