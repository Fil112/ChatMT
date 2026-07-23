package mt.chat.broadcast;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class AutoBroadcaster {

    private final MonolithLoader loader;
    private BukkitTask task;
    private int currentIndex = 0;

    public AutoBroadcaster(MonolithLoader loader) {
        this.loader = loader;
    }

    public void start() {
        // Если уже был запущен (например, при /mt reload) — отменяем старый таймер
        stop();

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("broadcast.enabled", true);
        if (!enabled) return;

        List<String> messages = loader.getConfigManager().getConfig().getStringList("broadcast.messages");
        if (messages.isEmpty()) return;

        long intervalSeconds = loader.getConfigManager().getConfig().getLong("broadcast.interval", 300);
        long intervalTicks = intervalSeconds * 20L; // В 1 секунде 20 тиков сервера

        String prefix = loader.getConfigManager().getConfig().getString("broadcast.prefix", "&8[&bИнфо&8] &f");

        task = new BukkitRunnable() {
            @Override
            public void run() {
                // Если игроков нет, нет смысла спамить в пустой чат
                if (Bukkit.getOnlinePlayers().isEmpty()) return;

                // Берем текущее сообщение и переходим к следующему
                String message = messages.get(currentIndex);
                currentIndex++;

                // Если дошли до конца списка — начинаем сначала
                if (currentIndex >= messages.size()) {
                    currentIndex = 0;
                }

                // Склеиваем префикс и сообщение, затем красим
                String finalMessage = ColorUtils.colorize(prefix + message);
                Bukkit.broadcastMessage(finalMessage);
            }
        }.runTaskTimer(loader.getPlugin(), intervalTicks, intervalTicks);

        loader.getPlugin().getLogger().info(" -> Авто-оповещения запущены (сообщений: " + messages.size() + ")");
    }

    public void stop() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
}