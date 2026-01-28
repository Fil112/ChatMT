package me.chatmt.commands;

import me.chatmt.ChatMT;
import me.chatmt.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainCommand implements CommandExecutor, TabCompleter {

    private final ChatMT plugin = ChatMT.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!checkPerm(sender, "chatmt.admin.reload")) return true;
                plugin.reloadPlugin();
                msg(sender, plugin.getMsgManager().getMessage("reload-success"));
                break;

            case "clear":
                if (!checkPerm(sender, "chatmt.staff.clear")) return true;
                for (int i = 0; i < 100; i++) Bukkit.broadcastMessage("");
                Bukkit.broadcastMessage(ColorUtil.parseToLegacy(plugin.getMsgManager().getMessage("chat-cleared")));
                break;

            case "punish":
                if (!(sender instanceof Player)) return true;
                if (!checkPerm(sender, "chatmt.staff.punish")) return true;
                if (args.length < 2) return false;
                plugin.getPunishModule().openPunishMenu((Player) sender, args[1]);
                break;

            case "unmute":
                if (!checkPerm(sender, "chatmt.staff.unmute")) return true;
                if (args.length < 2) return false;
                plugin.getPunishModule().unmute(args[1]);
                msg(sender, "<#55FFBB>Мут с игрока " + args[1] + " снят.");
                break;

            case "unban":
                if (!checkPerm(sender, "chatmt.staff.unban")) return true;
                if (args.length < 2) return false;
                plugin.getPunishModule().unban(args[1]);
                msg(sender, "<#55FFBB>Игрок " + args[1] + " разбанен.");
                break;

            default:
                msg(sender, "<#FF5555>Неизвестная подкоманда. Используйте /mt help");
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        msg(sender, plugin.getMsgManager().getMessage("help-title"));
        msg(sender, "<gray>/mt reload <dark_gray>- <white>Перезагрузка");
        msg(sender, "<gray>/mt clear <dark_gray>- <white>Очистить чат");
        msg(sender, "<gray>/mt punish <player> <dark_gray>- <white>Меню наказаний");
        msg(sender, "<gray>/mt unmute/unban <player> <dark_gray>- <white>Снять наказание");
    }

    // Вспомогательный метод для отправки через Adventure
    private void msg(CommandSender sender, String text) {
        plugin.getAdventure().sender(sender).sendMessage(ColorUtil.parse(text));
    }

    private boolean checkPerm(CommandSender s, String p) {
        if (!s.hasPermission(p)) {
            msg(s, plugin.getMsgManager().getMessage("no-permission"));
            return false;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("help", "reload", "clear", "punish", "unmute", "unban").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("punish") || args[0].equalsIgnoreCase("unmute") || args[0].equalsIgnoreCase("unban"))) {
            return null;
        }
        return new ArrayList<>();
    }
}