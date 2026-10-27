package io.izzel.arclight.common.mixin.bukkit.util;

import io.izzel.arclight.common.bridge.core.world.level.IWorldWriterBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import org.bukkit.craftbukkit.util.BlockStateListPopulator;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockStateListPopulator.class)
public class BlockStateListPopulatorMixin implements IWorldWriterBridge {
    @Shadow @Final private LevelAccessor level;

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return ((IWorldWriterBridge) this.level).bridge$addEntity(entity, reason);
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        ((IWorldWriterBridge) this.level).bridge$pushAddEntityReason(reason);
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return ((IWorldWriterBridge) this.level).bridge$getAddEntityReason();
    }
}
