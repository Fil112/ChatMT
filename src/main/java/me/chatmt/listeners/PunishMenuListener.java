package me.chatmt.listeners;

import me.chatmt.ChatMT;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public class PunishMenuListener implements Listener {

    private final ChatMT plugin = ChatMT.getInstance();

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Наказание:") && !title.contains("Время мута:")) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;

        Player staff = (Player) event.getWhoClicked();
        // Парсим имя цели из заголовка окна
        String target = title.split(": ")[1];

        if (title.contains("Наказание:")) {
            // Главное меню
            if (event.getSlot() == 2) { // Мут -> Открыть выбор времени
                plugin.getPunishModule().openTimeMenu(staff, target);
            }
            else if (event.getSlot() == 4) { // Кик
                staff.closeInventory();
                staff.performCommand("mt kick " + target + " Нарушение правил");
            }
            else if (event.getSlot() == 6) { // Бан
                staff.closeInventory();
                staff.performCommand("mt ban " + target + " Нарушение правил");
            }
        }
        else if (title.contains("Время мута:")) {
            // Меню времени
            // Хитрый ход: мы берем "код времени" из лора, но для простоты здесь я сделаю жесткую привязку к слотам,
            // так как в PunishModule мы ставим 15m, 1h, 1d в лор, но для команды это нужно обработать.

            // Здесь я упрощу и буду просто выполнять команду с аргументом времени
            String timeArg = "15m";
            if (event.getSlot() == 4) timeArg = "1h";
            if (event.getSlot() == 6) timeArg = "1d";

            staff.closeInventory();
            // Выполняем команду, чтобы она прошла через MainCommand (где можно добавить логику парсинга времени)
            // Но пока MainCommand просто принимает текст, поэтому пишем так:
            staff.performCommand("mt mute " + target + " " + timeArg + " (Нарушение правил)");
        }
    }
}