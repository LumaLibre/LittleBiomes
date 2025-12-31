package dev.lumas.biomes.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.NoArgsConstructor;
import dev.lumas.biomes.LittleBiomes;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

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

    // TODO: Better logging
    public static BukkitTask runAsync(Runnable runnable) {
        return Bukkit.getScheduler().runTaskAsynchronously(PLUGIN, () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    // Synchronous

    public static BukkitTask delayedSync(long delay, Runnable runnable) {
        return Bukkit.getScheduler().runTaskLater(PLUGIN, runnable, delay);
    }

    public static BukkitTask sync(Runnable runnable) {
        return Bukkit.getScheduler().runTask(PLUGIN, () -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        });
    }

    public static void runSync(Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
        } else {
            sync(runnable);
        }
    }


}
