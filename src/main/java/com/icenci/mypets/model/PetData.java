package com.icenci.mypets.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 宠物数据模型，对应 JSON 中每个宠物的数据结构
 */
public class PetData {

    private String uuid;
    private String type;
    private String customName;
    private double health;
    private double maxHealth;
    private boolean alive = true;
    private String ownerUuid;
    private Integer boundFarmIndex;
    private boolean locked = false;   // 基础版蛋锁定标记

    // 马匹属性
    private Double speed;
    private Double jumpStrength;
    private Boolean saddle;
    private String color;
    private String style;
    private String armorType;
    private Boolean hasChest;
    private List<Map<String, Object>> chestContents;

    // 猫属性
    private String catType;
    private Boolean isSitting;

    // 狼属性
    private String collarColor;
    private Boolean isAngry;

    // 鹦鹉属性
    private String variant;

    // 羊驼属性
    private String carpetColor;
    private Integer strength;

    // 共享玩家 UUID 列表
    private List<String> sharedPlayers;

    public PetData() {
        this.alive = true;
        this.sharedPlayers = new ArrayList<>();
        this.chestContents = new ArrayList<>();
    }

    // ============ Getter / Setter ============

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }

    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }

    public double getMaxHealth() { return maxHealth; }
    public void setMaxHealth(double maxHealth) { this.maxHealth = maxHealth; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String ownerUuid) { this.ownerUuid = ownerUuid; }

    public Integer getBoundFarmIndex() { return boundFarmIndex; }
    public void setBoundFarmIndex(Integer boundFarmIndex) { this.boundFarmIndex = boundFarmIndex; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }

    public Double getJumpStrength() { return jumpStrength; }
    public void setJumpStrength(Double jumpStrength) { this.jumpStrength = jumpStrength; }

    public Boolean getSaddle() { return saddle; }
    public void setSaddle(Boolean saddle) { this.saddle = saddle; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getArmorType() { return armorType; }
    public void setArmorType(String armorType) { this.armorType = armorType; }

    public Boolean getHasChest() { return hasChest; }
    public void setHasChest(Boolean hasChest) { this.hasChest = hasChest; }

    public List<Map<String, Object>> getChestContents() { return chestContents; }
    public void setChestContents(List<Map<String, Object>> chestContents) { this.chestContents = chestContents; }

    public String getCatType() { return catType; }
    public void setCatType(String catType) { this.catType = catType; }

    public Boolean getIsSitting() { return isSitting; }
    public void setIsSitting(Boolean isSitting) { this.isSitting = isSitting; }

    public String getCollarColor() { return collarColor; }
    public void setCollarColor(String collarColor) { this.collarColor = collarColor; }

    public Boolean getIsAngry() { return isAngry; }
    public void setIsAngry(Boolean isAngry) { this.isAngry = isAngry; }

    public String getVariant() { return variant; }
    public void setVariant(String variant) { this.variant = variant; }

    public String getCarpetColor() { return carpetColor; }
    public void setCarpetColor(String carpetColor) { this.carpetColor = carpetColor; }

    public Integer getStrength() { return strength; }
    public void setStrength(Integer strength) { this.strength = strength; }

    public List<String> getSharedPlayers() { return sharedPlayers; }
    public void setSharedPlayers(List<String> sharedPlayers) { this.sharedPlayers = sharedPlayers; }
}