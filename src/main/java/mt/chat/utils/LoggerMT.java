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
        // Создаем папку logs внутри папки плагина
        this.logsFolder = new File(loader.getPlugin().getDataFolder(), "logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }
    }

    // Метод для записи сообщений чата
    public void logChat(String playerName, String message, boolean isGlobal) {
        String channel = isGlobal ? "[Global]" : "[Local]";
        String logLine = "[" + dateFormat.format(new Date()) + "] " + channel + " " + playerName + ": " + message;
        writeToFile("chat.log", logLine);
    }

    // Метод для записи наказаний (кто кого и за что)
    public void logPunish(String admin, String target, String type, String reason) {
        String logLine = "[" + dateFormat.format(new Date()) + "] " + admin + " выдал " + type + " игроку " + target + ". Причина: " + reason;
        writeToFile("punishments.log", logLine);
    }

    // Асинхронная запись в файл, чтобы не вешать основной поток сервера
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