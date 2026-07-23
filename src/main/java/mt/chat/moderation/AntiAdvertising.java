package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntiAdvertising {

    private final MonolithLoader loader;
    // Регулярка для отлова ссылок и IP-адресов. Ловит всё: от site.com до 192.168.0.1
    private final Pattern urlPattern = Pattern.compile("(?i)\\b((?:[a-z0-9-]+\\.)+[a-z]{2,}|(?:\\d{1,3}\\.){3}\\d{1,3})\\b");

    public AntiAdvertising(MonolithLoader loader) {
        this.loader = loader;
    }

    public boolean hasAds(Player player, String message) {
        // У админов и ютуберов иммунитет
        if (player.hasPermission("chatmt.bypass.ads")) return false;

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("filters.advertising.enabled", true);
        if (!enabled) return false;

        // Схлопываем сообщение: убираем пробелы и меняем запятые на точки,
        // чтобы пробить хитрые попытки обхода фильтра
        String noSpaces = message.replace(" ", "").replace(",", ".");
        Matcher matcher = urlPattern.matcher(noSpaces);

        if (matcher.find()) {
            String foundDomain = matcher.group(1).toLowerCase();
            List<String> whitelist = loader.getConfigManager().getConfig().getStringList("filters.advertising.whitelist");

            // Проверяем, есть ли найденная ссылка в белом списке
            for (String allowed : whitelist) {
                if (foundDomain.contains(allowed.toLowerCase())) {
                    return false; // Это своя ссылка, пропускаем
                }
            }

            // Ссылки нет в вайтлисте — рубим на корню
            String blockMsg = loader.getConfigManager().getMessages().getString("system.ad-blocked", "<red>Реклама на сервере запрещена!");
            player.sendMessage(ColorUtils.colorize(blockMsg));
            return true;
        }

        return false;
    }
}