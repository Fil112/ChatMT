package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PunishCmd implements CommandExecutor {

    private final MonolithLoader loader;

    public PunishCmd(MonolithLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // Проверка прав (например, chatmt.punish.kick)
        if (!sender.hasPermission("chatmt.punish." + cmdName)) {
            sender.sendMessage(ColorUtils.colorize("<red>У вас нет прав на эту команду!"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ColorUtils.colorize("<red>Использование: /" + cmdName + " <игрок> [причина]"));
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        // Склеиваем причину, если она есть
        String reason = "Нарушение правил";
        if (args.length > 1) {
            reason = String.join(" ", args).substring(args[0].length() + 1);
        }

        // --- Обработка KICK ---
        if (cmdName.equals("kick")) {
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage(ColorUtils.colorize("<gray>Игрок не найден."));
                return true;
            }
            targetPlayer.kickPlayer(ColorUtils.colorize("<red>Вы были кикнуты!\n<gray>Причина: <white>" + reason));
            Bukkit.broadcastMessage(ColorUtils.colorize("<dark_gray>[<red>!<dark_gray>] <white>" + targetName + " <gray>был кикнут администратором. Причина: <white>" + reason));

            // Записываем действие в лог-файл
            loader.getLoggerMT().logPunish(sender.getName(), targetName, "KICK", reason);
            return true;
        }

        // --- Обработка BAN (Используем встроенный Bukkit BanList) ---
        if (cmdName.equals("ban")) {
            Bukkit.getBanList(BanList.Type.NAME).addBan(targetName, reason, null, sender.getName());
            if (targetPlayer != null && targetPlayer.isOnline()) {
                targetPlayer.kickPlayer(ColorUtils.colorize("<red>Вы были забанены на сервере!\n<gray>Причина: <white>" + reason));
            }
            Bukkit.broadcastMessage(ColorUtils.colorize("<dark_gray>[<red>!<dark_gray>] <white>" + targetName + " <gray>забанен. Причина: <white>" + reason));

            // Записываем действие в лог-файл
            loader.getLoggerMT().logPunish(sender.getName(), targetName, "BAN", reason);
            return true;
        }

        // --- Обработка MUTE ---
        if (cmdName.equals("mute")) {
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                sender.sendMessage(ColorUtils.colorize("<gray>Игрок оффлайн. Мут пока выдается только онлайн игрокам."));
                return true;
            }

            // Выдаем мут на 30 минут по умолчанию
            long duration = 30 * 60 * 1000L;
            loader.getPunishManager().mutePlayer(targetPlayer.getUniqueId(), duration);

            Bukkit.broadcastMessage(ColorUtils.colorize("<dark_gray>[<red>!<dark_gray>] <white>" + targetName + " <gray>получил мут. Причина: <white>" + reason));

            // Записываем действие в лог-файл
            loader.getLoggerMT().logPunish(sender.getName(), targetName, "MUTE", reason);
            return true;
        }

        // --- Обработка UNMUTE ---
        if (cmdName.equals("unmute")) {
            if (targetPlayer != null) {
                loader.getPunishManager().unmutePlayer(targetPlayer.getUniqueId());
                sender.sendMessage(ColorUtils.colorize("<green>Мут снят с " + targetName));
                targetPlayer.sendMessage(ColorUtils.colorize("<green>Ваш мут был снят!"));

                // Записываем действие в лог-файл
                loader.getLoggerMT().logPunish(sender.getName(), targetName, "UNMUTE", "Снятие наказания");
            } else {
                sender.sendMessage(ColorUtils.colorize("<gray>Игрок не найден."));
            }
            return true;
        }

        return true;
    }
}