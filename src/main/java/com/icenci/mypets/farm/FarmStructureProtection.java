package com.icenci.mypets.farm;

import com.icenci.mypets.MyPets;
import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.gui.InsuranceBoxGUI;
import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;

public class FarmStructureProtection implements Listener {

    private final LangManager lang;
    private final DataManager data;
    private final MyPets plugin;
    private final Set<String> protectedChests = new HashSet<>();
    private final Set<String> protectedSigns = new HashSet<>();
    private final Map<String, String[]> signToFarm = new HashMap<>();
    private final Map<String, String[]> chestToFarm = new HashMap<>();

    public FarmStructureProtection(LangManager lang, DataManager data, MyPets plugin) {
        this.lang = lang;
        this.data = data;
        this.plugin = plugin;
        loadProtectedBlocks();
    }

    private void loadProtectedBlocks() {
        java.io.File farmsDir = new java.io.File(
            Bukkit.getPluginManager().getPlugin("MyPets").getDataFolder(), "data/farms");
        if (!farmsDir.exists()) return;
        for (java.io.File file : farmsDir.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;
            String uuid = file.getName().replace(".json", "");
            List<FarmData> farms = data.loadPlayerFarms(uuid);
            for (int i = 0; i < farms.size(); i++) {
                FarmData farm = farms.get(i);
                if (farm.getChestX() != null) {
                    String chestKey = farm.getWorld() + ":" + farm.getChestX() + ":" + farm.getChestY() + ":" + farm.getChestZ();
                    protectedChests.add(chestKey);
                    chestToFarm.put(chestKey, new String[]{uuid, String.valueOf(i)});
                }
                if (farm.getSignX() != null) {
                    String signKey = farm.getWorld() + ":" + farm.getSignX() + ":" + farm.getSignY() + ":" + farm.getSignZ();
                    protectedSigns.add(signKey);
                    signToFarm.put(signKey, new String[]{uuid, String.valueOf(i)});
                }
            }
        }
    }

    public void addChestProtection(String world, int x, int y, int z, String ownerUuid, int farmIndex) {
        String key = world + ":" + x + ":" + y + ":" + z;
        protectedChests.add(key);
        chestToFarm.put(key, new String[]{ownerUuid, String.valueOf(farmIndex)});
    }

    public void addChestProtection(String world, int x, int y, int z) {
        protectedChests.add(world + ":" + x + ":" + y + ":" + z);
    }

    public void addSignProtection(String world, int x, int y, int z, String ownerUuid, int farmIndex) {
        String key = world + ":" + x + ":" + y + ":" + z;
        protectedSigns.add(key);
        signToFarm.put(key, new String[]{ownerUuid, String.valueOf(farmIndex)});
    }

    public void removeChestProtection(String world, int x, int y, int z) {
        String key = world + ":" + x + ":" + y + ":" + z;
        protectedChests.remove(key);
        chestToFarm.remove(key);
    }

    public void removeSignProtection(String world, int x, int y, int z) {
        String key = world + ":" + x + ":" + y + ":" + z;
        protectedSigns.remove(key);
        signToFarm.remove(key);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Location loc = block.getLocation();
        String key = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        if (protectedChests.contains(key)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(lang.get("farm.chest.protected"));
            return;
        }
        if (protectedSigns.contains(key)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(lang.get("farm.sign.protected"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.OAK_WALL_SIGN && block.getType() != Material.OAK_SIGN) return;

        Location loc = block.getLocation();
        String key = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        if (!signToFarm.containsKey(key)) return;

        event.setCancelled(true);
        String[] info = signToFarm.get(key);
        String ownerUuid = info[0];
        int farmIndex = Integer.parseInt(info[1]);

        List<FarmData> farms = data.loadPlayerFarms(ownerUuid);
        if (farmIndex >= 0 && farmIndex < farms.size()) {
            FarmData farm = farms.get(farmIndex);
            Player player = event.getPlayer();
            lang.sendWithPrefix(player, "farm.info.header", farm.getName());
            player.sendMessage(lang.get("farm.info.world", farm.getWorld()));
            if (farm.getPos1() != null)
                player.sendMessage(lang.get("farm.info.point1", farm.getPos1().get("x"), farm.getPos1().get("y"), farm.getPos1().get("z")));
            if (farm.getPos2() != null)
                player.sendMessage(lang.get("farm.info.point2", farm.getPos2().get("x"), farm.getPos2().get("y"), farm.getPos2().get("z")));
            if (farm.getChestX() != null)
                player.sendMessage(lang.get("farm.info.chest", farm.getChestX(), farm.getChestY(), farm.getChestZ()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChestInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.BARREL) return;

        Location loc = block.getLocation();
        String key = loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
        if (!chestToFarm.containsKey(key)) return;

        Player player = event.getPlayer();
        String[] info = chestToFarm.get(key);
        String ownerUuid = info[0];
        int farmIndex = Integer.parseInt(info[1]);

        if (!plugin.getConfigManager().isPremium()) {
            return; // 基础版不做干涉，玩家正常打开木桶
        }

        if (!player.getUniqueId().toString().equals(ownerUuid)
                && !player.hasPermission("mypets.bypass.chest")) {
            player.sendMessage(lang.get("farm.chest.not_owner"));
            event.setCancelled(true);
            return;
        }

        // 高级版：打开自定义 GUI
        event.setCancelled(true);
        List<FarmData> farms = data.loadPlayerFarms(ownerUuid); // 注意这里加载的是实时的列表
        if (farmIndex >= 0 && farmIndex < farms.size()) {
            FarmData farm = farms.get(farmIndex);
            InsuranceBoxGUI.getInstance(plugin).open(player, farm, farms, farmIndex);
        }
    }
}