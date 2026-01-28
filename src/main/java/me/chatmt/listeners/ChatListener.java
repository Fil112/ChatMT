package me.chatmt.listeners;

import me.chatmt.ChatMT;
import me.chatmt.modules.chat.AntiSpamModule;
import me.chatmt.modules.chat.ReplacerModule;
import me.chatmt.utils.ColorUtil;
import me.chatmt.utils.WorldGuardHook;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatListener implements Listener {

    private final ChatMT plugin;
    private final AntiSpamModule antiSpam;
    private final ReplacerModule replacer;

    public ChatListener(ChatMT plugin) {
        this.plugin = plugin;
        this.antiSpam = new AntiSpamModule(plugin);
        this.replacer = new ReplacerModule(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();

        // 1. Проверки
        if (isRestricted(player, event)) return;

        String rawMessage = event.getMessage();

        // 2. Обработка сообщения
        rawMessage = replacer.replace(rawMessage);
        if (plugin.getConfig().getBoolean("modules.anti-caps")) {
            rawMessage = applyAntiCaps(rawMessage, player);
        }

        // 3. Форматирование
        String format = plugin.getConfig().getString("chats.local.format", "%player%: %message%");
        String step1 = format.replace("%player%", player.getName())
                .replace("%message%", rawMessage);

        // 4. PAPI
        String step2 = step1;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            step2 = PlaceholderAPI.setPlaceholders(player, step1);
        }

        // 5. Финальная покраска
        Component finalComponent = ColorUtil.parse(step2);

        event.setCancelled(true);
        plugin.getAdventure().all().sendMessage(finalComponent);
    }

    private boolean isRestricted(Player player, AsyncPlayerChatEvent event) {
        if (plugin.getConfig().getBoolean("worldguard.enabled")) {
            List<String> quietRegions = plugin.getConfig().getStringList("worldguard.quiet-regions");
            if (WorldGuardHook.isPlayerInRegion(player, quietRegions)) {
                event.setCancelled(true);
                plugin.getAdventure().player(player).sendMessage(ColorUtil.parse("<#FF5555>Здесь нельзя писать в чат!"));
                return true;
            }
        }

        if (plugin.getPunishModule().isMuted(player.getName())) {
            event.setCancelled(true);
            String msg = plugin.getMsgManager().getMessage("mute-message");
            plugin.getAdventure().player(player).sendMessage(ColorUtil.parse(msg));
            return true;
        }

        if (antiSpam.isSpamming(player)) {
            event.setCancelled(true);
            String msg = plugin.getMsgManager().getMessage("spam-cooldown");
            plugin.getAdventure().player(player).sendMessage(ColorUtil.parse(msg));
            return true;
        }
        return false;
    }

    private String applyAntiCaps(String msg, Player p) {
        if (p.hasPermission("chatmt.bypass.caps") || msg.length() < 5) return msg;
        long caps = msg.chars().filter(Character::isUpperCase).count();
        return ((double) caps / msg.length() > 0.7) ? msg.toLowerCase() : msg;
    }
}