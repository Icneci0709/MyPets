package com.icenci.mypets.pets.adapters;

import com.icenci.mypets.model.PetData;
import com.icenci.mypets.pets.RideablePetAdapter;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

public class LlamaAdapter implements RideablePetAdapter {

    @Override
    public boolean supports(LivingEntity entity) {
        return entity instanceof Llama;
    }

    @Override
    public void extractAttributes(LivingEntity entity, PetData data) {
        Llama llama = (Llama) entity;
        data.setSpeed(llama.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
        data.setSaddle(llama.getInventory().getSaddle() != null);

        if (llama.getColor() != null) {
            data.setCarpetColor(llama.getColor().name());
        }
        data.setStrength(llama.getStrength());
    }

    @Override
    public void applyAttributes(LivingEntity entity, PetData data, Player owner) {
        Llama llama = (Llama) entity;

        if (data.getSpeed() != null) {
            llama.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(data.getSpeed());
        }
        if (data.getSaddle() != null && data.getSaddle()) {
            llama.getInventory().setSaddle(new ItemStack(Material.SADDLE));
        }
        if (data.getCarpetColor() != null) {
            try {
                llama.setColor(Llama.Color.valueOf(data.getCarpetColor()));
            } catch (Exception ignored) {}
        }
        if (data.getStrength() != null) {
            llama.setStrength(data.getStrength());
        }
    }
}
