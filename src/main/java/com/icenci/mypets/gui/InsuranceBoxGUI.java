package com.icenci.mypets.gui;

import com.icenci.mypets.MyPets;
import com.icenci.mypets.model.FarmData;
import com.icenci.mypets.utils.LangManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

public class InsuranceBoxGUI implements Listener, InventoryHolder {

    private static InsuranceBoxGUI instance;
    private final JavaPlugin plugin;
    private final LangManager lang;
    private final Map<UUID, FarmData> openFarmMap = new HashMap<>();
    private final Map<UUID, Integer> openPageMap = new HashMap<>();
    // 新增：保存打开的农场列表和索引，以便关闭时正确保存
    private final Map<UUID, List<FarmData>> openFarmListMap = new HashMap<>();
    private final Map<UUID, Integer> openFarmIndexMap = new HashMap<>();

    private static final int GUI_SIZE = 54;
    private static final int STORAGE_START = 0;
    private static final int STORAGE_END = 44;
    private static final int BUTTON_PREV = 45;
    private static final int BUTTON_NEXT = 53;
    private static final int[] BLACK_GLASS_SLOTS = {46, 47, 48, 49, 50, 51, 52};

    private final ItemStack BLACK_GLASS = makeItem(Material.BLACK_STAINED_GLASS_PANE, "§0 ");
    private final ItemStack GRAY_GLASS_PREV = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§7已是第一页");
    private final ItemStack GRAY_GLASS_NEXT = makeItem(Material.GRAY_STAINED_GLASS_PANE, "§7已是最后一页");
    private final ItemStack PREV_BUTTON = makeItem(Material.ARROW, "§a上一页");
    private final ItemStack NEXT_BUTTON = makeItem(Material.ARROW, "§a下一页");

    private InsuranceBoxGUI(JavaPlugin plugin) {
        this.plugin = plugin;
        this.lang = ((MyPets) plugin).getLangManager();
    }

    public static InsuranceBoxGUI getInstance(JavaPlugin plugin) {
        if (instance == null) {
            instance = new InsuranceBoxGUI(plugin);
        }
        return instance;
    }

    private ItemStack makeItem(Material mat, String name) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        if (m != null) {
            m.setDisplayName(name);
            i.setItemMeta(m);
        }
        return i;
    }

    /**
     * 打开高级保险箱 GUI
     * @param player 玩家
     * @param farm 农场数据
     * @param farms 玩家农场列表（用于保存）
     * @param farmIndex 农场在列表中的索引
     */
    public void open(Player player, FarmData farm, List<FarmData> farms, int farmIndex) {
        int page = 0;
        Map<Integer, String> pages = farm.getInsurancePages();
        if (pages == null) {
            pages = new HashMap<>();
            farm.setInsurancePages(pages);
        }

        Inventory inv = Bukkit.createInventory(this, GUI_SIZE, "农场保险箱 §7(" + farm.getName() + ") - 第" + (page + 1) + "页");
        loadPage(inv, pages, page);
        setBottomDecorators(inv, false, pages.isEmpty() || !pages.containsKey(page + 1));

        openFarmMap.put(player.getUniqueId(), farm);
        openPageMap.put(player.getUniqueId(), page);
        openFarmListMap.put(player.getUniqueId(), farms);
        openFarmIndexMap.put(player.getUniqueId(), farmIndex);
        player.openInventory(inv);
    }

    private void loadPage(Inventory inv, Map<Integer, String> pages, int page) {
        String data = pages.get(page);
        if (data != null) {
            ItemStack[] items = deserializeItems(data);
            for (int i = STORAGE_START; i <= STORAGE_END; i++) {
                if (items != null && i < items.length && items[i] != null) {
                    inv.setItem(i, items[i].clone());
                }
            }
        }
    }

    private void setBottomDecorators(Inventory inv, boolean isFirstPage, boolean isLastPage) {
        if (isFirstPage) {
            inv.setItem(BUTTON_PREV, GRAY_GLASS_PREV);
        } else {
            inv.setItem(BUTTON_PREV, PREV_BUTTON);
        }
        if (isLastPage) {
            inv.setItem(BUTTON_NEXT, GRAY_GLASS_NEXT);
        } else {
            inv.setItem(BUTTON_NEXT, NEXT_BUTTON);
        }
        for (int slot : BLACK_GLASS_SLOTS) {
            inv.setItem(slot, BLACK_GLASS);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof InsuranceBoxGUI)) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        FarmData farm = openFarmMap.get(player.getUniqueId());
        if (farm == null) {
            player.closeInventory();
            return;
        }

        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();

        if (slot < 0 || slot >= GUI_SIZE) {
            event.setCancelled(false);
            return;
        }

        if (slot >= 45) {
            if (slot == BUTTON_PREV) {
                int currentPage = openPageMap.getOrDefault(player.getUniqueId(), 0);
                if (currentPage > 0) {
                    int newPage = currentPage - 1;
                    openPageMap.put(player.getUniqueId(), newPage);
                    Inventory newInv = Bukkit.createInventory(this, GUI_SIZE, "农场保险箱 §7(" + farm.getName() + ") - 第" + (newPage + 1) + "页");
                    loadPage(newInv, farm.getInsurancePages(), newPage);
                    setBottomDecorators(newInv, newPage == 0, !farm.getInsurancePages().containsKey(newPage + 1));
                    player.openInventory(newInv);
                }
            } else if (slot == BUTTON_NEXT) {
                int currentPage = openPageMap.getOrDefault(player.getUniqueId(), 0);
                Map<Integer, String> pages = farm.getInsurancePages();
                if (pages.containsKey(currentPage + 1)) {
                    int newPage = currentPage + 1;
                    openPageMap.put(player.getUniqueId(), newPage);
                    Inventory newInv = Bukkit.createInventory(this, GUI_SIZE, "农场保险箱 §7(" + farm.getName() + ") - 第" + (newPage + 1) + "页");
                    loadPage(newInv, pages, newPage);
                    setBottomDecorators(newInv, false, !pages.containsKey(newPage + 1));
                    player.openInventory(newInv);
                }
            }
            return;
        }

        event.setCancelled(false);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof InsuranceBoxGUI)) return;
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();
        FarmData farm = openFarmMap.remove(uuid);
        int page = openPageMap.remove(uuid);
        List<FarmData> farms = openFarmListMap.remove(uuid);
        Integer farmIndex = openFarmIndexMap.remove(uuid);

        if (farm == null || farms == null || farmIndex == null) return;

        // 保存当前页的物品到 farm 对象
        Map<Integer, String> pages = farm.getInsurancePages();
        if (pages == null) {
            pages = new HashMap<>();
            farm.setInsurancePages(pages);
        }

        ItemStack[] contents = event.getInventory().getContents();
        ItemStack[] saveSlots = new ItemStack[45];
        for (int i = 0; i < 45; i++) {
            saveSlots[i] = contents[i] != null ? contents[i].clone() : null;
        }
        pages.put(page, serializeItems(saveSlots));

        // 从农场列表中获取同一个 farm 对象，以确保修改生效（实际上 farm 已经是引用）
        // 直接使用已有的 farms 列表保存
        MyPets pluginInstance = (MyPets) plugin;
        pluginInstance.getDataManager().savePlayerFarms(farm.getOwnerUuid(), farms);
    }

    /**
     * 尝试将封印蛋存入高级保险箱（自动翻页，理论无限容量）
     * @return true 成功存入
     */
    public static boolean addEggToFarm(FarmData farm, ItemStack egg) {
        Map<Integer, String> pages = farm.getInsurancePages();
        if (pages == null) {
            pages = new HashMap<>();
            farm.setInsurancePages(pages);
        }

        for (int p = 0; ; p++) {
            String data = pages.get(p);
            ItemStack[] items;
            if (data != null) {
                items = deserializeItems(data);
                if (items == null) items = new ItemStack[45];
            } else {
                items = new ItemStack[45];
            }

            for (int i = 0; i < 45; i++) {
                if (items[i] == null || items[i].getType() == Material.AIR) {
                    items[i] = egg.clone();
                    pages.put(p, serializeItems(items));
                    return true;
                }
            }
            // 当前页满了，循环进入下一页
        }
    }

    // ================= 序列化工具 =================
    private static String serializeItems(ItemStack[] items) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeInt(items.length);
            for (ItemStack item : items) {
                dataOutput.writeObject(item);
            }
            dataOutput.close();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("序列化物品失败", e);
        }
    }

    private static ItemStack[] deserializeItems(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            int length = dataInput.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                items[i] = (ItemStack) dataInput.readObject();
            }
            dataInput.close();
            return items;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("反序列化物品失败", e);
        }
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}