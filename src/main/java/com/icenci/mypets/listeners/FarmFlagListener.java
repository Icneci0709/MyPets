package com.icenci.mypets.listeners;

import com.icenci.mypets.MyPets;
import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.farm.FarmStructureManager;
import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

/**
 * 农场权限Flag监听器（高级版功能）
 * 默认全部禁止，主人可开放各项权限
 */
public class FarmFlagListener implements Listener {

    private final MyPets plugin;
    private final LangManager lang;
    private final DataManager data;

    public static final List<String> FLAGS = Arrays.asList(
        "build", "break", "use", "container", "pvp", "explosion", "mob_damage"
    );

    public FarmFlagListener(MyPets plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLangManager();
        this.data = plugin.getDataManager();
    }

    /**
     * 检查位置是否在某个农场内，返回农场主人UUID和农场索引
     * @return [ownerUuid, farmIndex] 或 null
     */
    private String[] getFarmAt(Location loc) {
        java.io.File farmsDir = new java.io.File(
            plugin.getDataFolder(), "data/farms");
        if (!farmsDir.exists()) return null;
        for (java.io.File file : farmsDir.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;
            String uuid = file.getName().replace(".json", "");
            List<FarmData> farms = data.loadPlayerFarms(uuid);
            for (int i = 0; i < farms.size(); i++) {
                FarmData farm = farms.get(i);
                if (!loc.getWorld().getName().equals(farm.getWorld())) continue;
                if (FarmStructureManager.isInsideFarm(farm, loc)) {
                    return new String[]{uuid, String.valueOf(i)};
                }
            }
        }
        return null;
    }

    /**
     * 获取玩家在某农场的权限值
     */
    private boolean getFlag(Player player, String[] farmInfo, String flag) {
        String ownerUuid = farmInfo[0];
        int farmIndex = Integer.parseInt(farmInfo[1]);
        List<FarmData> farms = data.loadPlayerFarms(ownerUuid);
        if (farmIndex >= farms.size()) return false;
        FarmData farm = farms.get(farmIndex);

        // 主人始终有全部权限
        if (player.getUniqueId().toString().equals(ownerUuid)) return true;
        // OP 和权限节点绕过
        if (player.hasPermission("mypets.bypass.flag")) return true;

        Map<String, Boolean> flags = farm.getFlags();
        if (flags == null) return false; // 默认全部禁止
        return flags.getOrDefault(flag, false);
    }

    // ========== build: 放置方块 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        String[] farm = getFarmAt(event.getBlock().getLocation());
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;
        if (!getFlag(event.getPlayer(), farm, "build")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(lang.get("farm.flag.deny.build"));
        }
    }

    // ========== break: 破坏方块 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        String[] farm = getFarmAt(event.getBlock().getLocation());
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;
        if (!getFlag(event.getPlayer(), farm, "break")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(lang.get("farm.flag.deny.break"));
        }
    }

    // ========== use: 使用门/按钮/拉杆/活板门等 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        String[] farm = getFarmAt(block.getLocation());
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;

        Material type = block.getType();
        boolean isInteractable = type.name().contains("DOOR") || type.name().contains("TRAPDOOR")
            || type.name().contains("GATE") || type == Material.LEVER
            || type.name().contains("BUTTON") || type == Material.NOTE_BLOCK
            || type == Material.REPEATER || type == Material.COMPARATOR
            || type == Material.DAYLIGHT_DETECTOR || type.name().contains("PRESSURE_PLATE")
            || type == Material.BELL;

        if (isInteractable) {
            if (!getFlag(event.getPlayer(), farm, "use")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(lang.get("farm.flag.deny.use"));
            }
        }
    }

    // ========== container: 打开容器 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onContainerInteract(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        String[] farm = getFarmAt(block.getLocation());
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;

        Material type = block.getType();
        boolean isContainer = type == Material.CHEST || type == Material.TRAPPED_CHEST
            || type == Material.BARREL || type.name().contains("SHULKER_BOX")
            || type == Material.FURNACE || type == Material.BLAST_FURNACE
            || type == Material.SMOKER || type == Material.HOPPER
            || type == Material.DISPENSER || type == Material.DROPPER
            || type == Material.BREWING_STAND || type == Material.ENCHANTING_TABLE
            || type == Material.ANVIL || type == Material.BEACON
            || type == Material.CRAFTING_TABLE || type == Material.LOOM
            || type == Material.STONECUTTER || type == Material.GRINDSTONE
            || type == Material.CARTOGRAPHY_TABLE || type == Material.SMITHING_TABLE;

        if (isContainer) {
            if (!getFlag(event.getPlayer(), farm, "container")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(lang.get("farm.flag.deny.container"));
            }
        }
    }

    // ========== pvp: PVP 伤害 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        Player attacker = getAttacker(event.getDamager());
        if (attacker == null) return;

        String[] farm = getFarmAt(victim.getLocation());
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;
        if (victim.getUniqueId().toString().equals(farm[0])) return;

        if (!getFlag(attacker, farm, "pvp")) {
            event.setCancelled(true);
            attacker.sendMessage(lang.get("farm.flag.deny.pvp"));
        }
    }

    private Player getAttacker(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            Projectile proj = (Projectile) damager;
            if (proj.getShooter() instanceof Player) return (Player) proj.getShooter();
        }
        return null;
    }

    // ========== explosion: 爆炸保护 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        String[] farm = getFarmAt(loc);
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;
        List<FarmData> farms = data.loadPlayerFarms(farm[0]);
        int idx = Integer.parseInt(farm[1]);
        if (idx < farms.size()) {
            FarmData f = farms.get(idx);
            Map<String, Boolean> flags = f.getFlags();
            if (flags == null || !flags.getOrDefault("explosion", false)) {
                event.blockList().clear();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation();
        String[] farm = getFarmAt(loc);
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;
        List<FarmData> farms = data.loadPlayerFarms(farm[0]);
        int idx = Integer.parseInt(farm[1]);
        if (idx < farms.size()) {
            FarmData f = farms.get(idx);
            Map<String, Boolean> flags = f.getFlags();
            if (flags == null || !flags.getOrDefault("explosion", false)) {
                event.blockList().clear();
            }
        }
    }

    // ========== mob_damage: 怪物伤害玩家 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player victim = (Player) event.getEntity();
        String[] farm = getFarmAt(victim.getLocation());
        if (farm == null) return;
        if (!plugin.getConfigManager().isPremiumFlags()) return;
        if (victim.getUniqueId().toString().equals(farm[0])) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
            || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
            || event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE) {
            return; // PVP 伤害由 onPvP 处理
        }

        List<FarmData> farms = data.loadPlayerFarms(farm[0]);
        int idx = Integer.parseInt(farm[1]);
        if (idx < farms.size()) {
            FarmData f = farms.get(idx);
            Map<String, Boolean> flags = f.getFlags();
            if (flags == null || !flags.getOrDefault("mob_damage", false)) {
                event.setCancelled(true);
            }
        }
    }
}
