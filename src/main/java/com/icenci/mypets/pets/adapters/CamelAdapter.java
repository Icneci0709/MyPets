package com.icenci.mypets.pets.adapters;

import com.icenci.mypets.model.PetData;
import com.icenci.mypets.pets.RideablePetAdapter;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

public class CamelAdapter implements RideablePetAdapter {

    @Override
    public boolean supports(LivingEntity entity) {
        return entity instanceof Camel;
    }

    @Override
    public void extractAttributes(LivingEntity entity, PetData data) {
        Camel camel = (Camel) entity;
        data.setSaddle(camel.getInventory().getSaddle() != null);
        data.setSpeed(camel.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
    }

    @Override
    public void applyAttributes(LivingEntity entity, PetData data, Player owner) {
        Camel camel = (Camel) entity;

        if (data.getSaddle() != null && data.getSaddle()) {
            camel.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        }
        if (data.getSpeed() != null) {
            camel.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(data.getSpeed());
        }
    }
}
