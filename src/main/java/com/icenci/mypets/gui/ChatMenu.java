package com.icenci.mypets.gui;

import com.icenci.mypets.utils.LangManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public class ChatMenu {

    private final LangManager lang;
    private final String prefix;

    public ChatMenu(LangManager lang, String prefix) {
        this.lang = lang;
        this.prefix = prefix;
    }

    private void sendClickable(Player player, String text, String command, String hover) {
        TextComponent msg = new TextComponent(prefix + text);
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hover != null && !hover.isEmpty()) {
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));
        }
        player.spigot().sendMessage(msg);
    }

    /**
     * 静态发送可点击消息（供其他类使用）
     */
    public static void sendClickable(Player player, String prefix, String text, String command, String hover) {
        TextComponent msg = new TextComponent(prefix + text);
        msg.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
        if (hover != null && !hover.isEmpty()) {
            msg.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hover).create()));
        }
        player.spigot().sendMessage(msg);
    }

    public void openPetMainMenu(Player player) {
        player.sendMessage(prefix + lang.get("gui.pet.main_title"));
        sendClickable(player, lang.get("gui.pet.list_btn"), "/pet list", lang.get("gui.pet.list_hover"));
        sendClickable(player, lang.get("gui.pet.help_btn"), "/pet help", lang.get("gui.pet.help_hover"));
    }

    public void openPetManageMenu(Player player, String petName, boolean hasMount) {
        player.sendMessage(prefix + lang.get("gui.pet.manage_title", petName));
        sendClickable(player, lang.get("gui.pet.capture_btn"), "/pet capture", lang.get("gui.pet.capture_hover"));
        sendClickable(player, lang.get("gui.pet.rename_btn"), "/pet rename ", lang.get("gui.pet.rename_hover"));
        sendClickable(player, lang.get("gui.pet.setfarm_btn"), "/pet setfarm ", lang.get("gui.pet.setfarm_hover"));
        sendClickable(player, lang.get("gui.pet.kill_btn"), "/pet kill", lang.get("gui.pet.kill_hover"));
        sendClickable(player, lang.get("gui.pet.release_btn"), "/pet release", lang.get("gui.pet.release_hover"));
        if (hasMount) {
            player.sendMessage(prefix + lang.get("gui.pet.ride_title"));
            sendClickable(player, lang.get("gui.pet.share_add_btn"), "/pet share add ", lang.get("gui.pet.share_add_hover"));
            sendClickable(player, lang.get("gui.pet.share_remove_btn"), "/pet share remove ", lang.get("gui.pet.share_remove_hover"));
        }
    }

    public void openFarmMainMenu(Player player) {
        player.sendMessage(prefix + lang.get("gui.farm.main_title"));
        sendClickable(player, lang.get("gui.farm.create_btn"), "/farm create ", lang.get("gui.farm.create_hover"));
        sendClickable(player, lang.get("gui.farm.list_btn"), "/farm list", lang.get("gui.farm.list_hover"));
        sendClickable(player, lang.get("gui.farm.help_btn"), "/farm help", lang.get("gui.farm.help_hover"));
    }

    public void openFarmManageMenu(Player player, String farmName) {
        player.sendMessage(prefix + lang.get("gui.farm.manage_title", farmName));
        sendClickable(player, lang.get("gui.farm.rename_btn"), "/farm rename ", lang.get("gui.farm.rename_hover"));
        sendClickable(player, lang.get("gui.farm.box_btn"), "/farm box", lang.get("gui.farm.box_hover"));
        sendClickable(player, lang.get("gui.farm.setspawn_btn"), "/farm setspawn", lang.get("gui.farm.setspawn_hover"));
        sendClickable(player, lang.get("gui.farm.flag_btn"), "/farm flag", lang.get("gui.farm.flag_hover"));
        sendClickable(player, lang.get("gui.farm.remove_btn"), "/farm remove", lang.get("gui.farm.remove_hover"));
    }
}
