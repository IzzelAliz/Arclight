package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.bukkit.craftbukkit.v.entity.CraftVillager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ResetProfession.class)
public class ResetProfessionMixin {

    @Redirect(method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;setVillagerData(Lnet/minecraft/world/entity/npc/VillagerData;)V"))
    private void arclight$careerChangeHook(Villager villagerEntity, VillagerData villagerData) {
        VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(villagerEntity,
            CraftVillager.nmsToBukkitProfession(VillagerProfession.NONE),
            VillagerCareerChangeEvent.ChangeReason.LOSING_JOB); // 这里本来是 EMPLOYED 但是我怀疑他打错了
        if (!event.isCancelled()) {
            VillagerData newData = villagerEntity.getVillagerData().setProfession(CraftVillager.bukkitToNmsProfession(event.getProfession()));
            villagerEntity.setVillagerData(newData);
        }
    }
}
