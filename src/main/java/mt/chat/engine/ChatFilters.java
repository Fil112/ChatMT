package mt.chat.engine;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatFilters {

    private final MonolithLoader loader;
    // Храним время последнего сообщения каждого игрока в миллисекундах
    private final Map<UUID, Long> lastMessageTime = new HashMap<>();

    public ChatFilters(MonolithLoader loader) {
        this.loader = loader;
    }

    /**
     * Проверяет сообщение на спам.
     * @return true, если сообщение нужно заблокировать (спам), false - если всё ок.
     */
    public boolean isSpamming(Player player) {
        // У администрации (и тех, кому выдано право) кулдауна нет
        if (player.hasPermission("chatmt.bypass.spam")) return false;

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("filters.spam.enabled", true);
        if (!enabled) return false;

        double delaySeconds = loader.getConfigManager().getConfig().getDouble("filters.spam.delay-seconds", 1.5);
        long delayMillis = (long) (delaySeconds * 1000);

        long currentTime = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(player.getUniqueId(), 0L);

        // Если прошло меньше времени, чем указано в конфиге - блокируем
        if (currentTime - lastTime < delayMillis) {
            String cooldownMsg = loader.getConfigManager().getMessages().getString("system.cooldown", "<red>Не пишите так часто!");
            player.sendMessage(ColorUtils.colorize(cooldownMsg));
            return true;
        }

        // Записываем новое время сообщения
        lastMessageTime.put(player.getUniqueId(), currentTime);
        return false;
    }

    /**
     * Проверяет сообщение на обилие КАПСА и, при необходимости, понижает регистр.
     */
    public String applyAntiCaps(Player player, String message) {
        if (player.hasPermission("chatmt.bypass.caps")) return message;

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("filters.caps.enabled", true);
        if (!enabled) return message;

        int minLength = loader.getConfigManager().getConfig().getInt("filters.caps.min-length", 5);
        int maxPercent = loader.getConfigManager().getConfig().getInt("filters.caps.max-percent", 70);

        // Короткие сообщения (типа "ОК", "ДА") не трогаем
        if (message.length() < minLength) return message;

        int capsCount = 0;
        int letterCount = 0;

        // Считаем только буквы (игнорируем цифры и пробелы)
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                letterCount++;
                if (Character.isUpperCase(c)) {
                    capsCount++;
                }
            }
        }

        if (letterCount > 0) {
            int percent = (capsCount * 100) / letterCount;
            // Если КАПСА слишком много - понижаем регистр всего сообщения
            if (percent >= maxPercent) {
                return message.toLowerCase();
            }
        }

        return message; // Возвращаем как было, если всё в рамках правил
    }
}