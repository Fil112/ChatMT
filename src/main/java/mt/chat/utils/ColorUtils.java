package mt.chat.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtils {

    // Инициализируем инструменты один раз, чтобы не нагружать память
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat() // Важно для совместимости со Spigot
            .build();

    /**
     * Превращает MiniMessage формат в стандартную legacy/HEX строку.
     */
    public static String colorize(String text) {
        if (text == null || text.isEmpty()) return "";

        Component component = miniMessage.deserialize(text);
        return legacySerializer.serialize(component);
    }
}