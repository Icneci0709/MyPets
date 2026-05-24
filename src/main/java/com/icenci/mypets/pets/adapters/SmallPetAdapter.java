package com.icenci.mypets.pets.adapters;

import com.icenci.mypets.model.PetData;
import com.icenci.mypets.pets.RideablePetAdapter;
import org.bukkit.DyeColor;
import org.bukkit.entity.*;

/**
 * 处理小型可驯服宠物（猫、狼、鹦鹉、犰狳）
 */
public class SmallPetAdapter implements RideablePetAdapter {

    @Override
    public boolean supports(LivingEntity entity) {
        String type = entity.getType().name();
        return entity instanceof Cat || entity instanceof Wolf || entity instanceof Parrot
            || type.equals("ARMADILLO");
    }

    @Override
    public void extractAttributes(LivingEntity entity, PetData data) {
        if (entity instanceof Cat) {
            Cat cat = (Cat) entity;
            data.setCatType(cat.getCatType().name());
            data.setIsSitting(cat.isSitting());
        } else if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            data.setCollarColor(wolf.getCollarColor() != null ? wolf.getCollarColor().name() : null);
            data.setIsSitting(wolf.isSitting());
            data.setIsAngry(wolf.isAngry());
        } else if (entity instanceof Parrot) {
            Parrot parrot = (Parrot) entity;
            data.setVariant(parrot.getVariant().name());
            data.setIsSitting(parrot.isSitting());
        }
        // Armadillo 无特殊属性需保存
    }

    @Override
    public void applyAttributes(LivingEntity entity, PetData data, Player owner) {
        if (entity instanceof Cat) {
            Cat cat = (Cat) entity;
            if (data.getCatType() != null) {
                try { cat.setCatType(Cat.Type.valueOf(data.getCatType())); } catch (IllegalArgumentException ignored) {}
            }
            if (data.getIsSitting() != null && data.getIsSitting()) cat.setSitting(true);
        } else if (entity instanceof Wolf) {
            Wolf wolf = (Wolf) entity;
            if (data.getCollarColor() != null) {
                try { wolf.setCollarColor(DyeColor.valueOf(data.getCollarColor())); } catch (IllegalArgumentException ignored) {}
            }
            if (data.getIsSitting() != null && data.getIsSitting()) wolf.setSitting(true);
            if (data.getIsAngry() != null && data.getIsAngry()) wolf.setAngry(true);
        } else if (entity instanceof Parrot) {
            Parrot parrot = (Parrot) entity;
            if (data.getVariant() != null) {
                try { parrot.setVariant(Parrot.Variant.valueOf(data.getVariant())); } catch (IllegalArgumentException ignored) {}
            }
            if (data.getIsSitting() != null && data.getIsSitting()) parrot.setSitting(true);
        }
        // Armadillo 无特殊属性需恢复
    }
}