package com.icenci.mypets.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.model.PetData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {

    private final JavaPlugin plugin;
    private final Gson gson;
    private final File dataFolder;

    public DataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    // ========== 宠物数据 ==========

    private File getPetsFile(String uuid) {
        return new File(dataFolder, "pets/" + uuid + ".json");
    }

    public List<PetData> loadPlayerPets(String uuid) {
        File file = getPetsFile(uuid);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<PetData>>(){}.getType();
            List<PetData> pets = gson.fromJson(reader, listType);
            return pets != null ? pets : new ArrayList<>();
        } catch (Exception e) {
            plugin.getLogger().warning("加载宠物数据失败: " + uuid + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void savePlayerPets(String uuid, List<PetData> pets) {
        File file = getPetsFile(uuid);
        file.getParentFile().mkdirs();
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(pets, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("保存宠物数据失败: " + uuid + " - " + e.getMessage());
        }
    }

    // ========== 农场数据 ==========

    private File getFarmsFile(String uuid) {
        return new File(dataFolder, "farms/" + uuid + ".json");
    }

    public List<FarmData> loadPlayerFarms(String uuid) {
        File file = getFarmsFile(uuid);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<FarmData>>(){}.getType();
            List<FarmData> farms = gson.fromJson(reader, listType);
            return farms != null ? farms : new ArrayList<>();
        } catch (Exception e) {
            plugin.getLogger().warning("加载农场数据失败: " + uuid + " - " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public void savePlayerFarms(String uuid, List<FarmData> farms) {
        File file = getFarmsFile(uuid);
        file.getParentFile().mkdirs();
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(farms, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("保存农场数据失败: " + uuid + " - " + e.getMessage());
        }
    }

    // ========== 工具方法 ==========

    public boolean removePetFromData(String ownerUuid, String petUuid) {
        List<PetData> pets = loadPlayerPets(ownerUuid);
        boolean removed = pets.removeIf(p -> petUuid.equals(p.getUuid()));
        if (removed) {
            savePlayerPets(ownerUuid, pets);
        }
        return removed;
    }

    public void bindPetToFarm(String uuid, String petUuid, int farmIndex) {
        List<PetData> pets = loadPlayerPets(uuid);
        for (PetData pet : pets) {
            if (petUuid.equals(pet.getUuid())) {
                pet.setBoundFarmIndex(farmIndex);
                savePlayerPets(uuid, pets);
                return;
            }
        }
    }

    public int countAlivePetsInFarm(String ownerUuid, int farmIndex) {
        List<PetData> pets = loadPlayerPets(ownerUuid);
        int count = 0;
        for (PetData pet : pets) {
            if (pet.isAlive() && pet.getBoundFarmIndex() != null
                    && pet.getBoundFarmIndex() == farmIndex) {
                count++;
            }
        }
        return count;
    }

    public Integer getFirstFarmIndex(String uuid) {
        List<FarmData> farms = loadPlayerFarms(uuid);
        return farms.isEmpty() ? null : 0;
    }

    public boolean checkFarmOverlap(String world, Map<String, Integer> pos1, Map<String, Integer> pos2) {
        File[] files = new File(dataFolder, "farms").listFiles();
        if (files == null) return false;

        for (File file : files) {
            if (!file.getName().endsWith(".json")) continue;
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<FarmData>>(){}.getType();
                List<FarmData> farms = gson.fromJson(reader, listType);
                if (farms == null) continue;

                for (FarmData existing : farms) {
                    if (!world.equals(existing.getWorld())) continue;
                    if (isOverlapping(existing.getPos1(), existing.getPos2(), pos1, pos2)) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean isOverlapping(Map<String, Integer> p1_1, Map<String, Integer> p2_1,
                                   Map<String, Integer> p1_2, Map<String, Integer> p2_2) {
        int minX1 = Math.min(p1_1.get("x"), p2_1.get("x"));
        int maxX1 = Math.max(p1_1.get("x"), p2_1.get("x"));
        int minY1 = Math.min(p1_1.get("y"), p2_1.get("y"));
        int maxY1 = Math.max(p1_1.get("y"), p2_1.get("y"));
        int minZ1 = Math.min(p1_1.get("z"), p2_1.get("z"));
        int maxZ1 = Math.max(p1_1.get("z"), p2_1.get("z"));

        int minX2 = Math.min(p1_2.get("x"), p2_2.get("x"));
        int maxX2 = Math.max(p1_2.get("x"), p2_2.get("x"));
        int minY2 = Math.min(p1_2.get("y"), p2_2.get("y"));
        int maxY2 = Math.max(p1_2.get("y"), p2_2.get("y"));
        int minZ2 = Math.min(p1_2.get("z"), p2_2.get("z"));
        int maxZ2 = Math.max(p1_2.get("z"), p2_2.get("z"));

        return (maxX1 >= minX2 && maxX2 >= minX1) &&
               (maxY1 >= minY2 && maxY2 >= minY1) &&
               (maxZ1 >= minZ2 && maxZ2 >= minZ1);
    }

    public void rebindPetsAfterFarmRemoval(String ownerUuid, int removedIndex) {
        List<PetData> pets = loadPlayerPets(ownerUuid);
        List<FarmData> farms = loadPlayerFarms(ownerUuid);
        boolean changed = false;

        for (PetData pet : pets) {
            Integer idx = pet.getBoundFarmIndex();
            if (idx == null) continue;

            if (idx == removedIndex) {
                // 宠物绑定到被删除的农场 → 重新绑定到其他农场或解绑
                if (!farms.isEmpty()) {
                    pet.setBoundFarmIndex(0);
                } else {
                    pet.setBoundFarmIndex(null);
                }
                changed = true;
            } else if (idx > removedIndex) {
                // 宠物绑定到被删除农场之后的索引 → 索引减1
                pet.setBoundFarmIndex(idx - 1);
                changed = true;
            }
        }

        if (changed) {
            savePlayerPets(ownerUuid, pets);
        }
    }

    /**
     * 根据索引获取农场数据
     */
    public FarmData getFarmByIndex(String ownerUuid, int index) {
        List<FarmData> farms = loadPlayerFarms(ownerUuid);
        if (index < 0 || index >= farms.size()) return null;
        return farms.get(index);
    }

    // ========== 宠物查询与共享方法 ==========

    public String findOwnerUuidByPetUuid(String petUuid) {
        File petsFolder = new File(dataFolder, "pets");
        if (!petsFolder.exists() || !petsFolder.isDirectory()) {
            return null;
        }

        File[] files = petsFolder.listFiles();
        if (files == null) return null;

        for (File file : files) {
            if (!file.getName().endsWith(".json")) continue;
            String ownerUuid = file.getName().replace(".json", "");
            List<PetData> pets = loadPlayerPets(ownerUuid);
            for (PetData pet : pets) {
                if (petUuid.equals(pet.getUuid())) {
                    return ownerUuid;
                }
            }
        }
        return null;
    }

    public boolean isPlayerShared(String ownerUuid, String petUuid, String targetUuid) {
        List<PetData> pets = loadPlayerPets(ownerUuid);
        for (PetData pet : pets) {
            if (petUuid.equals(pet.getUuid())) {
                List<String> shared = pet.getSharedPlayers();
                return shared != null && shared.contains(targetUuid);
            }
        }
        return false;
    }

    public boolean addSharedPlayer(String ownerUuid, String petUuid, String targetUuid) {
        List<PetData> pets = loadPlayerPets(ownerUuid);
        for (PetData pet : pets) {
            if (petUuid.equals(pet.getUuid())) {
                List<String> shared = pet.getSharedPlayers();
                if (shared == null) {
                    shared = new ArrayList<>();
                    pet.setSharedPlayers(shared);
                }
                if (shared.contains(targetUuid)) return false;
                shared.add(targetUuid);
                savePlayerPets(ownerUuid, pets);
                return true;
            }
        }
        return false;
    }

    public boolean removeSharedPlayer(String ownerUuid, String petUuid, String targetUuid) {
        List<PetData> pets = loadPlayerPets(ownerUuid);
        for (PetData pet : pets) {
            if (petUuid.equals(pet.getUuid())) {
                List<String> shared = pet.getSharedPlayers();
                if (shared != null && shared.remove(targetUuid)) {
                    savePlayerPets(ownerUuid, pets);
                    return true;
                }
                return false;
            }
        }
        return false;
    }

    // ========== 孤立保险数据（农场删除后保留） ==========

    private File getInsuranceFile(String uuid) {
        return new File(dataFolder, "insurance/" + uuid + ".json");
    }

    /**
     * 保存从已删除农场遗留的保险箱物品（合并到已有孤立数据）
     */
    public void saveOrphanedInsurance(String uuid, Map<Integer, String> pages) {
        if (pages == null || pages.isEmpty()) return;
        File file = getInsuranceFile(uuid);
        file.getParentFile().mkdirs();

        // 加载已有孤立数据，合并新页面（偏移页码避免冲突）
        Map<Integer, String> merged = new HashMap<>();
        if (file.exists()) {
            try (InputStreamReader reader = new InputStreamReader(
                    new FileInputStream(file), StandardCharsets.UTF_8)) {
                Type mapType = new TypeToken<Map<Integer, String>>(){}.getType();
                Map<Integer, String> existing = gson.fromJson(reader, mapType);
                if (existing != null) merged.putAll(existing);
            } catch (Exception e) {
                plugin.getLogger().warning("加载已有孤立保险数据失败: " + uuid + " - " + e.getMessage());
            }
        }

        // 找到最大页码，新数据偏移后合并
        int offset = merged.keySet().stream().max(Integer::compareTo).orElse(-1) + 1;
        for (Map.Entry<Integer, String> entry : pages.entrySet()) {
            merged.put(offset + entry.getKey(), entry.getValue());
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            gson.toJson(merged, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("保存孤立保险数据失败: " + uuid + " - " + e.getMessage());
        }
    }

    /**
     * 取出并清除孤立保险数据（取出后删除文件）
     */
    public Map<Integer, String> takeOrphanedInsurance(String uuid) {
        File file = getInsuranceFile(uuid);
        if (!file.exists()) return null;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            Type mapType = new TypeToken<Map<Integer, String>>(){}.getType();
            Map<Integer, String> pages = gson.fromJson(reader, mapType);
            file.delete(); // 取出后删除
            return pages;
        } catch (Exception e) {
            plugin.getLogger().warning("加载孤立保险数据失败: " + uuid + " - " + e.getMessage());
            return null;
        }
    }
}