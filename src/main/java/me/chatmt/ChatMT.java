package me.chatmt;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMT extends JavaPlugin {

    private static ChatMT instance;
    private List<String> mutedPlayers = new ArrayList<>();
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Создание необходимых файлов и папок
        saveDefaultConfig();
        saveResourceIfNotExists("lang/ru.yml");
        saveResourceIfNotExists("lang/en.yml");
        saveResourceIfNotExists("wiki/wiki_ru.md");
        saveResourceIfNotExists("wiki/wiki_en.md");

        // 2. Загрузка локализации
        loadLangFile();

        // 3. Проверка интеграций (PAPI, Vault, LuckPerms)
        checkIntegrations();

        // 4. Регистрация команд
        getCommand("chatmt").setExecutor(new ChatCommand());
        getCommand("mtmute").setExecutor(new ChatCommand());
        getCommand("mtkick").setExecutor(new ChatCommand());
        getCommand("mtban").setExecutor(new ChatCommand());

        // 5. Регистрация слушателя событий
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        // 6. Запуск модуля авто-сообщений
        startAutoMessages();

        getLogger().info("ChatMT v1.1.0 has been successfully enabled on Java 16+");
    }

    // Метод для безопасного сохранения ресурсов (чтобы не перезаписывать измененные файлы)
    private void saveResourceIfNotExists(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }

    private void loadLangFile() {
        String lang = getConfig().getString("language", "ru");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            langFile = new File(getDataFolder(), "lang/ru.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private void checkIntegrations() {
        String[] mods = {"PlaceholderAPI", "Vault", "LuckPerms"};
        for (String mod : mods) {
            if (getServer().getPluginManager().getPlugin(mod) != null) {
                getLogger().info("[+] Integration with " + mod + " found.");
            }
        }
    }

    private void startAutoMessages() {
        if (!getConfig().getBoolean("modules.auto-messages")) return;

        long interval = getConfig().getLong("auto-messages.interval", 300) * 20L;
        List<String> messages = getConfig().getStringList("auto-messages.messages");

        if (messages.isEmpty()) return;

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            String randomMsg = messages.get((int) (Math.random() * messages.size()));
            Bukkit.broadcastMessage(translateHexColorCodes(randomMsg));
        }, interval, interval);
    }

    // Универсальный метод для перевода HEX и обычных кодов цветов
    public String translateHexColorCodes(String message) {
        if (message == null) return "";

        // Поддержка формата &#FFFFFF
        final Pattern hexPattern = Pattern.compile("&#" + "([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuilder buffer = new StringBuilder();

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + group).toString());
        }
        String hexProcessed = matcher.appendTail(buffer).toString();

        // Перевод стандартных кодов (&a, &l и т.д.)
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', hexProcessed);
    }

    // Получение сообщения из lang файла по ключу
    public String getLangMsg(String key) {
        if (langConfig == null) return "Lang file not loaded";
        String msg = langConfig.getString(key, "Message key not found: " + key);
        return translateHexColorCodes(msg);
    }

    // Управление списком мутов
    public List<String> getMutedPlayers() {
        return mutedPlayers;
    }

    public static ChatMT getInstance() {
        return instance;
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatMT disabled.");
    }
}