package dev.lumas.biomes.commands;

import dev.lumas.biomes.commands.subcommand.GiveAnchorCommand;
import dev.lumas.biomes.commands.subcommand.NearestAnchorCommand;
import dev.lumas.biomes.commands.subcommand.ReloadCommand;
import dev.lumas.biomes.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommandManager implements TabExecutor {

    private final List<Subcommand> subcommands = List.of(
            new GiveAnchorCommand(),
            new NearestAnchorCommand(),
            new ReloadCommand()
    );


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            TextUtil.msg(sender, "Please provide a subcommand.");
            return true;
        }

        String subcommandLabel = args[0];
        for (Subcommand subcommand : subcommands) {
            if (!subcommand.options().label().equalsIgnoreCase(subcommandLabel)) {
                continue;
            }

            List<String> subArgs = args.length > 1 ? List.of(args).subList(1, args.length) : List.of();
            boolean value = subcommand.execute(sender, label, subArgs);
            if (!value) {
                TextUtil.msg(sender, "Usage: " + subcommand.options().usage().replace("<command>", label));
            }
            return true;
        }

        TextUtil.msg(sender, "Unknown subcommand: " + subcommandLabel);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return subcommands.stream()
                    .map(subcommand -> subcommand.options().label())
                    .toList();
        }

        String subcommandLabel = args[0];
        for (Subcommand subcommand : subcommands) {
            if (!subcommand.options().label().equalsIgnoreCase(subcommandLabel)) {
                continue;
            }

            List<String> subArgs = args.length > 2 ? List.of(args).subList(1, args.length) : List.of();
            return subcommand.tabComplete(sender, label, subArgs);
        }

        return null;
    }
}
