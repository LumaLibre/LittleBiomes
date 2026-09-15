package dev.lumas.biomes.commands.subcommand;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.commands.Subcommand;
import dev.lumas.biomes.configuration.OkaeriLittleBiome;
import dev.lumas.biomes.util.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GiveAnchorCommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            return false;
        }
        String biomeName = args.getFirst();

        OkaeriLittleBiome okaeriLittleBiome = LittleBiomes.okaeriConfig().getLittleBiomeByName(biomeName);
        if (okaeriLittleBiome == null) {
            TextUtil.msg(sender, "No little biome found with name: " + biomeName);
            return true;
        }

        Player target;
        if (args.size() > 1) {
            target = Bukkit.getPlayer(args.get(1));
            if (target == null) {
                TextUtil.msg(sender, "No online player found with name: " + args.get(1));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            TextUtil.msg(sender, "You must specify a player when running this command from console.");
            return true;
        }

        target.give(okaeriLittleBiome.anchorItem());
        if (target != sender) {
            TextUtil.msg(sender, "Gave " + okaeriLittleBiome.name() + " anchor to " + target.getName() + ".");
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, List<String> args) {
        if (args.size() == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toList();
        }
        if (args.size() > 2) {
            return List.of();
        }
        return LittleBiomes.okaeriConfig().littleBiomes().stream()
                .map(OkaeriLittleBiome::name)
                .toList();
    }

    @Override
    public Options options() {
        return Options.builder()
                .label("giveanchor")
                .permission("littlebiomes.command.giveanchor")
                .playerOnly(false)
                .usage("/<command> giveanchor <biome> [player]")
                .build();
    }
}
