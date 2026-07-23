package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PunishManager {

    private final MonolithLoader loader;

    // Храним UUID игрока и время окончания мута в миллисекундах (или -1 для пермача)
    private final Map<UUID, Long> mutedPlayers = new HashMap<>();

    public PunishManager(MonolithLoader loader) {
        this.loader = loader;
    }

    public void mutePlayer(UUID uuid, long durationMillis) {
        if (durationMillis == -1) {
            mutedPlayers.put(uuid, -1L); // Навсегда
        } else {
            mutedPlayers.put(uuid, System.currentTimeMillis() + durationMillis);
        }
    }

    public void unmutePlayer(UUID uuid) {
        mutedPlayers.remove(uuid);
    }

    public boolean isMuted(UUID uuid) {
        if (!mutedPlayers.containsKey(uuid)) return false;

        long unMuteTime = mutedPlayers.get(uuid);

        if (unMuteTime == -1) return true; // Перманентный мут

        // Если время мута вышло, снимаем его
        if (System.currentTimeMillis() >= unMuteTime) {
            mutedPlayers.remove(uuid);
            return false;
        }

        return true;
    }

    public String getRemainingTime(UUID uuid) {
        if (!mutedPlayers.containsKey(uuid)) return "";

        long unMuteTime = mutedPlayers.get(uuid);
        if (unMuteTime == -1) return "Навсегда";

        long remainingMillis = unMuteTime - System.currentTimeMillis();
        long seconds = (remainingMillis / 1000) % 60;
        long minutes = (remainingMillis / (1000 * 60)) % 60;
        long hours = (remainingMillis / (1000 * 60 * 60)) % 24;

        return String.format("%02d ч. %02d мин. %02d сек.", hours, minutes, seconds);
    }
}