package io.izzel.arclight.neoforge.mixin.core.world.entity.npc;

import io.izzel.arclight.common.bridge.core.world.entity.npc.VillagerBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.neoforged.neoforge.event.EventHooks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Villager.class)
public abstract class VillagerMixin_NeoForge implements VillagerBridge {

    @Override
    public boolean bridge$forge$canZombify() {
        // The API has no killedEntity caller to provide Neo's normal infection veto.
        return EventHooks.canLivingConvert((Villager) (Object) this, EntityType.ZOMBIE_VILLAGER, timer -> {});
    }

    @Override
    public void bridge$forge$finalizeZombifySpawn(ServerLevel level, ZombieVillager zombie) {
        // Neo's finalizeSpawn redirector only visits selected native host classes,
        // not the default bridge method. Enter its real mod-editable hook explicitly.
        EventHooks.finalizeMobSpawn(zombie, level, level.getCurrentDifficultyAt(zombie.blockPosition()), EntitySpawnReason.CONVERSION, new Zombie.ZombieGroupData(false, true));
    }
}
