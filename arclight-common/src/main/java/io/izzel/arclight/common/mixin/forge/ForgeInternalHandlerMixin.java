package io.izzel.arclight.common.mixin.forge;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.Entity;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeInternalHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ForgeInternalHandler.class)
public class ForgeInternalHandlerMixin {

    // Workaround for MinecraftForge#7519
    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$addDuringWorldGen(World world, Entity entityIn) {
        if (world instanceof ServerWorld && ArclightCaptures.isWorldGenAdd()) {
            world.getServer().enqueue(new TickDelayedTask(world.getServer().getTickCounter(), () -> {
                IChunk ichunk = world.getChunk(MathHelper.floor(entityIn.getPosX() / 16.0D), MathHelper.floor(entityIn.getPosZ() / 16.0D), ChunkStatus.FULL, entityIn.forceSpawn);
                if (ichunk instanceof Chunk) {
                    ichunk.addEntity(entityIn);
                }
            }));
            return ((ServerWorld) world).addEntityIfNotDuplicate(entityIn);
        }
        return world.addEntity(entityIn);
    }
}
