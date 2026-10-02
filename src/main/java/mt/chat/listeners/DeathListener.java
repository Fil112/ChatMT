package mt.chat.listeners;

import mt.chat.system.MonolithLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class DeathListener implements Listener {

    private final MonolithLoader loader;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public DeathListener(MonolithLoader loader) {
        this.loader = loader;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        EntityDamageEvent lastDamage = victim.getLastDamageCause();

        // Проверяем тоггл в конфиге
        boolean enabled = loader.getConfigManager().getConfig().getBoolean("features.death-messages", true);
        if (!enabled) {
            return;
        }

        String rawMessage;
        Player killer = victim.getKiller();

        // 1. PvP — смерть от рук другого игрока
        if (killer != null && !killer.equals(victim)) {
            rawMessage = getMessage("deaths.pvp", "<gray><red>%victim%</red> был убит игроком <yellow>%killer%</yellow> при помощи <white>%weapon%</white>");

            ItemStack weapon = killer.getInventory().getItemInMainHand();
            String weaponName;

            if (weapon.getType() == Material.AIR) {
                weaponName = getMessage("deaths.weapon-bare-hands", "кулаков");
            } else if (weapon.hasItemMeta() && weapon.getItemMeta().hasDisplayName()) {
                weaponName = weapon.getItemMeta().getDisplayName();
            } else {
                weaponName = formatMaterialName(weapon.getType());
            }

            rawMessage = rawMessage.replace("%victim%", victim.getName())
                    .replace("%killer%", killer.getName())
                    .replace("%weapon%", weaponName);

        } else if (lastDamage instanceof EntityDamageByEntityEvent) {
            // 2. PvE — загрыз моб или босс (без паттерн-матчинга под Java 8)
            EntityDamageByEntityEvent damageByEntity = (EntityDamageByEntityEvent) lastDamage;
            if (damageByEntity.getDamager() instanceof LivingEntity) {
                LivingEntity damager = (LivingEntity) damageByEntity.getDamager();
                rawMessage = getMessage("deaths.mob", "<gray><red>%victim%</red> пал от лап <yellow>%killer%</yellow>");
                String mobName = damager.getCustomName() != null ? damager.getCustomName() : damager.getName();
                rawMessage = rawMessage.replace("%victim%", victim.getName())
                        .replace("%killer%", mobName);
            } else {
                rawMessage = getMessageByCause(lastDamage.getCause(), victim);
            }

        } else if (lastDamage != null) {
            // 3. Окружение (лава, суицид, падение и т.д.)
            rawMessage = getMessageByCause(lastDamage.getCause(), victim);
        } else {
            rawMessage = getMessage("deaths.unknown", "<gray><red>%victim%</red> загадочно погиб.");
            rawMessage = rawMessage.replace("%victim%", victim.getName());
        }

        // Переводим MiniMessage в формат Spigot через утилиту и ставим сообщение
        event.setDeathMessage(mt.chat.utils.ColorUtils.colorize(rawMessage));
    }

    private String getMessageByCause(EntityDamageEvent.DamageCause cause, Player victim) {
        String key;
        String def;

        switch (cause) {
            case FALL:
                key = "deaths.fall";
                def = "<gray><red>%victim%</red> разбился вдребезги.";
                break;
            case DROWNING:
                key = "deaths.drowning";
                def = "<gray><red>%victim%</red> утонул.";
                break;
            case LAVA:
                key = "deaths.lava";
                def = "<gray><red>%victim%</red> решил поплавать в лаве.";
                break;
            case FIRE:
            case FIRE_TICK:
                key = "deaths.fire";
                def = "<gray><red>%victim%</red> сгорел заживо.";
                break;
            case VOID:
                key = "deaths.void";
                def = "<gray><red>%victim%</red> провалился в бездну.";
                break;
            case BLOCK_EXPLOSION:
            case ENTITY_EXPLOSION:
                key = "deaths.explosion";
                def = "<gray><red>%victim%</red> был разорван взрывом.";
                break;
            case STARVATION:
                key = "deaths.starvation";
                def = "<gray><red>%victim%</red> умер от истощения.";
                break;
            case SUICIDE:
                key = "deaths.suicide";
                def = "<gray><red>%victim%</red> покончил с собой.";
                break;
            case MAGIC:
            case POISON:
                key = "deaths.magic";
                def = "<gray><red>%victim%</red> пал жертвой магии.";
                break;
            default:
                key = "deaths.unknown";
                def = "<gray><red>%victim%</red> погиб при странных обстоятельствах.";
                break;
        }

        return getMessage(key, def).replace("%victim%", victim.getName());
    }

    private String getMessage(String path, String def) {
        String msg = loader.getConfigManager().getMessages().getString(path);
        return (msg != null && !msg.isEmpty()) ? msg : def;
    }

    // DIAMOND_SWORD -> Diamond Sword
    private String formatMaterialName(Material material) {
        String[] parts = material.name().toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}