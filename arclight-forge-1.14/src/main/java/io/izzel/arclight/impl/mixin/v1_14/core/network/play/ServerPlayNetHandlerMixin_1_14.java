package io.izzel.arclight.impl.mixin.v1_14.core.network.play;

import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.StringNBT;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerPlayNetHandler.class)
public abstract class ServerPlayNetHandlerMixin_1_14 implements ServerPlayNetHandlerBridge {

    @Override
    public boolean bridge$worldNoCollision(ServerWorld world, Entity entity, AxisAlignedBB aabb) {
        return world.isCollisionBoxesEmpty(entity, aabb);
    }

    @Override
    public StringNBT bridge$stringNbt(String s) {
        return new StringNBT(s);
    }

    @Override
    public void bridge$dropItems(ServerPlayerEntity player, boolean all) {
        player.dropItem(all);
    }
}
