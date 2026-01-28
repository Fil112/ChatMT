package me.chatmt.modules.chat;

import me.chatmt.ChatMT;
import org.bukkit.configuration.ConfigurationSection;

public class ReplacerModule {

    private final ChatMT plugin;

    public ReplacerModule(ChatMT plugin) {
        this.plugin = plugin;
    }

    public String replace(String message) {
        if (!plugin.getConfig().getBoolean("modules.replacer")) return message;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("replacer");
        if (section == null) return message;

        for (String key : section.getKeys(false)) {
            // Используем replace, чтобы заменить все вхождения
            if (message.contains(key)) {
                message = message.replace(key, section.getString(key));
            }
        }
        return message;
    }
}