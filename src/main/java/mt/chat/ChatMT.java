package mt.chat;

import mt.chat.system.MonolithLoader;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatMT extends JavaPlugin {

    private MonolithLoader loader;

    @Override
    public void onEnable() {
        // Выводим лого в консоль при старте
        getLogger().info("=======================================");
        getLogger().info(" ChatMT V2 Запускается...");
        getLogger().info("=======================================");

        // Создаем и запускаем наш загрузчик (DI контейнер)
        this.loader = new MonolithLoader(this);
        this.loader.init();

        getLogger().info("[ChatMT] Монолит успешно загружен и готов к работе!");
    }

    @Override
    public void onDisable() {
        // Аккуратно тушим все процессы, если плагин вырубают или сервер стопается
        if (loader != null) {
            loader.shutdown();
        }
        getLogger().info("[ChatMT] Выключен.");
    }
}