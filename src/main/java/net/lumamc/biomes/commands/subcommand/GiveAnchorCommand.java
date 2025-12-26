package net.lumamc.biomes.commands.subcommand;

import com.google.common.base.Preconditions;
import net.lumamc.biomes.PetiteBiomes;
import net.lumamc.biomes.commands.Subcommand;
import net.lumamc.biomes.configuration.OkaeriLittleBiome;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class GiveAnchorCommand implements Subcommand {
    @Override
    public boolean execute(CommandSender sender, String label, List<String> args) {
        if (args.isEmpty()) {
            return false;
        }
        String biomeName = Preconditions.checkNotNull(args.getFirst(), "You must provide a biome name.");

        OkaeriLittleBiome okaeriLittleBiome =  PetiteBiomes.okaeriConfig().getLittleBiomeByName(biomeName);
        Preconditions.checkNotNull(okaeriLittleBiome, "No little biome found with name: " + biomeName);

        Player player = (Player) sender;
        player.give(okaeriLittleBiome.anchorItem());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, List<String> args) {
        return PetiteBiomes.okaeriConfig().littleBiomes().stream()
                .map(OkaeriLittleBiome::name)
                .toList();
    }

    @Override
    public Options options() {
        return Options.builder()
                .label("giveanchor")
                .permission("petitebiomes.command.giveanchor")
                .playerOnly(true)
                .usage("/<command> give <biome> [amount]")
                .build();
    }
}
