package me.chatmt;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;

public class ChatListener implements Listener {
    private final ChatMT plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ChatListener(ChatMT plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.getConfig().getBoolean("modules.join-quit"))
            e.setJoinMessage(plugin.translateHexColorCodes(plugin.getConfig().getString("join-quit.join").replace("%player%", e.getPlayer().getName())));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.getConfig().getBoolean("modules.join-quit"))
            e.setQuitMessage(plugin.translateHexColorCodes(plugin.getConfig().getString("join-quit.quit").replace("%player%", e.getPlayer().getName())));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 1. Проверка Мута
        if (plugin.getMutedPlayers().contains(player.getName())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLangMsg("mute-message"));
            return;
        }

        // 2. Анти-спам
        if (plugin.getConfig().getBoolean("modules.anti-spam") && !player.hasPermission("chatmt.admin")) {
            long now = System.currentTimeMillis();
            if (now - cooldowns.getOrDefault(player.getUniqueId(), 0L) < plugin.getConfig().getInt("anti-spam.cooldown") * 1000L) {
                event.setCancelled(true);
                player.sendMessage(plugin.getLangMsg("spam-cooldown"));
                return;
            }
            cooldowns.put(player.getUniqueId(), now);
        }

        // 3. Авто-исправление КАПСА
        if (plugin.getConfig().getBoolean("modules.anti-caps") && message.length() > 5) {
            int upper = 0;
            for (char c : message.toCharArray()) if (Character.isUpperCase(c)) upper++;
            if ((double) upper / message.length() > 0.5) message = message.toLowerCase();
        }

        // 4. Цензура и Наказание
        if (plugin.getConfig().getBoolean("modules.censorship")) {
            for (String bad : plugin.getConfig().getStringList("censorship.blocked-words")) {
                if (message.toLowerCase().contains(bad.toLowerCase())) {
                    event.setCancelled(true);
                    String cmd = plugin.getConfig().getString("censorship.punishment-command").replace("%player%", player.getName());
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                    return;
                }
            }
        }

        // 5. Авто-замена (Replacer)
        if (plugin.getConfig().getBoolean("modules.replacer")) {
            for (String key : plugin.getConfig().getConfigurationSection("replacer").getKeys(false)) {
                message = message.replace(key, plugin.getConfig().getString("replacer." + key));
            }
        }

        // 6. Упоминания (Mentions)
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (message.contains(online.getName())) {
                message = message.replace(online.getName(), plugin.translateHexColorCodes(plugin.getConfig().getString("mentions.color") + online.getName() + "&r"));
                online.playSound(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }

        // 7. Система ЧАТОВ (Локальный/Глобальный)
        if (plugin.getConfig().getBoolean("modules.multi-chat")) {
            event.setCancelled(true);
            String chatKey = "local";
            for (String key : plugin.getConfig().getConfigurationSection("chats").getKeys(false)) {
                String sym = plugin.getConfig().getString("chats." + key + ".symbol");
                if (!sym.isEmpty() && message.startsWith(sym)) {
                    chatKey = key;
                    message = message.substring(sym.length()).trim();
                    break;
                }
            }

            int radius = plugin.getConfig().getInt("chats." + chatKey + ".radius");
            String format = plugin.getConfig().getString("chats." + chatKey + ".format");

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) format = PlaceholderAPI.setPlaceholders(player, format);
            String colored = player.hasPermission("chatmt.color") ? plugin.translateHexColorCodes(message) : message;
            String finalMsg = plugin.translateHexColorCodes(format.replace("%message%", colored));

            for (Player rec : event.getRecipients()) {
                if (radius == -1 || (radius == 0 && rec.getWorld().equals(player.getWorld())) ||
                (radius > 0 && rec.getWorld().equals(player.getWorld()) && rec.getLocation().distance(player.getLocation()) <= radius)) {
                    rec.sendMessage(finalMsg);
                }
            }
            Bukkit.getConsoleSender().sendMessage(finalMsg);
        }
    }

    // Ограничение команд (Контроль команд)
    @EventHandler
    public void onCmd(PlayerCommandPreprocessEvent e) {
        if (!plugin.getConfig().getBoolean("modules.commands-control") || e.getPlayer().hasPermission("chatmt.admin")) return;
        String cmd = e.getMessage().split(" ")[0].replace("/", "").toLowerCase();
        boolean isBlack = plugin.getConfig().getString("commands-control.list-type").equals("BLACK_LIST");
        if (isBlack == plugin.getConfig().getStringList("commands-control.list").contains(cmd)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(plugin.getLangMsg("command-blocked"));
        }
    }
}