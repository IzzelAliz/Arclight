package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionFromJobSiteMixin {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;setVillagerData(Lnet/minecraft/world/entity/npc/villager/VillagerData;)V"))
    private static void arclight$jobChange(Villager instance, VillagerData villagerData) {
        VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(instance, CraftVillager.CraftProfession.minecraftToBukkit(villagerData.profession().value()), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
        if (!event.isCancelled()) {
            VillagerData newData = villagerData.withProfession(CraftVillager.CraftProfession.bukkitToMinecraftHolder(event.getProfession()));
            instance.setVillagerData(newData);
        }
    }
}
