package dev.lumas.biomes.commands.subcommand;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.commands.Subcommand;
import dev.lumas.biomes.configuration.Config;
import dev.lumas.biomes.configuration.OkaeriLittleBiome;
import dev.lumas.biomes.events.BadRegistryPrevention;
import dev.lumas.biomes.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

import java.util.List;
import java.util.UUID;

public class ReloadCommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, String label, List<String> args) {
        Config config = LittleBiomes.okaeriConfig();
        config.load(true);

        List<UUID> playerUUIDs = Bukkit.getOnlinePlayers().stream()
                .map(Entity::getUniqueId)
                .toList();

        for (OkaeriLittleBiome okaeriLittleBiome : config.littleBiomes()) {
            if (okaeriLittleBiome.isRegistered()) {
                okaeriLittleBiome.modify();
            } else {
                okaeriLittleBiome.register();
                okaeriLittleBiome.addToPacketHandler();
                BadRegistryPrevention.populate(okaeriLittleBiome.biomeResourceKey(), playerUUIDs);
            }
        }
        TextUtil.msg(sender, "LittleBiomes configuration reloaded. Relogging is required to see biome changes.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, List<String> args) {
        return List.of();
    }

    @Override
    public Options options() {
        return Options.builder()
                .label("reload")
                .permission("littlebiomes.command.reload")
                .playerOnly(false)
                .usage("/<command> reload")
                .build();
    }
}
