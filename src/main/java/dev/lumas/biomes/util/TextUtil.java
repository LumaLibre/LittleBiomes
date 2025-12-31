package dev.lumas.biomes.util;

import lombok.NoArgsConstructor;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

@NoArgsConstructor
public final class TextUtil {

    public static Component minimessage(String message) {
        return MiniMessage.miniMessage().deserialize(message);
    }

    public static void msg(Audience audience, String message) {
        audience.sendMessage(minimessage(message));
    }
}
