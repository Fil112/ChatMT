package mt.chat.utils;

import mt.chat.system.MonolithLoader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SpyManager {

    private final MonolithLoader loader;

    // Храним UUID игроков, у которых включен шпионаж
    private final Set<UUID> socialSpyActive = new HashSet<>();
    private final Set<UUID> commandSpyActive = new HashSet<>();

    public SpyManager(MonolithLoader loader) {
        this.loader = loader;
    }

    // --- Social Spy (Шпионаж за ЛС) ---

    public void toggleSocialSpy(Player player) {
        if (socialSpyActive.contains(player.getUniqueId())) {
            socialSpyActive.remove(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("<red>Social Spy выключен."));
        } else {
            socialSpyActive.add(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("<green>Social Spy включен. Вы видите чужие ЛС."));
        }
    }

    public void sendSocialSpyLog(Player sender, Player target, String message) {
        String logMessage = ColorUtils.colorize("<dark_gray>[<red>Spy<dark_gray>] <gray>" + sender.getName() + " -> " + target.getName() + ": <white>" + message);
        for (UUID uuid : socialSpyActive) {
            Player admin = Bukkit.getPlayer(uuid);
            // Отправляем лог админу, если он онлайн и не является ни отправителем, ни получателем
            if (admin != null && admin.isOnline() && !admin.equals(sender) && !admin.equals(target)) {
                admin.sendMessage(logMessage);
            }
        }
    }

    // --- Command Spy (Шпионаж за командами) ---

    public void toggleCommandSpy(Player player) {
        if (commandSpyActive.contains(player.getUniqueId())) {
            commandSpyActive.remove(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("<red>Command Spy выключен."));
        } else {
            commandSpyActive.add(player.getUniqueId());
            player.sendMessage(ColorUtils.colorize("<green>Command Spy включен. Вы видите вводимые команды."));
        }
    }

    public void sendCommandSpyLog(Player sender, String command) {
        String logMessage = ColorUtils.colorize("<dark_gray>[<yellow>Cmd<dark_gray>] <gray>" + sender.getName() + ": <white>" + command);
        for (UUID uuid : commandSpyActive) {
            Player admin = Bukkit.getPlayer(uuid);
            if (admin != null && admin.isOnline() && !admin.equals(sender)) {
                admin.sendMessage(logMessage);
            }
        }
    }
}