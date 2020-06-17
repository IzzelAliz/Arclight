package io.izzel.arclight.common.mixin.core.entity.ai.brain.task;

import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.AssignProfessionTask;
import net.minecraft.entity.merchant.villager.VillagerData;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.merchant.villager.VillagerProfession;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.entity.CraftVillager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AssignProfessionTask.class)
public class AssignProfessionTaskMixin {

    /*
    @SuppressWarnings("UnresolvedMixinReference")
    @Redirect(method = "*(Lnet/minecraft/entity/merchant/villager/VillagerEntity;Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/entity/merchant/villager/VillagerProfession;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/merchant/villager/VillagerEntity;setVillagerData(Lnet/minecraft/entity/merchant/villager/VillagerData;)V"))
    private void arclight$careerChangeHook(VillagerEntity villagerEntity, VillagerData villagerData) {
        VillagerProfession profession = villagerData.getProfession();
        VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(villagerEntity, CraftVillager.nmsToBukkitProfession(profession), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
        if (!event.isCancelled()) {
            VillagerData newData = villagerEntity.getVillagerData().withProfession(CraftVillager.bukkitToNmsProfession(event.getProfession()));
            villagerEntity.setVillagerData(newData);
        }
    }*/

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void startExecuting(ServerWorld worldIn, VillagerEntity entityIn, long gameTimeIn) {
        GlobalPos globalpos = entityIn.getBrain().getMemory(MemoryModuleType.JOB_SITE).get();
        MinecraftServer minecraftserver = worldIn.getServer();
        minecraftserver.getWorld(globalpos.getDimension()).getPointOfInterestManager().getType(globalpos.getPos()).ifPresent((p_220390_2_) -> {
            Registry.VILLAGER_PROFESSION.stream().filter((p_220389_1_) -> {
                return p_220389_1_.getPointOfInterest() == p_220390_2_;
            }).findFirst().ifPresent((p_220388_2_) -> {
                VillagerData villagerData = entityIn.getVillagerData().withProfession(p_220388_2_);
                VillagerProfession profession = villagerData.getProfession();
                VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(entityIn, CraftVillager.nmsToBukkitProfession(profession), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
                if (!event.isCancelled()) {
                    VillagerData newData = entityIn.getVillagerData().withProfession(CraftVillager.bukkitToNmsProfession(event.getProfession()));
                    entityIn.setVillagerData(newData);
                    entityIn.resetBrain(worldIn);
                }
            });
        });
    }
}
