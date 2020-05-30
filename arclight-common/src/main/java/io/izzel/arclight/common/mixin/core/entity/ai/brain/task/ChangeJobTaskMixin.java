package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.task.ChangeJobTask;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import org.bukkit.craftbukkit.v.entity.CraftVillager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChangeJobTask.class)
public class ChangeJobTaskMixin {

    @Redirect(method = "startExecuting", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;setVillagerData(Lnet/minecraft/entity/merchant/villager/VillagerData;)V"))
    private void arclight$careerChangeHook(VillagerEntity villagerEntity, VillagerData villagerData) {
        VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(villagerEntity,
            CraftVillager.nmsToBukkitProfession(VillagerProfession.NONE),
            VillagerCareerChangeEvent.ChangeReason.LOSING_JOB); // 这里本来是 EMPLOYED 但是我怀疑他打错了
        if (!event.isCancelled()) {
            VillagerData newData = villagerEntity.getVillagerData().withProfession(CraftVillager.bukkitToNmsProfession(event.getProfession()));
            villagerEntity.setVillagerData(newData);
        }
    }
}
