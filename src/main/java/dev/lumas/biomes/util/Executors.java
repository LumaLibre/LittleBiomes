package dev.lumas.biomes.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.NoArgsConstructor;
import dev.lumas.biomes.LittleBiomes;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@NoArgsConstructor
public final class Executors {

    private static final LittleBiomes PLUGIN = LittleBiomes.instance();

    public static ScheduledTask runRepeatingAsync(long delay, long period, TimeUnit timeUnit, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(PLUGIN, consumer, delay, period, timeUnit);
    }

    public static ScheduledTask runDelayedAsync(long delay, TimeUnit timeUnit, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runDelayed(PLUGIN, consumer, delay, timeUnit);
    }

    public static ScheduledTask runRepeatingAsync(long period, TimeUnit timeUnit, Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runAtFixedRate(PLUGIN, consumer, 0, period, timeUnit);
    }

    public static ScheduledTask runAsync(Consumer<ScheduledTask> consumer) {
        return Bukkit.getAsyncScheduler().runNow(PLUGIN, consumer);
    }

    // Synchronous

    public static ScheduledTask delayedGlobalSync(long delay, Runnable runnable) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(PLUGIN, task -> runnable.run(), delay);
    }

    public static void syncPlayerDelayed(Player player, long delayTicks, Runnable runnable) {
        player.getScheduler().runDelayed(PLUGIN, task -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }, null, delayTicks);
    }

    public static ScheduledTask sync(World world, int chunkX, int chunkZ, Runnable runnable) {
        return Bukkit.getRegionScheduler().run(PLUGIN, world, chunkX, chunkZ, (task) -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public static ScheduledTask sync(Chunk chunk, Runnable runnable) {
        return Bukkit.getRegionScheduler().run(PLUGIN, chunk.getWorld(), chunk.getX(), chunk.getZ(), (task) -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

}
