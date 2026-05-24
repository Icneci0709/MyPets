package com.icenci.mypets.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家数据模型，对应一个玩家的完整数据（宠物列表 + 农场列表）
 */
public class PlayerData {

    private List<PetData> pets;
    private List<FarmData> farms;

    public PlayerData() {
        this.pets = new ArrayList<>();
        this.farms = new ArrayList<>();
    }

    public List<PetData> getPets() { return pets; }
    public void setPets(List<PetData> pets) { this.pets = pets; }

    public List<FarmData> getFarms() { return farms; }
    public void setFarms(List<FarmData> farms) { this.farms = farms; }
}