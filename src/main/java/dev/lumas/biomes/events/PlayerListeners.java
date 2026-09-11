package dev.lumas.biomes.events;

import dev.lumas.biomes.model.AnchorScanner;
import dev.lumas.biomes.model.PersonalBiomes;
import dev.lumas.biomes.util.Executors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListeners implements Listener {

    private static final long SCAN_DELAY_TICKS = 40L;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEarly(PlayerJoinEvent event) {
        PersonalBiomes.INSTANCE.load(event.getPlayer());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Executors.syncPlayerDelayed(event.getPlayer(), SCAN_DELAY_TICKS, () -> {
            if (event.getPlayer().isOnline()) {
                AnchorScanner.scanAround(event.getPlayer());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        PersonalBiomes.INSTANCE.unload(event.getPlayer());
    }
}
