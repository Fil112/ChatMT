package mt.chat.system;

import mt.chat.ChatMT;
import mt.chat.ai.AiManager;
import mt.chat.broadcast.AnnounceCmd;
import mt.chat.broadcast.AutoBroadcaster;
import mt.chat.database.DatabaseManager;
import mt.chat.engine.*;
import mt.chat.listeners.ChatListener;
import mt.chat.listeners.CommandListener;
import mt.chat.listeners.PlayerJoinListener;
import mt.chat.listeners.DeathListener;
import mt.chat.moderation.*;
import mt.chat.network.NetworkManager;
import mt.chat.utils.LoggerMT;
import mt.chat.utils.SpyManager;
import org.bukkit.plugin.PluginManager;

public class MonolithLoader {

    private final ChatMT plugin;

    // --- Базовые менеджеры ---
    private ConfigManager configManager;
    private LoggerMT loggerMT;
    private AiManager aiManager;
    private DatabaseManager databaseManager;

    // --- Модерация и фильтры ---
    private ChatFilters chatFilters;
    private AntiSwear antiSwear;
    private AntiAdvertising antiAdvertising;
    private PunishManager punishManager;
    private IgnoreManager ignoreManager;
    private SpyManager spyManager;

    // --- Чат и уведомления ---
    private MentionManager mentionManager;
    private ChatEngine chatEngine;
    private AutoBroadcaster autoBroadcaster;
    private ChatGamesManager chatGamesManager;
    private ChannelManager channelManager;

    // --- Network (Bungee/Velocity) ---
    private NetworkManager networkManager;

    public MonolithLoader(ChatMT plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.getLogger().info(" -> Загрузка конфигураций...");
        this.configManager = new ConfigManager(this);

        plugin.getLogger().info(" -> Подключение к Базе Данных...");
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.connect();

        plugin.getLogger().info(" -> Запуск логгера...");
        this.loggerMT = new LoggerMT(this);

        plugin.getLogger().info(" -> Подключение ИИ-модуля (Universal / Gemini / Local)...");
        this.aiManager = new AiManager(this);

        plugin.getLogger().info(" -> Подключение фильтров модерации...");
        this.chatFilters = new ChatFilters(this);
        this.antiSwear = new AntiSwear(this);
        this.antiAdvertising = new AntiAdvertising(this);

        plugin.getLogger().info(" -> Запуск системы наказаний...");
        this.punishManager = new PunishManager(this);

        plugin.getLogger().info(" -> Запуск системы игноров...");
        this.ignoreManager = new IgnoreManager(this);

        plugin.getLogger().info(" -> Запуск системы шпионажа...");
        this.spyManager = new SpyManager(this);

        plugin.getLogger().info(" -> Запуск системы упоминаний...");
        this.mentionManager = new MentionManager(this);

        plugin.getLogger().info(" -> Подключение сетевого моста Bungee/Velocity...");
        this.networkManager = new NetworkManager(this);
        this.networkManager.register();

        plugin.getLogger().info(" -> Подключение движка чата...");
        this.chatEngine = new ChatEngine(this);

        plugin.getLogger().info(" -> Запуск системы авто-оповещений...");
        this.autoBroadcaster = new AutoBroadcaster(this);
        this.autoBroadcaster.start();

        plugin.getLogger().info(" -> Запуск модуля чат-игр...");
        this.chatGamesManager = new ChatGamesManager(this);
        this.chatGamesManager.start();

        plugin.getLogger().info(" -> Подключение системы каналов чата...");
        this.channelManager = new ChannelManager(this);

        plugin.getLogger().info(" -> Регистрация слушателей и команд...");
        registerListeners();
        registerCommands();
    }

    public void shutdown() {
        plugin.getLogger().info(" -> Остановка процессов ChatMT...");

        if (this.autoBroadcaster != null) {
            this.autoBroadcaster.stop();
        }

        if (this.chatGamesManager != null) {
            this.chatGamesManager.stop();
        }

        if (this.databaseManager != null) {
            this.databaseManager.disconnect();
        }

        if (this.networkManager != null) {
            this.networkManager.unregister();
        }
    }

    private void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new ChatListener(this), plugin);
        pm.registerEvents(new CommandListener(this), plugin);
        pm.registerEvents(new PlayerJoinListener(this), plugin);
        pm.registerEvents(new DeathListener(this), plugin);
    }

    private void registerCommands() {
        // Главная команда управления
        MtCmd mtCmd = new MtCmd(this);
        plugin.getCommand("mt").setExecutor(mtCmd);
        plugin.getCommand("mt").setTabCompleter(mtCmd);

        // Личные сообщения
        PrivateMessages pm = new PrivateMessages(this);
        plugin.getCommand("msg").setExecutor(pm);
        plugin.getCommand("reply").setExecutor(pm);

        // Команды модерации
        PunishCmd punishCmd = new PunishCmd(this);
        plugin.getCommand("kick").setExecutor(punishCmd);
        plugin.getCommand("ban").setExecutor(punishCmd);
        plugin.getCommand("unban").setExecutor(punishCmd);
        plugin.getCommand("mute").setExecutor(punishCmd);
        plugin.getCommand("unmute").setExecutor(punishCmd);
        plugin.getCommand("warn").setExecutor(punishCmd);
        plugin.getCommand("unwarn").setExecutor(punishCmd);

        // Оповещения
        plugin.getCommand("broadcast").setExecutor(new AnnounceCmd());
    }

    // =========================================================
    // Геттеры модулей
    // =========================================================

    public ChatMT getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public LoggerMT getLoggerMT() {
        return loggerMT;
    }

    public AiManager getAiManager() {
        return aiManager;
    }

    // Оставляем для совместимости
    public AiManager getGeminiManager() {
        return aiManager;
    }

    public ChatFilters getChatFilters() {
        return chatFilters;
    }

    public AntiSwear getAntiSwear() {
        return antiSwear;
    }

    public AntiAdvertising getAntiAdvertising() {
        return antiAdvertising;
    }

    public PunishManager getPunishManager() {
        return punishManager;
    }

    public IgnoreManager getIgnoreManager() {
        return ignoreManager;
    }

    public SpyManager getSpyManager() {
        return spyManager;
    }

    public MentionManager getMentionManager() {
        return mentionManager;
    }

    public ChatEngine getChatEngine() {
        return chatEngine;
    }

    public AutoBroadcaster getAutoBroadcaster() {
        return autoBroadcaster;
    }

    public ChatGamesManager getChatGamesManager() {
        return chatGamesManager;
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }
}