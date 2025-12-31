package dev.lumas.biomes.events;

import me.outspending.biomesapi.registry.BiomeResourceKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BadRegistryPrevention implements Listener {

    // lazy disconnect prevention when admins reload

    private static Map<BiomeResourceKey, List<UUID>> recentlyRegistered = new HashMap<>();


    public static void populate(BiomeResourceKey biomeKey, Collection<UUID> playerUUIDs) {
        if (!recentlyRegistered.containsKey(biomeKey)) {
            recentlyRegistered.put(biomeKey, new ArrayList<>());
        }
        recentlyRegistered.get(biomeKey).addAll(playerUUIDs);
    }

    public static boolean shouldPrevent(BiomeResourceKey biomeKey, Player player) {
        if (!recentlyRegistered.containsKey(biomeKey)) {
            return false;
        }
        List<UUID> uuidList = recentlyRegistered.get(biomeKey);
        return uuidList.contains(player.getUniqueId());
    }


    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        for (var entry : recentlyRegistered.entrySet()) {
            BiomeResourceKey biomeKey = entry.getKey();
            List<UUID> uuidList = entry.getValue();
            uuidList.remove(playerUUID);

            if (uuidList.isEmpty()) {
                recentlyRegistered.remove(biomeKey);
            }
        }
    }
}
