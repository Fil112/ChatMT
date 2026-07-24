package mt.chat.utils;

import mt.chat.system.MonolithLoader;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggerMT {

    private final MonolithLoader loader;
    private final File logsFolder;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public LoggerMT(MonolithLoader loader) {
        this.loader = loader;
        // Создаем папку logs внутри папки плагина, если её нет
        this.logsFolder = new File(loader.getPlugin().getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    // ===============================================
    // МЕТОДЫ ЛОГИРОВАНИЯ ОШИБОК И СИСТЕМНЫХ СОБЫТИЙ
    // ===============================================

    // Метод, который мы использовали в менеджерах БД для отлова SQLException
    public void error(String message) {
        // Дублируем ошибку в консоль сервера (красным цветом)
        loader.getPlugin().getLogger().severe(message);

        // И сохраняем в отдельный лог для удобного дебага
        String logLine = "[" + dateFormat.format(new Date()) + "] [ERROR]: " + message;
        writeToFile("errors.log", logLine);
    }

    public void info(String message) {
        loader.getPlugin().getLogger().info(message);
        String logLine = "[" + dateFormat.format(new Date()) + "] [INFO]: " + message;
        writeToFile("system.log", logLine);
    }

    // ===============================================
    // МЕТОДЫ ЛОГИРОВАНИЯ ИГРОКОВ (ЧАТ, ЛС, НАКАЗАНИЯ)
    // ===============================================

    // Метод для записи сообщений чата
    public void logChat(String playerName, String message, boolean isGlobal) {
        String channel = isGlobal ? "[Global]" : "[Local]";
        String logLine = "[" + dateFormat.format(new Date()) + "] " + channel + " " + playerName + ": " + message;
        writeToFile("chat.log", logLine);
    }

    // Метод для записи личных сообщений (ЛС)
    public void logPrivateMessage(String sender, String target, String message) {
        String logLine = "[" + dateFormat.format(new Date()) + "] [PM] " + sender + " -> " + target + ": " + message;
        // Пишем ЛС в отдельный файл, чтобы не засорять глобальный чат-лог
        writeToFile("private_messages.log", logLine);
    }

    // Метод для записи наказаний (кто кого и за что)
    public void logPunish(String admin, String target, String type, String reason) {
        String logLine = "[" + dateFormat.format(new Date()) + "] " + admin + " выдал " + type + " игроку " + target + ". Причина: " + reason;
        writeToFile("punishments.log", logLine);
    }

    // ===============================================
    // УТИЛИТЫ ДЛЯ ФАЙЛОВ
    // ===============================================

    // Асинхронная запись в файл, чтобы не вешать основной поток сервера при медленных жестких дисках
    private void writeToFile(String fileName, String text) {
        Bukkit.getScheduler().runTaskAsynchronously(loader.getPlugin(), () -> {
            File file = new File(logsFolder, fileName);
            try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
                out.println(text);
            } catch (IOException e) {
                loader.getPlugin().getLogger().warning("Ошибка записи в лог " + fileName + ": " + e.getMessage());
            }
        });
    }
}