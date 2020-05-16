package io.izzel.arclight.mixin.core.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.task.AssignProfessionTask;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftVillager;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AssignProfessionTask.class)
public class AssignProfessionTaskMixin {

    @Redirect(method = "func_212831_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;setVillagerData(Lnet/minecraft/entity/merchant/villager/VillagerData;)V"))
    private void arclight$careerChangeHook(VillagerEntity villagerEntity, VillagerData villagerData) {
        VillagerProfession profession = villagerData.getProfession();
        VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(villagerEntity, CraftVillager.nmsToBukkitProfession(profession), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
        if (!event.isCancelled()) {
            VillagerData newData = villagerEntity.getVillagerData().withProfession(CraftVillager.bukkitToNmsProfession(event.getProfession()));
            villagerEntity.setVillagerData(newData);
        }

    }
}
