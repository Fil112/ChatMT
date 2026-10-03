package mt.chat.engine;

import mt.chat.system.MonolithLoader;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChannelManager {

    private final MonolithLoader loader;
    private final List<ChatChannel> channels = new ArrayList<ChatChannel>();

    public ChannelManager(MonolithLoader loader) {
        this.loader = loader;
        loadChannels();
    }

    public void loadChannels() {
        channels.clear();

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("channels.enabled", true);
        if (!enabled) return;

        ConfigurationSection section = loader.getConfigManager().getConfig().getConfigurationSection("channels.list");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection chSec = section.getConfigurationSection(key);
            if (chSec == null) continue;

            String prefix = chSec.getString("prefix", "#" + key);
            List<String> aliases = chSec.getStringList("aliases");
            if (aliases == null) {
                aliases = Collections.emptyList();
            }
            String format = chSec.getString("format", "<dark_gray>[<yellow>" + key + "<dark_gray>] <gray>%player_name% <dark_gray>» <white><message>");
            String permission = chSec.getString("permission", "");
            int radius = chSec.getInt("radius", -1);

            channels.add(new ChatChannel(key, prefix, aliases, format, permission, radius));
        }

        loader.getPlugin().getLogger().info(" -> Загружено каналов чата: " + channels.size());
    }

    /**
     * Находит подходящий канал по тексту сообщения
     */
    public ChatChannel findChannel(String message) {
        for (ChatChannel channel : channels) {
            if (channel.matches(message)) {
                return channel;
            }
        }
        return null;
    }

    public List<ChatChannel> getChannels() {
        return channels;
    }
}