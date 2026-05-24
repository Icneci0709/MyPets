package com.icenci.mypets.commands;

import com.icenci.mypets.MyPets;
import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.gui.ChatMenu;
import com.icenci.mypets.listeners.CaptureListener;
import com.icenci.mypets.model.PetData;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.entity.Sittable;
import java.util.*;
import java.util.stream.Collectors;

public class PetCommand implements CommandExecutor, TabCompleter {

    private final MyPets plugin;
    private final LangManager lang;
    private static final List<String> SUB = Arrays.asList("list","info","rename","summon","recall","kill","release","capture","share","setfarm","close","help");
    private static final List<String> SHARE_SUB = Arrays.asList("add","remove");

    public PetCommand(MyPets plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLangManager();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (args.length == 1) return SUB.stream().filter(s->s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        if (args.length == 2 && args[0].equalsIgnoreCase("share")) return SHARE_SUB.stream().filter(s->s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        if (args.length == 3 && args[0].equalsIgnoreCase("share") && (args[1].equalsIgnoreCase("add")||args[1].equalsIgnoreCase("remove")))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
        return Collections.emptyList();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage(lang.get("command.player_only")); return true; }
        Player player = (Player) sender;
        DataManager data = plugin.getDataManager();

        if (args.length == 0) {
            if (plugin.getConfigManager().isPremium()) {
                // 检测是否骑乘宠物或对准坐下宠物，有则打开个体管理菜单
                String uuid = player.getUniqueId().toString();
                PetData targetPet = null;
                boolean hasMount = false;
                Entity vehicle = player.getVehicle();
                if (vehicle instanceof Tameable) {
                    Tameable t = (Tameable) vehicle;
                    if (t.isTamed() && t.getOwner() == player) {
                        targetPet = findPetByUuid(data, uuid, vehicle.getUniqueId().toString());
                        hasMount = true;
                    }
                }
                if (targetPet == null) {
                    Entity target = player.getTargetEntity(5);
                    if (target instanceof Tameable) {
                        Tameable t = (Tameable) target;
                        if (t.isTamed() && t.getOwner() == player && isSitting(target)) {
                            targetPet = findPetByUuid(data, uuid, target.getUniqueId().toString());
                        }
                    }
                }
                ChatMenu menu = new ChatMenu(lang, plugin.getConfigManager().getPrefix());
                if (targetPet != null) {
                    String petName = targetPet.getCustomName() != null ? targetPet.getCustomName() : lang.get("pet.list.unnamed");
                    menu.openPetManageMenu(player, petName, hasMount);
                } else {
                    menu.openPetMainMenu(player);
                }
            } else {
                lang.sendWithPrefix(player, "pet.menu.header");
                for (String key : Arrays.asList(
                        "pet.menu.list", "pet.menu.info", "pet.menu.rename",
                        "pet.menu.summon", "pet.menu.recall",
                        "pet.menu.kill", "pet.menu.release", "pet.menu.help")) {
                    player.sendMessage(lang.get(key));
                }
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        // list
        if (sub.equals("list")) {
            List<PetData> pets = data.loadPlayerPets(player.getUniqueId().toString());
            if (pets.isEmpty()) { lang.sendWithPrefix(player, "pet.list.empty"); return true; }
            lang.sendWithPrefix(player, "pet.list.header");
            int i = 1;
            String prefix = plugin.getConfigManager().getPrefix();
            for (PetData pet : pets) {
                String type = pet.getType() != null ? pet.getType() : "?";
                String name = pet.getCustomName() != null ? pet.getCustomName() : lang.get("pet.list.unnamed");
                String status = pet.isAlive() ? "" : lang.get("pet.info.status_dead_tag");
                String base = lang.get("pet.list.entry", i, name, type + status);
                player.sendMessage(prefix + base);
                if (pet.isAlive()) {
                    ChatMenu.sendClickable(player, "", lang.get("pet.list.summon_btn"), "/pet summon " + i, lang.get("pet.list.summon_hover"));
                    ChatMenu.sendClickable(player, "", lang.get("pet.list.recall_btn"), "/pet recall " + i, lang.get("pet.list.recall_hover"));
                }
                i++;
            }
            return true;
        }

        // capture
        if (sub.equals("capture")) {
            if (!player.hasPermission("mypets.command.pet.capture")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            new CaptureListener(plugin, plugin.getAdapters()).capturePet(player); return true; }

        // info
        if (sub.equals("info")) {
            PetData pet = getTargetPet(player, args.length > 1 ? args[1] : null);
            if (pet == null) { lang.sendWithPrefix(player, "pet.info.no_target"); return true; }
            lang.sendWithPrefix(player, "pet.info.header");
            String name = pet.getCustomName() != null ? pet.getCustomName() : lang.get("pet.list.unnamed");
            player.sendMessage(lang.get("pet.info.name", name));
            player.sendMessage(lang.get("pet.info.type", pet.getType() != null ? pet.getType() : "?"));
            player.sendMessage(pet.isAlive() ? lang.get("pet.info.status_alive") : lang.get("pet.info.status_dead"));
            player.sendMessage(lang.get("pet.info.health", pet.getHealth(), pet.getMaxHealth()));
            if (pet.getBoundFarmIndex() != null) {
                List<com.icenci.mypets.model.FarmData> farms = data.loadPlayerFarms(player.getUniqueId().toString());
                if (pet.getBoundFarmIndex() < farms.size()) player.sendMessage(lang.get("pet.info.farm_bound", farms.get(pet.getBoundFarmIndex()).getName()));
                else player.sendMessage(lang.get("pet.info.farm_deleted"));
            } else player.sendMessage(lang.get("pet.info.farm_none"));
            return true;
        }

        // rename
        if (sub.equals("rename")) {
            if (args.length < 2) { lang.sendWithPrefix(player, "pet.rename.usage"); return true; }
            PetData pet = getTargetPet(player, args.length > 2 ? args[2] : null);
            if (pet == null) { lang.sendWithPrefix(player, "pet.rename.no_target"); return true; }
            Entity entity = Bukkit.getEntity(UUID.fromString(pet.getUuid()));
            if (entity instanceof LivingEntity) { ((LivingEntity) entity).setCustomName(args[1]); ((LivingEntity) entity).setCustomNameVisible(true); }
            pet.setCustomName(args[1]);
            List<PetData> pets = data.loadPlayerPets(player.getUniqueId().toString());
            for (PetData p : pets) if (p.getUuid().equals(pet.getUuid())) { p.setCustomName(args[1]); break; }
            data.savePlayerPets(player.getUniqueId().toString(), pets);
            lang.sendWithPrefix(player, "pet.rename.success", args[1]);
            return true;
        }

        // kill
        if (sub.equals("kill")) {
            if (!player.hasPermission("mypets.command.pet.kill")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            PetData pet = getTargetPet(player, args.length > 1 ? args[1] : null);
            if (pet == null) { lang.sendWithPrefix(player, "pet.kill.no_target"); return true; }
            data.removePetFromData(player.getUniqueId().toString(), pet.getUuid());
            Entity entity = Bukkit.getEntity(UUID.fromString(pet.getUuid()));
            if (entity instanceof LivingEntity && !entity.isDead()) ((LivingEntity) entity).damage(99999);
            lang.sendWithPrefix(player, "pet.kill.success", pet.getCustomName() != null ? pet.getCustomName() : lang.get("pet.list.unnamed"));
            return true;
        }

        // release
        if (sub.equals("release")) {
            if (!player.hasPermission("mypets.command.pet.release")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            PetData pet = getTargetPet(player, args.length > 1 ? args[1] : null);
            if (pet == null) { lang.sendWithPrefix(player, "pet.release.no_target"); return true; }
            Entity entity = Bukkit.getEntity(UUID.fromString(pet.getUuid()));
            if (entity instanceof Tameable) { ((Tameable) entity).setOwner(null); ((Tameable) entity).setTamed(false); if (entity instanceof Sittable) ((Sittable) entity).setSitting(false); }
            data.removePetFromData(player.getUniqueId().toString(), pet.getUuid());
            lang.sendWithPrefix(player, "pet.release_success", pet.getCustomName() != null ? pet.getCustomName() : lang.get("pet.list.unnamed"));
            return true;
        }

        // summon
        if (sub.equals("summon")) {
            if (args.length < 2) { lang.sendWithPrefix(player, "pet.summon.usage"); return true; }
            PetData pet = getTargetPet(player, args[1]);
            if (pet == null || !pet.isAlive()) { lang.sendWithPrefix(player, "pet.summon.dead"); return true; }
            Entity entity = Bukkit.getEntity(UUID.fromString(pet.getUuid()));
            if (entity == null) { lang.sendWithPrefix(player, "pet.summon.not_loaded"); return true; }
            entity.teleport(player.getLocation());
            lang.sendWithPrefix(player, "pet.summon.success", pet.getCustomName() != null ? pet.getCustomName() : lang.get("pet.list.unnamed"));
            return true;
        }

        // recall
        if (sub.equals("recall")) {
            if (args.length < 2) { lang.sendWithPrefix(player, "pet.recall.usage"); return true; }
            PetData pet = getTargetPet(player, args[1]);
            if (pet == null || !pet.isAlive()) { lang.sendWithPrefix(player, "pet.recall.dead"); return true; }
            if (pet.getBoundFarmIndex() == null) { lang.sendWithPrefix(player, "pet.recall.no_farm"); return true; }
            List<com.icenci.mypets.model.FarmData> farms = data.loadPlayerFarms(player.getUniqueId().toString());
            if (pet.getBoundFarmIndex() >= farms.size()) { lang.sendWithPrefix(player, "pet.recall.farm_lost"); return true; }
            Entity entity = Bukkit.getEntity(UUID.fromString(pet.getUuid()));
            if (entity == null) { lang.sendWithPrefix(player, "pet.recall.not_loaded"); return true; }
            com.icenci.mypets.model.FarmData farm = farms.get(pet.getBoundFarmIndex());
            entity.teleport(new Location(Bukkit.getWorld(farm.getWorld()),
                (farm.getPos1().get("x") + farm.getPos2().get("x")) / 2.0,
                Math.max(farm.getPos1().get("y"), farm.getPos2().get("y")),
                (farm.getPos1().get("z") + farm.getPos2().get("z")) / 2.0));
            lang.sendWithPrefix(player, "pet.recall.success", pet.getCustomName() != null ? pet.getCustomName() : lang.get("pet.list.unnamed"), farm.getName());
            return true;
        }

        // setfarm
        if (sub.equals("setfarm")) {
            if (!player.hasPermission("mypets.command.pet.setfarm")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            if (!plugin.getConfigManager().isPremium()) { lang.sendWithPrefix(player, "pet.setfarm.premium_only"); return true; }
            if (args.length < 2) { lang.sendWithPrefix(player, "pet.setfarm.usage"); return true; }
            String uuid = player.getUniqueId().toString();
            // 获取目标宠物：优先骑乘的，其次准星对准且坐下的
            PetData targetPet = null;
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof Tameable) {
                Tameable t = (Tameable) vehicle;
                if (t.isTamed() && t.getOwner() == player) {
                    targetPet = findPetByUuid(data, uuid, vehicle.getUniqueId().toString());
                }
            }
            if (targetPet == null) {
                Entity target = player.getTargetEntity(5);
                if (target instanceof Tameable) {
                    Tameable t = (Tameable) target;
                    if (t.isTamed() && t.getOwner() == player && isSitting(target)) {
                        targetPet = findPetByUuid(data, uuid, target.getUniqueId().toString());
                    }
                }
            }
            if (targetPet == null) { lang.sendWithPrefix(player, "pet.setfarm.no_target"); return true; }
            // 解析农场序号
            int farmIndex;
            try { farmIndex = Integer.parseInt(args[1]) - 1; } catch (NumberFormatException e) { lang.sendWithPrefix(player, "common.invalid_index"); return true; }
            List<com.icenci.mypets.model.FarmData> farms = data.loadPlayerFarms(uuid);
            if (farmIndex < 0 || farmIndex >= farms.size()) { lang.sendWithPrefix(player, "pet.setfarm.farm_not_found"); return true; }
            // 绑定
            List<PetData> pets = data.loadPlayerPets(uuid);
            for (PetData p : pets) {
                if (p.getUuid().equals(targetPet.getUuid())) { p.setBoundFarmIndex(farmIndex); break; }
            }
            data.savePlayerPets(uuid, pets);
            String petName = targetPet.getCustomName() != null ? targetPet.getCustomName() : lang.get("pet.list.unnamed");
            lang.sendWithPrefix(player, "pet.setfarm.success", petName, farms.get(farmIndex).getName());
            return true;
        }

        // share
        if (sub.equals("share")) {
            if (!player.hasPermission("mypets.command.pet.share")) { lang.sendWithPrefix(player, "common.no_permission"); return true; }
            if (!plugin.getConfigManager().isPremium()) { lang.sendWithPrefix(player, "pet.share.premium_only"); return true; }
            Entity vehicle = player.getVehicle();
            if (vehicle == null || !(vehicle instanceof LivingEntity)) { lang.sendWithPrefix(player, "pet.share.no_mount"); return true; }
            String petUuid = vehicle.getUniqueId().toString();
            PetData targetPet = findPetByUuid(data, player.getUniqueId().toString(), petUuid);
            if (targetPet == null || targetPet.getOwnerUuid() == null || !targetPet.getOwnerUuid().equals(player.getUniqueId().toString())) { lang.sendWithPrefix(player, "pet.share.no_mount"); return true; }
            if (args.length >= 2) {
                String action = args[1].toLowerCase();
                if (action.equals("add")) {
                    if (args.length < 3) { lang.sendWithPrefix(player, "pet.share.add.usage"); return true; }
                    Player target = Bukkit.getPlayer(args[2]);
                    if (target == null || !target.isOnline()) { lang.sendWithPrefix(player, "gui.share.player_offline"); return true; }
                    if (data.addSharedPlayer(player.getUniqueId().toString(), petUuid, target.getUniqueId().toString())) {
                        lang.sendWithPrefix(player, "gui.share.add_success", target.getName());
                        lang.sendWithPrefix(target, "gui.share.received", player.getName());
                    } else lang.sendWithPrefix(player, "gui.share.already_shared");
                    return true;
                } else if (action.equals("remove")) {
                    if (args.length < 3) { lang.sendWithPrefix(player, "pet.share.remove.usage"); return true; }
                    Player target = Bukkit.getPlayer(args[2]);
                    String targetUuid = (target != null) ? target.getUniqueId().toString() : args[2];
                    if (data.removeSharedPlayer(player.getUniqueId().toString(), petUuid, targetUuid)) {
                        lang.sendWithPrefix(player, "gui.share.remove_success", args[2]);
                        if (target != null && target.isOnline()) lang.sendWithPrefix(target, "gui.share.removed_notify", player.getName());
                    } else lang.sendWithPrefix(player, "gui.share.not_shared");
                    return true;
                }
            }
            List<String> shared = targetPet.getSharedPlayers();
            if (shared == null || shared.isEmpty()) lang.sendWithPrefix(player, "pet.share.no_shared");
            else {
                lang.sendWithPrefix(player, "pet.share.list_header");
                for (String suid : shared) { Player p = Bukkit.getPlayer(UUID.fromString(suid)); player.sendMessage(lang.get("pet.share.list_entry", p != null ? p.getName() : suid)); }
            }
            lang.sendWithPrefix(player, "pet.share.hint"); return true;
        }

        // help (新增)
        if (sub.equals("help")) {
            lang.sendWithPrefix(player, "pet.menu.header");
            player.sendMessage(lang.get("pet.menu.capture"));
            player.sendMessage(lang.get("pet.menu.list"));
            player.sendMessage(lang.get("pet.menu.info"));
            player.sendMessage(lang.get("pet.menu.rename"));
            player.sendMessage(lang.get("pet.menu.summon"));
            player.sendMessage(lang.get("pet.menu.recall"));
            player.sendMessage(lang.get("pet.menu.kill"));
            player.sendMessage(lang.get("pet.menu.release"));
            return true;
        }

        lang.sendWithPrefix(player, "pet.menu.closed");
        return true;
    }

    private PetData findPetByUuid(DataManager data, String ownerUuid, String petUuid) {
        for (PetData p : data.loadPlayerPets(ownerUuid)) if (petUuid.equals(p.getUuid())) return p;
        return null;
    }

    private boolean isSitting(Entity entity) {
        return entity instanceof Sittable && ((Sittable) entity).isSitting();
    }

    private PetData getTargetPet(Player player, String indexStr) {
        DataManager data = plugin.getDataManager();
        String uuid = player.getUniqueId().toString();
        Entity target = player.getTargetEntity(5);
        if (target instanceof Tameable) {
            Tameable tameable = (Tameable) target;
            if (tameable.isTamed() && tameable.getOwner() == player) {
                for (PetData p : data.loadPlayerPets(uuid)) if (p.getUuid().equals(target.getUniqueId().toString())) return p;
            }
        }
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof Tameable) {
            Tameable tameable = (Tameable) vehicle;
            if (tameable.isTamed() && tameable.getOwner() == player) {
                for (PetData p : data.loadPlayerPets(uuid)) if (p.getUuid().equals(vehicle.getUniqueId().toString())) return p;
            }
        }
        if (indexStr != null) {
            try {
                int index = Integer.parseInt(indexStr);
                List<PetData> pets = data.loadPlayerPets(uuid);
                if (index >= 1 && index <= pets.size()) return pets.get(index - 1);
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }
}