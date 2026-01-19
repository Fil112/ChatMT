package me.chatmt;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatMT extends JavaPlugin {

    private FileConfiguration langConfig;
    private FileConfiguration dataConfig;
    private File dataFile;
    private int autoMsgTask = -1;
    private final Set<String> mutedPlayers = new HashSet<>();
    private final Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadLang();
        loadData();
        createWiki();
        startAutoMessages();

        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        ChatCommand commandExecutor = new ChatCommand(this);
        getCommand("chatmt").setExecutor(commandExecutor);
        getCommand("mtkick").setExecutor(commandExecutor);
        getCommand("mtban").setExecutor(commandExecutor);
        getCommand("mtmute").setExecutor(commandExecutor);

        getLogger().info("ChatMT v2.0 enabled with HEX & Data support!");
    }

    // Обработка HEX цветов (&#FFFFFF)
    public String translateHexColorCodes(String message) {
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String color = matcher.group(1);
            matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of("#" + color).toString());
        }
        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
    }

    public void loadLang() {
        String lang = getConfig().getString("language", "en");
        File langFile = new File(getDataFolder(), "lang/" + lang + ".yml");
        if (!langFile.exists()) {
            saveResource("lang/en.yml", false);
            saveResource("lang/ru.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    // Работа с файлом данных (муты)
    public void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        mutedPlayers.clear();
        mutedPlayers.addAll(dataConfig.getStringList("muted"));
    }

    public void saveData() {
        dataConfig.set("muted", new java.util.ArrayList<>(mutedPlayers));
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void startAutoMessages() {
        if (autoMsgTask != -1) Bukkit.getScheduler().cancelTask(autoMsgTask);
        if (getConfig().getBoolean("modules.auto-messages")) {
            long interval = getConfig().getLong("auto-messages.interval") * 20L;
            List<String> messages = getConfig().getStringList("auto-messages.messages");
            autoMsgTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
                if (!messages.isEmpty()) {
                    String msg = messages.get(new Random().nextInt(messages.size()));
                    Bukkit.broadcastMessage(translateHexColorCodes(msg));
                }
            }, interval, interval);
        }
    }

    public String getLangMsg(String key) {
        return translateHexColorCodes(langConfig.getString(key, "&cMissing key: " + key));
    }

    public Set<String> getMutedPlayers() { return mutedPlayers; }

    private void createWiki() {
        File wikiFolder = new File(getDataFolder(), "wiki");
        if (!wikiFolder.exists()) wikiFolder.mkdirs();
        String[] files = {"wiki_ru.md", "wiki_en.md", "contacts.md"};
        for (String f : files) {
            if (!new File(wikiFolder, f).exists()) saveResource("wiki/" + f, false);
        }
    }
}