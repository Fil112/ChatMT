package me.chatmt;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final ChatMT plugin;
    private FileConfiguration langConfig;

    public ConfigManager(ChatMT plugin) {
        this.plugin = plugin;
        loadLang();
    }

    public void loadLang() {
        String langName = plugin.getConfig().getString("settings.language", "ru");
        File langFile = new File(plugin.getDataFolder(), "lang/" + langName + ".yml");

        if (!langFile.exists()) {
            // Если файла нет, можно попробовать сохранить дефолтный из ресурсов
            plugin.saveResource("lang/ru.yml", false);
            langFile = new File(plugin.getDataFolder(), "lang/ru.yml");
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public String getMessage(String key) {
        if (langConfig == null) return "Lang Error";
        String msg = langConfig.getString(key);
        // Возвращаем сырую строку, парсить в Component будем при отправке
        if (msg == null) return "<red>Missing key: " + key;
        return msg;
    }
}