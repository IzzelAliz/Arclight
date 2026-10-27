package io.izzel.arclight.common.mod.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.AgeableWaterCreature;
import net.minecraft.world.entity.animal.fish.WaterAnimal;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.raid.Raider;

public enum TrackingCategory {
    ANIMAL, MONSTER, OTHER;

    public static TrackingCategory of(Entity entity) {
        if (entity instanceof WaterAnimal || entity instanceof AgeableWaterCreature || entity instanceof AbstractNautilus || entity instanceof Villager) {
            return ANIMAL;
        }
        if (entity instanceof Ghast || entity instanceof Phantom || entity instanceof Raider || entity instanceof Enemy) {
            return MONSTER;
        }
        return entity instanceof PathfinderMob || entity instanceof AmbientCreature ? ANIMAL : OTHER;
    }
}
