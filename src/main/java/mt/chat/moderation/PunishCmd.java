package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PunishCmd implements CommandExecutor {

    private final MonolithLoader loader;

    public PunishCmd(MonolithLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        // Проверка прав (например, chatmt.punish.kick)
        if (!sender.hasPermission("chatmt.punish." + cmdName) && !sender.hasPermission("chatmt.unban")) {
            String noPerm = loader.getConfigManager().getMessages().getString("system.no-permission", "<red>У вас нет прав на эту команду!");
            sender.sendMessage(ColorUtils.colorize(noPerm));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ColorUtils.colorize("<red>Использование: /" + cmdName + " <игрок> [причина/время]"));
            return true;
        }

        String targetName = args[0];
        Player targetPlayer = Bukkit.getPlayer(targetName);

        // Склеиваем причину, если она есть
        String reason = loader.getConfigManager().getMessages().getString("punishments.default-reason", "Нарушение правил");
        if (args.length > 1) {
            reason = String.join(" ", args).substring(args[0].length() + 1);
        }

        // --- Обработка UNBAN ---
        if (cmdName.equals("unban")) {
            loader.getPunishManager().unbanPlayer(sender, targetName);
            loader.getLoggerMT().logPunish(sender.getName(), targetName, "UNBAN", "Снятие блокировки");
            return true;
        }

        // --- Обработка KICK ---
        if (cmdName.equals("kick")) {
            if (targetPlayer == null || !targetPlayer.isOnline()) {
                String offlineMsg = loader.getConfigManager().getMessages().getString("system.player-offline", "<gray>Игрок не найден или оффлайн.");
                sender.sendMessage(ColorUtils.colorize(offlineMsg));
                return true;
            }

            String rawMessage = loader.getConfigManager().getMessages().getString("punishments.kick-screen", "<red>Вы были кикнуты!\n<gray>Причина: <white>%reason%");
            targetPlayer.kickPlayer(ColorUtils.colorize(rawMessage.replace("%reason%", reason)));

            String bcMsg = loader.getConfigManager().getMessages().getString("punishments.kick-broadcast", "<dark_gray>[<red>!<dark_gray>] <white>%player% <gray>был кикнут. Причина: <white>%reason%");
            Bukkit.broadcastMessage(ColorUtils.colorize(bcMsg.replace("%player%", targetName).replace("%reason%", reason)));

            loader.getLoggerMT().logPunish(sender.getName(), targetName, "KICK", reason);
            return true;
        }

        // --- Обработка BAN (Теперь через нашу БД) ---
        if (cmdName.equals("ban")) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            UUID targetUUID = offlineTarget.getUniqueId();

            // Выдаем перманентный бан (в будущем тут можно будет парсить время)
            loader.getPunishManager().banPlayer(targetUUID, -1L, sender.getName(), reason);

            String bcMsg = loader.getConfigManager().getMessages().getString("punishments.ban-broadcast", "<dark_gray>[<red>!<dark_gray>] <white>%player% <gray>забанен. Причина: <white>%reason%");
            Bukkit.broadcastMessage(ColorUtils.colorize(bcMsg.replace("%player%", targetName).replace("%reason%", reason)));

            loader.getLoggerMT().logPunish(sender.getName(), targetName, "BAN", reason);
            return true;
        }

        // --- Обработка MUTE ---
        if (cmdName.equals("mute")) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

            if (offlineTarget == null || !offlineTarget.hasPlayedBefore() && !offlineTarget.isOnline()) {
                String notFound = loader.getConfigManager().getMessages().getString("system.player-not-found", "<gray>Игрок никогда не играл на сервере.");
                sender.sendMessage(ColorUtils.colorize(notFound));
                return true;
            }

            // Выдаем мут через новый метод менеджера
            loader.getPunishManager().mutePlayer(offlineTarget.getUniqueId(), -1L, sender.getName(), reason);

            String bcMsg = loader.getConfigManager().getMessages().getString("punishments.mute-broadcast", "<dark_gray>[<red>!<dark_gray>] <white>%player% <gray>получил мут. Причина: <white>%reason%");
            Bukkit.broadcastMessage(ColorUtils.colorize(bcMsg.replace("%player%", targetName).replace("%reason%", reason)));

            loader.getLoggerMT().logPunish(sender.getName(), targetName, "MUTE", reason);
            return true;
        }

        // --- Обработка UNMUTE ---
        if (cmdName.equals("unmute")) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

            if (!loader.getPunishManager().isMuted(offlineTarget.getUniqueId())) {
                sender.sendMessage(ColorUtils.colorize("<red>Игрок " + targetName + " не находится в муте!"));
                return true;
            }

            loader.getPunishManager().unmutePlayer(offlineTarget.getUniqueId());
            sender.sendMessage(ColorUtils.colorize("<green>Мут снят с " + targetName));

            if (offlineTarget.isOnline()) {
                ((Player) offlineTarget).sendMessage(ColorUtils.colorize("<green>Ваш мут был снят!"));
            }

            loader.getLoggerMT().logPunish(sender.getName(), targetName, "UNMUTE", "Снятие наказания");
            return true;
        }

        // --- Обработка WARN ---
        if (cmdName.equals("warn")) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

            loader.getPunishManager().warnPlayer(offlineTarget.getUniqueId(), targetName);

            int currentWarns = loader.getPunishManager().getWarns(offlineTarget.getUniqueId());
            int maxWarns = loader.getConfigManager().getConfig().getInt("punishments.warn-limit", 3);

            // Если currentWarns == 0, значит варн стал последним и игрок получил мут (счетчик обнулился)
            if (currentWarns > 0) {
                String bcMsg = loader.getConfigManager().getMessages().getString(
                        "punishments.warn-broadcast",
                        "<dark_gray>[<red>!<dark_gray>] <white>%player% <gray>получил предупреждение (<red>%current%/%max%<gray>). Причина: <white>%reason%"
                );
                Bukkit.broadcastMessage(ColorUtils.colorize(bcMsg
                        .replace("%player%", targetName)
                        .replace("%reason%", reason)
                        .replace("%current%", String.valueOf(currentWarns))
                        .replace("%max%", String.valueOf(maxWarns))));
            }

            loader.getLoggerMT().logPunish(sender.getName(), targetName, "WARN", reason);
            return true;
        }

        // --- Обработка UNWARN ---
        if (cmdName.equals("unwarn")) {
            @SuppressWarnings("deprecation")
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);

            int currentWarns = loader.getPunishManager().getWarns(offlineTarget.getUniqueId());
            if (currentWarns <= 0) {
                String noWarns = loader.getConfigManager().getMessages().getString("punishments.no-warns", "<red>У игрока <yellow>%player% <red>нет предупреждений!");
                sender.sendMessage(ColorUtils.colorize(noWarns.replace("%player%", targetName)));
                return true;
            }

            loader.getPunishManager().unwarnPlayer(offlineTarget.getUniqueId());

            String successMsg = loader.getConfigManager().getMessages().getString("punishments.unwarn-success", "<green>Вы сняли предупреждение с <white>%player%<green>. Осталось варнов: <white>%warns%");
            sender.sendMessage(ColorUtils.colorize(successMsg
                    .replace("%player%", targetName)
                    .replace("%warns%", String.valueOf(currentWarns - 1))));

            loader.getLoggerMT().logPunish(sender.getName(), targetName, "UNWARN", "Снятие предупреждения");
            return true;
        }

        return true;
    }
}