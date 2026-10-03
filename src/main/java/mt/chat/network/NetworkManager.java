package mt.chat.network;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import mt.chat.system.MonolithLoader;
import mt.chat.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;

public class NetworkManager implements PluginMessageListener {

    private final MonolithLoader loader;
    private static final String BUNGEE_CHANNEL = "BungeeCord";
    private static final String SUB_CHANNEL = "ChatMT";

    public NetworkManager(MonolithLoader loader) {
        this.loader = loader;
    }

    public void register() {
        if (!isEnabled()) return;

        // Регистрируем входящий и исходящий каналы BungeeCord
        Bukkit.getMessenger().registerOutgoingPluginChannel(loader.getPlugin(), BUNGEE_CHANNEL);
        Bukkit.getMessenger().registerIncomingPluginChannel(loader.getPlugin(), BUNGEE_CHANNEL, this);
        loader.getPlugin().getLogger().info(" -> Мост BungeeCord/Velocity успешно инициализирован.");
    }

    public void unregister() {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(loader.getPlugin(), BUNGEE_CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(loader.getPlugin(), BUNGEE_CHANNEL, this);
    }

    public boolean isEnabled() {
        return loader.getConfigManager().getConfig().getBoolean("network.enabled", false);
    }

    public boolean isSyncGlobal() {
        return isEnabled() && loader.getConfigManager().getConfig().getBoolean("network.sync-global", true);
    }

    public boolean isSyncStaff() {
        return isEnabled() && loader.getConfigManager().getConfig().getBoolean("network.sync-staff", true);
    }

    public boolean isSyncPm() {
        return isEnabled() && loader.getConfigManager().getConfig().getBoolean("network.sync-pm", true);
    }

    public String getServerName() {
        return loader.getConfigManager().getConfig().getString("network.server-name", "server");
    }

    // ==========================================
    // ОТПРАВКА ДАННЫХ В СЕТЬ (ОТПРАВИТЕЛЬ)
    // ==========================================

    /**
     * Синхронизация глобального чата между серверами сети
     */
    public void sendGlobalChat(String senderName, String message) {
        if (!isSyncGlobal()) return;

        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);

            out.writeUTF("GLOBAL_CHAT");
            out.writeUTF(getServerName());
            out.writeUTF(senderName);
            out.writeUTF(message);

            sendForwardPacket("ALL", byteOut.toByteArray());
        } catch (IOException e) {
            loader.getLoggerMT().error("Ошибка отправки глобального чата в сеть: " + e.getMessage());
        }
    }

    /**
     * Синхронизация персонала (#staff)
     */
    public void sendStaffChat(String senderName, String message) {
        if (!isSyncStaff()) return;

        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);

            out.writeUTF("STAFF_CHAT");
            out.writeUTF(getServerName());
            out.writeUTF(senderName);
            out.writeUTF(message);

            sendForwardPacket("ALL", byteOut.toByteArray());
        } catch (IOException e) {
            loader.getLoggerMT().error("Ошибка отправки staff-чата в сеть: " + e.getMessage());
        }
    }

    /**
     * Отправка личного сообщения игроку на другой сервер
     */
    public void sendPrivateMessage(Player sender, String targetName, String message) {
        if (!isSyncPm()) return;

        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(byteOut);

            out.writeUTF("PM_REQUEST");
            out.writeUTF(getServerName());
            out.writeUTF(sender.getName());
            out.writeUTF(targetName);
            out.writeUTF(message);

            sendForwardPacket("ALL", byteOut.toByteArray());

            // Сообщение отправителю, что запрос передан по сети
            String sendFormat = loader.getConfigManager().getMessages().getString(
                    "formats.pm-send",
                    "<gray>Вы -> %target%: <white>%message%"
            );
            sender.sendMessage(ColorUtils.colorize(sendFormat.replace("%target%", targetName).replace("%message%", message)));
        } catch (IOException e) {
            loader.getLoggerMT().error("Ошибка отправки ЛС через сеть: " + e.getMessage());
        }
    }

    /**
     * Упаковывает данные в стандартный подканал Forward прокси BungeeCord / Velocity
     */
    private void sendForwardPacket(String targetServer, byte[] payload) {
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return; // Канал Spigot требует хотя бы 1 игрока на сервере для транзита пакета
        }

        Player messenger = Bukkit.getOnlinePlayers().iterator().next();

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF(targetServer);
        out.writeUTF(SUB_CHANNEL);
        out.writeShort(payload.length);
        out.write(payload);

        messenger.sendPluginMessage(loader.getPlugin(), BUNGEE_CHANNEL, out.toByteArray());
    }

    // ==========================================
    // ПРИЕМ И ОБРАБОТКА ДАННЫХ (ПОЛУЧАТЕЛЬ)
    // ==========================================

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals(BUNGEE_CHANNEL)) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (!subChannel.equals(SUB_CHANNEL)) return;

        short len = in.readShort();
        byte[] msgBytes = new byte[len];
        in.readFully(msgBytes);

        try {
            DataInputStream msgIn = new DataInputStream(new ByteArrayInputStream(msgBytes));
            String packetType = msgIn.readUTF();

            if (packetType.equals("GLOBAL_CHAT")) {
                handleNetworkGlobal(msgIn);
            } else if (packetType.equals("STAFF_CHAT")) {
                handleNetworkStaff(msgIn);
            } else if (packetType.equals("PM_REQUEST")) {
                handleNetworkPM(msgIn);
            }
        } catch (IOException e) {
            loader.getLoggerMT().error("Сбой чтения сетевого пакета ChatMT: " + e.getMessage());
        }
    }

    private void handleNetworkGlobal(DataInputStream in) throws IOException {
        String originServer = in.readUTF();
        String senderName = in.readUTF();
        String message = in.readUTF();

        // Не дублируем на том же сервере, где игрок уже отправил
        if (originServer.equalsIgnoreCase(getServerName())) return;

        String template = loader.getConfigManager().getConfig().getString(
                "network.format-global",
                "<dark_gray>[<aqua>%server%<dark_gray>] [<gold>G<dark_gray>] <gray>%player% <dark_gray>» <white>%message%"
        );

        String ready = ColorUtils.colorize(template
                .replace("%server%", originServer)
                .replace("%player%", senderName)
                .replace("%message%", message));

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(ready);
        }
    }

    private void handleNetworkStaff(DataInputStream in) throws IOException {
        String originServer = in.readUTF();
        String senderName = in.readUTF();
        String message = in.readUTF();

        if (originServer.equalsIgnoreCase(getServerName())) return;

        String template = loader.getConfigManager().getConfig().getString(
                "network.format-staff",
                "<dark_gray>[<red>Staff<dark_gray>|<aqua>%server%<dark_gray>] <gray>%player% <dark_gray>» <red>%message%"
        );

        String ready = ColorUtils.colorize(template
                .replace("%server%", originServer)
                .replace("%player%", senderName)
                .replace("%message%", message));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("chatmt.channel.staff")) {
                p.sendMessage(ready);
            }
        }
    }

    private void handleNetworkPM(DataInputStream in) throws IOException {
        String originServer = in.readUTF();
        String senderName = in.readUTF();
        String targetName = in.readUTF();
        String message = in.readUTF();

        Player target = Bukkit.getPlayerExact(targetName);
        if (target != null && target.isOnline()) {
            String formatFrom = loader.getConfigManager().getMessages().getString(
                    "formats.pm-receive",
                    "<gray>%sender% -> Вам: <white>%message%"
            );
            target.sendMessage(ColorUtils.colorize(formatFrom.replace("%sender%", senderName).replace("%message%", message)));
        }
    }
}