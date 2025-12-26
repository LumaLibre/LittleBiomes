package net.lumamc.biomes.commands.subcommand;

import net.lumamc.biomes.PetiteBiomes;
import net.lumamc.biomes.commands.Subcommand;
import net.lumamc.biomes.configuration.Config;
import net.lumamc.biomes.configuration.OkaeriLittleBiome;
import net.lumamc.biomes.util.TextUtil;
import org.bukkit.command.CommandSender;

import java.util.List;

public class ReloadCommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, String label, List<String> args) {
        Config config = PetiteBiomes.okaeriConfig();
        config.load(true);

        for (OkaeriLittleBiome okaeriLittleBiome : config.littleBiomes()) {
            if (okaeriLittleBiome.isRegistered()) {
                okaeriLittleBiome.modify();
            } else {
                okaeriLittleBiome.register();
            }
        }
        TextUtil.msg(sender, "PetiteBiomes configuration reloaded.");
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
                .permission("petitebiomes.command.reload")
                .playerOnly(false)
                .usage("/<command> reload")
                .build();
    }
}
