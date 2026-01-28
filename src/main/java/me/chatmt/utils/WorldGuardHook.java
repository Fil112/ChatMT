package me.chatmt.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.entity.Player;

import java.util.List;

public class WorldGuardHook {

    public static boolean isPlayerInRegion(Player player, List<String> regionNames) {
        if (player == null || regionNames == null || regionNames.isEmpty()) return false;

        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionManager regions = container.get(BukkitAdapter.adapt(player.getWorld()));

            if (regions == null) return false;

            BlockVector3 position = BukkitAdapter.asBlockVector(player.getLocation());
            ApplicableRegionSet set = regions.getApplicableRegions(position);

            for (ProtectedRegion region : set) {
                if (regionNames.contains(region.getId())) {
                    return true;
                }
            }
        } catch (NoClassDefFoundError | Exception e) {
            // WorldGuard не установлен или ошибка версии
            return false;
        }
        return false;
    }
}