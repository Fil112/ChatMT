package mt.chat.engine;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ChatGamesManager implements Listener {

    private final MonolithLoader loader;

    private BukkitTask gameTimerTask;
    private BukkitTask timeoutTask;

    // Состояние текущей викторины
    private volatile boolean isGameActive = false;
    private volatile String currentAnswer = "";
    private long gameStartTime = 0L;

    public ChatGamesManager(MonolithLoader loader) {
        this.loader = loader;
        Bukkit.getPluginManager().registerEvents(this, loader.getPlugin());
    }

    public void start() {
        stop();

        boolean enabled = loader.getConfigManager().getConfig().getBoolean("games.enabled", true);
        if (!enabled) return;

        long intervalSeconds = loader.getConfigManager().getConfig().getLong("games.interval", 300);
        long intervalTicks = intervalSeconds * 20L;

        gameTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                startRandomGame();
            }
        }.runTaskTimer(loader.getPlugin(), intervalTicks, intervalTicks);
    }

    public void stop() {
        if (gameTimerTask != null && !gameTimerTask.isCancelled()) {
            gameTimerTask.cancel();
        }
        if (timeoutTask != null && !timeoutTask.isCancelled()) {
            timeoutTask.cancel();
        }
        isGameActive = false;
        currentAnswer = "";
    }

    /**
     * Запуск случайного события (Математика или Анаграмма)
     */
    public void startRandomGame() {
        int minPlayers = loader.getConfigManager().getConfig().getInt("games.min-players", 2);
        if (Bukkit.getOnlinePlayers().size() < minPlayers) {
            return;
        }

        if (isGameActive) {
            return;
        }

        boolean playMath = ThreadLocalRandom.current().nextBoolean();
        if (playMath) {
            generateMathGame();
        } else {
            generateAnagramGame();
        }

        isGameActive = true;
        gameStartTime = System.currentTimeMillis();

        // Запускаем таймаут: если никто не ответит за указанное время
        long timeToAnswerSeconds = loader.getConfigManager().getConfig().getLong("games.time-to-answer", 30);
        timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isGameActive) {
                    String timeoutMsg = loader.getConfigManager().getMessages().getString(
                            "games.timeout",
                            "<dark_gray>[<red>ИГРА<dark_gray>] <gray>Время вышло! Никто не дал верный ответ. Было загадано: <yellow>%answer%"
                    );
                    Bukkit.broadcastMessage(ColorUtils.colorize(timeoutMsg.replace("%answer%", currentAnswer)));
                    isGameActive = false;
                    currentAnswer = "";
                }
            }
        }.runTaskLater(loader.getPlugin(), timeToAnswerSeconds * 20L);
    }

    private void generateMathGame() {
        int a = ThreadLocalRandom.current().nextInt(10, 100);
        int b = ThreadLocalRandom.current().nextInt(5, 50);
        int op = ThreadLocalRandom.current().nextInt(3); // 0: +, 1: -, 2: *

        String question;
        int result;

        switch (op) {
            case 0:
                result = a + b;
                question = a + " + " + b;
                break;
            case 1:
                result = a - b;
                question = a + " - " + b;
                break;
            case 2:
            default:
                int multA = ThreadLocalRandom.current().nextInt(3, 15);
                int multB = ThreadLocalRandom.current().nextInt(3, 12);
                result = multA * multB;
                question = multA + " * " + multB;
                break;
        }

        this.currentAnswer = String.valueOf(result);

        String msg = loader.getConfigManager().getMessages().getString(
                "games.math-question",
                "<dark_gray>[<gradient:#ffaa00:#ff5500>ИГРА</gradient><dark_gray>] <white>Решите пример: <yellow>%question% <gray>(напишите ответ в чат!)"
        );
        Bukkit.broadcastMessage(ColorUtils.colorize(msg.replace("%question%", question)));
    }

    private void generateAnagramGame() {
        List<String> words = loader.getConfigManager().getConfig().getStringList("games.words");
        if (words.isEmpty()) {
            words = Arrays.asList("алмаз", "эндермен", "крипер", "незерит", "верстак", "обсидиан");
        }

        String targetWord = words.get(ThreadLocalRandom.current().nextInt(words.size()));
        this.currentAnswer = targetWord.trim().toLowerCase();

        // Перемешиваем буквы
        List<Character> letters = new ArrayList<Character>();
        for (char c : targetWord.toCharArray()) {
            letters.add(c);
        }
        Collections.shuffle(letters);

        StringBuilder scrambled = new StringBuilder();
        for (char c : letters) {
            scrambled.append(c);
        }

        String msg = loader.getConfigManager().getMessages().getString(
                "games.anagram-question",
                "<dark_gray>[<gradient:#ffaa00:#ff5500>ИГРА</gradient><dark_gray>] <white>Анаграмма: расшифруйте слово <yellow>%scrambled% <gray>(первый ответивший получит приз!)"
        );
        Bukkit.broadcastMessage(ColorUtils.colorize(msg.replace("%scrambled%", scrambled.toString().toUpperCase())));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!isGameActive || currentAnswer.isEmpty()) {
            return;
        }

        String msg = event.getMessage().trim();

        // Проверяем точное совпадение ответа (без учета регистра)
        if (msg.equalsIgnoreCase(currentAnswer)) {
            // Отменяем сообщение, чтобы не спамить голым ответом в чат
            event.setCancelled(true);

            Player winner = event.getPlayer();
            isGameActive = false;

            if (timeoutTask != null && !timeoutTask.isCancelled()) {
                timeoutTask.cancel();
            }

            double duration = (System.currentTimeMillis() - gameStartTime) / 1000.0;
            String timeFormatted = String.format(Locale.US, "%.1f", duration);

            // Оповещаем весь сервер
            String winMsg = loader.getConfigManager().getMessages().getString(
                    "games.winner",
                    "<dark_gray>[<gradient:#00ff88:#00b4d8>ПОБЕДА</gradient><dark_gray>] <white>Игрок <yellow>%player% <white>первым ответил правильно: <green>%answer% <gray>(за %time% сек.)!"
            );
            winMsg = winMsg.replace("%player%", winner.getName())
                    .replace("%answer%", currentAnswer)
                    .replace("%time%", timeFormatted);

            Bukkit.broadcastMessage(ColorUtils.colorize(winMsg));

            // Выдаем награды в главном потоке сервера через консольные команды
            List<String> commands = loader.getConfigManager().getConfig().getStringList("games.rewards");
            Bukkit.getScheduler().runTask(loader.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    for (String cmd : commands) {
                        String parsedCmd = cmd.replace("%player%", winner.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsedCmd);
                    }
                }
            });

            currentAnswer = "";
        }
    }
}