package me.chatmt;

import me.chatmt.commands.MainCommand;
import me.chatmt.listeners.ChatListener;
import me.chatmt.listeners.JoinListener;
import me.chatmt.listeners.PunishMenuListener;
import me.chatmt.modules.chat.AutoMessageModule;
import me.chatmt.modules.punish.PunishModule;
import me.chatmt.utils.ColorUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class ChatMT extends JavaPlugin {

    private static ChatMT instance;
    private BukkitAudiences adventure;
    private static Chat vaultChat = null;

    private ConfigManager configManager;
    private PunishModule punishModule;

    private FileConfiguration dataConfig;
    private File dataFile;

    @Override
    public void onEnable() {
        instance = this;

        // 1. Инициализация Adventure
        this.adventure = BukkitAudiences.create(this);

        // 2. Файловая система
        saveDefaultConfig();
        setupLangFiles();
        setupWikiFiles();
        loadData();

        // 3. Менеджеры
        this.configManager = new ConfigManager(this);
        this.punishModule = new PunishModule(this);

        // 4. Зависимости
        setupVaultChat();

        // 5. Модули
        if (getConfig().getBoolean("modules.auto-messages", true)) {
            new AutoMessageModule(this);
        }

        // 6. Регистрация событий
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PunishMenuListener(), this);

        // 7. Команды
        MainCommand mainCommand = new MainCommand();
        getCommand("chatmt").setExecutor(mainCommand);
        getCommand("chatmt").setTabCompleter(mainCommand);

        // Исправленный лог (через Adventure Console)
        this.adventure.console().sendMessage(ColorUtil.parse("\n<#55FFBB>ChatMT v1.4.3 <white>успешно загружен!"));
        this.adventure.console().sendMessage(ColorUtil.parse("<gray>Лицензия: <white>MIT | Интеграции: <#55FFBB>Vault, PAPI, WG"));
    }

    @Override
    public void onDisable() {
        saveData();
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

    private void setupLangFiles() {
        File langDir = new File(getDataFolder(), "lang");
        if (!langDir.exists()) langDir.mkdirs();
        saveResourceIfNotExists("lang/ru.yml");
        saveResourceIfNotExists("lang/en.yml");
    }

    private void setupWikiFiles() {
        File wikiDir = new File(getDataFolder(), "wiki");
        if (!wikiDir.exists()) wikiDir.mkdirs();
        saveResourceIfNotExists("wiki/ru.md");
        saveResourceIfNotExists("wiki/en.md");
    }

    private void saveResourceIfNotExists(String path) {
        if (!new File(getDataFolder(), path).exists()) saveResource(path, false);
    }

    public void loadData() {
        dataFile = new File(getDataFolder(), "data.yml");
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void saveData() {
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    public void reloadPlugin() {
        reloadConfig();
        loadData();
        configManager.loadLang();
    }

    private void setupVaultChat() {
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Chat> rsp = getServer().getServicesManager().getRegistration(Chat.class);
            if (rsp != null) vaultChat = rsp.getProvider();
        }
    }

    public static ChatMT getInstance() { return instance; }
    public BukkitAudiences getAdventure() { return adventure; }
    public ConfigManager getMsgManager() { return configManager; }
    public PunishModule getPunishModule() { return punishModule; }
    public FileConfiguration getDataConfig() { return dataConfig; }
    public static Chat getVaultChat() { return vaultChat; }
}