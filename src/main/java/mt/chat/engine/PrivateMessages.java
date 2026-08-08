package mt.chat.engine;

import mt.chat.system.MonolithLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivateMessages implements CommandExecutor {

    private final MonolithLoader loader;
    // Храним историю: кто кому писал последним, чтобы работала команда /reply
    private final Map<UUID, UUID> lastConversations = new HashMap<>();

    public PrivateMessages(MonolithLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут писать в ЛС.");
            return true;
        }

        Player player = (Player) sender;

        // Обработка /msg <игрок> <текст>
        if (command.getName().equalsIgnoreCase("msg")) {
            if (args.length < 2) {
                String usageMsg = loader.getConfigManager().getMessages().getString("formats.msg-usage", "<red>Использование: /msg <ник> <сообщение>");
                sendConverted(player, usageMsg);
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                String offlineMsg = loader.getConfigManager().getMessages().getString("system.player-offline", "<gray>Игрок не найден или оффлайн.");
                sendConverted(player, offlineMsg);
                return true;
            }

            if (target.equals(player)) {
                sendConverted(player, "<gray>Нельзя писать самому себе.");
                return true;
            }

            // Безопасное склеивание сообщения из аргументов
            StringBuilder messageBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                messageBuilder.append(args[i]).append(" ");
            }
            String message = messageBuilder.toString().trim();

            sendMessage(player, target, message);
            return true;
        }

        // Обработка /reply <текст>
        if (command.getName().equalsIgnoreCase("reply")) {
            if (args.length < 1) {
                String usageMsg = loader.getConfigManager().getMessages().getString("formats.reply-usage", "<red>Использование: /reply <сообщение>");
                sendConverted(player, usageMsg);
                return true;
            }

            if (!lastConversations.containsKey(player.getUniqueId())) {
                sendConverted(player, "<gray>Вам некому отвечать.");
                return true;
            }

            UUID targetId = lastConversations.get(player.getUniqueId());
            Player target = Bukkit.getPlayer(targetId);

            if (target == null || !target.isOnline()) {
                sendConverted(player, "<gray>Игрок уже вышел с сервера.");
                return true;
            }

            String message = String.join(" ", args);
            sendMessage(player, target, message);
            return true;
        }

        return true;
    }

    private void sendMessage(Player sender, Player target, String message) {
        // 1. Проверка системы игноров (отменяем отправку, если мы в ЧС)
        if (loader.getIgnoreManager().isIgnored(target.getUniqueId(), sender.getUniqueId())) {
            String ignoredMsg = loader.getConfigManager().getMessages().getString(
                    "ignore.you-are-ignored",
                    "<red>Упс! Этот игрок ограничил доступ к своим личным сообщениям для вас."
            );
            sendConverted(sender, ignoredMsg);
            return;
        }

        // 2. Пропускаем текст через умный антимат
        message = loader.getAntiSwear().filterSwear(sender, message);

        // 3. Форматируем сообщения (MiniMessage)
        String formatTo = loader.getConfigManager().getMessages().getString("formats.pm-send", "<gray>Вы -> %target%: <white>%message%");
        String formatFrom = loader.getConfigManager().getMessages().getString("formats.pm-receive", "<gray>%sender% -> Вам: <white>%message%");

        // Заменяем плейсхолдеры
        String finalTo = formatTo.replace("%target%", target.getName())
                .replace("%message%", message)
                .replace("<message>", message);

        String finalFrom = formatFrom.replace("%sender%", sender.getName())
                .replace("%message%", message)
                .replace("<message>", message);

        // 4. Отправка
        sendConverted(sender, finalTo);
        sendConverted(target, finalFrom);

        // 5. Обновляем историю переписки для команды /reply
        lastConversations.put(sender.getUniqueId(), target.getUniqueId());
        lastConversations.put(target.getUniqueId(), sender.getUniqueId());

        // 6. Отправка в шпионскую систему для админов (Social Spy)
        if (loader.getSpyManager() != null) {
            loader.getSpyManager().sendSocialSpyLog(sender, target, message);
        }

        // 7. Логируем в файл
        if (loader.getLoggerMT() != null) {
            loader.getLoggerMT().logPrivateMessage(sender.getName(), target.getName(), message);
        }
    }

    /**
     * Вспомогательный метод для перевода MiniMessage Component в строку,
     * понятную ванильному ядру Spigot.
     */
    private void sendConverted(Player player, String miniMessageText) {
        Component comp = MiniMessage.miniMessage().deserialize(miniMessageText);
        String legacyText = LegacyComponentSerializer.legacySection().serialize(comp);
        player.sendMessage(legacyText);
    }
}