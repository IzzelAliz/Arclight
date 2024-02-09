package io.izzel.arclight.common.mixin.core.world.entity.monster;

import net.minecraft.world.entity.monster.SpellcasterIllager;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpellcasterIllager.SpellcasterUseSpellGoal.class)
public abstract class SpellcastingIllager_UseSpellGoalMixin {
    @SuppressWarnings("target")
    @Shadow(aliases = {"this$0", "f_33776_", "field_7386"}, remap = false)
    private SpellcasterIllager outerThis;

    @Shadow
    protected abstract SpellcasterIllager.IllagerSpell getSpell();

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/SpellcasterIllager$SpellcasterUseSpellGoal;performSpellCasting()V"))
    private void arclight$castSpell(CallbackInfo ci) {
        if (!CraftEventFactory.handleEntitySpellCastEvent(outerThis, this.getSpell())) {
            ci.cancel();
        }
    }
}