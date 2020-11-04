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

import java.util.Optional;

@Mixin(AssignProfessionTask.class)
public class AssignProfessionTaskMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void startExecuting(ServerWorld worldIn, VillagerEntity entityIn, long gameTimeIn) {
        GlobalPos globalpos = entityIn.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();
        entityIn.getBrain().removeMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        entityIn.getBrain().setMemory(MemoryModuleType.JOB_SITE, globalpos);
        worldIn.setEntityState(entityIn, (byte) 14);
        if (entityIn.getVillagerData().getProfession() == VillagerProfession.NONE) {
            MinecraftServer minecraftserver = worldIn.getServer();
            Optional.ofNullable(minecraftserver.getWorld(globalpos.getDimension())).flatMap((world) -> {
                return world.getPointOfInterestManager().getType(globalpos.getPos());
            }).flatMap((poiType) -> {
                return Registry.VILLAGER_PROFESSION.stream().filter((profession) -> {
                    return profession.getPointOfInterest() == poiType;
                }).findFirst();
            }).ifPresent((profession) -> {
                VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(entityIn, CraftVillager.nmsToBukkitProfession(profession), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
                if (!event.isCancelled()) {
                    VillagerData newData = entityIn.getVillagerData().withProfession(CraftVillager.bukkitToNmsProfession(event.getProfession()));
                    entityIn.setVillagerData(newData);
                    entityIn.resetBrain(worldIn);
                }
            });
        }
    }
}
