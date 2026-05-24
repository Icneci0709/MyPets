package com.icenci.mypets.farm;

import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.model.FarmData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

public class FarmManager {

    private final Map<String, Map<String, Location>> selections = new HashMap<>();
    private final DataManager data;

    public FarmManager(DataManager data) {
        this.data = data;
    }

    public void setPos1(Player player, Location loc) {
        selections.computeIfAbsent(player.getUniqueId().toString(), k -> new HashMap<>()).put("pos1", loc);
    }

    public void setPos2(Player player, Location loc) {
        selections.computeIfAbsent(player.getUniqueId().toString(), k -> new HashMap<>()).put("pos2", loc);
    }

    public Location getPos1(Player player) {
        Map<String, Location> sel = selections.get(player.getUniqueId().toString());
        return sel != null ? sel.get("pos1") : null;
    }

    public Location getPos2(Player player) {
        Map<String, Location> sel = selections.get(player.getUniqueId().toString());
        return sel != null ? sel.get("pos2") : null;
    }

    public void clearSelection(Player player) {
        selections.remove(player.getUniqueId().toString());
    }

    public Location getSafeLocation(World world, int x, int y, int z) {
        int maxY = world.getMaxHeight() - 2;
        int minY = world.getMinHeight();
        for (int dy = 0; dy < 15; dy++) {
            int cy = y + dy;
            if (cy > maxY) break;
            if (world.getBlockAt(x, cy - 1, z).isSolid() &&
                world.getBlockAt(x, cy, z).isPassable() &&
                world.getBlockAt(x, cy + 1, z).isPassable()) {
                return new Location(world, x + 0.5, cy, z + 0.5);
            }
        }
        for (int dy = 1; dy < 10; dy++) {
            int cy = y - dy;
            if (cy < minY) break;
            if (world.getBlockAt(x, cy - 1, z).isSolid() &&
                world.getBlockAt(x, cy, z).isPassable() &&
                world.getBlockAt(x, cy + 1, z).isPassable()) {
                return new Location(world, x + 0.5, cy, z + 0.5);
            }
        }
        return null;
    }

    public Location getFarmSafeCenter(FarmData farm) {
        World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
        if (world == null) return null;

        if (farm.getSpawnX() != null) {
            return new Location(world, farm.getSpawnX() + 0.5, farm.getSpawnY(), farm.getSpawnZ() + 0.5);
        }

        Map<String, Integer> p1 = farm.getPos1();
        Map<String, Integer> p2 = farm.getPos2();
        if (p1 == null || p2 == null) return null;

        int cx = (p1.get("x") + p2.get("x")) / 2;
        int cz = (p1.get("z") + p2.get("z")) / 2;
        int baseY = Math.min(p1.get("y"), p2.get("y"));
        return getSafeLocation(world, cx, baseY, cz);
    }

    public boolean isSizeValid(int dx, int dy, int dz, int maxX, int maxY, int maxZ) {
        return dx <= maxX && dy <= maxY && dz <= maxZ;
    }

    public boolean isOverlapping(String world, Map<String, Integer> pos1, Map<String, Integer> pos2) {
        return data.checkFarmOverlap(world, pos1, pos2);
    }
}