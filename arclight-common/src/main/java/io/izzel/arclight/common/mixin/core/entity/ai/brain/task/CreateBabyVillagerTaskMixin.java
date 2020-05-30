package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.task.CreateBabyVillagerTask;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(CreateBabyVillagerTask.class)
public class CreateBabyVillagerTaskMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private Optional<VillagerEntity> func_220480_a(VillagerEntity lona, VillagerEntity anonymous) {
        VillagerEntity villager = lona.createChild(anonymous);

        if (CraftEventFactory.callEntityBreedEvent(villager, lona, anonymous, null, null, 0).isCancelled()) {
            return Optional.empty();
        }

        if (villager == null) {
            return Optional.empty();
        } else {
            lona.setGrowingAge(6000);
            anonymous.setGrowingAge(6000);
            villager.setGrowingAge(-24000);
            villager.setLocationAndAngles(lona.posX, lona.posY, lona.posZ, 0.0F, 0.0F);
            lona.world.addEntity(villager);
            lona.world.setEntityState(villager, (byte) 12);
            return Optional.of(villager);
        }
    }
}
