package com.icenci.mypets;

import com.icenci.mypets.commands.PetCommand;
import com.icenci.mypets.commands.FarmCommand;
import com.icenci.mypets.config.ConfigManager;
import com.icenci.mypets.data.DataManager;
import com.icenci.mypets.listeners.PetListener;
import com.icenci.mypets.listeners.CaptureListener;
import com.icenci.mypets.listeners.FarmFlagListener;
import com.icenci.mypets.pets.RideablePetAdapter;
import com.icenci.mypets.pets.adapters.*;
import com.icenci.mypets.utils.LangManager;
import com.icenci.mypets.farm.FarmStructureProtection;
import com.icenci.mypets.gui.InsuranceBoxGUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class MyPets extends JavaPlugin {

    private LangManager lang;
    private ConfigManager config;
    private DataManager data;
    private List<RideablePetAdapter> adapters;
    private FarmStructureProtection structureProtection;
    private PetListener petListener;

    private static final List<String> MP_SUB = Arrays.asList("reload", "help", "lang", "world");
    private static final List<String> WORLD_SUB = Arrays.asList("add", "remove", "list", "enable", "disable");
    private static final List<String> LANG_LIST = Arrays.asList("zh_CN", "zh_TW", "en_US", "ja_JP", "ko_KR");

    @Override
    public void onEnable() {
        config = new ConfigManager(this);
        lang = new LangManager(this, config.getLanguage(), config);
        lang.saveDefaultLangFiles();
        lang.loadLang(config.getLanguage()); // 覆盖后重新加载
        data = new DataManager(this);

        printBanner();

        adapters = new ArrayList<>();
        registerAdapterIfExists("org.bukkit.entity.Horse", new HorseAdapter());
        registerAdapterIfExists("org.bukkit.entity.Llama", new LlamaAdapter());
        registerAdapterIfExists("org.bukkit.entity.Camel", new CamelAdapter());
        registerAdapterIfExists("org.bukkit.entity.Strider", new StriderAdapter());
        registerAdapterIfExists("org.bukkit.entity.Cat", new SmallPetAdapter());
        // Armadillo 在 1.20.5+ 可用，1.20.4 会跳过
        try { Class.forName("org.bukkit.entity.Armadillo"); getLogger().info("适配器已注册: SmallPetAdapter(Armadillo)"); } catch (ClassNotFoundException ignored) {}

        getLogger().info(lang.get("plugin.loaded"));
        getLogger().info("已注册 " + adapters.size() + " 个宠物适配器");

        getCommand("pet").setExecutor(new PetCommand(this));

        structureProtection = new FarmStructureProtection(lang, data, this);
        getServer().getPluginManager().registerEvents(structureProtection, this);

        FarmCommand farmCmd = new FarmCommand(this, structureProtection);
        getCommand("farm").setExecutor(farmCmd);
        getCommand("farm").setTabCompleter(farmCmd);
        getServer().getPluginManager().registerEvents(farmCmd, this);

        ReloadCommand reloadCmd = new ReloadCommand();
        getCommand("mp").setExecutor(reloadCmd);
        getCommand("mp").setTabCompleter(reloadCmd);

        petListener = new PetListener(this, adapters);
        getServer().getPluginManager().registerEvents(petListener, this);
        getServer().getPluginManager().registerEvents(new CaptureListener(this, adapters), this);
        getServer().getPluginManager().registerEvents(InsuranceBoxGUI.getInstance(this), this);
        getServer().getPluginManager().registerEvents(new FarmFlagListener(this), this);
    }

    private void printBanner() {
        String gray = "§7";
        String my = "§b";
        String m = "§b§l";
        String pets = "§d";
        String p = "§d§l";
        String authorColor = "§e";

        String[] lines = {
            my + "  ███╗   ███╗██    ██╗" + pets + "██████╗ ███████╗████████╗███████╗",
            my + "  ████╗ ████║ ██  ██╔╝" + pets + "██╔══██╗██╔════╝╚══██╔══╝██╔════╝",
            my + "  ██╔████╔██║  ████╔╝ " + pets + "██████╔╝█████╗     ██║   ███████╗",
            my + "  ██║╚██╔╝██║   ██╔╝  " + pets + "██╔═══╝ ██╔══╝     ██║   ╚════██║",
            my + "  ██║ ╚═╝ ██║   ██║   " + pets + "██║     ███████╗   ██║   ███████║",
            my + "  ╚═╝     ╚═╝   ╚═╝   " + pets + "╚═╝     ╚══════╝   ╚═╝   ╚══════╝"
        };
        for (String line : lines) getLogger().info(line);
        getLogger().info("");
        getLogger().info(gray + "               " + m + "My" + p + "Pets" + gray + "  宠物管理系统  v" + getDescription().getVersion());
        getLogger().info(gray + "                       作者: " + authorColor + "Icenci");
    }

    @Override
    public void onDisable() { getLogger().info("MyPets 宠物插件已卸载！"); }

    private void registerAdapterIfExists(String className, RideablePetAdapter adapter) {
        try {
            Class.forName(className); adapters.add(adapter);
            getLogger().info("适配器已注册: " + adapter.getClass().getSimpleName());
        } catch (ClassNotFoundException e) { getLogger().info("跳过未支持的生物: " + className); }
    }

    public FarmStructureProtection getStructureProtection() { return structureProtection; }
    public PetListener getPetListener() { return petListener; }

    private class ReloadCommand implements CommandExecutor, TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) return MP_SUB.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
            if (args.length == 2 && args[0].equalsIgnoreCase("world")) return WORLD_SUB.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            if (args.length == 2 && args[0].equalsIgnoreCase("lang")) return LANG_LIST.stream().filter(s -> s.startsWith(args[1].toLowerCase())).collect(Collectors.toList());
            return new ArrayList<>();
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                sender.sendMessage(lang.get("mp.help.header"));
                sender.sendMessage(lang.get("mp.help.reload"));
                sender.sendMessage(lang.get("mp.help.help"));
                sender.sendMessage(lang.get("mp.help.world"));
                if (config.isPremium()) sender.sendMessage(lang.get("mp.help.lang"));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("mypets.admin.reload")) { sender.sendMessage(config.getPrefix() + lang.get("common.no_permission")); return true; }
                config.reload(); lang.setLang(config.getLanguage());
                sender.sendMessage(config.getPrefix() + lang.get("mp.reload.success"));
                getLogger().info(lang.get("mp.reload.success")); return true;
            }
            if (args[0].equalsIgnoreCase("lang")) {
                if (!config.isPremium()) { sender.sendMessage(config.getPrefix() + lang.get("mp.lang.premium_only")); return true; }
                if (!sender.hasPermission("mypets.admin.lang")) { sender.sendMessage(config.getPrefix() + lang.get("common.no_permission")); return true; }
                if (args.length < 2) { sender.sendMessage(config.getPrefix() + lang.get("mp.lang.usage")); sender.sendMessage(lang.get("mp.lang.available", String.join(", ", LANG_LIST))); return true; }
                String langCode = args[1].toLowerCase();
                File langFile = new File(getDataFolder(), "lang/" + langCode + ".json");
                if (!langFile.exists()) { sender.sendMessage(config.getPrefix() + lang.get("mp.lang.not_found", langCode)); return true; }
                lang.setLang(langCode); config.getConfig().set("language", langCode); config.save();
                for (Player p : Bukkit.getOnlinePlayers()) p.sendMessage(config.getPrefix() + lang.get("language.changed", langCode));
                return true;
            }
            if (args[0].equalsIgnoreCase("world")) {
                if (!sender.hasPermission("mypets.admin.world")) { sender.sendMessage(config.getPrefix() + lang.get("common.no_permission")); return true; }
                if (args.length < 2) { sender.sendMessage(config.getPrefix() + lang.get("mp.world.usage")); return true; }
                String worldSub = args[1].toLowerCase();
                switch (worldSub) {
                    case "add": if (args.length < 3) { sender.sendMessage(config.getPrefix() + lang.get("mp.world.add_usage")); return true; } config.addWorldToWhitelist(args[2]); sender.sendMessage(config.getPrefix() + lang.get("mp.world.added", args[2])); break;
                    case "remove": if (args.length < 3) { sender.sendMessage(config.getPrefix() + lang.get("mp.world.remove_usage")); return true; } config.removeWorldFromWhitelist(args[2]); sender.sendMessage(config.getPrefix() + lang.get("mp.world.removed", args[2])); break;
                    case "list": List<String> whitelist = config.getWorldWhitelist(); if (whitelist.isEmpty()) sender.sendMessage(config.getPrefix() + lang.get("mp.world.list_empty")); else { sender.sendMessage(config.getPrefix() + lang.get("mp.world.list_header")); for (String w : whitelist) sender.sendMessage(lang.get("mp.world.list_entry", w)); } break;
                    case "enable": config.setWorldWhitelistEnabled(true); sender.sendMessage(config.getPrefix() + lang.get("mp.world.enabled")); break;
                    case "disable": config.setWorldWhitelistEnabled(false); sender.sendMessage(config.getPrefix() + lang.get("mp.world.disabled")); break;
                    default: sender.sendMessage(config.getPrefix() + lang.get("mp.world.unknown", worldSub)); break;
                }
                return true;
            }
            return false;
        }
    }

    public ConfigManager getConfigManager() { return config; }
    public LangManager getLangManager() { return lang; }
    public DataManager getDataManager() { return data; }
    public List<RideablePetAdapter> getAdapters() { return adapters; }
}