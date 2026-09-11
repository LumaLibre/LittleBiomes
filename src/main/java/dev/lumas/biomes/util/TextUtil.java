package dev.lumas.biomes.util;

import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

@NoArgsConstructor
public final class TextUtil {

    private static final Component PREFIX = Component.empty()
        .append(Component.text("Info").color(TextColor.fromHexString("#b986f9")).decorate(TextDecoration.BOLD))
        .append(Component.text(" » ").color(NamedTextColor.DARK_GRAY));

    public static Component minimessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static void msg(Audience audience, String message) {
        audience.sendMessage(PREFIX.append(minimessage(message)));
    }
}
