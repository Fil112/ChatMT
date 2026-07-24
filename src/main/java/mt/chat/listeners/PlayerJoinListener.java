package mt.chat.listeners;

import mt.chat.system.MonolithLoader;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
// Добавляем импорт сериализатора из Kyori
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class PlayerJoinListener implements Listener {

    private final MonolithLoader loader;

    public PlayerJoinListener(MonolithLoader loader) {
        this.loader = loader;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        loader.getPunishManager().loadPlayerPunishments(event.getUniqueId());
        loader.getIgnoreManager().loadIgnores(event.getUniqueId());

        if (loader.getPunishManager().isBanned(event.getUniqueId())) {

            String remainingTime = loader.getPunishManager().getBanRemainingTime(event.getUniqueId());

            String rawMessage = loader.getConfigManager().getMessages().getString("punishments.ban-screen", "&cВы заблокированы!\n&7Истекает через: &e%time%");

            String kickMessage = rawMessage.replace("%time%", remainingTime);
            kickMessage = ChatColor.translateAlternateColorCodes('&', kickMessage);

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickMessage);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.setJoinMessage(null);

        String joinMessageRaw = loader.getConfigManager().getMessages().getString("formats.join");
        if (joinMessageRaw != null && !joinMessageRaw.isEmpty()) {
            joinMessageRaw = joinMessageRaw.replace("%player_name%", event.getPlayer().getName());
            Component joinMsg = MiniMessage.miniMessage().deserialize(joinMessageRaw);

            // Сериализуем компонент в строку для Spigot
            String legacyJoin = LegacyComponentSerializer.legacySection().serialize(joinMsg);
            loader.getPlugin().getServer().broadcastMessage(legacyJoin);
        }

        String titleRaw = loader.getConfigManager().getMessages().getString("system.join-title");
        String subtitleRaw = loader.getConfigManager().getMessages().getString("system.join-subtitle");

        if (titleRaw != null && subtitleRaw != null) {
            Component titleComp = MiniMessage.miniMessage().deserialize(titleRaw);
            Component subtitleComp = MiniMessage.miniMessage().deserialize(subtitleRaw);

            // Сериализуем тайтлы в классическую строку
            String finalTitle = LegacyComponentSerializer.legacySection().serialize(titleComp);
            String finalSubtitle = LegacyComponentSerializer.legacySection().serialize(subtitleComp);

            // Spigot метод sendTitle (время указывается в игровых тиках: 20 тиков = 1 секунда)
            // 10 тиков = 0.5 сек | 60 тиков = 3 сек | 10 тиков = 0.5 сек
            event.getPlayer().sendTitle(finalTitle, finalSubtitle, 10, 60, 10);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);

        loader.getPunishManager().unloadPlayer(event.getPlayer().getUniqueId());
        loader.getIgnoreManager().unloadIgnores(event.getPlayer().getUniqueId());

        String quitMessageRaw = loader.getConfigManager().getMessages().getString("formats.quit");
        if (quitMessageRaw != null && !quitMessageRaw.isEmpty()) {
            quitMessageRaw = quitMessageRaw.replace("%player_name%", event.getPlayer().getName());
            Component quitMsg = MiniMessage.miniMessage().deserialize(quitMessageRaw);

            // Сериализуем компонент в строку для Spigot
            String legacyQuit = LegacyComponentSerializer.legacySection().serialize(quitMsg);
            loader.getPlugin().getServer().broadcastMessage(legacyQuit);
        }
    }
}