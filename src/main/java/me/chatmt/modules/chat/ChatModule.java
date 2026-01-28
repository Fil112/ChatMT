package me.chatmt.modules.chat;

import me.chatmt.ChatMT;
import me.chatmt.utils.ColorUtil;
import me.chatmt.utils.WorldGuardHook;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.*;

public class ChatModule implements Listener {

    private final ChatMT plugin;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public ChatModule(ChatMT plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();

        // 1. Проверка WorldGuard
        if (plugin.getConfig().getBoolean("worldguard.enabled")) {
            List<String> quietRegions = plugin.getConfig().getStringList("worldguard.quiet-regions");
            if (WorldGuardHook.isPlayerInRegion(player, quietRegions)) {
                event.setCancelled(true);
                plugin.getAdventure().player(player).sendMessage(ColorUtil.parse("<#FF5555>Вы находитесь в зоне тишины!"));
                return;
            }
        }

        // 2. Проверка мута
        if (plugin.getPunishModule() != null && plugin.getPunishModule().isMuted(player.getName())) {
            event.setCancelled(true);
            plugin.getAdventure().player(player).sendMessage(ColorUtil.parse(plugin.getMsgManager().getMessage("mute-message")));
            return;
        }

        // 3. Анти-спам
        if (plugin.getConfig().getBoolean("modules.anti-spam")) {
            int cooldownTime = plugin.getConfig().getInt("anti-spam.cooldown", 1);
            long lastMsg = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            if ((System.currentTimeMillis() - lastMsg) / 1000 < cooldownTime && !player.hasPermission("chatmt.bypass.spam")) {
                event.setCancelled(true);
                plugin.getAdventure().player(player).sendMessage(ColorUtil.parse(plugin.getMsgManager().getMessage("spam-cooldown")));
                return;
            }
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }

        String message = event.getMessage();

        // 4. Цензура
        if (plugin.getConfig().getBoolean("modules.censorship")) {
            for (String badWord : plugin.getConfig().getStringList("censorship.blocked-words")) {
                if (message.toLowerCase().contains(badWord.toLowerCase())) {
                    event.setCancelled(true);
                    plugin.getAdventure().player(player).sendMessage(ColorUtil.parse(plugin.getConfig().getString("censorship.cancel-message")));
                    return;
                }
            }
        }

        // 5. Анти-капс
        if (plugin.getConfig().getBoolean("modules.anti-caps") && message.length() > 5 && !player.hasPermission("chatmt.bypass.caps")) {
            long capsCount = message.chars().filter(Character::isUpperCase).count();
            if ((double) capsCount / message.length() > 0.7) {
                message = message.toLowerCase();
            }
        }

        // 6. Replacer
        if (plugin.getConfig().getBoolean("modules.replacer")) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("replacer");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    message = message.replace(key, section.getString(key));
                }
            }
        }

        // 7. Форматирование
        if (plugin.getConfig().getBoolean("modules.chat-format")) {
            event.setCancelled(true); // Отменяем стандартный чат

            String format = plugin.getConfig().getString("chats.local.format", "%player%: %message%");

            // Vault
            Chat vaultChat = ChatMT.getVaultChat();
            String prefix = (vaultChat != null) ? vaultChat.getPlayerPrefix(player) : "";
            String suffix = (vaultChat != null) ? vaultChat.getPlayerSuffix(player) : "";

            // PAPI
            if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                format = PlaceholderAPI.setPlaceholders(player, format);
            }

            // Замена
            format = format.replace("%player%", player.getName())
                    .replace("%luckperms_prefix%", prefix)
                    .replace("%luckperms_suffix%", suffix);

            // Обработка сообщения (если есть права на цвет)
            if (!player.hasPermission("chatmt.color")) {
                message = message.replace("&", "").replace("<", "");
            }

            // Финальный сборка через Component
            Component finalComponent = ColorUtil.parse(format.replace("%message%", message));

            // Отправка через Adventure
            plugin.getAdventure().all().sendMessage(finalComponent);
        }
    }
}