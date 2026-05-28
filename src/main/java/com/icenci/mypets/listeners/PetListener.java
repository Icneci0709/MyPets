package com.icenci.mypets.listeners;

import com.icenci.mypets.MyPets;
import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.model.PetData;
import com.icenci.mypets.pets.RideablePetAdapter;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PetListener implements Listener {

    private final MyPets plugin;
    private final LangManager lang;
    private final DataManager data;
    private final List<RideablePetAdapter> adapters;
    private final Map<String, Integer> resurrectionTasks = new HashMap<>(); // petUuid -> taskId

    public PetListener(MyPets plugin, List<RideablePetAdapter> adapters) {
        this.plugin = plugin;
        this.lang = plugin.getLangManager();
        this.data = plugin.getDataManager();
        this.adapters = adapters;
        startRideCheckTask();
        startFarmHealTask();
    }

    private void startRideCheckTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    Entity vehicle = player.getVehicle();
                    if (!(vehicle instanceof LivingEntity)) continue;

                    LivingEntity mount = (LivingEntity) vehicle;
                    // 只检查已驯服的坐骑（有主人的）
                    if (!(mount instanceof Tameable)) continue;
                    Tameable tameable = (Tameable) mount;
                    if (!tameable.isTamed() || tameable.getOwner() == null) continue;

                    String petUuid = mount.getUniqueId().toString();
                    String playerUuid = player.getUniqueId().toString();
                    String ownerUuid = data.findOwnerUuidByPetUuid(petUuid);

                    // 无主，不处理
                    if (ownerUuid == null) continue;
                    // 主人，不处理
                    if (ownerUuid.equals(playerUuid)) continue;
                    // 共享者，不处理
                    if (data.isPlayerShared(ownerUuid, petUuid, playerUuid)) continue;
                    // 管理员绕过
                    if (player.hasPermission("mypets.bypass.ride")) continue;

                    // 非法骑乘 → 强制踢下
                    mount.eject();
                    player.sendMessage(lang.get("pet.capture.not_owner"));
                }
            }
        }.runTaskTimer(plugin, 20L, 40L);
    }

    /**
     * 农场回血任务：每2秒恢复农场内宠物1点生命值
     */
    private void startFarmHealTask() {
        if (!plugin.getConfigManager().isPremiumFarmHeal()) return;
        new BukkitRunnable() {
            @Override
            public void run() {
                // 遍历所有宠物文件，不限于在线玩家
                java.io.File petsDir = new java.io.File(plugin.getDataFolder(), "data/pets");
                if (!petsDir.exists()) return;
                for (java.io.File file : petsDir.listFiles()) {
                    if (!file.getName().endsWith(".json")) continue;
                    String uuid = file.getName().replace(".json", "");
                    List<PetData> pets = data.loadPlayerPets(uuid);
                    List<com.icenci.mypets.model.FarmData> farms = data.loadPlayerFarms(uuid);
                    boolean changed = false;
                    for (PetData pet : pets) {
                        if (!pet.isAlive()) continue;
                        Integer idx = pet.getBoundFarmIndex();
                        if (idx == null || idx >= farms.size()) continue;
                        com.icenci.mypets.model.FarmData farm = farms.get(idx);
                        Entity entity = Bukkit.getEntity(java.util.UUID.fromString(pet.getUuid()));
                        if (!(entity instanceof LivingEntity)) continue;
                        if (!entity.getWorld().getName().equals(farm.getWorld())) continue;
                        if (!com.icenci.mypets.farm.FarmStructureManager.isInsideFarm(farm, entity.getLocation())) continue;
                        LivingEntity living = (LivingEntity) entity;
                        if (living.getHealth() < living.getMaxHealth()) {
                            double newHealth = Math.min(living.getHealth() + 1, living.getMaxHealth());
                            living.setHealth(newHealth);
                            pet.setHealth(newHealth);
                            changed = true;
                        }
                    }
                    if (changed) data.savePlayerPets(uuid, pets);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // 2秒后开始，每2秒
    }

    // 同时保留事件阻止，作为第一道防线
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        if (!(event.getMount() instanceof LivingEntity)) return;

        Player player = (Player) event.getEntity();
        LivingEntity mount = (LivingEntity) event.getMount();

        // 只处理已驯服的坐骑
        if (!(mount instanceof Tameable)) return;
        Tameable tameable = (Tameable) mount;
        if (!tameable.isTamed() || tameable.getOwner() == null) return;

        String petUuid = mount.getUniqueId().toString();
        String playerUuid = player.getUniqueId().toString();
        String ownerUuid = data.findOwnerUuidByPetUuid(petUuid);

        if (ownerUuid == null) return;
        if (ownerUuid.equals(playerUuid)) return;
        if (data.isPlayerShared(ownerUuid, petUuid, playerUuid)) return;
        if (player.hasPermission("mypets.bypass.ride")) return;

        event.setCancelled(true);
        player.sendMessage(lang.get("pet.capture.not_owner"));
    }

    // ========== 禁止栓走别人的宠物 ==========
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity)) return;
        Player player = event.getPlayer();
        String petUuid = entity.getUniqueId().toString();
        String ownerUuid = data.findOwnerUuidByPetUuid(petUuid);
        if (ownerUuid == null) return;
        // 主人自己可以栓
        if (ownerUuid.equals(player.getUniqueId().toString())) return;
        // 共享玩家可以栓
        if (data.isPlayerShared(ownerUuid, petUuid, player.getUniqueId().toString())) return;
        // 管理员绕过
        if (player.hasPermission("mypets.bypass.leash")) return;
        event.setCancelled(true);
        player.sendMessage(lang.get("pet.leash.not_owner"));
    }

    @EventHandler
    public void onTame(EntityTameEvent event) {
        LivingEntity entity = event.getEntity();
        AnimalTamer owner = event.getOwner();
        if (!(owner instanceof Player)) return;
        registerPet((Player) owner, entity);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity target = event.getRightClicked();
        if (!(target instanceof Tameable)) return;
        Tameable tameable = (Tameable) target;
        if (!tameable.isTamed() || tameable.getOwner() == null) return;
        if (!tameable.getOwner().getUniqueId().equals(player.getUniqueId())) return;
        String petUuid = target.getUniqueId().toString();
        // 已注册则跳过
        if (data.findOwnerUuidByPetUuid(petUuid) != null) return;
        registerPet(player, (LivingEntity) target);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Tameable)) return;
        Tameable mount = (Tameable) vehicle;
        if (!mount.isTamed() || mount.getOwner() == null) return;
        if (!mount.getOwner().getUniqueId().equals(player.getUniqueId())) return;
        String petUuid = ((Entity) mount).getUniqueId().toString();
        if (isAlreadyRegistered(player, petUuid)) return;
        if (data.findOwnerUuidByPetUuid(petUuid) != null) return;
        registerPet(player, (LivingEntity) mount);
    }

    // ========== 骑乘传送 + 栓绳传送 ==========
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (to == null) return;

        // 骑乘传送（高级版）
        if (plugin.getConfigManager().isPremiumRideTeleport()) {
            Entity vehicle = player.getVehicle();
            if (vehicle instanceof LivingEntity && vehicle.isValid() && !vehicle.isDead()) {
                vehicle.eject();
                vehicle.teleport(to);
                // 延迟一 tick 重新骑上，避免异步问题
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (vehicle.isValid() && !vehicle.isDead() && player.isOnline()) {
                        vehicle.addPassenger(player);
                    }
                });
                return;
            }
        }

        boolean isPremium = plugin.getConfigManager().isPremiumLeashTeleport();
        int maxLeashed = isPremium ? Integer.MAX_VALUE : 1;
        int count = 0;

        // 遍历玩家附近的实体，找出被该玩家拴绳拴住的生物
        for (Entity entity : player.getNearbyEntities(10, 10, 10)) {
            if (!(entity instanceof LivingEntity)) continue;
            if (!entity.isValid() || entity.isDead()) continue;
            if (!isLeashedTo(entity, player)) continue;

            count++;
            if (count > maxLeashed) break;

            // 传送该生物到目的地
            Location dest = to.clone();
            // 稍微偏移避免与玩家重叠
            dest.add(entity.getLocation().subtract(player.getLocation()).toVector().normalize().multiply(2));
            entity.teleport(dest);
        }

        if (count > maxLeashed) {
            player.sendMessage(lang.get("pet.leash_teleport.limit", maxLeashed, count));
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isLeashedTo(Entity entity, Player player) {
        return entity instanceof LivingEntity && ((LivingEntity) entity).isLeashed()
            && player.equals(((LivingEntity) entity).getLeashHolder());
    }

    private void registerPet(Player player, LivingEntity entity) {
        String uuid = player.getUniqueId().toString();
        String petUuid = entity.getUniqueId().toString();
        try {
            if (data.findOwnerUuidByPetUuid(petUuid) == null) {
                if (entity instanceof Tameable) {
                    Tameable tameable = (Tameable) entity;
                    tameable.setTamed(true);
                    tameable.setOwner(player);
                }
            }
            PetData petData = extractPetInfo(entity);
            petData.setUuid(petUuid);
            petData.setOwnerUuid(uuid);
            List<PetData> pets = data.loadPlayerPets(uuid);
            boolean found = false;
            for (PetData existing : pets) {
                if (petUuid.equals(existing.getUuid())) { copyPetInfo(petData, existing); petData = existing; found = true; break; }
            }
            if (!found) pets.add(petData);
            // 自动绑定到玩家第一个农场
            if (petData.getBoundFarmIndex() == null) {
                Integer firstFarm = data.getFirstFarmIndex(uuid);
                if (firstFarm != null) {
                    petData.setBoundFarmIndex(firstFarm);
                }
            }
            data.savePlayerPets(uuid, pets);
            // 基础版宠物上限检查
            CaptureListener.enforcePetLimit(plugin, entity, petData, player);
        } catch (Exception e) {
            plugin.getLogger().severe("注册宠物失败: " + e.getMessage());
        }
    }

    private boolean isAlreadyRegistered(Player player, String petUuid) {
        for (PetData p : data.loadPlayerPets(player.getUniqueId().toString())) if (petUuid.equals(p.getUuid())) return true;
        return false;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Tameable)) return;
        Tameable tameable = (Tameable) entity;
        if (!tameable.isTamed()) return;
        AnimalTamer owner = tameable.getOwner();
        if (!(owner instanceof Player)) return;
        Player player = (Player) owner;
        String uuid = player.getUniqueId().toString();
        String petUuid = entity.getUniqueId().toString();
        List<PetData> pets = data.loadPlayerPets(uuid);
        PetData petRecord = null;
        for (PetData p : pets) if (petUuid.equals(p.getUuid())) { petRecord = p; break; }
        if (petRecord == null) return;
        Integer farmIndex = petRecord.getBoundFarmIndex();
        if (farmIndex == null) {
            // 没有绑定农场，直接删除宠物数据
            pets.remove(petRecord);
            data.savePlayerPets(uuid, pets);
            return;
        }
        // 清除掉落并标记死亡
        event.getDrops().clear();
        petRecord.setAlive(false);
        data.savePlayerPets(uuid, pets);
        // 更新宠物属性（死亡时的最终状态）
        updatePetDataFromEntity(petRecord, entity);

        // 取消之前的复活任务（如果有）
        Integer oldTask = resurrectionTasks.remove(petUuid);
        if (oldTask != null) Bukkit.getScheduler().cancelTask(oldTask);

        // 计划1分钟后复活
        String farmName = data.getFarmByIndex(uuid, farmIndex) != null
            ? data.getFarmByIndex(uuid, farmIndex).getName() : "?";
        player.sendMessage(lang.get("pet.death.will_resurrect", farmName));
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                resurrectionTasks.remove(petUuid);
                resurrectPet(uuid, petUuid);
            }
        }.runTaskLater(plugin, 1200L).getTaskId(); // 60秒 = 1200 ticks
        resurrectionTasks.put(petUuid, taskId);
    }

    /**
     * 复活宠物：在绑定农场的安全位置重新生成
     */
    private void resurrectPet(String ownerUuid, String oldPetUuid) {
        List<PetData> pets = data.loadPlayerPets(ownerUuid);
        PetData petRecord = null;
        for (PetData p : pets) {
            if (oldPetUuid.equals(p.getUuid())) { petRecord = p; break; }
        }
        if (petRecord == null) return;

        Integer farmIndex = petRecord.getBoundFarmIndex();
        // 如果农场已被删除解绑，宠物永久死亡
        if (farmIndex == null) {
            pets.remove(petRecord);
            data.savePlayerPets(ownerUuid, pets);
            Player owner = Bukkit.getPlayer(java.util.UUID.fromString(ownerUuid));
            if (owner != null && owner.isOnline()) {
                owner.sendMessage(lang.get("pet.death.no_farm"));
            }
            return;
        }

        List<FarmData> farms = data.loadPlayerFarms(ownerUuid);
        if (farmIndex < 0 || farmIndex >= farms.size()) {
            // 农场索引无效，永久死亡
            pets.remove(petRecord);
            data.savePlayerPets(ownerUuid, pets);
            return;
        }
        FarmData farm = farms.get(farmIndex);

        // 获取复活位置：优先自定义出生点，否则农场安全中心
        Location spawnLoc = null;
        World world = Bukkit.getWorld(farm.getWorld());
        if (world != null) {
            if (farm.getSpawnX() != null) {
                spawnLoc = new Location(world, farm.getSpawnX() + 0.5, farm.getSpawnY(), farm.getSpawnZ() + 0.5);
            } else {
                // 使用农场中心安全位置
                com.icenci.mypets.farm.FarmManager fm = new com.icenci.mypets.farm.FarmManager(data);
                spawnLoc = fm.getFarmSafeCenter(farm);
            }
        }

        if (spawnLoc == null) {
            // 无法获取安全位置，使用箱子位置作为后备
            if (farm.getChestX() != null && world != null) {
                spawnLoc = new Location(world, farm.getChestX() + 0.5, farm.getChestY() + 1, farm.getChestZ() + 0.5);
            }
        }

        if (spawnLoc == null) {
            plugin.getLogger().warning("无法为宠物 " + oldPetUuid + " 找到复活位置");
            return;
        }

        // 生成新实体
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(petRecord.getType());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("未知实体类型: " + petRecord.getType());
            return;
        }

        LivingEntity newEntity = (LivingEntity) spawnLoc.getWorld().spawnEntity(spawnLoc, entityType);
        applyPetAttributes(newEntity, petRecord);
        // 复活后仅 1 点生命值，由农场回血任务逐步恢复
        newEntity.setHealth(1.0);
        petRecord.setHealth(1.0);

        // 更新宠物UUID并标记为存活
        String newUuid = newEntity.getUniqueId().toString();
        petRecord.setUuid(newUuid);
        petRecord.setAlive(true);
        data.savePlayerPets(ownerUuid, pets);

        // 通知主人
        Player owner = Bukkit.getPlayer(java.util.UUID.fromString(ownerUuid));
        if (owner != null && owner.isOnline()) {
            String petName = petRecord.getCustomName() != null ? petRecord.getCustomName() : petRecord.getType();
            owner.sendMessage(lang.get("pet.death.resurrected", petName));
        }
    }

    /**
     * 取消指定农场所有待复活任务
     */
    public void cancelResurrectionsForFarm(String ownerUuid, int farmIndex) {
        List<PetData> pets = data.loadPlayerPets(ownerUuid);
        for (PetData pet : pets) {
            if (!pet.isAlive() && pet.getBoundFarmIndex() != null && pet.getBoundFarmIndex() == farmIndex) {
                Integer taskId = resurrectionTasks.remove(pet.getUuid());
                if (taskId != null) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            }
        }
    }

    private void updatePetDataFromEntity(PetData data, LivingEntity entity) {
        data.setType(entity.getType().name());
        data.setCustomName(entity.getCustomName());
        data.setHealth(entity.getHealth());
        data.setMaxHealth(entity.getMaxHealth());
        for (RideablePetAdapter a : adapters) if (a.supports(entity)) { a.extractAttributes(entity, data); break; }
    }

    private void applyPetAttributes(LivingEntity entity, PetData data) {
        if (data.getCustomName() != null) {
            entity.setCustomName(data.getCustomName());
            entity.setCustomNameVisible(true);
        }
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            tameable.setTamed(true);
            // 设置主人
            if (data.getOwnerUuid() != null) {
                Player owner = Bukkit.getPlayer(java.util.UUID.fromString(data.getOwnerUuid()));
                if (owner != null) tameable.setOwner(owner);
            }
        }
        for (RideablePetAdapter a : adapters) {
            if (a.supports(entity)) {
                Player owner = data.getOwnerUuid() != null
                    ? Bukkit.getPlayer(java.util.UUID.fromString(data.getOwnerUuid())) : null;
                a.applyAttributes(entity, data, owner);
                break;
            }
        }
        // 恢复血量
        if (data.getHealth() > 0) entity.setHealth(Math.min(data.getHealth(), entity.getMaxHealth()));
    }

    private PetData extractPetInfo(LivingEntity entity) {
        PetData info = new PetData();
        info.setType(entity.getType().name());
        info.setCustomName(entity.getCustomName());
        info.setHealth(entity.getHealth());
        info.setMaxHealth(entity.getMaxHealth());
        info.setAlive(true);
        if (entity instanceof Tameable) {
            Tameable t = (Tameable) entity;
            if (t.getOwner() instanceof Player) info.setOwnerUuid(t.getOwner().getUniqueId().toString());
        }
        for (RideablePetAdapter a : adapters) if (a.supports(entity)) { a.extractAttributes(entity, info); break; }
        return info;
    }

    private void copyPetInfo(PetData from, PetData to) {
        to.setType(from.getType()); to.setCustomName(from.getCustomName()); to.setHealth(from.getHealth());
        to.setMaxHealth(from.getMaxHealth()); to.setAlive(from.isAlive()); to.setOwnerUuid(from.getOwnerUuid());
        to.setSpeed(from.getSpeed()); to.setJumpStrength(from.getJumpStrength()); to.setSaddle(from.getSaddle());
        to.setColor(from.getColor()); to.setStyle(from.getStyle()); to.setArmorType(from.getArmorType());
        to.setCatType(from.getCatType()); to.setIsSitting(from.getIsSitting()); to.setCollarColor(from.getCollarColor());
        to.setIsAngry(from.getIsAngry()); to.setVariant(from.getVariant()); to.setStrength(from.getStrength());
        to.setCarpetColor(from.getCarpetColor());
    }
}