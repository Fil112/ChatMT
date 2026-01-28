package me.chatmt.modules.chat;

import me.chatmt.ChatMT;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AntiSpamModule {

    private final ChatMT plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public AntiSpamModule(ChatMT plugin) {
        this.plugin = plugin;
    }

    public boolean isSpamming(Player player) {
        if (!plugin.getConfig().getBoolean("modules.anti-spam")) return false;
        if (player.hasPermission("chatmt.bypass.spam")) return false;

        int cooldownSeconds = plugin.getConfig().getInt("anti-spam.cooldown", 1);
        long lastMessage = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        long timeElapsed = (System.currentTimeMillis() - lastMessage) / 1000;

        if (timeElapsed < cooldownSeconds) {
            return true; // Это спам
        }

        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        return false;
    }
}