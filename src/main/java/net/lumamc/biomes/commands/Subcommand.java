package net.lumamc.biomes.commands;

import lombok.Builder;
import org.bukkit.command.CommandSender;

import java.util.List;

public interface Subcommand {

    boolean execute(CommandSender sender, String label, List<String> args);

    List<String> tabComplete(CommandSender sender, String label, List<String> args);

    Options options();

    @Builder
    record Options(String label, String permission, boolean playerOnly, String usage) {
    }
}
