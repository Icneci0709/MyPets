package com.icenci.mypets.farm;

import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;

import java.util.*;

public class FarmStructureManager {

    private final Set<String> protectedChests = new HashSet<>();
    private final Set<String> protectedSigns = new HashSet<>();
    private final LangManager lang;

    public FarmStructureManager(LangManager lang) {
        this.lang = lang;
    }

    /**
     * 在农场中心生成木桶
     */
    public int[] generateChest(FarmData farm, FarmManager farmManager) {
        Map<String, Integer> p1 = farm.getPos1();
        Map<String, Integer> p2 = farm.getPos2();
        World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
        if (world == null || p1 == null || p2 == null) return null;

        int minX = Math.min(p1.get("x"), p2.get("x"));
        int maxX = Math.max(p1.get("x"), p2.get("x"));
        int minZ = Math.min(p1.get("z"), p2.get("z"));
        int maxZ = Math.max(p1.get("z"), p2.get("z"));
        int centerX = (minX + maxX) / 2;
        int centerZ = (minZ + maxZ) / 2;
        int baseY = Math.min(p1.get("y"), p2.get("y"));

        Location safeLoc = farmManager.getSafeLocation(world, centerX, baseY, centerZ);
        int cx, cy, cz;
        if (safeLoc != null) {
            cx = safeLoc.getBlockX();
            cy = safeLoc.getBlockY();
            cz = safeLoc.getBlockZ();
        } else {
            cx = centerX;
            cy = baseY;
            cz = centerZ;
        }

        Block block = world.getBlockAt(cx, cy, cz);
        block.setType(Material.BARREL);

        try {
            if (block.getBlockData() instanceof Directional) {
                Directional directional = (Directional) block.getBlockData();
                directional.setFacing(BlockFace.EAST);
                block.setBlockData(directional, false);
            }
        } catch (Exception ignored) {}

        // 设置木桶名称为保险箱
        if (block.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) block.getState();
            barrel.setCustomName("§6农场保险箱");
            barrel.update(true);
        }

        String key = farm.getWorld() + ":" + cx + ":" + cy + ":" + cz;
        protectedChests.add(key);

        return new int[]{cx, cy, cz};
    }

    /**
     * 在箱子正面放置木牌
     */
    public int[] placeFarmSign(FarmData farm, int chestX, int chestY, int chestZ) {
        World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
        if (world == null) return null;

        Map<String, Integer> p1 = farm.getPos1();
        Map<String, Integer> p2 = farm.getPos2();
        if (p1 == null || p2 == null) return null;

        double centerX = (p1.get("x") + p2.get("x")) / 2.0;
        double centerZ = (p1.get("z") + p2.get("z")) / 2.0;
        double dx = centerX - chestX;
        double dz = centerZ - chestZ;

        BlockFace mainFace;
        BlockFace sideFace1, sideFace2;
        if (Math.abs(dx) >= Math.abs(dz)) {
            mainFace = dx > 0 ? BlockFace.EAST : BlockFace.WEST;
            sideFace1 = BlockFace.NORTH;
            sideFace2 = BlockFace.SOUTH;
        } else {
            mainFace = dz > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
            sideFace1 = BlockFace.EAST;
            sideFace2 = BlockFace.WEST;
        }

        BlockFace[] priorityFaces = {mainFace, sideFace1, sideFace2};

        for (BlockFace face : priorityFaces) {
            int sx = chestX + face.getModX();
            int sy = chestY;
            int sz = chestZ + face.getModZ();
            Block signBlock = world.getBlockAt(sx, sy, sz);
            if (signBlock.isEmpty() || signBlock.isPassable()) {
                signBlock.setType(Material.OAK_WALL_SIGN);
                if (signBlock.getBlockData() instanceof WallSign) {
                    WallSign wallSign = (WallSign) signBlock.getBlockData();
                    wallSign.setFacing(face);
                    signBlock.setBlockData(wallSign);
                }
                if (signBlock.getState() instanceof Sign) {
                    Sign sign = (Sign) signBlock.getState();
                    sign.setLine(0, lang.get("sign.line1", farm.getIndex() + 1));
                    sign.setLine(1, lang.get("sign.line2", farm.getName()));
                    sign.setLine(2, lang.get("sign.line3", farm.getOwnerName()));
                    sign.setLine(3, lang.get("sign.line4", 0));
                    sign.update();
                    return new int[]{sx, sy, sz};
                } else {
                    signBlock.setType(Material.AIR);
                }
            }
        }

        // 头顶备选
        int sx = chestX, sy = chestY + 1, sz = chestZ;
        Block topBlock = world.getBlockAt(sx, sy, sz);
        if (topBlock.isEmpty() || topBlock.isPassable()) {
            topBlock.setType(Material.OAK_SIGN);
            if (topBlock.getState() instanceof Sign) {
                Sign sign = (Sign) topBlock.getState();
                sign.setLine(0, lang.get("sign.line1", farm.getIndex() + 1));
                sign.setLine(1, lang.get("sign.line2", farm.getName()));
                sign.setLine(2, lang.get("sign.line3", farm.getOwnerName()));
                sign.setLine(3, lang.get("sign.line4", 0));
                sign.update();
                return new int[]{sx, sy, sz};
            } else {
                topBlock.setType(Material.AIR);
            }
        }

        return null;
    }

    /**
     * 在指定位置重新放置木桶和木牌，方向面对玩家
     * @param targetLoc 准星瞄准的位置（木桶放置位置）
     * @param player 执行玩家
     * @return int[]{chestX, chestY, chestZ, signX, signY, signZ} 或 null
     */
    public int[] relocateChestAndSign(Location targetLoc, Player player, FarmData farm) {
        World world = targetLoc.getWorld();
        int cx = targetLoc.getBlockX();
        int cy = targetLoc.getBlockY();
        int cz = targetLoc.getBlockZ();

        Block block = world.getBlockAt(cx, cy, cz);
        if (!block.isEmpty() && !block.isPassable()) {
            return null; // 被占用
        }

        block.setType(Material.BARREL);

        // 设置木桶名称为保险箱
        if (block.getState() instanceof Barrel) {
            Barrel barrel = (Barrel) block.getState();
            barrel.setCustomName("§6农场保险箱");
            barrel.update(true);
        }

        // 获取玩家面向方向，木桶正面朝向玩家
        BlockFace facing = getPlayerFacing(player).getOppositeFace();
        try {
            if (block.getBlockData() instanceof Directional) {
                Directional directional = (Directional) block.getBlockData();
                directional.setFacing(facing);
                block.setBlockData(directional, false);
            }
        } catch (Exception ignored) {}

        // 放置木牌：在木桶朝向玩家的那一侧
        int sx = cx + facing.getModX();
        int sy = cy;
        int sz = cz + facing.getModZ();
        Block signBlock = world.getBlockAt(sx, sy, sz);
        if (!signBlock.isEmpty() && !signBlock.isPassable()) {
            // 备选头顶
            sx = cx; sy = cy + 1; sz = cz;
            signBlock = world.getBlockAt(sx, sy, sz);
            if (!signBlock.isEmpty() && !signBlock.isPassable()) {
                return null; // 无处放木牌
            }
        }
        signBlock.setType(Material.OAK_WALL_SIGN);
        if (signBlock.getBlockData() instanceof WallSign) {
            WallSign wallSign = (WallSign) signBlock.getBlockData();
            wallSign.setFacing(facing);
            signBlock.setBlockData(wallSign);
        }
        if (signBlock.getState() instanceof Sign) {
            Sign sign = (Sign) signBlock.getState();
            sign.setLine(0, lang.get("sign.line1", farm.getIndex() + 1));
            sign.setLine(1, lang.get("sign.line2", farm.getName()));
            sign.setLine(2, lang.get("sign.line3", farm.getOwnerName()));
            sign.setLine(3, lang.get("sign.line4", 0));
            sign.update();
        }

        return new int[]{cx, cy, cz, sx, sy, sz};
    }

    /**
     * 检查一个位置是否在农场区域内
     */
    public static boolean isInsideFarm(FarmData farm, Location loc) {
        if (!loc.getWorld().getName().equals(farm.getWorld())) return false;
        Map<String, Integer> p1 = farm.getPos1();
        Map<String, Integer> p2 = farm.getPos2();
        if (p1 == null || p2 == null) return false;

        int minX = Math.min(p1.get("x"), p2.get("x"));
        int maxX = Math.max(p1.get("x"), p2.get("x"));
        int minY = Math.min(p1.get("y"), p2.get("y"));
        int maxY = Math.max(p1.get("y"), p2.get("y"));
        int minZ = Math.min(p1.get("z"), p2.get("z"));
        int maxZ = Math.max(p1.get("z"), p2.get("z"));

        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private BlockFace getPlayerFacing(Player player) {
        float yaw = player.getLocation().getYaw();
        if (yaw < 0) yaw += 360;
        if (yaw >= 315 || yaw < 45) return BlockFace.SOUTH;
        if (yaw >= 45 && yaw < 135) return BlockFace.WEST;
        if (yaw >= 135 && yaw < 225) return BlockFace.NORTH;
        return BlockFace.EAST;
    }

    // ==================== 以下方法不变 ====================

    public void updateSignEggCount(FarmData farm, int count) {
        if (farm.getSignX() == null) return;
        World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(farm.getSignX(), farm.getSignY(), farm.getSignZ());
        if (block.getType() == Material.OAK_WALL_SIGN || block.getType() == Material.OAK_SIGN) {
            if (block.getState() instanceof Sign) {
                Sign sign = (Sign) block.getState();
                sign.setLine(0, lang.get("sign.line1", farm.getIndex() + 1));
                sign.setLine(1, lang.get("sign.line2", farm.getName()));
                sign.setLine(2, lang.get("sign.line3", farm.getOwnerName()));
                sign.setLine(3, lang.get("sign.line4", count));
                sign.update();
            }
        }
    }

    public void removeChest(FarmData farm) {
        if (farm.getChestX() == null) return;
        World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(farm.getChestX(), farm.getChestY(), farm.getChestZ());
        if (block.getType() == Material.BARREL) {
            block.setType(Material.AIR);
        }
        String key = farm.getWorld() + ":" + farm.getChestX() + ":" + farm.getChestY() + ":" + farm.getChestZ();
        protectedChests.remove(key);
    }

    public void removeSign(FarmData farm) {
        if (farm.getSignX() == null) return;
        World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
        if (world == null) return;
        Block block = world.getBlockAt(farm.getSignX(), farm.getSignY(), farm.getSignZ());
        if (block.getType() == Material.OAK_WALL_SIGN || block.getType() == Material.OAK_SIGN) {
            block.setType(Material.AIR);
        }
        String key = farm.getWorld() + ":" + farm.getSignX() + ":" + farm.getSignY() + ":" + farm.getSignZ();
        protectedSigns.remove(key);
    }

    public boolean isProtectedChest(String world, int x, int y, int z) {
        return protectedChests.contains(world + ":" + x + ":" + y + ":" + z);
    }

    public boolean isProtectedSign(String world, int x, int y, int z) {
        return protectedSigns.contains(world + ":" + x + ":" + y + ":" + z);
    }
}