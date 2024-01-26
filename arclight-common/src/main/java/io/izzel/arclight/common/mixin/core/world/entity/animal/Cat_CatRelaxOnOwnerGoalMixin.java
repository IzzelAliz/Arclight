package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.minecraft.world.entity.animal.Cat$CatRelaxOnOwnerGoal")
public class Cat_CatRelaxOnOwnerGoalMixin {

    @Shadow @Final private Cat cat;

    @Redirect(method = "giveMorningGift", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$dropItem(Level instance, Entity entity) {
        var event = new EntityDropItemEvent(this.cat.bridge$getBukkitEntity(), (Item) entity.bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return instance.addFreshEntity(entity);
        }
        return false;
    }
}
