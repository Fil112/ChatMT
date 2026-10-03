package mt.chat.listeners;

import mt.chat.ai.AiManager;
import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
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

        // 0. Проверка на активный мут игрока
        if (loader.getPunishManager().isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            String muteMsg = loader.getConfigManager().getMessages().getString(
                    "punishments.muted",
                    "<red>У вас мут чата! Осталось: <yellow>%time%"
            );
            String timeLeft = loader.getPunishManager().getMuteRemainingTime(player.getUniqueId());

            Component comp = MiniMessage.miniMessage().deserialize(muteMsg.replace("%time%", timeLeft));
            player.sendMessage(LegacyComponentSerializer.legacySection().serialize(comp));
            return;
        }

        // 1. Проверка на вызов ИИ-ассистента
        AiManager ai = loader.getAiManager();
        if (ai != null && ai.isEnabled()) {
            String trigger = ai.getTrigger();
            if (message.toLowerCase().startsWith(trigger.toLowerCase())) {
                event.setCancelled(true);

                // Защищаем API от флуда
                if (loader.getChatFilters().isSpamming(player)) return;

                String prompt = message.substring(trigger.length()).trim();
                if (!prompt.isEmpty()) {
                    String thinkingMsg = loader.getConfigManager().getMessages().getString(
                            "ai.thinking",
                            "<gray><i>[ИИ] Думаю над ответом...</i>"
                    );
                    player.sendMessage(ColorUtils.colorize(thinkingMsg));

                    ai.askAi(prompt, response -> {
                        Bukkit.getScheduler().runTask(loader.getPlugin(), () -> {
                            String format = loader.getConfigManager().getMessages().getString(
                                    "ai.format",
                                    "<dark_gray>[<gradient:#00f2fe:#4facfe>ИИ</gradient><dark_gray>] <white>%response%"
                            );
                            Bukkit.broadcastMessage(ColorUtils.colorize(format.replace("%response%", response)));
                        });
                    });
                }
                return;
            }
        }

        // 2. Анти-Спам кулдаун
        if (loader.getChatFilters().isSpamming(player)) {
            event.setCancelled(true);
            return;
        }

        // 3. Фильтр ссылок и рекламы
        if (loader.getAntiAdvertising().hasAds(player, message)) {
            event.setCancelled(true);
            return;
        }

        // 4. Антимат (чистим обходы и цензурим)
        message = loader.getAntiSwear().filterSwear(player, message);

        // 5. Антикапс
        String safeMessage = loader.getChatFilters().applyAntiCaps(player, message);
        event.setMessage(safeMessage);

        // 6. Передаем в ChatEngine (каналы, локал/глобал, ховеры)
        loader.getChatEngine().processChat(event);
    }
}