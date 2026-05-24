package com.icenci.mypets.pets;

import com.icenci.mypets.model.PetData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * 可骑乘宠物属性适配器接口。
 * 每种需要特殊处理的生物类型实现此接口，
 * 将属性的提取和恢复逻辑封装在独立的适配器中。
 */
public interface RideablePetAdapter {

    /**
     * 判断该适配器是否适用于给定实体
     * @param entity 待检查的实体
     * @return true 表示此适配器可以处理该实体
     */
    boolean supports(LivingEntity entity);

    /**
     * 从实体提取属性并存入 PetData
     * @param entity 源实体
     * @param data   目标数据对象
     */
    void extractAttributes(LivingEntity entity, PetData data);

    /**
     * 从 PetData 恢复属性到实体
     * @param entity 目标实体
     * @param data   源数据对象
     * @param owner  宠物主人
     */
    void applyAttributes(LivingEntity entity, PetData data, Player owner);
}