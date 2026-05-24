package com.icenci.mypets.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public String getLanguage() { return config.getString("language", "zh_CN"); }
    public int getMaxFarms() { return config.getInt("max_farms", 1); }
    public String getSelectionTool() { return config.getString("selection_tool", "WOODEN_SHOVEL"); }
    public double getPricePerBlock() { return config.getDouble("price_per_block", 0.2); }

    public int getMaxFarmSizeX() { return config.getInt("max_farm_size_x", 20); }
    public int getMaxFarmSizeY() { return config.getInt("max_farm_size_y", 20); }
    public int getMaxFarmSizeZ() { return config.getInt("max_farm_size_z", 20); }

    public boolean isPremiumEnableCapture() { return false; }
    public boolean isPremiumGuiChest() { return false; }
    public boolean isPremiumCustomSpawn() { return false; }
    public boolean isPremiumCustomChest() { return false; }
    public boolean isPremiumFarmHeal() { return false; }
    public boolean isPremiumFarmPricing() { return false; }
    public boolean isPremiumFlags() { return false; }
    public boolean isPremiumLeashTeleport() { return false; }
    public boolean isPremiumRideTeleport() { return false; }
    public int getPremiumMaxPetsPerFarm() { return 5; }
    public int getPremiumMaxFarms() { return 1; }
    public int getPremiumMaxFarmSizeX() { return 20; }
    public int getPremiumMaxFarmSizeY() { return 20; }
    public int getPremiumMaxFarmSizeZ() { return 20; }

    public boolean isPremium() {
        return false;
    }

    public String getPrefix() {
        String defaultPrefix = "§7[§b§lMy§d§lPets§7] §r";
        if (isPremium() && config.getBoolean("prefix_customizable", false)) {
            return config.getString("prefix", defaultPrefix);
        }
        return defaultPrefix;
    }

    public FileConfiguration getConfig() { return this.config; }
    public void save() { plugin.saveConfig(); }
    public void reload() { plugin.reloadConfig(); this.config = plugin.getConfig(); }

    // 世界白名单
    public List<String> getWorldWhitelist() { return config.getStringList("world_whitelist"); }
    public boolean isWorldWhitelistEnabled() { return config.getBoolean("world_whitelist_enabled", false); }
    public void setWorldWhitelistEnabled(boolean enabled) { config.set("world_whitelist_enabled", enabled); save(); }
    public void addWorldToWhitelist(String worldName) {
        List<String> list = getWorldWhitelist();
        if (!list.contains(worldName)) { list.add(worldName); config.set("world_whitelist", list); save(); }
    }
    public void removeWorldFromWhitelist(String worldName) {
        List<String> list = getWorldWhitelist();
        if (list.remove(worldName)) { config.set("world_whitelist", list); save(); }
    }
}