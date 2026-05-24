package com.icenci.mypets.listeners;

import com.google.gson.Gson;
import com.icenci.mypets.MyPets;
import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.model.PetData;
import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.pets.RideablePetAdapter;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Barrel;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CaptureListener implements Listener {

    private final MyPets plugin;
    private final LangManager lang;
    private final DataManager data;
    private final NamespacedKey petDataKey;
    private final Gson gson;
    private final List<RideablePetAdapter> adapters;

    public CaptureListener(MyPets plugin, List<RideablePetAdapter> adapters) {
        this.plugin = plugin;
        this.lang = plugin.getLangManager();
        this.data = plugin.getDataManager();
        this.petDataKey = new NamespacedKey(plugin, "pet_data");
        this.gson = new Gson();
        this.adapters = adapters;
    }

    // ========== 右键释放封印蛋 ==========
    @EventHandler(priority = EventPriority.LOWEST)
    public void onEggInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
            event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.EGG) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String petDataJson = pdc.get(petDataKey, PersistentDataType.STRING);
        if (petDataJson == null) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();

        PetData petData = gson.fromJson(petDataJson, PetData.class);
        if (petData == null) {
            player.sendMessage(lang.get("pet.release.data_corrupted"));
            return;
        }

        // 检查释放权限
        boolean isOriginalOwner = petData.getOwnerUuid() != null
            && petData.getOwnerUuid().equals(player.getUniqueId().toString());
        if (!isOriginalOwner && !plugin.getConfigManager().isPremium()) {
            player.sendMessage(lang.get("pet.release.not_owner"));
            return;
        }

        Location loc = player.getLocation();
        EntityType entityType = EntityType.fromName(petData.getType());
        if (entityType == null) {
            player.sendMessage(lang.get("pet.release.unknown_type"));
            return;
        }

        LivingEntity entity = (LivingEntity) player.getWorld().spawnEntity(loc, entityType);
        applyPetAttributes(entity, petData, player);

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        player.updateInventory();

        petData.setUuid(entity.getUniqueId().toString());
        petData.setAlive(true);

        // 新主人释放：转移所有权
        if (!isOriginalOwner) {
            petData.setOwnerUuid(player.getUniqueId().toString());
            petData.setBoundFarmIndex(null); // 清除旧农场绑定
        }

        List<PetData> pets = data.loadPlayerPets(player.getUniqueId().toString());
        pets.add(petData);
        data.savePlayerPets(player.getUniqueId().toString(), pets);

        // 绑定农场
        if (isOriginalOwner && petData.getBoundFarmIndex() != null) {
            // 原主人释放：验证原农场是否还存在
            List<FarmData> farms = data.loadPlayerFarms(player.getUniqueId().toString());
            int idx = petData.getBoundFarmIndex();
            if (idx < farms.size()) {
                data.bindPetToFarm(player.getUniqueId().toString(), entity.getUniqueId().toString(), idx);
                player.sendMessage(lang.get("pet.release.bound_restored"));
            } else {
                // 原农场已删除，回退到第一个农场
                Integer firstFarm = data.getFirstFarmIndex(player.getUniqueId().toString());
                if (firstFarm != null) {
                    petData.setBoundFarmIndex(firstFarm);
                    for (PetData p : pets) if (p.getUuid().equals(entity.getUniqueId().toString())) p.setBoundFarmIndex(firstFarm);
                    data.bindPetToFarm(player.getUniqueId().toString(), entity.getUniqueId().toString(), firstFarm);
                    player.sendMessage(lang.get("pet.release.bound_fallback", idx + 1, farms.get(firstFarm).getName()));
                } else {
                    player.sendMessage(lang.get("pet.release.no_farm"));
                }
            }
        } else {
            // 新主人释放 或 原主人无绑定：绑定到第一个农场
            Integer firstFarm = data.getFirstFarmIndex(player.getUniqueId().toString());
            if (firstFarm != null) {
                data.bindPetToFarm(player.getUniqueId().toString(), entity.getUniqueId().toString(), firstFarm);
                if (!isOriginalOwner) {
                    player.sendMessage(lang.get("pet.release.bound_new_owner"));
                }
            } else {
                player.sendMessage(lang.get("pet.release.no_farm"));
            }
        }

        // 基础版宠物上限检查（超限自动转为封印蛋）
        if (enforcePetLimit(plugin, entity, petData, player)) return;

        player.sendMessage(lang.get("pet.release.success"));
    }

    // ========== 阻止封印蛋被投掷 ==========
    @EventHandler(priority = EventPriority.LOWEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Egg)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player player = (Player) event.getEntity().getShooter();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (isPetEgg(item)) {
            event.setCancelled(true);
            player.updateInventory();
            return;
        }
        item = player.getInventory().getItemInOffHand();
        if (isPetEgg(item)) {
            event.setCancelled(true);
            player.updateInventory();
        }
    }

    private boolean isPetEgg(ItemStack item) {
        if (item == null || item.getType() != Material.EGG) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().get(petDataKey, PersistentDataType.STRING) != null;
    }

    // ========== 封印宠物 ==========
    public boolean capturePet(Player player) {
        if (!plugin.getConfigManager().isPremiumEnableCapture()) {
            player.sendMessage(lang.get("pet.capture.premium_only"));
            return false;
        }

        // 优先骑乘的宠物
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity) {
            LivingEntity livingVehicle = (LivingEntity) vehicle;
            if (isSupportedByAnyAdapter(livingVehicle)) {
                captureEntity(player, livingVehicle);
                return true;
            }
        }

        // 准星对准的宠物
        Entity target = player.getTargetEntity(5);
        if (target instanceof Tameable) {
            Tameable tameable = (Tameable) target;
            if (!tameable.isTamed()) {
                player.sendMessage(lang.get("pet.capture.not_tamed"));
                return true;
            }
            if (tameable.getOwner() != player && !player.isOp() &&
                !player.hasPermission("mypets.capture.others")) {
                player.sendMessage(lang.get("pet.capture.not_owner"));
                return true;
            }
            if (target instanceof Cat && !((Cat) target).isSitting()) {
                player.sendMessage(lang.get("pet.capture.cat_not_sitting"));
                return true;
            }
            if (target instanceof Wolf && !((Wolf) target).isSitting()) {
                player.sendMessage(lang.get("pet.capture.wolf_not_sitting"));
                return true;
            }
            if (target instanceof Parrot && !((Parrot) target).isSitting()) {
                player.sendMessage(lang.get("pet.capture.parrot_not_sitting"));
                return true;
            }
            if (target instanceof LivingEntity) {
                captureEntity(player, (LivingEntity) target);
            }
            return true;
        }

        player.sendMessage(lang.get("pet.capture.no_target"));
        return true;
    }

    private boolean isSupportedByAnyAdapter(LivingEntity entity) {
        for (RideablePetAdapter adapter : adapters) {
            if (adapter.supports(entity)) return true;
        }
        return false;
    }

    private void captureEntity(Player player, LivingEntity entity) {
        String ownerUuid = player.getUniqueId().toString();
        String petUuid = entity.getUniqueId().toString();

        List<PetData> pets = data.loadPlayerPets(ownerUuid);
        PetData petRecord = null;
        for (PetData p : pets) {
            if (petUuid.equals(p.getUuid())) {
                petRecord = p;
                break;
            }
        }

        if (petRecord == null) {
            player.sendMessage(lang.get("egg.horse.cannot_read"));
            return;
        }

        updatePetDataFromEntity(petRecord, entity);
        data.removePetFromData(ownerUuid, petUuid);

        ItemStack egg = createEggItem(petRecord, player.getName());
        entity.remove();
        player.getInventory().addItem(egg);
        player.sendMessage(lang.get("egg.horse.captured"));
    }

    private void updatePetDataFromEntity(PetData data, LivingEntity entity) {
        data.setType(entity.getType().name());
        data.setCustomName(entity.getCustomName());
        data.setHealth(entity.getHealth());
        data.setMaxHealth(entity.getMaxHealth());

        for (RideablePetAdapter adapter : adapters) {
            if (adapter.supports(entity)) {
                adapter.extractAttributes(entity, data);
                break;
            }
        }
    }

    private void applyPetAttributes(LivingEntity entity, PetData data, Player player) {
        if (data.getCustomName() != null) {
            entity.setCustomName(data.getCustomName());
            entity.setCustomNameVisible(true);
        }
        if (entity instanceof Tameable) {
            Tameable tameable = (Tameable) entity;
            tameable.setTamed(true);
            tameable.setOwner(player);
        }
        for (RideablePetAdapter adapter : adapters) {
            if (adapter.supports(entity)) {
                adapter.applyAttributes(entity, data, player);
                break;
            }
        }
    }

    // ========== 创建封印蛋（多语言Lore） ==========
    private ItemStack createEggItem(PetData petData, String ownerName) {
        ItemStack item = new ItemStack(Material.EGG, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String type = petData.getType() != null ? petData.getType() : "HORSE";
        String localizedType = getLocalizedName(type);
        meta.setDisplayName("§6§l" + localizedType + lang.get("egg.suffix"));

        List<String> lore = new ArrayList<>();
        lore.add("§7" + lang.get("egg.lore.type") + ": " + localizedType);
        if (petData.getCustomName() != null && !petData.getCustomName().isEmpty()) {
            lore.add("§7" + lang.get("egg.lore.name") + ": §f" + petData.getCustomName());
        }
        lore.add("§7" + lang.get("egg.lore.owner") + ": §f" + ownerName);
        lore.add("§7" + lang.get("egg.lore.health") + ": §c" + (int) petData.getHealth() + "❤");

        // 马匹特有属性
        if (petData.getColor() != null) {
            lore.add("§7" + lang.get("egg.lore.color") + ": §a" + getLocalizedColor("horse", petData.getColor()));
        }
        if (petData.getStyle() != null) {
            lore.add("§7" + lang.get("egg.lore.style") + ": §a" + petData.getStyle());
        }
        if (petData.getArmorType() != null && !petData.getArmorType().equals("null")) {
            lore.add("§7" + lang.get("egg.lore.armor") + ": §a" + petData.getArmorType());
        }
        if (petData.getSpeed() != null) {
            double speedPercent = (petData.getSpeed() / 0.3375) * 100;
            lore.add("§7" + lang.get("egg.lore.speed") + ": §e" + String.format("%.1f%%", speedPercent));
        }
        if (petData.getJumpStrength() != null) {
            lore.add("§7" + lang.get("egg.lore.jump") + ": §a" + String.format("%.1f格", petData.getJumpStrength() * 5.6));
        }

        // 猫特有
        if (petData.getCatType() != null) {
            lore.add("§7" + lang.get("egg.lore.color") + ": §a" + getLocalizedColor("cat", petData.getCatType()));
        }

        // 狼特有
        if (petData.getCollarColor() != null) {
            lore.add("§7" + lang.get("egg.lore.collar") + ": §a" + petData.getCollarColor());
        }

        // 鹦鹉特有
        if (petData.getVariant() != null) {
            lore.add("§7" + lang.get("egg.lore.color") + ": §a" + getLocalizedColor("parrot", petData.getVariant()));
        }

        // 羊驼特有
        if (petData.getCarpetColor() != null) {
            lore.add("§7" + lang.get("egg.lore.carpet") + ": §a" + petData.getCarpetColor());
        }
        if (petData.getStrength() != null) {
            lore.add("§7" + lang.get("egg.lore.strength") + ": §a" + petData.getStrength());
        }

        // 箱子属性
        if (petData.getHasChest() != null && petData.getHasChest()) {
            lore.add("§7" + lang.get("egg.lore.chest") + ": " + lang.get("egg.lore.chest_yes"));
        }

        lore.add("§e" + lang.get("egg.lore.release"));
        meta.setLore(lore);
        item.setItemMeta(meta);

        // 存储完整宠物数据 JSON
        String json = gson.toJson(petData);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(petDataKey, PersistentDataType.STRING, json);
        item.setItemMeta(meta);

        return item;
    }

    /**
     * 获取宠物的本地化名称
     */
    private String getLocalizedName(String type) {
        if (type == null) return lang.get("egg.entity_unknown");
        switch (type.toUpperCase()) {
            case "HORSE": return lang.get("entity.horse.name");
            case "DONKEY": return lang.get("entity.donkey.name");
            case "MULE": return lang.get("entity.mule.name");
            case "SKELETON_HORSE": return lang.get("entity.skeleton_horse.name");
            case "ZOMBIE_HORSE": return lang.get("entity.zombie_horse.name");
            case "LLAMA": return lang.get("entity.llama.name");
            case "TRADER_LLAMA": return lang.get("entity.trader_llama.name");
            case "CAMEL": return lang.get("entity.camel.name");
            case "STRIDER": return lang.get("entity.strider.name");
            case "CAT": return lang.get("entity.cat.name");
            case "WOLF": return lang.get("entity.wolf.name");
            case "PARROT": return lang.get("entity.parrot.name");
            case "ARMADILLO": return lang.get("entity.armadillo.name");
            default: return type;
        }
    }

    /**
     * 获取颜色/品种的本地化名称
     */
    private String getLocalizedColor(String category, String value) {
        if (value == null) return lang.get("egg.color_unknown");
        String key = category + ".color." + value.toLowerCase();
        return lang.get(key);
    }

    // ========== 宠物数量限制（基础版：每个农场最多5只） ==========

    private static final int MAX_PETS_PER_FARM_BASIC = 5;

    /**
     * 检查并强制执行农场宠物上限。超限则将宠物转为封印蛋存入木桶。
     * @return true 表示宠物已被转为蛋（从玩家列表中移除）
     */
    public static boolean enforcePetLimit(MyPets plugin, LivingEntity entity, PetData petData, Player player) {
        Integer farmIndex = petData.getBoundFarmIndex();
        if (farmIndex == null) return false;

        DataManager dm = plugin.getDataManager();
        List<FarmData> farms = dm.loadPlayerFarms(player.getUniqueId().toString());
        if (farmIndex >= farms.size()) return false;

        int maxPets;
        if (plugin.getConfigManager().isPremium()) {
            maxPets = plugin.getConfigManager().getPremiumMaxPetsPerFarm();
        } else {
            maxPets = MAX_PETS_PER_FARM_BASIC;
        }

        int count = dm.countAlivePetsInFarm(player.getUniqueId().toString(), farmIndex);
        if (count <= maxPets) return false;

        // 超限：转为封印蛋
        FarmData farm = farms.get(farmIndex);
        LangManager lm = plugin.getLangManager();

        // 创建封印蛋
        ItemStack egg = createEggItemStatic(petData, player.getName(), lm);
        entity.remove();

        // 存入农场木桶
        boolean stored = false;
        if (farm.getChestX() != null) {
            World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
            if (world != null) {
                org.bukkit.block.Block block = world.getBlockAt(farm.getChestX(), farm.getChestY(), farm.getChestZ());
                if (block.getType() == Material.BARREL && block.getState() instanceof Barrel) {
                    Barrel barrel = (Barrel) block.getState();
                    java.util.Map<Integer, org.bukkit.inventory.ItemStack> leftover = barrel.getInventory().addItem(egg);
                    if (!leftover.isEmpty()) {
                        // 木桶满了，掉落在地上
                        world.dropItemNaturally(block.getLocation(), egg);
                    }
                    stored = true;
                    // 更新木牌蛋数量
                    int newCount = countAlivePetsAndEggsInFarm(dm, player.getUniqueId().toString(), farmIndex, farm);
                    com.icenci.mypets.farm.FarmStructureManager fsm = new com.icenci.mypets.farm.FarmStructureManager(lm);
                    fsm.updateSignEggCount(farm, newCount);
                }
            }
        }
        if (!stored) {
            player.getWorld().dropItemNaturally(player.getLocation(), egg);
        }

        // 从玩家数据中移除
        dm.removePetFromData(player.getUniqueId().toString(), petData.getUuid());
        lm.sendWithPrefix(player, "pet.limit.exceeded", farm.getName(), maxPets);
        return true;
    }

    /**
     * 统计农场木桶中封印蛋数量 + 存活宠物数
     */
    private static int countAlivePetsAndEggsInFarm(DataManager dm, String uuid, int farmIndex, FarmData farm) {
        int eggCount = 0;
        if (farm.getChestX() != null) {
            World world = org.bukkit.Bukkit.getWorld(farm.getWorld());
            if (world != null) {
                org.bukkit.block.Block block = world.getBlockAt(farm.getChestX(), farm.getChestY(), farm.getChestZ());
                if (block.getState() instanceof Barrel) {
                    Barrel barrel = (Barrel) block.getState();
                    for (org.bukkit.inventory.ItemStack item : barrel.getInventory().getContents()) {
                        if (item != null && item.getType() == Material.EGG) {
                            ItemMeta meta = item.getItemMeta();
                            if (meta != null && meta.getPersistentDataContainer()
                                    .get(new NamespacedKey(org.bukkit.Bukkit.getPluginManager().getPlugin("MyPets"), "pet_data"),
                                        PersistentDataType.STRING) != null) {
                                eggCount++;
                            }
                        }
                    }
                }
            }
        }
        return eggCount + dm.countAlivePetsInFarm(uuid, farmIndex);
    }

    /**
     * 静态版封印蛋创建（供外部调用）
     */
    private static ItemStack createEggItemStatic(PetData petData, String ownerName, LangManager lm) {
        ItemStack item = new ItemStack(Material.EGG, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String type = petData.getType() != null ? petData.getType() : "HORSE";
        meta.setDisplayName("§6§l" + type + lm.get("egg.suffix"));

        List<String> lore = new ArrayList<>();
        lore.add("§7" + lm.get("egg.lore.type") + ": " + type);
        if (petData.getCustomName() != null && !petData.getCustomName().isEmpty()) {
            lore.add("§7" + lm.get("egg.lore.name") + ": §f" + petData.getCustomName());
        }
        lore.add("§7" + lm.get("egg.lore.owner") + ": §f" + ownerName);
        lore.add("§7" + lm.get("egg.lore.health") + ": §c" + (int) petData.getHealth() + "❤");
        lore.add("§7" + lm.get("egg.lore.release"));

        meta.setLore(lore);

        // 存储宠物数据
        Gson gson = new Gson();
        NamespacedKey key = new NamespacedKey(
            org.bukkit.Bukkit.getPluginManager().getPlugin("MyPets"), "pet_data");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, gson.toJson(petData));
        item.setItemMeta(meta);
        return item;
    }
}