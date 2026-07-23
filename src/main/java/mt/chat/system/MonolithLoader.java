package mt.chat.system;

import mt.chat.ChatMT;
import mt.chat.ai.GeminiManager;
import mt.chat.broadcast.AnnounceCmd;
import mt.chat.broadcast.AutoBroadcaster;
import mt.chat.engine.ChatEngine;
import mt.chat.engine.ChatFilters;
import mt.chat.engine.MentionManager;
import mt.chat.engine.PrivateMessages;
import mt.chat.listeners.ChatListener;
import mt.chat.listeners.CommandListener;
import mt.chat.listeners.PlayerJoinListener;
import mt.chat.moderation.AntiAdvertising;
import mt.chat.moderation.AntiSwear;
import mt.chat.moderation.MtCmd;
import mt.chat.moderation.PunishCmd;
import mt.chat.moderation.PunishManager;
import mt.chat.utils.LoggerMT;
import mt.chat.utils.SpyManager;
import org.bukkit.plugin.PluginManager;

public class MonolithLoader {

    private final ChatMT plugin;

    // --- Базовые менеджеры ---
    private ConfigManager configManager;
    private LoggerMT loggerMT;
    private GeminiManager geminiManager;

    // --- Модерация и фильтры ---
    private ChatFilters chatFilters;
    private AntiSwear antiSwear;
    private AntiAdvertising antiAdvertising;
    private PunishManager punishManager;
    private SpyManager spyManager;

    // --- Чат и уведомления ---
    private MentionManager mentionManager;
    private ChatEngine chatEngine;
    private AutoBroadcaster autoBroadcaster;

    public MonolithLoader(ChatMT plugin) {
        this.plugin = plugin;
    }

    public void init() {
        plugin.getLogger().info(" -> Загрузка конфигураций...");
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        plugin.getLogger().info(" -> Запуск логгера...");
        this.loggerMT = new LoggerMT(this);

        plugin.getLogger().info(" -> Подключение ИИ (Gemini)...");
        this.geminiManager = new GeminiManager(this);

        plugin.getLogger().info(" -> Подключение фильтров модерации...");
        this.chatFilters = new ChatFilters(this);
        this.antiSwear = new AntiSwear(this);
        this.antiAdvertising = new AntiAdvertising(this);

        plugin.getLogger().info(" -> Запуск системы наказаний...");
        this.punishManager = new PunishManager(this);

        plugin.getLogger().info(" -> Запуск системы шпионажа...");
        this.spyManager = new SpyManager(this);

        plugin.getLogger().info(" -> Запуск системы упоминаний...");
        this.mentionManager = new MentionManager(this);

        plugin.getLogger().info(" -> Подключение движка чата...");
        this.chatEngine = new ChatEngine(this);

        plugin.getLogger().info(" -> Запуск системы авто-оповещений...");
        this.autoBroadcaster = new AutoBroadcaster(this);
        this.autoBroadcaster.start();

        plugin.getLogger().info(" -> Регистрация слушателей и команд...");
        registerListeners();
        registerCommands();
    }

    public void shutdown() {
        plugin.getLogger().info(" -> Остановка процессов ChatMT...");
        // Обязательно тушим таймер автоброадкастера, чтобы не было утечек при релоаде сервера
        if (this.autoBroadcaster != null) {
            this.autoBroadcaster.stop();
        }
    }

    private void registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();

        pm.registerEvents(new ChatListener(this), plugin);
        pm.registerEvents(new CommandListener(this), plugin);
        pm.registerEvents(new PlayerJoinListener(this), plugin);
    }

    private void registerCommands() {
        // Главная команда
        MtCmd mtCmd = new MtCmd(this);
        plugin.getCommand("mt").setExecutor(mtCmd);
        plugin.getCommand("mt").setTabCompleter(mtCmd);

        // Личные сообщения
        PrivateMessages pm = new PrivateMessages(this);
        plugin.getCommand("msg").setExecutor(pm);
        plugin.getCommand("reply").setExecutor(pm);

        // Наказания
        PunishCmd punishCmd = new PunishCmd(this);
        plugin.getCommand("kick").setExecutor(punishCmd);
        plugin.getCommand("ban").setExecutor(punishCmd);
        plugin.getCommand("mute").setExecutor(punishCmd);
        plugin.getCommand("unmute").setExecutor(punishCmd);

        // Объявления
        plugin.getCommand("broadcast").setExecutor(new AnnounceCmd());
    }

    // =========================================================
    // Геттеры для доступа ко всем модулям из любой точки плагина
    // =========================================================

    public ChatMT getPlugin() {
        return plugin;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LoggerMT getLoggerMT() {
        return loggerMT;
    }

    public GeminiManager getGeminiManager() {
        return geminiManager;
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
}