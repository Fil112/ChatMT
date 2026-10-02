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
    private final LegacyComponentSerializer legacySerializer;

    public ChatEngine(MonolithLoader loader) {
        this.loader = loader;
        this.miniMessage = MiniMessage.miniMessage();
        this.legacySerializer = LegacyComponentSerializer.builder()
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
    }

    public void processChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String originalMessage = event.getMessage();

        event.setCancelled(true);

        int localRadius = loader.getConfigManager().getConfig().getInt("chat.local-radius", 100);
        String globalPrefix = loader.getConfigManager().getConfig().getString("chat.global-prefix", "!");

        boolean isGlobal = false;
        String formatPath = "formats.local";
        String finalMessage = originalMessage;

        // 1. Проверяем локал / глобал
        if (localRadius == -1) {
            isGlobal = true;
            formatPath = "formats.global";
        } else if (originalMessage.startsWith(globalPrefix)) {
            isGlobal = true;
            formatPath = "formats.global";
            finalMessage = originalMessage.substring(globalPrefix.length()).trim();

            if (finalMessage.isEmpty()) return;
        }

        // 2. Достаем формат из конфига
        String format = loader.getConfigManager().getMessages().getString(formatPath, "<gray>%player_name% <dark_gray>» <white><message>");

        // 3. Интерактивный никнейм игрока
        String hoverText = loader.getConfigManager().getMessages().getString(
                "formats.chat-hover",
                "<gray>Нажмите, чтобы написать в ЛС"
        );
        String interactiveName = "<click:suggest_command:'/msg " + sender.getName() + " '>" +
                "<hover:show_text:'" + hoverText + "'>" +
                sender.getName() +
                "</hover></click>";

        format = format.replace("%player_name%", interactiveName);

        // 4. PlaceholderAPI
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            format = PlaceholderAPI.setPlaceholders(sender, format);
        }

        // 5. Обработка упоминаний (передаем отправителя для детектора AFK)
        if (loader.getMentionManager() != null) {
            finalMessage = loader.getMentionManager().processMentions(sender, finalMessage);
        }

        format = format.replace("<message>", finalMessage);

        // 6. Подготовка сообщений: для обычных игроков и для админов (с кнопками модерации)
        Component parsedComponent = miniMessage.deserialize(format);
        String readyMessage = legacySerializer.serialize(parsedComponent);

        boolean hoverEnabled = loader.getConfigManager().getConfig().getBoolean("chat.hover-moderation.enabled", true);
        String staffReadyMessage = readyMessage;
        if (hoverEnabled) {
            String staffFormat = getHoverModeration(sender.getName()) + format;
            Component staffComponent = miniMessage.deserialize(staffFormat);
            staffReadyMessage = legacySerializer.serialize(staffComponent);
        }

        // 7. Рассылка сообщений
        if (isGlobal) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(sender) || !loader.getIgnoreManager().isIgnored(p.getUniqueId(), sender.getUniqueId())) {
                    // Админам шлем строку с кнопками
                    if (hoverEnabled && p.hasPermission("chatmt.admin.hover") && !p.equals(sender)) {
                        p.sendMessage(staffReadyMessage);
                    } else {
                        p.sendMessage(readyMessage);
                    }
                }
            }
            Bukkit.getConsoleSender().sendMessage("[Global] " + readyMessage);
        } else {
            int receiversCount = 0;
            for (Player p : sender.getWorld().getPlayers()) {
                if (p.getLocation().distance(sender.getLocation()) <= localRadius) {
                    if (p.equals(sender) || !loader.getIgnoreManager().isIgnored(p.getUniqueId(), sender.getUniqueId())) {
                        if (hoverEnabled && p.hasPermission("chatmt.admin.hover") && !p.equals(sender)) {
                            p.sendMessage(staffReadyMessage);
                        } else {
                            p.sendMessage(readyMessage);
                        }
                        receiversCount++;
                    }
                }
            }

            Bukkit.getConsoleSender().sendMessage("[Local] " + readyMessage);

            if (receiversCount == 1) {
                String nobodyMsg = loader.getConfigManager().getMessages().getString(
                        "system.nobody-heard",
                        "<gray>[<red>!<gray>] <red>Вас никто не услышал... Напишите <yellow>%prefix% <red>перед сообщением для глобального чата."
                );
                Component nobodyComp = miniMessage.deserialize(nobodyMsg.replace("%prefix%", globalPrefix));
                sender.sendMessage(legacySerializer.serialize(nobodyComp));
            }
        }

        // 8. Логирование
        if (loader.getLoggerMT() != null) {
            loader.getLoggerMT().logChat(sender.getName(), originalMessage, isGlobal);
        }
    }

    /**
     * Создает кликабельные кнопки [М] [В] [К] для админов
     */
    private String getHoverModeration(String targetName) {
        String muteHover = loader.getConfigManager().getMessages().getString(
                "punishments.hover-mute",
                "<red>Нажмите, чтобы выдать мут"
        );
        String warnHover = loader.getConfigManager().getMessages().getString(
                "punishments.hover-warn",
                "<yellow>Нажмите, чтобы выдать варн"
        );
        String kickHover = loader.getConfigManager().getMessages().getString(
                "punishments.hover-kick",
                "<dark_red>Нажмите, чтобы кикнуть"
        );

        String muteBtn = "<click:suggest_command:'/mute " + targetName + " 1h '><hover:show_text:'" + muteHover + "'><dark_gray>[<red>М<dark_gray>]</hover></click>";
        String warnBtn = "<click:suggest_command:'/warn " + targetName + " '><hover:show_text:'" + warnHover + "'><dark_gray>[<yellow>В<dark_gray>]</hover></click>";
        String kickBtn = "<click:suggest_command:'/kick " + targetName + " '><hover:show_text:'" + kickHover + "'><dark_gray>[<dark_red>К<dark_gray>]</hover></click>";

        return muteBtn + " " + warnBtn + " " + kickBtn + " ";
    }
}