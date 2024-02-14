package io.izzel.arclight.fabric.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.fabric.mixin.core.world.entity.LivingEntityMixin_Fabric;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Player.class)
public abstract class PlayerMixin_Fabric extends LivingEntityMixin_Fabric implements PlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract Abilities getAbilities();
    // @formatter:on
}
