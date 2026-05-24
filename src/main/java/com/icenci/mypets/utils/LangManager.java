package com.icenci.mypets.utils;

import com.icenci.mypets.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LangManager {

    private final JavaPlugin plugin;
    private final ConfigManager config;
    private final String defaultLang;
    private String currentLang;
    private Map<String, String> translations;

    public LangManager(JavaPlugin plugin, String defaultLang, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.defaultLang = defaultLang;
        this.currentLang = defaultLang;
        this.translations = new HashMap<>();
        loadLang(currentLang);
    }

    public void loadLang(String langCode) {
        String fileName = "lang/" + langCode + ".json";
        File langFile = new File(plugin.getDataFolder(), fileName);
        if (!langFile.exists()) {
            plugin.getLogger().warning("语言文件 " + fileName + " 不存在，尝试加载默认语言 " + defaultLang);
            langCode = defaultLang;
            fileName = "lang/" + defaultLang + ".json";
            langFile = new File(plugin.getDataFolder(), fileName);
        }
        if (!langFile.exists()) {
            plugin.getLogger().warning("默认语言文件也不存在，使用空翻译表。");
            this.translations = new HashMap<>();
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(langFile), StandardCharsets.UTF_8)) {
            Gson gson = new Gson();
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> loaded = gson.fromJson(reader, type);
            if (loaded != null) {
                this.translations = loaded;
                this.currentLang = langCode;
                plugin.getLogger().info("语言文件 " + langCode + " 加载成功，共 " + translations.size() + " 条文本。");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("语言文件加载失败: " + e.getMessage());
            this.translations = new HashMap<>();
        }
    }

    public String get(String key, Object... args) {
        String text = translations.getOrDefault(key, key);
        if (args.length > 0) {
            try {
                text = String.format(text, args);
            } catch (Exception e) {
                plugin.getLogger().warning("格式化文本失败: " + key + " - " + e.getMessage());
            }
        }
        return text;
    }

    public void sendWithPrefix(Player player, String key, Object... args) {
        String message = get(key, args);
        String prefix = config.getPrefix();
        player.sendMessage(prefix + message);
    }

    public String getCurrentLang() { return currentLang; }

    public void setLang(String langCode) {
        loadLang(langCode);
    }

    public void saveDefaultLangFiles() {
        String[] languages = {"zh_CN", "zh_TW", "en_US", "ja_JP", "ko_KR"};
        for (String lang : languages) {
            String fileName = "lang/" + lang + ".json";
            try {
                plugin.saveResource(fileName, true);
                plugin.getLogger().info("已从 jar 中更新语言文件: " + fileName);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("语言文件 " + fileName + " 未包含在 jar 中，跳过。");
            }
        }
    }
}