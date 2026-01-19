package me.chatmt;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ChatCommand implements CommandExecutor {
    private final ChatMT plugin;

    public ChatCommand(ChatMT plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Команда управления плагином (/chatmt reload)
        if (label.equalsIgnoreCase("chatmt")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("chatmt.admin")) {
                    sender.sendMessage(plugin.getLangMsg("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.loadLang();
                plugin.loadData();
                plugin.startAutoMessages();
                sender.sendMessage(plugin.getLangMsg("reload-success"));
                return true;
            }
            sender.sendMessage(plugin.getLangMsg("usage"));
            return true;
        }

        // Команды наказаний (Kick, Ban, Mute)
        if (label.equalsIgnoreCase("mtkick") || label.equalsIgnoreCase("mtban") || label.equalsIgnoreCase("mtmute")) {
            if (!sender.hasPermission("chatmt.staff")) {
                sender.sendMessage(plugin.getLangMsg("no-permission"));
                return true;
            }

            if (args.length < 1) {
                sender.sendMessage(plugin.getLangMsg("punish-usage").replace("%cmd%", label));
                return true;
            }

            String targetName = args[0];

            // Логика МУТА с сохранением в data.yml
            if (label.equalsIgnoreCase("mtmute")) {
                if (plugin.getMutedPlayers().contains(targetName)) {
                    plugin.getMutedPlayers().remove(targetName);
                    sender.sendMessage(plugin.getLangMsg("unmuted").replace("%player%", targetName));
                } else {
                    plugin.getMutedPlayers().add(targetName);
                    sender.sendMessage(plugin.getLangMsg("muted").replace("%player%", targetName));
                }
                plugin.saveData(); // Сохраняем изменения в файл
                return true;
            }

            // Логика БАНА и КИКА
            StringBuilder reasonBuilder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                reasonBuilder.append(args[i]).append(" ");
            }

            String reason = reasonBuilder.toString().trim();
            if (reason.isEmpty()) {
                reason = plugin.getLangMsg("default-reason");
            }

            String vanillaCmd = label.replace("mt", "");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), vanillaCmd + " " + targetName + " " + reason);

            sender.sendMessage(plugin.getLangMsg("action-executed"));
            return true;
        }

        return false;
    }
}