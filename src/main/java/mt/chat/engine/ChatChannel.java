package mt.chat.engine;

import java.util.List;

public class ChatChannel {

    private final String id;
    private final String prefix;
    private final List<String> aliases;
    private final String format;
    private final String permission;
    private final int radius;

    public ChatChannel(String id, String prefix, List<String> aliases, String format, String permission, int radius) {
        this.id = id;
        this.prefix = prefix;
        this.aliases = aliases;
        this.format = format;
        this.permission = permission;
        this.radius = radius;
    }

    public String getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public String getFormat() {
        return format;
    }

    public String getPermission() {
        return permission;
    }

    public int getRadius() {
        return radius;
    }

    /**
     * Проверяет, начинается ли строка с префикса или алиаса канала
     */
    public boolean matches(String message) {
        String lower = message.toLowerCase();
        if (lower.startsWith(prefix.toLowerCase())) {
            return true;
        }
        for (String alias : aliases) {
            if (lower.startsWith(alias.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Отрезает префикс канала от сообщения
     */
    public String extractMessage(String message) {
        String lower = message.toLowerCase();
        if (lower.startsWith(prefix.toLowerCase())) {
            return message.substring(prefix.length()).trim();
        }
        for (String alias : aliases) {
            if (lower.startsWith(alias.toLowerCase())) {
                return message.substring(alias.length()).trim();
            }
        }
        return message.trim();
    }
}