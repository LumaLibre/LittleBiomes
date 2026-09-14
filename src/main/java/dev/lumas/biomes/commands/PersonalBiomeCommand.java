package dev.lumas.biomes.commands;

import dev.lumas.biomes.LittleBiomes;
import dev.lumas.biomes.configuration.OkaeriLittleBiome;
import dev.lumas.biomes.gui.PersonalAnchorMenu;
import dev.lumas.biomes.model.PersonalBiomes;
import dev.lumas.biomes.util.TextUtil;
import dev.wyck.keys.ResourceKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PersonalBiomeCommand implements TabExecutor {

    public static final String TOGGLE_PERMISSION = "littlebiomes.command.pbiome";
    public static final String ANCHOR_PERMISSION = "littlebiomes.command.pbiome.anchor";

    private static final String ANCHOR_LABEL = "anchor";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            TextUtil.msg(sender, "This command can only be executed by a player.");
            return true;
        }

        if (args.length == 0) {
            toggle(player);
            return true;
        }

        if (!args[0].equalsIgnoreCase(ANCHOR_LABEL)) {
            TextUtil.msg(player, "Usage: /%s [%s]".formatted(label, ANCHOR_LABEL));
            return true;
        }

        if (!player.hasPermission(ANCHOR_PERMISSION)) {
            TextUtil.msg(player, "You do not have permission to execute this command.");
            return true;
        }

        PersonalAnchorMenu.open(player);
        return true;
    }

    private void toggle(Player player) {
        if (!player.hasPermission(TOGGLE_PERMISSION)) {
            TextUtil.msg(player, "You do not have permission to execute this command.");
            return;
        }

        PersonalBiomes.State state = PersonalBiomes.INSTANCE.state(player);
        ResourceKey anchorKey = state.anchorKey();
        if (anchorKey == null) {
            TextUtil.msg(player, "<red>You have no anchor stored. Put one in with <white>/pbiome anchor</white> first.");
            return;
        }

        boolean enabled = !state.enabled();
        if (enabled && PersonalBiomes.INSTANCE.isDisabledIn(player.getWorld())) {
            TextUtil.msg(player, "<red>Personal biomes are disabled in this world.");
            return;
        }

        PersonalBiomes.INSTANCE.setEnabled(player, enabled);
        PersonalBiomes.INSTANCE.refresh(player);

        TextUtil.msg(player, enabled
                ? "Your personal biome (%s<reset>) is now on.".formatted(describe(anchorKey))
                : "Your personal biome is now off.");
    }

    private static String describe(ResourceKey anchorKey) {
        OkaeriLittleBiome littleBiome = LittleBiomes.okaeriConfig().getLittleBiome(anchorKey);
        return littleBiome != null ? littleBiome.anchorDisplayName() : "<gray>" + anchorKey.key().value();
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1 && sender.hasPermission(ANCHOR_PERMISSION)) {
            return List.of(ANCHOR_LABEL);
        }
        return List.of();
    }
}
