package io.izzel.arclight.common.mixin.vanilla.world.entity.player;

import io.izzel.arclight.common.mixin.vanilla.world.entity.LivingEntityMixin_Vanilla;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin_Vanilla extends LivingEntityMixin_Vanilla {

    // @formatter:off
    @Shadow public abstract Abilities getAbilities();
    // @formatter:on
}
