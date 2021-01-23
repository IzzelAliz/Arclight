package io.izzel.arclight.common.mixin.forge;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
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
        return world instanceof ServerWorld && ArclightCaptures.isWorldGenAdd()
            ? ((ServerWorld) world).addEntityIfNotDuplicate(entityIn)
            : world.addEntity(entityIn);
    }
}
