package mt.chat.engine;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MentionManager implements Listener {

    private final MonolithLoader loader;

    // Тайминги последней активности игроков для встроенного детектора AFK
    private final Map<UUID, Long> lastActivity = new ConcurrentHashMap<>();

    public MentionManager(MonolithLoader loader) {
        this.loader = loader;
        // Регистрируем листенер активности прямо здесь
        Bukkit.getPluginManager().registerEvents(this, loader.getPlugin());
    }

    /**
     * Оставил для обратной совместимости, если где-то вызывается без sender
     */
    public String processMentions(String message) {
        return processMentions(null, message);
    }

    /**
     * Подсветка ника, звонкий звук и проверка на AFK
     */
    public String processMentions(Player sender, String message) {
        boolean enabled = loader.getConfigManager().getConfig().getBoolean("chat.mentions.enabled", true);
        if (!enabled) return message;

        // По умолчанию считаем игрока отошедшим через 3 минуты (180 сек)
        long afkTimeout = loader.getConfigManager().getConfig().getLong("chat.mentions.afk-seconds", 180) * 1000L;
        String processedMessage = message;

        for (Player target : Bukkit.getOnlinePlayers()) {
            String mentionTag = "@" + target.getName();

            // Если в сообщении тегнули игрока
            if (processedMessage.toLowerCase().contains(mentionTag.toLowerCase())) {
                // Красим тег в желтый
                processedMessage = processedMessage.replaceAll("(?i)" + mentionTag, "<yellow>" + mentionTag + "</yellow><white>");

                // Приятный звонкий звук (Pitch 1.2) со страховкой под старые ядра
                try {
                    target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                } catch (Exception e) {
                    try {
                        target.playSound(target.getLocation(), Sound.valueOf("ORB_PICKUP"), 1.0f, 1.2f);
                    } catch (Exception ignored) {}
                }

                // Проверяем AFK (не спамим, если игрок тегнул сам себя)
                if (sender != null && !sender.equals(target)) {
                    long lastActive = lastActivity.getOrDefault(target.getUniqueId(), System.currentTimeMillis());
                    if (System.currentTimeMillis() - lastActive > afkTimeout) {
                        String afkNotice = loader.getConfigManager().getMessages().getString(
                                "system.afk-notice",
                                "<dark_gray>[<yellow>!<dark_gray>] <gray>Игрок <yellow>%target% <gray>отошел от клавиатуры (AFK)."
                        );
                        sender.sendMessage(ColorUtils.colorize(afkNotice.replace("%target%", target.getName())));
                    }
                }
            }
        }

        return processedMessage;
    }

    // ==========================================
    // --- Отслеживание активности для AFK ---
    // ==========================================

    private void updateActivity(UUID uuid) {
        lastActivity.put(uuid, System.currentTimeMillis());
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        updateActivity(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        updateActivity(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        // Оптимизация: не триггерим при обычном повороте камеры, только при шагах между блоками
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
                event.getFrom().getBlockZ() != event.getTo().getBlockZ() ||
                event.getFrom().getBlockY() != event.getTo().getBlockY()) {
            updateActivity(event.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        lastActivity.remove(event.getPlayer().getUniqueId());
    }
}