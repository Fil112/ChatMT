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

public class ChatListener implements Listener {
    private final ChatMT plugin;

    public ChatListener(ChatMT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        // 1. Проверка мута
        if (plugin.getMutedPlayers().contains(player.getName())) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLangMsg("mute-message"));
            return;
        }

        String message = event.getMessage();

        // 2. Анти-капс (если включен и сообщение длиннее 5 символов)
        if (plugin.getConfig().getBoolean("modules.anti-caps") && message.length() > 5) {
            int upperCount = 0;
            for (char c : message.toCharArray()) if (Character.isUpperCase(c)) upperCount++;
            if ((double) upperCount / message.length() > 0.5) {
                message = message.toLowerCase();
            }
        }

        // 3. Цензура (блокировка слов)
        if (plugin.getConfig().getBoolean("modules.censorship")) {
            for (String word : plugin.getConfig().getStringList("censorship.blocked-words")) {
                if (message.toLowerCase().contains(word.toLowerCase())) {
                    event.setCancelled(true);
                    String cmd = plugin.getConfig().getString("censorship.punishment-command")
                            .replace("%player%", player.getName());
                    Bukkit.getScheduler().runTask(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
                    return;
                }
            }
        }

        // 4. Упоминания (Mentions)
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (message.contains(online.getName())) {
                String mentionColor = plugin.getConfig().getString("mentions.color", "&e&l");
                message = message.replace(online.getName(), mentionColor + online.getName() + "§r");
                online.playSound(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }

        // 5. Форматирование чата (PAPI + HEX)
        if (plugin.getConfig().getBoolean("modules.chat-format")) {
            String format = plugin.getConfig().getString("chat-format.format");

            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                format = PlaceholderAPI.setPlaceholders(player, format);
            }

            // Если у игрока есть право на цвета - обрабатываем HEX и обычные коды
            String coloredMessage = player.hasPermission("chatmt.color") ?
                    plugin.translateHexColorCodes(message) : message;

            // Финальная сборка формата с HEX поддержкой
            String finalFormat = plugin.translateHexColorCodes(format.replace("%message%", coloredMessage));
            event.setFormat(finalFormat.replace("%", "%%"));
        } else {
            // Если формат отключен, просто красим само сообщение
            event.setMessage(player.hasPermission("chatmt.color") ?
                    plugin.translateHexColorCodes(message) : message);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("modules.commands-control")) return;

        Player player = event.getPlayer();
        if (player.hasPermission("chatmt.admin")) return;

        String rawCommand = event.getMessage().split(" ")[0].replace("/", "").toLowerCase();
        java.util.List<String> commandList = plugin.getConfig().getStringList("commands-control.list");
        String listType = plugin.getConfig().getString("commands-control.list-type", "BLACK_LIST");

        boolean shouldBlock = listType.equalsIgnoreCase("BLACK_LIST") ?
                commandList.contains(rawCommand) : !commandList.contains(rawCommand);

        if (shouldBlock) {
            event.setCancelled(true);
            player.sendMessage(plugin.getLangMsg("command-blocked"));
        }
    }
}