package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.world.entity.ai.behavior.ResetProfession;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.bukkit.craftbukkit.entity.CraftVillager;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ResetProfession.class)
public class ResetProfessionMixin {

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;setVillagerData(Lnet/minecraft/world/entity/npc/villager/VillagerData;)V"))
    private static void arclight$careerChangeHook(Villager villagerEntity, VillagerData villagerData) {
        var none = villagerEntity.registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION).getOrThrow(VillagerProfession.NONE).value();
        VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(villagerEntity,
            CraftVillager.CraftProfession.minecraftToBukkit(none),
            VillagerCareerChangeEvent.ChangeReason.LOSING_JOB);
        if (!event.isCancelled()) {
            VillagerData newData = villagerEntity.getVillagerData().withProfession(CraftVillager.CraftProfession.bukkitToMinecraftHolder(event.getProfession()));
            villagerEntity.setVillagerData(newData);
        }
    }
}
