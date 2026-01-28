package me.chatmt.modules.punish;

import me.chatmt.ChatMT;
import me.chatmt.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class PunishModule {

    private final ChatMT plugin;

    public PunishModule(ChatMT plugin) {
        this.plugin = plugin;
    }

    public void addHistory(String player, String type, String admin, String reason) {
        List<String> logs = plugin.getDataConfig().getStringList("history." + player);
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM HH:mm"));
        logs.add(date + " | " + type + " | Admin: " + admin + " | Reason: " + reason);
        plugin.getDataConfig().set("history." + player, logs);
        plugin.saveData();
    }

    public void ban(String target, String reason, String admin) {
        plugin.getDataConfig().set("bans." + target + ".reason", reason);
        plugin.saveData();
        addHistory(target, "BAN", admin, reason);

        Player p = Bukkit.getPlayer(target);
        if (p != null) p.kickPlayer(ColorUtil.parseToLegacy("<red>Вы забанены!<br><gray>Причина: " + reason));
    }

    public void mute(String target, String reason, String admin) {
        plugin.getDataConfig().set("mutes." + target + ".reason", reason);
        plugin.saveData();
        addHistory(target, "MUTE", admin, reason);
    }

    public void kick(String target, String reason, String admin) {
        Player p = Bukkit.getPlayer(target);
        if (p != null) {
            p.kickPlayer(ColorUtil.parseToLegacy("<red>Вы кикнуты!<br><gray>Админ: " + admin + "<br>Причина: " + reason));
            addHistory(target, "KICK", admin, reason);
        }
    }

    public void unmute(String target) {
        plugin.getDataConfig().set("mutes." + target, null);
        plugin.saveData();
    }

    public void unban(String target) {
        plugin.getDataConfig().set("bans." + target, null);
        plugin.saveData();
    }

    public boolean isBanned(String name) { return plugin.getDataConfig().contains("bans." + name); }
    public boolean isMuted(String name) { return plugin.getDataConfig().contains("mutes." + name); }
    public String getBanReason(String name) { return plugin.getDataConfig().getString("bans." + name + ".reason"); }

    // GUI - Заголовки должны быть String (Legacy)
    public void openPunishMenu(Player staff, String target) {
        Inventory gui = Bukkit.createInventory(null, 9, ColorUtil.parseToLegacy("<#FF5555>Наказание: " + target));

        gui.setItem(2, createItem(Material.ORANGE_WOOL, "<#FFBB00>ВЫДАТЬ МУТ", "<gray>Выбор времени..."));
        gui.setItem(4, createItem(Material.YELLOW_WOOL, "<#FFFF55>КИКНУТЬ", "<gray>Кикнуть игрока с сервера."));
        gui.setItem(6, createItem(Material.RED_WOOL, "<#FF5555>ЗАБАНИТЬ", "<gray>Забанить навсегда."));

        staff.openInventory(gui);
    }

    public void openTimeMenu(Player staff, String target) {
        Inventory gui = Bukkit.createInventory(null, 9, ColorUtil.parseToLegacy("<#FFBB00>Время мута: " + target));

        gui.setItem(2, createItem(Material.CLOCK, "<#55FFBB>15 МИНУТ", "15m"));
        gui.setItem(4, createItem(Material.CLOCK, "<#55FFBB>1 ЧАС", "1h"));
        gui.setItem(6, createItem(Material.CLOCK, "<#55FFBB>1 ДЕНЬ", "1d"));

        staff.openInventory(gui);
    }

    private ItemStack createItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.parseToLegacy(name));
            meta.setLore(Arrays.asList(ColorUtil.parseToLegacy(lore)));
            item.setItemMeta(meta);
        }
        return item;
    }
}