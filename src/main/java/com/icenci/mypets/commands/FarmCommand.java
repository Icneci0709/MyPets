package com.icenci.mypets.commands;

import com.icenci.mypets.MyPets;
import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.farm.FarmManager;
import com.icenci.mypets.farm.FarmStructureManager;
import com.icenci.mypets.farm.FarmStructureProtection;
import com.icenci.mypets.gui.ChatMenu;
import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;  // ← 新增导入，修复编译错误
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.*;
import java.util.stream.Collectors;

public class FarmCommand implements CommandExecutor, TabCompleter, Listener {

    private final MyPets plugin;
    private final LangManager lang;
    private final DataManager data;
    private final FarmManager farmManager;
    private final FarmStructureProtection structureProtection;

    private static final List<String> SUB = Arrays.asList(
        "create", "confirm", "list", "info", "rename", "remove", "tp", "clear", "close", "setspawn", "box", "flag", "help"
    );

    private final Map<String, Object[]> pendingCreations = new HashMap<>();

    public FarmCommand(MyPets plugin, FarmStructureProtection protection) {
        this.plugin = plugin;
        this.lang = plugin.getLangManager();
        this.data = plugin.getDataManager();
        this.farmManager = new FarmManager(data);
        this.structureProtection = protection;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            return SUB.stream()
                .filter(s -> s.startsWith(prefix))
                .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("rename")
                || args[0].equalsIgnoreCase("remove") || args[0].equalsIgnoreCase("tp"))) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                List<FarmData> farms = data.loadPlayerFarms(player.getUniqueId().toString());
                List<String> indices = new ArrayList<>();
                for (int i = 1; i <= farms.size(); i++) {
                    indices.add(String.valueOf(i));
                }
                return indices;
            }
        }
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.get("command.player_only"));
            return true;
        }
        Player player = (Player) sender;
        String uuid = player.getUniqueId().toString();
        List<FarmData> farms = data.loadPlayerFarms(uuid);

        if (args.length == 0) {
            int index = getPlayerCurrentFarmIndex(player, farms);
            ChatMenu menu = new ChatMenu(lang, plugin.getConfigManager().getPrefix());
            if (index != -1 && farms.get(index).getOwnerUuid().equals(uuid)) {
                menu.openFarmManageMenu(player, farms.get(index).getName());
            } else if (plugin.getConfigManager().isPremium()) {
                menu.openFarmMainMenu(player);
            } else {
                lang.sendWithPrefix(player, "farm.help.header");
                for (String key : Arrays.asList(
                        "farm.help.create", "farm.help.confirm", "farm.help.list",
                        "farm.help.info", "farm.help.rename", "farm.help.remove",
                        "farm.help.tp", "farm.help.clear")) {
                    player.sendMessage(lang.get(key));
                }
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // /farm create
        if (sub.equals("create")) {
            if (args.length < 2) { lang.sendWithPrefix(player, "farm.create.usage"); return true; }
            Location pos1 = farmManager.getPos1(player);
            Location pos2 = farmManager.getPos2(player);
            if (pos1 == null || pos2 == null) { lang.sendWithPrefix(player, "farm.create.no_selection"); return true; }
            if (!pos1.getWorld().equals(pos2.getWorld())) { lang.sendWithPrefix(player, "farm.create.different_world"); return true; }
            if (plugin.getConfigManager().isWorldWhitelistEnabled()) {
                List<String> whitelist = plugin.getConfigManager().getWorldWhitelist();
                if (!whitelist.isEmpty() && !whitelist.contains(pos1.getWorld().getName())) {
                    lang.sendWithPrefix(player, "farm.create.world_blocked");
                    return true;
                }
            }
            int maxFarms = plugin.getConfigManager().isPremium()
                ? plugin.getConfigManager().getPremiumMaxFarms()
                : plugin.getConfigManager().getMaxFarms();
            if (farms.size() >= maxFarms) { lang.sendWithPrefix(player, "farm.create.max_reached", maxFarms); return true; }

            int dx = Math.abs(pos1.getBlockX() - pos2.getBlockX()) + 1;
            int dy = Math.abs(pos1.getBlockY() - pos2.getBlockY()) + 1;
            int dz = Math.abs(pos1.getBlockZ() - pos2.getBlockZ()) + 1;
            int maxX, maxY, maxZ;
            if (plugin.getConfigManager().isPremiumFarmPricing()) {
                maxX = plugin.getConfigManager().getPremiumMaxFarmSizeX();
                maxY = plugin.getConfigManager().getPremiumMaxFarmSizeY();
                maxZ = plugin.getConfigManager().getPremiumMaxFarmSizeZ();
            } else {
                maxX = plugin.getConfigManager().getMaxFarmSizeX();
                maxY = plugin.getConfigManager().getMaxFarmSizeY();
                maxZ = plugin.getConfigManager().getMaxFarmSizeZ();
            }
            if (!farmManager.isSizeValid(dx, dy, dz, maxX, maxY, maxZ)) {
                String axis = dx > maxX ? "X" : (dy > maxY ? "Y" : "Z");
                int limit = axis.equals("X") ? maxX : (axis.equals("Y") ? maxY : maxZ);
                lang.sendWithPrefix(player, plugin.getConfigManager().isPremiumFarmPricing() ?
                    "farm.create.size_exceeded" : "farm.create.size_exceeded_free", axis, limit);
                return true;
            }

            Map<String, Integer> p1Map = new HashMap<>();
            p1Map.put("x", pos1.getBlockX()); p1Map.put("y", pos1.getBlockY()); p1Map.put("z", pos1.getBlockZ());
            Map<String, Integer> p2Map = new HashMap<>();
            p2Map.put("x", pos2.getBlockX()); p2Map.put("y", pos2.getBlockY()); p2Map.put("z", pos2.getBlockZ());
            if (data.checkFarmOverlap(pos1.getWorld().getName(), p1Map, p2Map)) { lang.sendWithPrefix(player, "farm.create.overlap"); return true; }

            double price = 0;
            if (plugin.getConfigManager().isPremiumFarmPricing()) { price = dx * dy * dz * plugin.getConfigManager().getPricePerBlock(); }

            pendingCreations.put(uuid, new Object[]{args[1], price});
            lang.sendWithPrefix(player, "farm.create.prompt_title");
            lang.sendWithPrefix(player, "farm.create.prompt_name", args[1]);
            lang.sendWithPrefix(player, "farm.create.prompt_point1", pos1.getBlockX(), pos1.getBlockY(), pos1.getBlockZ());
            lang.sendWithPrefix(player, "farm.create.prompt_point2", pos2.getBlockX(), pos2.getBlockY(), pos2.getBlockZ());
            if (plugin.getConfigManager().isPremiumFarmPricing()) { lang.sendWithPrefix(player, "farm.create.volume_and_price", dx * dy * dz, price); }
            lang.sendWithPrefix(player, "farm.create.warning_no_protection");
            lang.sendWithPrefix(player, "farm.create.warning_safe_location");
            lang.sendWithPrefix(player, "farm.create.prompt_confirm");
            return true;
        }

        // /farm confirm
        if (sub.equals("confirm")) {
            Object[] info = pendingCreations.remove(uuid);
            if (info == null) { lang.sendWithPrefix(player, "farm.create.no_pending"); return true; }
            String name = (String) info[0];

            Location pos1 = farmManager.getPos1(player);
            Location pos2 = farmManager.getPos2(player);
            if (pos1 == null || pos2 == null) { lang.sendWithPrefix(player, "farm.create.selection_lost"); return true; }

            FarmData farm = new FarmData();
            farm.setName(name);
            farm.setWorld(pos1.getWorld().getName());
            Map<String, Integer> p1 = new HashMap<>();
            p1.put("x", pos1.getBlockX()); p1.put("y", pos1.getBlockY()); p1.put("z", pos1.getBlockZ());
            Map<String, Integer> p2 = new HashMap<>();
            p2.put("x", pos2.getBlockX()); p2.put("y", pos2.getBlockY()); p2.put("z", pos2.getBlockZ());
            farm.setPos1(p1);
            farm.setPos2(p2);
            farm.setOwnerName(player.getName());
            farm.setOwnerUuid(uuid);
            farm.setIndex(farms.size());

            FarmStructureManager structureManager = new FarmStructureManager(lang);
            int[] chestCoords = structureManager.generateChest(farm, farmManager);
            if (chestCoords != null) {
                farm.setChestX(chestCoords[0]);
                farm.setChestY(chestCoords[1]);
                farm.setChestZ(chestCoords[2]);

                int[] signCoords = structureManager.placeFarmSign(farm, chestCoords[0], chestCoords[1], chestCoords[2]);
                if (signCoords != null) {
                    farm.setSignX(signCoords[0]);
                    farm.setSignY(signCoords[1]);
                    farm.setSignZ(signCoords[2]);
                }

                structureProtection.addChestProtection(farm.getWorld(), chestCoords[0], chestCoords[1], chestCoords[2], uuid, farms.size());
                if (signCoords != null) {
                    structureProtection.addSignProtection(farm.getWorld(), signCoords[0], signCoords[1], signCoords[2], uuid, farms.size());
                }
            }

            farms.add(farm);
            // 高级版：迁移已删除农场的保险物品到新农场
            if (plugin.getConfigManager().isPremium()) {
                Map<Integer, String> orphaned = data.takeOrphanedInsurance(uuid);
                if (orphaned != null && !orphaned.isEmpty()) {
                    farm.setInsurancePages(orphaned);
                    lang.sendWithPrefix(player, "farm.create.insurance_restored");
                }
            }
            data.savePlayerFarms(uuid, farms);
            farmManager.clearSelection(player);
            lang.sendWithPrefix(player, "farm.create.success", name);
            return true;
        }

        // /farm list
        if (sub.equals("list")) {
            if (farms.isEmpty()) { lang.sendWithPrefix(player, "farm.list.empty"); return true; }
            lang.sendWithPrefix(player, "farm.list.header");
            String prefix = plugin.getConfigManager().getPrefix();
            int i = 1;
            for (FarmData farm : farms) {
                String base = lang.get("farm.list.entry", i, farm.getName(), farm.getWorld());
                player.sendMessage(prefix + base);
                ChatMenu.sendClickable(player, "", lang.get("farm.list.tp_btn"), "/farm tp " + i, lang.get("farm.list.tp_hover"));
                i++;
            }
            return true;
        }

        // /farm info [序号]
        if (sub.equals("info")) {
            int index = -1;
            if (args.length >= 2) {
                try { index = Integer.parseInt(args[1]) - 1; } catch (NumberFormatException e) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            } else {
                index = getPlayerCurrentFarmIndex(player, farms);
                if (index == -1) { lang.sendWithPrefix(player, "farm.info.usage"); return true; }
            }
            if (index < 0 || index >= farms.size()) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            FarmData farm = farms.get(index);
            lang.sendWithPrefix(player, "farm.info.header", farm.getName());
            player.sendMessage(lang.get("farm.info.world", farm.getWorld()));
            if (farm.getPos1() != null) player.sendMessage(lang.get("farm.info.point1", farm.getPos1().get("x"), farm.getPos1().get("y"), farm.getPos1().get("z")));
            if (farm.getPos2() != null) player.sendMessage(lang.get("farm.info.point2", farm.getPos2().get("x"), farm.getPos2().get("y"), farm.getPos2().get("z")));
            if (farm.getChestX() != null) player.sendMessage(lang.get("farm.info.chest", farm.getChestX(), farm.getChestY(), farm.getChestZ()));
            return true;
        }

        // /farm rename [序号] <新名称>
        if (sub.equals("rename")) {
            if (args.length < 2) { lang.sendWithPrefix(player, "farm.rename.usage"); return true; }
            int index = -1;
            String newName;
            if (args.length >= 3) {
                try { index = Integer.parseInt(args[1]) - 1; } catch (NumberFormatException e) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
                newName = args[2];
            } else {
                index = getPlayerCurrentFarmIndex(player, farms);
                if (index == -1) { lang.sendWithPrefix(player, "farm.rename.usage"); return true; }
                newName = args[1];
            }
            if (index < 0 || index >= farms.size()) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            FarmData farm = farms.get(index);
            String oldName = farm.getName();
            farm.setName(newName);
            data.savePlayerFarms(uuid, farms);
            lang.sendWithPrefix(player, "farm.rename.success", oldName, newName);
            return true;
        }

        // /farm remove [序号]
        if (sub.equals("remove")) {
            if (!player.hasPermission("mypets.command.farm.remove")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            int index = -1;
            if (args.length >= 2) {
                try { index = Integer.parseInt(args[1]) - 1; } catch (NumberFormatException e) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            } else {
                index = getPlayerCurrentFarmIndex(player, farms);
                if (index == -1) { lang.sendWithPrefix(player, "farm.remove.usage"); return true; }
            }
            if (index < 0 || index >= farms.size()) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            FarmData removed = farms.get(index);

            if (removed.getSignX() != null) {
                structureProtection.removeSignProtection(removed.getWorld(), removed.getSignX(), removed.getSignY(), removed.getSignZ());
                World world = Bukkit.getWorld(removed.getWorld());
                if (world != null) {
                    Block signBlock = world.getBlockAt(removed.getSignX(), removed.getSignY(), removed.getSignZ());
                    if (signBlock.getType() == Material.OAK_WALL_SIGN || signBlock.getType() == Material.OAK_SIGN) {
                        signBlock.setType(Material.AIR);
                    }
                }
            }
            if (removed.getChestX() != null) {
                structureProtection.removeChestProtection(removed.getWorld(), removed.getChestX(), removed.getChestY(), removed.getChestZ());
                World world = Bukkit.getWorld(removed.getWorld());
                if (world != null) {
                    Block chestBlock = world.getBlockAt(removed.getChestX(), removed.getChestY(), removed.getChestZ());
                    if (chestBlock.getType() == Material.BARREL) {
                        if (plugin.getConfigManager().isPremium()) {
                            // 高级版：保存保险数据，后续创建农场时迁移
                            Map<Integer, String> pages = removed.getInsurancePages();
                            if (pages != null && !pages.isEmpty()) {
                                data.saveOrphanedInsurance(uuid, pages);
                            }
                        } else {
                            // 基础版：掉落箱子内物品
                            if (chestBlock.getState() instanceof org.bukkit.block.Barrel) {
                                org.bukkit.block.Barrel barrel = (org.bukkit.block.Barrel) chestBlock.getState();
                                for (org.bukkit.inventory.ItemStack item : barrel.getInventory().getContents()) {
                                    if (item != null && item.getType() != Material.AIR) {
                                        world.dropItemNaturally(chestBlock.getLocation(), item);
                                    }
                                }
                                barrel.getInventory().clear();
                            }
                        }
                        chestBlock.setType(Material.AIR);
                    }
                }
            }

            farms.remove(index);
            data.savePlayerFarms(uuid, farms);
            // 重新绑定该农场的宠物到其他农场
            data.rebindPetsAfterFarmRemoval(uuid, index);
            // 取消该农场所有待复活任务
            plugin.getPetListener().cancelResurrectionsForFarm(uuid, index);
            lang.sendWithPrefix(player, "farm.remove.success", removed.getName());
            return true;
        }

        // /farm tp <序号>
        if (sub.equals("tp")) {
            if (args.length < 2) { lang.sendWithPrefix(player, "farm.tp.usage"); return true; }
            int index;
            try { index = Integer.parseInt(args[1]) - 1; } catch (NumberFormatException e) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            if (index < 0 || index >= farms.size()) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            FarmData farm = farms.get(index);
            Location safeLoc = farmManager.getFarmSafeCenter(farm);
            if (safeLoc == null) { lang.sendWithPrefix(player, "farm.tp.unsafe"); return true; }
            player.teleport(safeLoc);
            lang.sendWithPrefix(player, "farm.tp.success", farm.getName());
            return true;
        }

        // /farm setspawn
        if (sub.equals("setspawn")) {
            if (!player.hasPermission("mypets.command.farm.setspawn")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            int index = getPlayerCurrentFarmIndex(player, farms);
            if (index == -1) { lang.sendWithPrefix(player, "farm.setspawn.not_in_farm"); return true; }
            FarmData farm = farms.get(index);
            if (!player.getUniqueId().toString().equals(farm.getOwnerUuid())) {
                lang.sendWithPrefix(player, "farm.setspawn.not_owner");
                return true;
            }
            Location loc = player.getLocation();
            farm.setSpawnX(loc.getBlockX());
            farm.setSpawnY(loc.getBlockY());
            farm.setSpawnZ(loc.getBlockZ());
            data.savePlayerFarms(uuid, farms);
            lang.sendWithPrefix(player, "farm.setspawn.success", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            return true;
        }

        // /farm clear
        if (sub.equals("clear")) { farmManager.clearSelection(player); lang.sendWithPrefix(player, "farm.selection.cleared"); return true; }

        // /farm close
        if (sub.equals("close")) { lang.sendWithPrefix(player, "farm.close.message"); return true; }

        // ========== /farm box（高级版） ==========
        if (sub.equals("box")) {
            if (!player.hasPermission("mypets.command.farm.box")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            if (!plugin.getConfigManager().isPremium()) {
                lang.sendWithPrefix(player, "farm.box.premium_only");
                return true;
            }
            int index = getPlayerCurrentFarmIndex(player, farms);
            if (index == -1) {
                lang.sendWithPrefix(player, "farm.box.not_in_farm");
                return true;
            }
            FarmData targetFarm = farms.get(index);

            // 获取准星瞄准的方块面位置
            Block targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || targetBlock.getType() == Material.AIR) {
                lang.sendWithPrefix(player, "farm.box.no_target");
                return true;
            }
            BlockFace face = player.getTargetBlockFace(5);
            if (face == null) {
                lang.sendWithPrefix(player, "farm.box.no_target");
                return true;
            }
            Location placeLoc = targetBlock.getRelative(face).getLocation();

            // 检查放置位置是否在农场内
            if (!FarmStructureManager.isInsideFarm(targetFarm, placeLoc)) {
                lang.sendWithPrefix(player, "farm.box.outside");
                return true;
            }
            // 检查是否被占用
            Block placeBlock = placeLoc.getBlock();
            if (!placeBlock.isEmpty() && !placeBlock.isPassable()) {
                lang.sendWithPrefix(player, "farm.box.occupied");
                return true;
            }

            // 移除旧保护与方块（先木牌后箱子，避免木牌掉落）
            if (targetFarm.getSignX() != null) {
                structureProtection.removeSignProtection(targetFarm.getWorld(), targetFarm.getSignX(), targetFarm.getSignY(), targetFarm.getSignZ());
                World world = Bukkit.getWorld(targetFarm.getWorld());
                if (world != null) {
                    Block oldSign = world.getBlockAt(targetFarm.getSignX(), targetFarm.getSignY(), targetFarm.getSignZ());
                    if (oldSign.getType() == Material.OAK_WALL_SIGN || oldSign.getType() == Material.OAK_SIGN) {
                        oldSign.setType(Material.AIR);
                    }
                }
            }
            if (targetFarm.getChestX() != null) {
                structureProtection.removeChestProtection(targetFarm.getWorld(), targetFarm.getChestX(), targetFarm.getChestY(), targetFarm.getChestZ());
                World world = Bukkit.getWorld(targetFarm.getWorld());
                if (world != null) {
                    Block oldChest = world.getBlockAt(targetFarm.getChestX(), targetFarm.getChestY(), targetFarm.getChestZ());
                    if (oldChest.getType() == Material.BARREL) oldChest.setType(Material.AIR);
                }
            }

            // 放置新结构
            FarmStructureManager structureManager = new FarmStructureManager(lang);
            int[] result = structureManager.relocateChestAndSign(placeLoc, player, targetFarm);
            if (result == null || result.length < 6) {
                lang.sendWithPrefix(player, "farm.box.failed");
                return true;
            }
            int cx = result[0], cy = result[1], cz = result[2];
            int sx = result[3], sy = result[4], sz = result[5];

            // 更新数据
            targetFarm.setChestX(cx);
            targetFarm.setChestY(cy);
            targetFarm.setChestZ(cz);
            targetFarm.setSignX(sx);
            targetFarm.setSignY(sy);
            targetFarm.setSignZ(sz);

            structureProtection.addChestProtection(targetFarm.getWorld(), cx, cy, cz, uuid, index);
            structureProtection.addSignProtection(targetFarm.getWorld(), sx, sy, sz, uuid, index);

            data.savePlayerFarms(uuid, farms);
            lang.sendWithPrefix(player, "farm.box.success");
            return true;
        }

        // /farm flag [flag] [true/false]
        if (sub.equals("flag")) {
            if (!player.hasPermission("mypets.command.farm.flag")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            if (!plugin.getConfigManager().isPremiumFlags()) {
                lang.sendWithPrefix(player, "farm.flag.premium_only");
                return true;
            }
            int index = getPlayerCurrentFarmIndex(player, farms);
            if (index == -1) {
                lang.sendWithPrefix(player, "farm.flag.not_in_farm");
                return true;
            }
            FarmData targetFarm = farms.get(index);
            if (!player.getUniqueId().toString().equals(targetFarm.getOwnerUuid())) {
                lang.sendWithPrefix(player, "farm.flag.not_owner");
                return true;
            }

            if (args.length == 1) {
                // 显示当前权限状态
                lang.sendWithPrefix(player, "farm.flag.header", targetFarm.getName());
                Map<String, Boolean> flags = targetFarm.getFlags();
                if (flags == null) flags = new HashMap<>();
                for (String flag : com.icenci.mypets.listeners.FarmFlagListener.FLAGS) {
                    boolean value = flags.getOrDefault(flag, false);
                    String status = value ? lang.get("farm.flag.status_allow") : lang.get("farm.flag.status_deny");
                    player.sendMessage(lang.get("farm.flag.status", flag, status));
                }
                player.sendMessage(lang.get("farm.flag.usage_hint"));
                return true;
            }

            if (args.length < 3) {
                lang.sendWithPrefix(player, "farm.flag.usage");
                return true;
            }

            String flagName = args[1].toLowerCase();
            if (!com.icenci.mypets.listeners.FarmFlagListener.FLAGS.contains(flagName)) {
                lang.sendWithPrefix(player, "farm.flag.invalid");
                return true;
            }

            boolean value;
            if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("allow") || args[2].equalsIgnoreCase("yes")) {
                value = true;
            } else if (args[2].equalsIgnoreCase("false") || args[2].equalsIgnoreCase("deny") || args[2].equalsIgnoreCase("no")) {
                value = false;
            } else {
                lang.sendWithPrefix(player, "farm.flag.invalid_value");
                return true;
            }

            Map<String, Boolean> flags = targetFarm.getFlags();
            if (flags == null) {
                flags = new HashMap<>();
                targetFarm.setFlags(flags);
            }
            flags.put(flagName, value);
            data.savePlayerFarms(uuid, farms);

            String status = value ? lang.get("farm.flag.set_allow") : lang.get("farm.flag.set_deny");
            lang.sendWithPrefix(player, "farm.flag.set", flagName, status);
            return true;
        }

        // /farm help
        if (sub.equals("help")) {
            lang.sendWithPrefix(player, "farm.help.header");
            for (String key : Arrays.asList(
                    "farm.help.create", "farm.help.confirm", "farm.help.list",
                    "farm.help.info", "farm.help.rename", "farm.help.remove",
                    "farm.help.tp", "farm.help.clear")) {
                player.sendMessage(lang.get(key));
            }
            return true;
        }

        lang.sendWithPrefix(player, "farm.unknown_subcommand");
        return true;
    }

    @EventHandler
    public void onShovelInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null || event.getItem().getType() != Material.WOODEN_SHOVEL) return;
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            Location loc = event.getClickedBlock().getLocation();
            farmManager.setPos1(player, loc);
            lang.sendWithPrefix(player, "farm.selection.point1", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            Location loc = event.getClickedBlock().getLocation();
            farmManager.setPos2(player, loc);
            lang.sendWithPrefix(player, "farm.selection.point2", loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    }

    private int getPlayerCurrentFarmIndex(Player player, List<FarmData> farms) {
        Location loc = player.getLocation();
        for (int i = 0; i < farms.size(); i++) {
            FarmData farm = farms.get(i);
            if (!loc.getWorld().getName().equals(farm.getWorld())) continue;
            Map<String, Integer> p1 = farm.getPos1();
            Map<String, Integer> p2 = farm.getPos2();
            if (p1 == null || p2 == null) continue;
            int minX = Math.min(p1.get("x"), p2.get("x"));
            int maxX = Math.max(p1.get("x"), p2.get("x"));
            int minY = Math.min(p1.get("y"), p2.get("y"));
            int maxY = Math.max(p1.get("y"), p2.get("y"));
            int minZ = Math.min(p1.get("z"), p2.get("z"));
            int maxZ = Math.max(p1.get("z"), p2.get("z"));
            if (loc.getBlockX() >= minX && loc.getBlockX() <= maxX &&
                loc.getBlockY() >= minY && loc.getBlockY() <= maxY &&
                loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ) {
                return i;
            }
        }
        return -1;
    }
}