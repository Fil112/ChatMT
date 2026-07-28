package mt.chat.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import mt.chat.system.MonolithLoader;
import org.bukkit.Bukkit;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {

    private final MonolithLoader loader;
    private HikariDataSource dataSource;

    public DatabaseManager(MonolithLoader loader) {
        this.loader = loader;
    }

    // Метод инициализации базы при запуске плагина
    public void connect() {
        String type = loader.getConfigManager().getConfig().getString("database.type", "sqlite").toLowerCase();
        HikariConfig config = new HikariConfig();

        // Общие настройки пула для оптимизации производительности
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(10000); // 10 секунд на попытку подключения

        if (type.equals("mysql")) {
            // Подключаемся к удаленной MySQL базе
            String host = loader.getConfigManager().getConfig().getString("database.mysql.host");
            int port = loader.getConfigManager().getConfig().getInt("database.mysql.port");
            String dbName = loader.getConfigManager().getConfig().getString("database.mysql.database");
            String user = loader.getConfigManager().getConfig().getString("database.mysql.username");
            String pass = loader.getConfigManager().getConfig().getString("database.mysql.password");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false&autoReconnect=true");
            config.setUsername(user);
            config.setPassword(pass);
        } else {
            // Подключаемся к локальной SQLite
            File dbFile = new File(loader.getPlugin().getDataFolder(), "database.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC"); // Явно указываем драйвер для SQLite
        }

        try {
            dataSource = new HikariDataSource(config);
            loader.getPlugin().getLogger().info("Успешное подключение к базе данных (" + type.toUpperCase() + ")!");

            // Создаем таблицы, если их еще нет
            createTables();
        } catch (Exception e) {
            loader.getPlugin().getLogger().severe("Ошибка при подключении к базе данных!");
            e.printStackTrace();
        }
    }

    // Создание нужных таблиц при первом запуске
    private void createTables() {
        // Таблица мутов
        String mutesTable = "CREATE TABLE IF NOT EXISTS chatmt_mutes (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "admin_name VARCHAR(16) NOT NULL, " +
                "reason TEXT NOT NULL, " +
                "expires BIGINT NOT NULL" +
                ");";

        // Таблица игноров (система /ignore)
        String ignoresTable = "CREATE TABLE IF NOT EXISTS chatmt_ignores (" +
                "user_uuid VARCHAR(36) NOT NULL, " +
                "ignored_uuid VARCHAR(36) NOT NULL, " +
                "PRIMARY KEY (user_uuid, ignored_uuid)" +
                ");";

        // Таблица банов
        String bansTable = "CREATE TABLE IF NOT EXISTS chatmt_bans (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "admin_name VARCHAR(16) NOT NULL, " +
                "reason TEXT NOT NULL, " +
                "expires BIGINT NOT NULL" +
                ");";

        try (Connection connection = getConnection();
             PreparedStatement st1 = connection.prepareStatement(mutesTable);
             PreparedStatement st2 = connection.prepareStatement(ignoresTable);
             PreparedStatement st3 = connection.prepareStatement(bansTable)) {

            st1.execute();
            st2.execute();
            st3.execute(); // Создаем таблицу банов

        } catch (SQLException e) {
            loader.getPlugin().getLogger().severe("Не удалось создать таблицы в базе данных!");
            e.printStackTrace();
        }
    }

    // Этот метод будем дергать из других классов, чтобы получить коннект
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("База данных не инициализирована!");
        }
        return dataSource.getConnection();
    }

    /**
     * Удаление бана игрока из базы данных (Асинхронно)
     */
    public void removeBan(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            String query = "DELETE FROM chatmt_bans WHERE uuid = ?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(query)) {

                ps.setString(1, uuid.toString());
                ps.executeUpdate();

            } catch (SQLException e) {
                loader.getPlugin().getLogger().severe("Ошибка при снятии бана из БД: " + e.getMessage());
            }
        });
    }

    // Корректное закрытие пула соединений при выключении плагина
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            loader.getPlugin().getLogger().info("Соединение с базой данных закрыто.");
        }
    }
}