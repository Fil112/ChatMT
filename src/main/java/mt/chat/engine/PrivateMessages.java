package mt.chat.engine;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только игроки могут писать в ЛС.");
            return true;
        }

        Player player = (Player) sender;

        // Обработка /msg <игрок> <текст>
        if (command.getName().equalsIgnoreCase("msg")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtils.colorize("<red>Использование: /msg <ник> <сообщение>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(ColorUtils.colorize("<gray>Игрок не найден."));
                return true;
            }

            if (target.equals(player)) {
                player.sendMessage(ColorUtils.colorize("<gray>Нельзя писать самому себе."));
                return true;
            }

            // Склеиваем сообщение из аргументов
            String message = String.join(" ", args).substring(args[0].length() + 1);
            sendMessage(player, target, message);
            return true;
        }

        // Обработка /reply <текст>
        if (command.getName().equalsIgnoreCase("reply")) {
            if (args.length < 1) {
                player.sendMessage(ColorUtils.colorize("<red>Использование: /r <сообщение>"));
                return true;
            }

            if (!lastConversations.containsKey(player.getUniqueId())) {
                player.sendMessage(ColorUtils.colorize("<gray>Вам некому отвечать."));
                return true;
            }

            UUID targetId = lastConversations.get(player.getUniqueId());
            Player target = Bukkit.getPlayer(targetId);

            if (target == null || !target.isOnline()) {
                player.sendMessage(ColorUtils.colorize("<gray>Игрок уже вышел с сервера."));
                return true;
            }

            String message = String.join(" ", args);
            sendMessage(player, target, message);
            return true;
        }

        return true;
    }

    private void sendMessage(Player sender, Player target, String message) {
        // Форматы сообщений для отправителя и получателя
        String formatTo = loader.getConfigManager().getMessages().getString("formats.pm-send", "<gray>Вы -> %target%: <white><message>");
        String formatFrom = loader.getConfigManager().getMessages().getString("formats.pm-receive", "<gray>%sender% -> Вам: <white><message>");

        formatTo = formatTo.replace("%target%", target.getName()).replace("<message>", message);
        formatFrom = formatFrom.replace("%sender%", sender.getName()).replace("<message>", message);

        sender.sendMessage(ColorUtils.colorize(formatTo));
        target.sendMessage(ColorUtils.colorize(formatFrom));

        // Обновляем историю переписки для обоих игроков
        lastConversations.put(sender.getUniqueId(), target.getUniqueId());
        lastConversations.put(target.getUniqueId(), sender.getUniqueId());

        // Отправка в шпионскую систему для админов (Social Spy)
        loader.getSpyManager().sendSocialSpyLog(sender, target, message);
    }
}