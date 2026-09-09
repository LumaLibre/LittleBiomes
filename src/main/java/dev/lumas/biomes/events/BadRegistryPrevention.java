package dev.lumas.biomes.events;

import dev.wyck.keys.KeyChains;
import dev.wyck.keys.ResourceKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BadRegistryPrevention implements Listener {

    // lazy disconnect prevention when admins reload

    private static final Map<ResourceKey, Set<UUID>> recentlyRegistered = new ConcurrentHashMap<>();


    public static void populate(ResourceKey biomeKey, Collection<UUID> playerUUIDs) {
        recentlyRegistered
                .computeIfAbsent(biomeKey, key -> ConcurrentHashMap.newKeySet())
                .addAll(playerUUIDs);
    }

    public static boolean shouldPrevent(ResourceKey biomeKey, Player player) {
        Set<UUID> uuids = recentlyRegistered.get(biomeKey);
        if (uuids == null || uuids.isEmpty()) {
            return false;
        }
        return uuids.contains(player.getUniqueId()) && KeyChains.biomes().isRegistered(biomeKey);
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        recentlyRegistered.entrySet().removeIf(entry -> {
            entry.getValue().remove(playerUUID);
            return entry.getValue().isEmpty();
        });
    }
}
