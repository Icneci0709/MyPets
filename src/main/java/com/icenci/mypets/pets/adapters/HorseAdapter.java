package com.icenci.mypets.pets.adapters;

import com.icenci.mypets.model.PetData;
import com.icenci.mypets.pets.RideablePetAdapter;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

public class HorseAdapter implements RideablePetAdapter {

    @Override
    public boolean supports(LivingEntity entity) {
        return entity instanceof AbstractHorse;
    }

    @Override
    public void extractAttributes(LivingEntity entity, PetData data) {
        AbstractHorse horse = (AbstractHorse) entity;
        data.setSpeed(horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
        data.setJumpStrength(horse.getJumpStrength());
        data.setSaddle(horse.getInventory().getSaddle() != null);

        if (entity instanceof Horse) {
            Horse h = (Horse) entity;
            data.setColor(h.getColor().name());
            data.setStyle(h.getStyle().name());
            if (h.getInventory().getArmor() != null) {
                data.setArmorType(h.getInventory().getArmor().getType().name());
            }
        }
    }

    @Override
    public void applyAttributes(LivingEntity entity, PetData data, Player owner) {
        AbstractHorse horse = (AbstractHorse) entity;

        if (data.getSpeed() != null) {
            horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(data.getSpeed());
        }
        if (data.getJumpStrength() != null) {
            horse.setJumpStrength(data.getJumpStrength());
        }
        if (data.getSaddle() != null && data.getSaddle()) {
            horse.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        }

        if (entity instanceof Horse && data.getColor() != null) {
            Horse h = (Horse) entity;
            try { h.setColor(Horse.Color.valueOf(data.getColor())); } catch (Exception ignored) {}
            if (data.getStyle() != null) {
                try { h.setStyle(Horse.Style.valueOf(data.getStyle())); } catch (Exception ignored) {}
            }
            if (data.getArmorType() != null) {
                try {
                    Material mat = Material.valueOf(data.getArmorType());
                    h.getInventory().setArmor(new ItemStack(mat));
                } catch (Exception ignored) {}
            }
        }
    }
}
