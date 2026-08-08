package mt.chat.listeners;

import mt.chat.system.MonolithLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final MonolithLoader loader;

    public ChatListener(MonolithLoader loader) {
        this.loader = loader;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();

        // 0. Проверка на мут (используем конфиги и MiniMessage)
        if (loader.getPunishManager().isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            String muteMsg = loader.getConfigManager().getMessages().getString(
                    "punishments.muted",
                    "<red>У вас мут чата! Осталось: <yellow>%time%"
            );
            String timeLeft = loader.getPunishManager().getMuteRemainingTime(player.getUniqueId());

            // Конвертируем для Spigot
            Component comp = MiniMessage.miniMessage().deserialize(muteMsg.replace("%time%", timeLeft));
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(comp));
            return;
        }

        // 1. Проверка на обращение к ИИ (Gemini)
        boolean aiEnabled = loader.getConfigManager().getConfig().getBoolean("ai.gemini.enabled", true);
        String trigger = loader.getConfigManager().getConfig().getString("ai.gemini.trigger", "Бот,");

        if (aiEnabled && message.toLowerCase().startsWith(trigger.toLowerCase())) {
            event.setCancelled(true);
            if (loader.getChatFilters().isSpamming(player)) return; // Защита ИИ от спама

            String prompt = message.substring(trigger.length()).trim();
            if (!prompt.isEmpty()) {
                player.sendMessage("§7§o[Gemini] Думаю над ответом...");
                loader.getGeminiManager().askGemini(prompt, response -> {
                    Bukkit.getScheduler().runTask(loader.getPlugin(), () -> {
                        Bukkit.broadcastMessage("§8[§bGemini§8] §f" + response);
                    });
                });
            }
            return;
        }

        // 2. Анти-Спам
        if (loader.getChatFilters().isSpamming(player)) {
            event.setCancelled(true);
            return;
        }

        // 3. Анти-Реклама
        if (loader.getAntiAdvertising().hasAds(player, message)) {
            event.setCancelled(true);
            return;
        }

        // 4. Анти-Мат (цензурит текст)
        message = loader.getAntiSwear().filterSwear(player, message);

        // 5. Анти-Капс (понижает регистр, если нужно)
        String safeMessage = loader.getChatFilters().applyAntiCaps(player, message);
        event.setMessage(safeMessage);

        // 6. Отправляем в ядро чата для парсинга градиентов и радиуса
        loader.getChatEngine().processChat(event);
    }
}