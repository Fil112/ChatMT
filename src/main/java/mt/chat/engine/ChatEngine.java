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

        // 2. Достаем нужный формат из языкового файла
        String format = loader.getConfigManager().getMessages().getString(formatPath, "<gray>%player_name% <dark_gray>» <white><message>");

        // 3. Создаем интерактивный никнейм (клик + ховер)
        String hoverText = loader.getConfigManager().getMessages().getString(
                "formats.chat-hover",
                "<gray>Нажмите, чтобы написать в ЛС"
        );
        String interactiveName = "<click:suggest_command:'/msg " + sender.getName() + " '>" +
                "<hover:show_text:'" + hoverText + "'>" +
                sender.getName() +
                "</hover></click>";

        // Заменяем плейсхолдер ника ДО обработки PAPI, чтобы сохранить MiniMessage теги
        format = format.replace("%player_name%", interactiveName);

        // 4. Обрабатываем PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        }

        // 5. Вставляем само сообщение игрока в формат
        // Прогоняем текст через систему упоминаний (@Ник) перед отправкой
        if (loader.getMentionManager() != null) {
            finalMessage = loader.getMentionManager().processMentions(finalMessage);
        }

        format = format.replace("<message>", finalMessage);

        // 6. Превращаем MiniMessage-строку (<gradient:...>) в Bukkit Component, а затем в HEX-строку для Spigot
        Component parsedComponent = miniMessage.deserialize(format);
        String readyMessage = legacySerializer.serialize(parsedComponent);

        // 7. Рассылка сообщения с учетом системы игноров
        if (isGlobal) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                // Если игрок не игнорирует отправителя (или это сам отправитель) - отправляем
                if (p.equals(sender) || !loader.getIgnoreManager().isIgnored(p.getUniqueId(), sender.getUniqueId())) {
                    p.sendMessage(readyMessage);
                }
            }
            Bukkit.getConsoleSender().sendMessage("[Global] " + readyMessage);
        } else {
            // Локальный чат: ищем игроков только в том же мире и в нужном радиусе
            int receiversCount = 0;
            for (Player p : sender.getWorld().getPlayers()) {
                if (p.getLocation().distance(sender.getLocation()) <= localRadius) {
                    if (p.equals(sender) || !loader.getIgnoreManager().isIgnored(p.getUniqueId(), sender.getUniqueId())) {
                        p.sendMessage(readyMessage);
                        receiversCount++;
                    }
                }
            }

            Bukkit.getConsoleSender().sendMessage("[Local] " + readyMessage);

            // Если игрок орал в пустоту (рядом никого нет)
            if (receiversCount == 1) {
                String nobodyMsg = loader.getConfigManager().getMessages().getString(
                        "system.nobody-heard",
                        "<gray>[<red>!<gray>] <red>Вас никто не услышал... Напишите <yellow>%prefix% <red>перед сообщением для глобального чата."
                );
                Component nobodyComp = miniMessage.deserialize(nobodyMsg.replace("%prefix%", globalPrefix));
                sender.sendMessage(legacySerializer.serialize(nobodyComp));
            }
        }

        // 8. Записываем сообщение в лог-файл
        if (loader.getLoggerMT() != null) {
            loader.getLoggerMT().logChat(sender.getName(), originalMessage, isGlobal);
        }
    }
}