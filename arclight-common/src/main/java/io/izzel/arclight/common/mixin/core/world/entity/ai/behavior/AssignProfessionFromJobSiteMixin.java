package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import org.bukkit.craftbukkit.v.entity.CraftVillager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionFromJobSiteMixin {

    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;setVillagerData(Lnet/minecraft/world/entity/npc/villager/VillagerData;)V"))
    private static void arclight$jobChange(Villager instance, VillagerData villagerData) throws Throwable {
        VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(instance, CraftVillager.CraftProfession.minecraftHolderToBukkit(villagerData.profession()), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
        if (!event.isCancelled()) {
            VillagerData newData = villagerData.withProfession(CraftVillager.CraftProfession.bukkitToMinecraftHolder(event.getProfession()));
            DecorationOps.callsite().invoke(instance, newData);
        }
    }
}
