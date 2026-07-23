package mt.chat.listeners;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final MonolithLoader loader;

    public PlayerJoinListener(MonolithLoader loader) {
        this.loader = loader;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. Кастомное сообщение о входе
        String joinMsg = loader.getConfigManager().getMessages().getString("formats.join", "<dark_gray>[<green>+<dark_gray>] <gray>%player_name% зашёл на сервер");
        joinMsg = joinMsg.replace("%player_name%", player.getName());
        event.setJoinMessage(ColorUtils.colorize(joinMsg));

        // 2. Отправка Title (заголовок на экране)
        String title = loader.getConfigManager().getMessages().getString("system.join-title", "<gradient:#ff5e62:#ff9966>Добро пожаловать</gradient>");
        String subtitle = loader.getConfigManager().getMessages().getString("system.join-subtitle", "<gray>Приятной игры на сервере!");

        // Параметры: fade-in (10 тиков), stay (70 тиков), fade-out (20 тиков)
        player.sendTitle(ColorUtils.colorize(title), ColorUtils.colorize(subtitle), 10, 70, 20);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Кастомное сообщение о выходе
        String quitMsg = loader.getConfigManager().getMessages().getString("formats.quit", "<dark_gray>[<red>-<dark_gray>] <gray>%player_name% покинул сервер");
        quitMsg = quitMsg.replace("%player_name%", player.getName());
        event.setQuitMessage(ColorUtils.colorize(quitMsg));
    }
}