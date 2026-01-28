package me.chatmt.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Основной метод для получения Component (идеально для Adventure)
     */
    public static Component parse(String text) {
        if (text == null || text.isEmpty()) return Component.empty();

        // 1. Плейсхолдеры
        String processed = text
                .replace("%time%", LocalDateTime.now().format(timeFormatter))
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        // 2. HEX &#RRGGBB -> <#RRGGBB>
        Matcher matcher = HEX_PATTERN.matcher(processed);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, "<#" + matcher.group(1) + ">");
        }
        matcher.appendTail(sb);
        processed = sb.toString();

        // 3. Поддержка & и MiniMessage
        if (processed.contains("&")) {
            processed = processed.replace("&", "§");
            return LegacyComponentSerializer.legacySection().deserialize(processed);
        }

        return MiniMessage.miniMessage().deserialize(processed);
    }

    /**
     * Метод для случаев, где всё еще нужна старая String
     */
    public static String parseToLegacy(String text) {
        return LegacyComponentSerializer.legacySection().serialize(parse(text));
    }
}