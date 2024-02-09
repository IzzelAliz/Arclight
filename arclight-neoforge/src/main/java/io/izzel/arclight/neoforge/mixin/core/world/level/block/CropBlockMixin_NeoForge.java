package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CropBlock.class)
public abstract class CropBlockMixin_NeoForge {

    @Redirect(method = "entityInside", at = @At(value = "INVOKE", remap = false, target = "Lnet/neoforged/neoforge/event/EventHooks;getMobGriefingEvent(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean arclight$entityChangeBlock(Level world, Entity entity, BlockState state, Level worldIn, BlockPos pos) {
        boolean result = EventHooks.getMobGriefingEvent(world, entity);
        return !CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state, result);
    }

}
