package mt.chat.engine;

import me.clip.placeholderapi.PlaceholderAPI;
import mt.chat.system.MonolithLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatEngine {

    private final MonolithLoader loader;
    private final MiniMessage miniMessage;
    // Сериализатор для перевода красивых градиентов Adventure в понятный для Spigot 1.16+ HEX-формат
    private final LegacyComponentSerializer legacySerializer;

    public ChatEngine(MonolithLoader loader) {
        this.loader = loader;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.builder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat() // Поддержка старых ядер
                .build();
    }

    /**
     * Главный метод обработки чата. Вызывается из ChatListener.
     */
    public void processChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String originalMessage = event.getMessage();

        // Отменяем стандартный ивент майнкрафта, мы всё разошлем сами
        event.setCancelled(true);

        int localRadius = loader.getConfigManager().getConfig().getInt("chat.local-radius", 100);
        String globalPrefix = loader.getConfigManager().getConfig().getString("chat.global-prefix", "!");

        boolean isGlobal = false;
        String formatPath = "formats.local";
        String finalMessage = originalMessage;

        // 1. Проверяем, глобальный это чат или локальный
        // Если локальный чат выключен (радиус -1), то все сообщения глобальные
        if (localRadius == -1) {
            isGlobal = true;
            formatPath = "formats.global";
        } else if (originalMessage.startsWith(globalPrefix)) {
            // Если игрок написал "!", отрезаем этот символ и делаем сообщение глобальным
            isGlobal = true;
            formatPath = "formats.global";
            finalMessage = originalMessage.substring(globalPrefix.length()).trim();

            // Защита от пустых сообщений типа просто "!"
            if (finalMessage.isEmpty()) return;
        }

        // 2. Достаем нужный формат из messages.yml
        String format = loader.getConfigManager().getMessages().getString(formatPath, "<gray>%player_name% <dark_gray>» <white><message>");

        // 3. Обрабатываем PlaceholderAPI (Парсим %player_name% и другие PAPI)
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        } else {
            // Фолбэк, если PAPI не установлен
            format = format.replace("%player_name%", sender.getName());
        }

        // 4. Вставляем само сообщение игрока в формат
        // Прогоняем текст через систему упоминаний (@Ник) перед отправкой
        finalMessage = loader.getMentionManager().processMentions(finalMessage);

        format = format.replace("<message>", finalMessage);

        // 5. Превращаем MiniMessage-строку (<gradient:...>) в Bukkit Component, а затем в HEX-строку для Spigot
        Component parsedComponent = miniMessage.deserialize(format);
        String readyMessage = legacySerializer.serialize(parsedComponent);

        // 6. Рассылка сообщения
        if (isGlobal) {
            // Отправляем всем на сервере
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendMessage(readyMessage);
            }
            // Также дублируем в консоль для логов
            Bukkit.getConsoleSender().sendMessage("[Global] " + readyMessage);
        } else {
            // Локальный чат: ищем игроков только в том же мире и в нужном радиусе
            int receiversCount = 0;
            for (Player p : sender.getWorld().getPlayers()) {
                if (p.getLocation().distance(sender.getLocation()) <= localRadius) {
                    p.sendMessage(readyMessage);
                    receiversCount++;
                }
            }

            Bukkit.getConsoleSender().sendMessage("[Local] " + readyMessage);

            // Если игрок орал в пустоту (рядом никого нет)
            if (receiversCount == 1) {
                sender.sendMessage("§7[§c!§7] §cВас никто не услышал... Напишите §e" + globalPrefix + " §cперед сообщением для глобального чата.");
            }
        }

        // 7. Записываем сообщение в лог-файл (асинхронно, чтобы не грузить сервер)
        loader.getLoggerMT().logChat(sender.getName(), originalMessage, isGlobal);
    }
}