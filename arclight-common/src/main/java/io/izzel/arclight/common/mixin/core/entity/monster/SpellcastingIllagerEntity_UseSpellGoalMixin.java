package io.izzel.arclight.common.mixin.core.entity.monster;

import net.minecraft.entity.monster.SpellcastingIllagerEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpellcastingIllagerEntity.UseSpellGoal.class)
public abstract class SpellcastingIllagerEntity_UseSpellGoalMixin {

    // @formatter:off
    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "field_193323_e"}, remap = false) private SpellcastingIllagerEntity outerThis;
    @Shadow protected abstract SpellcastingIllagerEntity.SpellType getSpellType();
    // @formatter:on

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/SpellcastingIllagerEntity$UseSpellGoal;castSpell()V"))
    private void arclight$castSpell(CallbackInfo ci) {
        if (!CraftEventFactory.handleEntitySpellCastEvent(outerThis, this.getSpellType())) {
            ci.cancel();
        }
    }
}
