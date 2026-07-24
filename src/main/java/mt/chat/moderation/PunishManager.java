package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PunishManager {

    private final MonolithLoader loader;

    // Потокобезопасные коллекции для кэширования данных из БД
    private final Map<UUID, Long> mutedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> bannedPlayers = new ConcurrentHashMap<>();

    public PunishManager(MonolithLoader loader) {
        this.loader = loader;
    }

    // =====================================
    // 1. МУТЫ (MUTES)
    // =====================================

    public void mutePlayer(UUID uuid, long durationMillis, String adminName, String reason) {
        long expires = (durationMillis == -1) ? -1L : System.currentTimeMillis() + durationMillis;
        mutedPlayers.put(uuid, expires);

        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            String sql = "REPLACE INTO chatmt_mutes (uuid, admin_name, reason, expires) VALUES (?, ?, ?, ?)";
            try (Connection conn = loader.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, adminName);
                ps.setString(3, reason);
                ps.setLong(4, expires);
                ps.executeUpdate();
            } catch (SQLException e) {
                loader.getLoggerMT().error("Ошибка при сохранении мута в БД: " + e.getMessage());
            }
        });
    }

    // Легаси-метод для обратной совместимости с внешним API и старыми модулями
    public void mutePlayer(UUID uuid, long durationMillis) {
        String defaultReason = loader.getConfigManager().getMessages().getString("punishments.default-reason", "Нарушение правил");
        String consoleName = loader.getConfigManager().getMessages().getString("system.console-name", "Console");
        mutePlayer(uuid, durationMillis, consoleName, defaultReason);
    }

    public void unmutePlayer(UUID uuid) {
        mutedPlayers.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            String sql = "DELETE FROM chatmt_mutes WHERE uuid = ?";
            try (Connection conn = loader.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                loader.getLoggerMT().error("Ошибка при удалении мута из БД: " + e.getMessage());
            }
        });
    }

    public boolean isMuted(UUID uuid) {
        if (!mutedPlayers.containsKey(uuid)) return false;

        long unMuteTime = mutedPlayers.get(uuid);
        if (unMuteTime == -1) return true; // Перманент

        if (System.currentTimeMillis() >= unMuteTime) {
            unmutePlayer(uuid);
            return false;
        }
        return true;
    }

    // =====================================
    // 2. БАНЫ (BANS)
    // =====================================

    public void banPlayer(UUID uuid, long durationMillis, String adminName, String reason) {
        long expires = (durationMillis == -1) ? -1L : System.currentTimeMillis() + durationMillis;
        bannedPlayers.put(uuid, expires);

        // Если игрок онлайн, кикаем его моментально в основном потоке
        Player target = Bukkit.getPlayer(uuid);
        if (target != null && target.isOnline()) {
            Bukkit.getScheduler().runTask(loader.getPlugin(), () -> {
                String rawMessage = loader.getConfigManager().getMessages().getString("punishments.ban-screen", "&cВы заблокированы!\n&7Истекает через: &e%time%");
                String kickMessage = rawMessage.replace("%time%", getBanRemainingTime(uuid));
                target.kickPlayer(ChatColor.translateAlternateColorCodes('&', kickMessage));
            });
        }

        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            String sql = "REPLACE INTO chatmt_bans (uuid, admin_name, reason, expires) VALUES (?, ?, ?, ?)";
            try (Connection conn = loader.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.setString(2, adminName);
                ps.setString(3, reason);
                ps.setLong(4, expires);
                ps.executeUpdate();
            } catch (SQLException e) {
                loader.getLoggerMT().error("Ошибка при сохранении бана в БД: " + e.getMessage());
            }
        });
    }

    public void unbanPlayer(UUID uuid) {
        bannedPlayers.remove(uuid);

        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            String sql = "DELETE FROM chatmt_bans WHERE uuid = ?";
            try (Connection conn = loader.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, uuid.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                loader.getLoggerMT().error("Ошибка при удалении бана из БД: " + e.getMessage());
            }
        });
    }

    public boolean isBanned(UUID uuid) {
        if (!bannedPlayers.containsKey(uuid)) return false;

        long unBanTime = bannedPlayers.get(uuid);
        if (unBanTime == -1) return true;

        if (System.currentTimeMillis() >= unBanTime) {
            unbanPlayer(uuid);
            return false;
        }
        return true;
    }

    // =====================================
    // 3. УТИЛИТЫ И ЗАГРУЗКА
    // =====================================

    /**
     * Подгрузка наказаний из БД.
     * Вызывать строго асинхронно, например в AsyncPlayerPreLoginEvent!
     */
    public void loadPlayerPunishments(UUID uuid) {
        String muteSql = "SELECT expires FROM chatmt_mutes WHERE uuid = ?";
        String banSql = "SELECT expires FROM chatmt_bans WHERE uuid = ?";

        try (Connection conn = loader.getDatabaseManager().getConnection()) {

            try (PreparedStatement ps = conn.prepareStatement(muteSql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long expires = rs.getLong("expires");
                    if (expires == -1 || expires > System.currentTimeMillis()) {
                        mutedPlayers.put(uuid, expires);
                    } else {
                        unmutePlayer(uuid);
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(banSql)) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long expires = rs.getLong("expires");
                    if (expires == -1 || expires > System.currentTimeMillis()) {
                        bannedPlayers.put(uuid, expires);
                    } else {
                        unbanPlayer(uuid);
                    }
                }
            }
        } catch (SQLException e) {
            loader.getLoggerMT().error("Не удалось загрузить наказания для " + uuid.toString());
        }
    }

    /**
     * Очистка кэша наказаний при выходе игрока (PlayerQuitEvent)
     */
    public void unloadPlayer(UUID uuid) {
        mutedPlayers.remove(uuid);
        bannedPlayers.remove(uuid);
    }

    public String getMuteRemainingTime(UUID uuid) {
        if (!mutedPlayers.containsKey(uuid)) return "";
        return formatTime(mutedPlayers.get(uuid));
    }

    public String getBanRemainingTime(UUID uuid) {
        if (!bannedPlayers.containsKey(uuid)) return "";
        return formatTime(bannedPlayers.get(uuid));
    }

    private String formatTime(long expireTime) {
        if (expireTime == -1) {
            return loader.getConfigManager().getMessages().getString("time.permanent", "Навсегда");
        }

        long remainingMillis = expireTime - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return loader.getConfigManager().getMessages().getString("time.expired", "Истекло");
        }

        long seconds = (remainingMillis / 1000) % 60;
        long minutes = (remainingMillis / (1000 * 60)) % 60;
        long hours = (remainingMillis / (1000 * 60 * 60)) % 24;
        long days = (remainingMillis / (1000 * 60 * 60 * 24));

        if (days > 0) {
            String formatDays = loader.getConfigManager().getMessages().getString("time.format-days", "%d дн. %02d ч. %02d мин.");
            return String.format(formatDays, days, hours, minutes);
        }

        String formatHours = loader.getConfigManager().getMessages().getString("time.format-hours", "%02d ч. %02d мин. %02d сек.");
        return String.format(formatHours, hours, minutes, seconds);
    }
}