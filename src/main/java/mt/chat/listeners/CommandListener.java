package mt.chat.listeners;

import mt.chat.system.MonolithLoader;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {

    private final MonolithLoader loader;

    public CommandListener(MonolithLoader loader) {
        this.loader = loader;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage(); // Начинается со слэша, например "/spawn"

        // Игнорируем логины и пароли от плагинов авторизации, чтобы админы их не угнали
        String lowerCmd = command.toLowerCase();
        if (lowerCmd.startsWith("/login") || lowerCmd.startsWith("/l ") ||
                lowerCmd.startsWith("/register") || lowerCmd.startsWith("/reg ")) {
            return;
        }

        // Отправляем команду в шпионский менеджер
        loader.getSpyManager().sendCommandSpyLog(player, command);
    }
}