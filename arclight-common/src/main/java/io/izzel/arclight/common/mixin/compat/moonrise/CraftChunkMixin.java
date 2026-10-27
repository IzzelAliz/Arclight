package io.izzel.arclight.common.mixin.compat.moonrise;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.CraftChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftChunk.class, remap = false)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.PRESENT)
public abstract class CraftChunkMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private int x;
    @Shadow @Final private int z;

    @Inject(method = "getEntities", at = @At("HEAD"), cancellable = true)
    private void arclight$entities(CallbackInfoReturnable<org.bukkit.entity.Entity[]> cir) {
        org.spigotmc.AsyncCatcher.catchOp("getting chunk entities");
        var entities = new java.util.ArrayList<org.bukkit.entity.Entity>();
        for (var entity : this.level.getAllEntities()) {
            var pos = entity.chunkPosition();
            if (pos.x() == this.x && pos.z() == this.z) {
                var bukkit = entity.getBukkitEntity();
                if (bukkit != null && bukkit.isValid()) entities.add(bukkit);
            }
        }
        cir.setReturnValue(entities.toArray(org.bukkit.entity.Entity[]::new));
    }

    @Inject(method = "isEntitiesLoaded", at = @At("HEAD"), cancellable = true)
    private void arclight$entitiesLoaded(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.level.getChunkSource().getChunkNow(this.x, this.z) != null);
    }
}
