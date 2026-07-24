package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IgnoreManager {

    private final MonolithLoader loader;

    // Структура: UUID Игрока -> Список UUID тех, кого он заблокировал
    // Используем ConcurrentHashMap.newKeySet() для потокобезопасного списка
    private final ConcurrentHashMap<UUID, Set<UUID>> ignoreCache = new ConcurrentHashMap<>();

    public IgnoreManager(MonolithLoader loader) {
        this.loader = loader;
    }

    /**
     * Добавляет игрока в черный список.
     */
    public void addIgnore(UUID user, UUID target) {
        // Добавляем в кэш
        ignoreCache.computeIfAbsent(user, k -> ConcurrentHashMap.newKeySet()).add(target);

        // Сохраняем в БД асинхронно
        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            String sql = "REPLACE INTO chatmt_ignores (user_uuid, ignored_uuid) VALUES (?, ?)";
            try (Connection conn = loader.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.toString());
                ps.setString(2, target.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                loader.getLoggerMT().error("Ошибка при сохранении игнора в БД: " + e.getMessage());
            }
        });
    }

    /**
     * Удаляет игрока из черного списка.
     */
    public void removeIgnore(UUID user, UUID target) {
        Set<UUID> ignored = ignoreCache.get(user);
        if (ignored != null) {
            ignored.remove(target);
        }

        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            String sql = "DELETE FROM chatmt_ignores WHERE user_uuid = ? AND ignored_uuid = ?";
            try (Connection conn = loader.getDatabaseManager().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.toString());
                ps.setString(2, target.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                loader.getLoggerMT().error("Ошибка при удалении игнора из БД: " + e.getMessage());
            }
        });
    }

    /**
     * Проверяет, заблокировал ли user игрока target.
     * Используется при отправке сообщений в чат или ЛС.
     */
    public boolean isIgnored(UUID user, UUID target) {
        Set<UUID> ignored = ignoreCache.get(user);
        return ignored != null && ignored.contains(target);
    }

    /**
     * Загрузка списка игноров из БД.
     * Вызывать асинхронно при входе игрока (в AsyncPlayerPreLoginEvent).
     */
    public void loadIgnores(UUID playerUuid) {
        String sql = "SELECT ignored_uuid FROM chatmt_ignores WHERE user_uuid = ?";
        Set<UUID> ignored = ConcurrentHashMap.newKeySet();

        try (Connection conn = loader.getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ignored.add(UUID.fromString(rs.getString("ignored_uuid")));
            }
            // Кладем загруженный список в кэш
            ignoreCache.put(playerUuid, ignored);
        } catch (SQLException e) {
            loader.getLoggerMT().error("Не удалось загрузить игноры для " + playerUuid.toString());
        }
    }

    /**
     * Очистка памяти при выходе игрока.
     */
    public void unloadIgnores(UUID playerUuid) {
        ignoreCache.remove(playerUuid);
    }
}