package mt.chat.system;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final MonolithLoader loader;
    private FileConfiguration config;
    private FileConfiguration messages; // Здесь теперь хранится активный языковой файл

    public ConfigManager(MonolithLoader loader) {
        this.loader = loader;
        loadConfigs();
    }

    public void loadConfigs() {
        // 1. Загрузка основного config.yml
        File configFile = new File(loader.getPlugin().getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            loader.getPlugin().saveDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // 2. Создание папки language
        File langFolder = new File(loader.getPlugin().getDataFolder(), "language");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
            // Выгружаем стандартные языки из jar-файла при первом запуске
            saveDefaultLanguage(langFolder, "ru.yml");
            saveDefaultLanguage(langFolder, "en.yml");
        }

        // 3. Читаем выбранный язык из config.yml (по умолчанию ru)
        String langName = config.getString("system.language", "ru") + ".yml";
        File activeLangFile = new File(langFolder, langName);

        // Защита от дурака: если админ указал несуществующий язык (например, fr.yml),
        // плагин не крашнется, а просто создаст пустой файл или откатится
        if (!activeLangFile.exists()) {
            loader.getLoggerMT().error("Языковой файл " + langName + " не найден! Будет создан пустой файл.");
            saveDefaultLanguage(langFolder, langName);
        }

        messages = YamlConfiguration.loadConfiguration(activeLangFile);
    }

    private void saveDefaultLanguage(File langFolder, String fileName) {
        File file = new File(langFolder, fileName);
        if (!file.exists()) {
            // Пытаемся скопировать файл из папки resources/language/ внутри плагина
            if (loader.getPlugin().getResource("language/" + fileName) != null) {
                loader.getPlugin().saveResource("language/" + fileName, false);
            } else {
                // Если файла нет внутри плагина, создаем пустой, чтобы избежать NullPointerException
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    loader.getLoggerMT().error("Не удалось создать языковой файл: " + fileName);
                }
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public void reload() {
        loadConfigs();
    }
}