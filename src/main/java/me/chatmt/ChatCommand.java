package me.chatmt;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand implements CommandExecutor {
    private final ChatMT plugin = ChatMT.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Команда перезагрузки
        if (label.equalsIgnoreCase("chatmt")) {
            if (!sender.hasPermission("chatmt.admin")) {
                sender.sendMessage(plugin.getLangMsg("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(plugin.getLangMsg("reload-success"));
            return true;
        }

        // Команды модерации
        if (!sender.hasPermission("chatmt.staff")) {
            sender.sendMessage(plugin.getLangMsg("no-permission"));
            return true;
        }

        if (args.length < 1) return false;
        String targetName = args[0];

        if (label.equalsIgnoreCase("mtmute")) {
            if (plugin.getMutedPlayers().contains(targetName)) {
                plugin.getMutedPlayers().remove(targetName);
                sender.sendMessage("§aИгрок " + targetName + " размучен.");
            } else {
                plugin.getMutedPlayers().add(targetName);
                sender.sendMessage("§cИгрок " + targetName + " замучен.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("mtkick")) {
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) {
                target.kickPlayer(plugin.translateHexColorCodes(args.length > 1 ? args[1] : "Kicked by Staff"));
                sender.sendMessage("§aИгрок кикнут.");
            }
            return true;
        }

        if (label.equalsIgnoreCase("mtban")) {
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, "Banned by Staff", null, null);
            Player target = Bukkit.getPlayer(targetName);
            if (target != null) target.kickPlayer("Banned");
            sender.sendMessage("§aИгрок забанен.");
            return true;
        }

        return true;
    }
}