package io.izzel.arclight.common.mixin.core.world.entity.ai.behavior;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import org.bukkit.craftbukkit.v.entity.CraftVillager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionFromJobSiteMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void start(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        GlobalPos globalpos = entityIn.getBrain().getMemory(MemoryModuleType.POTENTIAL_JOB_SITE).get();
        entityIn.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
        entityIn.getBrain().setMemory(MemoryModuleType.JOB_SITE, globalpos);
        worldIn.broadcastEntityEvent(entityIn, (byte) 14);
        if (entityIn.getVillagerData().getProfession() == VillagerProfession.NONE) {
            MinecraftServer minecraftserver = worldIn.getServer();
            Optional.ofNullable(minecraftserver.getLevel(globalpos.dimension())).flatMap((world) -> {
                return world.getPoiManager().getType(globalpos.pos());
            }).flatMap((poiType) -> {
                return Registry.VILLAGER_PROFESSION.stream().filter((profession) -> {
                    return profession.getJobPoiType() == poiType;
                }).findFirst();
            }).ifPresent((profession) -> {
                VillagerCareerChangeEvent event = CraftEventFactory.callVillagerCareerChangeEvent(entityIn, CraftVillager.nmsToBukkitProfession(profession), VillagerCareerChangeEvent.ChangeReason.EMPLOYED);
                if (!event.isCancelled()) {
                    VillagerData newData = entityIn.getVillagerData().setProfession(CraftVillager.bukkitToNmsProfession(event.getProfession()));
                    entityIn.setVillagerData(newData);
                    entityIn.refreshBrain(worldIn);
                }
            });
        }
    }
}
