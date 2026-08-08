package mt.chat.moderation;

import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class IgnoreCmd implements CommandExecutor {

    private final MonolithLoader loader;

    public IgnoreCmd(MonolithLoader loader) {
        this.loader = loader;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Только для игроков!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length != 1) {
            player.sendMessage(ColorUtils.colorize("<red>Использование: /ignore <ник>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null || !target.isOnline()) {
            player.sendMessage(ColorUtils.colorize("<red>Игрок не найден или оффлайн!"));
            return true;
        }

        if (player.getUniqueId().equals(target.getUniqueId())) {
            String msg = loader.getConfigManager().getMessages().getString("ignore.cannot-ignore-self", "<red>Вы не можете игнорировать самого себя.");
            player.sendMessage(ColorUtils.colorize(msg));
            return true;
        }

        IgnoreManager ignoreManager = loader.getIgnoreManager();

        // Если игрок уже в черном списке - удаляем (переключатель)
        if (ignoreManager.isIgnored(player.getUniqueId(), target.getUniqueId())) {
            ignoreManager.removeIgnore(player.getUniqueId(), target.getUniqueId());
            String msg = loader.getConfigManager().getMessages().getString("ignore.removed", "<yellow>Игрок %target% удален из игнора.");
            player.sendMessage(ColorUtils.colorize(msg.replace("%target%", target.getName())));
        } else {
            // Если нет - добавляем
            ignoreManager.addIgnore(player.getUniqueId(), target.getUniqueId());
            String msg = loader.getConfigManager().getMessages().getString("ignore.added", "<green>Игрок %target% добавлен в игнор.");
            player.sendMessage(ColorUtils.colorize(msg.replace("%target%", target.getName())));
        }

        return true;
    }
}