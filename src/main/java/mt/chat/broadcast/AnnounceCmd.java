package mt.chat.broadcast;

import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AnnounceCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("chatmt.admin.broadcast")) {
            sender.sendMessage(ColorUtils.colorize("<red>У вас нет прав для объявлений."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ColorUtils.colorize("<red>Использование: /bc <сообщение>"));
            return true;
        }

        // Склеиваем аргументы в один текст
        String message = String.join(" ", args);

        // Добавляем красивый префикс (можно вынести в конфиг, но для простоты оставим тут)
        String broadcastPrefix = "<dark_gray>[<gradient:#f12711:#f5af19>ВНИМАНИЕ</gradient><dark_gray>] <white>";

        // Отправляем всем на сервере
        Bukkit.broadcastMessage(ColorUtils.colorize(broadcastPrefix + message));

        return true;
    }
}