package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import org.bukkit.entity.Player;

import java.util.List;

public class AntiSwear {

    private final MonolithLoader loader;

    public AntiSwear(MonolithLoader loader) {
        this.loader = loader;
    }

    public String filterSwear(Player player, String message) {
        if (player.hasPermission("chatmt.bypass.swear")) return message;

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("filters.swear.enabled", true);
        if (!enabled) return message;

        List<String> badWords = loader.getConfigManager().getConfig().getStringList("filters.swear.words");
        String filteredMessage = message;

        for (String word : badWords) {
            // Флаг (?i) делает поиск нечувствительным к регистру
            if (filteredMessage.toLowerCase().contains(word.toLowerCase())) {
                // Генерируем звёздочки по длине слова (мат -> ***)
                String stars = new String(new char[word.length()]).replace("\0", "*");
                filteredMessage = filteredMessage.replaceAll("(?i)" + word, stars);
            }
        }

        return filteredMessage;
    }
}