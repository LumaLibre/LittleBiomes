package dev.lumas.biomes.commands.subcommand;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.commands.Subcommand;
import dev.lumas.biomes.events.BadRegistryPrevention;
import dev.lumas.biomes.util.TextUtil;
import dev.wyck.connection.RegistryReconfigurer;
import dev.wyck.exceptions.HorriblePlayerLoginEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReconfigureCommand implements Subcommand {

    private static final RegistryReconfigurer REGISTRY_RECONFIGURER = RegistryReconfigurer.newReconfigurer(LittleBiomes.instance());

    @Override
    public boolean execute(CommandSender sender, String label, List<String> args) {
        ReloadCommand.reload();

        Player player = (Player) sender;
        try {
            REGISTRY_RECONFIGURER.resendRegistries(player, connection -> BadRegistryPrevention.forget(player));
        } catch (HorriblePlayerLoginEvent e) {
            TextUtil.msg(sender, "LittleBiomes configuration reloaded, but registries could not be resent: <red>%s</red>".formatted(e.getMessage()));
            return true;
        }

        TextUtil.msg(sender, "LittleBiomes configuration reloaded and registries resent.");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, List<String> args) {
        return List.of();
    }

    @Override
    public Options options() {
        return Options.builder()
                .label("reconfigure")
                .permission("littlebiomes.command.reconfigure")
                .playerOnly(true)
                .usage("/<command> reconfigure")
                .build();
    }
}
