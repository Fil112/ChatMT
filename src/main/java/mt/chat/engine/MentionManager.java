package mt.chat.engine;

import mt.chat.system.MonolithLoader;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MentionManager {

    private final MonolithLoader loader;

    public MentionManager(MonolithLoader loader) {
        this.loader = loader;
    }

    /**
     * Проверяет сообщение на наличие упоминаний @Ник
     * Подсвечивает ник и издает звук упомянутому игроку.
     * Возвращает измененную строку сообщения.
     */
    public String processMentions(String message) {
        boolean enabled = loader.getConfigManager().getConfig().getBoolean("chat.mentions.enabled", true);
        if (!enabled) return message;

        String processedMessage = message;

        for (Player target : Bukkit.getOnlinePlayers()) {
            String mentionTag = "@" + target.getName();

            // Если в сообщении есть ник игрока с собачкой
            if (processedMessage.toLowerCase().contains(mentionTag.toLowerCase())) {
                // Подсвечиваем ник (заменяем на цветной вариант, например, жёлтый)
                // Флаг (?i) означает игнорирование регистра
                processedMessage = processedMessage.replaceAll("(?i)" + mentionTag, "<yellow>" + mentionTag + "</yellow><white>");

                // Проигрываем звук уведомления
                try {
                    // Используем звук опыта, он есть на всех версиях 1.16+
                    target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                } catch (Exception ignored) {
                    // Глушим ошибку, если вдруг на каком-то ядре звук называется иначе
                }
            }
        }

        return processedMessage;
    }
}