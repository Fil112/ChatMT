package me.chatmt.modules.visual;

import me.chatmt.ChatMT;
import me.chatmt.utils.ColorUtil;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class VisualModule {

    private final ChatMT plugin;

    public VisualModule(ChatMT plugin) {
        this.plugin = plugin;
        startTasks();
    }

    private void startTasks() {
        int interval = plugin.getConfig().getInt("visual.update-interval", 20);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.isEnabled()) { cancel(); return; }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateTab(player);
                }
            }
        }.runTaskTimer(plugin, 20L, interval);
    }

    private void updateTab(Player player) {
        if (!plugin.getConfig().getBoolean("modules.tablist")) return;

        List<String> headerList = plugin.getConfig().getStringList("visual.tablist.header");
        List<String> footerList = plugin.getConfig().getStringList("visual.tablist.footer");

        String header = String.join("\n", headerList);
        String footer = String.join("\n", footerList);

        header = parse(player, header);
        footer = parse(player, footer);

        // Используем ColorUtil.parse, так как он возвращает Component
        plugin.getAdventure().player(player).sendPlayerListHeaderAndFooter(
                ColorUtil.parse(header),
                ColorUtil.parse(footer)
        );
    }

    private String parse(Player p, String text) {
        text = text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%player%", p.getName())
                .replace("%time%", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            text = PlaceholderAPI.setPlaceholders(p, text);
        }
        return text;
    }
}