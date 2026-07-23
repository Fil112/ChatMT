package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class MtCmd implements CommandExecutor, TabCompleter {

    private final MonolithLoader loader;

    public MtCmd(MonolithLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtils.colorize("<gradient:#ff5e62:#ff9966>ChatMT V2</gradient> <dark_gray>| <gray>Автор: <white>MT Studio"));
            sender.sendMessage(ColorUtils.colorize("<gray>Используйте <white>/mt reload <gray>или <white>/mt spy"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("chatmt.admin")) {
                sender.sendMessage(ColorUtils.colorize(loader.getConfigManager().getMessages().getString("system.no-permission", "<red>Нет прав!")));
                return true;
            }

            loader.getConfigManager().reload();
            loader.getAutoBroadcaster().start(); // Перезапуск таймера автосообщений

            sender.sendMessage(ColorUtils.colorize(loader.getConfigManager().getMessages().getString("system.reload", "<green>Конфигурация перезагружена!")));
            return true;
        }

        if (args[0].equalsIgnoreCase("spy")) {
            if (!sender.hasPermission("chatmt.spy")) {
                sender.sendMessage(ColorUtils.colorize("<red>Нет прав!"));
                return true;
            }
            if (!(sender instanceof Player)) {
                sender.sendMessage("Только для игроков.");
                return true;
            }
            Player p = (Player) sender;
            if (args.length < 2) {
                p.sendMessage(ColorUtils.colorize("<gray>Использование: <white>/mt spy <social/command>"));
                return true;
            }
            if (args[1].equalsIgnoreCase("social")) {
                loader.getSpyManager().toggleSocialSpy(p);
            } else if (args[1].equalsIgnoreCase("command")) {
                loader.getSpyManager().toggleCommandSpy(p);
            } else {
                p.sendMessage(ColorUtils.colorize("<red>Неизвестный тип шпионажа."));
            }
            return true;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("chatmt.admin")) {
            completions.add("reload");
            completions.add("spy");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("spy") && sender.hasPermission("chatmt.spy")) {
            completions.add("social");
            completions.add("command");
        }
        return completions;
    }
}