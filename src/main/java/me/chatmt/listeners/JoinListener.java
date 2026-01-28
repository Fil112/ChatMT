package me.chatmt.listeners;

import me.chatmt.ChatMT;
import me.chatmt.utils.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinListener implements Listener {

    private final ChatMT plugin;

    public JoinListener(ChatMT plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Проверка бана
        if (plugin.getPunishModule().isBanned(player.getName())) {
            String reason = plugin.getPunishModule().getBanReason(player.getName());

            // Используем стандартный kickPlayer, но с нашей покраской в Legacy-формат
            String kickMessage = ColorUtil.parseToLegacy("<red>Вы забанены!<br><gray>Причина: " + reason);
            player.kickPlayer(kickMessage);

            event.setJoinMessage(null);
            return;
        }

        if (!plugin.getConfig().getBoolean("modules.join-quit")) return;

        String msg = plugin.getConfig().getString("join-quit.join");
        if (msg != null && !msg.isEmpty()) {
            msg = msg.replace("%player%", player.getName());
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                msg = PlaceholderAPI.setPlaceholders(player, msg);
            }
            // Используем parseToLegacy, так как setJoinMessage принимает только String
            event.setJoinMessage(ColorUtil.parseToLegacy(msg));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("modules.join-quit")) return;

        String msg = plugin.getConfig().getString("join-quit.quit");
        if (msg != null && !msg.isEmpty()) {
            msg = msg.replace("%player%", event.getPlayer().getName());
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                msg = PlaceholderAPI.setPlaceholders(event.getPlayer(), msg);
            }
            event.setQuitMessage(ColorUtil.parseToLegacy(msg));
        }
    }
}