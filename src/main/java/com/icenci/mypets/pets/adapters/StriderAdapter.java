package com.icenci.mypets.pets.adapters;

import com.icenci.mypets.model.PetData;
import com.icenci.mypets.pets.RideablePetAdapter;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;

public class StriderAdapter implements RideablePetAdapter {

    @Override
    public boolean supports(LivingEntity entity) {
        return entity instanceof Strider;
    }

    @Override
    public void extractAttributes(LivingEntity entity, PetData data) {
        Strider strider = (Strider) entity;
        data.setSaddle(strider.hasSaddle());
        data.setSpeed(strider.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue());
    }

    @Override
    public void applyAttributes(LivingEntity entity, PetData data, Player owner) {
        Strider strider = (Strider) entity;

        if (data.getSaddle() != null && data.getSaddle()) {
            strider.setSaddle(true);
        }
        if (data.getSpeed() != null) {
            strider.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(data.getSpeed());
        }
    }
}
