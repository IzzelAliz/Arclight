package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityDyeEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Cat.class)
public abstract class CatMixin extends AnimalMixin {

    @Decorate(method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/feline/Cat;setCollarColor(Lnet/minecraft/world/item/DyeColor;)V"), require = 1, allow = 1)
    private void arclight$dyeCollar(Cat cat, DyeColor color, Player player, InteractionHand hand) throws Throwable {
        // The native caller has already checked ownership, dye tags/components and a changed color.
        var event = new EntityDyeEvent(this.getBukkitEntity(), org.bukkit.DyeColor.getByWoolData((byte) color.getId()),
            (org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity());
        if (!event.callEvent()) {
            // Exit before the native consume and persistence tail, not merely the setter.
            DecorationOps.cancel().invoke((InteractionResult) InteractionResult.FAIL);
            return;
        }
        color = DyeColor.byId(event.getColor().getWoolData());
        DecorationOps.callsite().invoke(cat, color);
    }

    @Decorate(method = "tryToTame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$tame(RandomSource instance, int i, Player player) throws Throwable {
        var ret = (int) DecorationOps.callsite().invoke(instance, i);
        return ret == 0 && this.bridge$common$animalTameEvent(player) ? ret : 1;
    }
}
