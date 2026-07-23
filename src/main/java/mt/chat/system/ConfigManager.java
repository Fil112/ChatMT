package mt.chat.system;

import mt.chat.ChatMT;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {

    private final MonolithLoader loader;
    private final ChatMT plugin;

    private FileConfiguration config;
    private FileConfiguration messages;

    private File configFile;
    private File messagesFile;

    public ConfigManager(MonolithLoader loader) {
        this.loader = loader;
        this.plugin = loader.getPlugin();
    }

    /**
     * Загружает конфигурационные файлы.
     * Если папки или файлов нет — создаёт их из ресурсов плагина.
     */
    public void load() {
        // Создаем папку plugins/ChatMT, если её не существует
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        configFile = new File(plugin.getDataFolder(), "config.yml");
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        // Если файлов конфигурации физически нет в папке, выгружаем дефолтные
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        // Читаем файлы и загружаем их в память сервера
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Быстрая перезагрузка конфигурации.
     * Используется при вводе команды /mt reload
     */
    public void reload() {
        load();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}