package me.chatmt.modules.chat;

import me.chatmt.ChatMT;
import me.chatmt.utils.ColorUtil;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class AutoMessageModule extends BukkitRunnable {

    private final ChatMT plugin;
    private int index = 0;

    public AutoMessageModule(ChatMT plugin) {
        this.plugin = plugin;
        int interval = plugin.getConfig().getInt("auto-messages.interval", 300);
        this.runTaskTimer(plugin, interval * 20L, interval * 20L);
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("modules.auto-messages")) return;

        List<String> messages = plugin.getConfig().getStringList("auto-messages.messages");
        if (messages.isEmpty()) return;

        if (index >= messages.size()) index = 0;

        String msg = messages.get(index);

        // Отправляем через Adventure всем игрокам
        plugin.getAdventure().all().sendMessage(ColorUtil.parse(msg));

        index++;
    }
}