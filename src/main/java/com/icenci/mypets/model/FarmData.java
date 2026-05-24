package com.icenci.mypets.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 农场数据模型，对应 JSON 中每个农场的数据结构
 */
public class FarmData {

    private String name;
    private String world;
    private Map<String, Integer> pos1;
    private Map<String, Integer> pos2;
    private String ownerName;
    private String ownerUuid;
    private List<String> failedEggs;

    private Integer chestX;
    private Integer chestY;
    private Integer chestZ;

    private Integer signX;
    private Integer signY;
    private Integer signZ;

    private Integer spawnX;
    private Integer spawnY;
    private Integer spawnZ;

    private Map<String, Boolean> flags;

    private Map<String, Map<String, Map<String, Object>>> chestItems;

    // 高级版保险箱多页存储
    private Map<Integer, String> insurancePages;

    private Integer index;

    public FarmData() {
        this.failedEggs = new ArrayList<>();
        this.flags = new HashMap<>();
        this.chestItems = new HashMap<>();
        this.insurancePages = new HashMap<>();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    public Map<String, Integer> getPos1() { return pos1; }
    public void setPos1(Map<String, Integer> pos1) { this.pos1 = pos1; }

    public Map<String, Integer> getPos2() { return pos2; }
    public void setPos2(Map<String, Integer> pos2) { this.pos2 = pos2; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String ownerUuid) { this.ownerUuid = ownerUuid; }

    public List<String> getFailedEggs() { return failedEggs; }
    public void setFailedEggs(List<String> failedEggs) { this.failedEggs = failedEggs; }

    public Integer getChestX() { return chestX; }
    public void setChestX(Integer chestX) { this.chestX = chestX; }

    public Integer getChestY() { return chestY; }
    public void setChestY(Integer chestY) { this.chestY = chestY; }

    public Integer getChestZ() { return chestZ; }
    public void setChestZ(Integer chestZ) { this.chestZ = chestZ; }

    public Integer getSignX() { return signX; }
    public void setSignX(Integer signX) { this.signX = signX; }

    public Integer getSignY() { return signY; }
    public void setSignY(Integer signY) { this.signY = signY; }

    public Integer getSignZ() { return signZ; }
    public void setSignZ(Integer signZ) { this.signZ = signZ; }

    public Integer getSpawnX() { return spawnX; }
    public void setSpawnX(Integer spawnX) { this.spawnX = spawnX; }

    public Integer getSpawnY() { return spawnY; }
    public void setSpawnY(Integer spawnY) { this.spawnY = spawnY; }

    public Integer getSpawnZ() { return spawnZ; }
    public void setSpawnZ(Integer spawnZ) { this.spawnZ = spawnZ; }

    public Map<String, Boolean> getFlags() { return flags; }
    public void setFlags(Map<String, Boolean> flags) { this.flags = flags; }

    public Map<String, Map<String, Map<String, Object>>> getChestItems() { return chestItems; }
    public void setChestItems(Map<String, Map<String, Map<String, Object>>> chestItems) { this.chestItems = chestItems; }

    public Map<Integer, String> getInsurancePages() { return insurancePages; }
    public void setInsurancePages(Map<Integer, String> insurancePages) { this.insurancePages = insurancePages; }

    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }
}