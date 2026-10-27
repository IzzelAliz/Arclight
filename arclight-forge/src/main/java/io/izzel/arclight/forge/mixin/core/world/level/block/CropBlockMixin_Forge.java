package io.izzel.arclight.forge.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CropBlock.class)
public class CropBlockMixin_Forge {
    @Redirect(method = "entityInside", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/event/ForgeEventFactory;getMobGriefingEvent(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;)Z"))
    public boolean arclight$entityChangeBlock(net.minecraft.server.level.ServerLevel world, Entity entity, BlockState state, Level worldIn, BlockPos pos, Entity insideEntity, net.minecraft.world.entity.InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        boolean result = ForgeEventFactory.getMobGriefingEvent((net.minecraft.server.level.ServerLevel) world, entity);
        return !CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state, result);
    }

}
