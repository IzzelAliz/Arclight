package io.izzel.arclight.common.mixin.core.world.entity.monster;

import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import io.izzel.arclight.api.EnumHelper;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Spellcaster;
import org.bukkit.event.entity.EntitySpellCastEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/world/entity/monster/illager/SpellcasterIllager$SpellcasterUseSpellGoal")
public abstract class SpellcastingIllager_UseSpellGoalMixin {
    @SuppressWarnings("target")
    @Shadow(aliases = {"this$0", "f_33776_", "field_7386"}, remap = false)
    private SpellcasterIllager outerThis;

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/illager/SpellcasterIllager$SpellcasterUseSpellGoal;performSpellCasting()V"))
    private void arclight$castSpell(CallbackInfo ci) {
        if (!arclight$handleEntitySpellCastEvent(outerThis, arclight$getSpell())) {
            ci.cancel();
        }
    }

    @Unique
    private Enum<?> arclight$getSpell() {
        for (var method : this.getClass().getSuperclass().getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getReturnType().isEnum()) {
                try {
                    method.setAccessible(true);
                    return (Enum<?>) method.invoke(this);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalStateException("Cannot find spell getter");
    }

    private static boolean arclight$handleEntitySpellCastEvent(SpellcasterIllager caster, Enum<?> spell) {
        EntitySpellCastEvent event = new EntitySpellCastEvent((Spellcaster) ((io.izzel.arclight.common.bridge.core.entity.EntityBridge) caster).bridge$getBukkitEntity(), arclight$toBukkitSpell(spell));
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private static Spellcaster.Spell arclight$toBukkitSpell(Enum<?> spell) {
        try {
            return Spellcaster.Spell.valueOf(spell.name());
        } catch (IllegalArgumentException e) {
            // Match CraftSpellcaster's extension path without naming the protected NMS enum.
            var newTypes = new ArrayList<Spellcaster.Spell>();
            var nmsValues = spell.getDeclaringClass().getEnumConstants();
            for (var id = Spellcaster.Spell.values().length; id < nmsValues.length; id++) {
                newTypes.add(EnumHelper.makeEnum(Spellcaster.Spell.class, nmsValues[id].name(), id, List.of(), List.of()));
            }
            EnumHelper.addEnums(Spellcaster.Spell.class, newTypes);
            return Spellcaster.Spell.valueOf(spell.name());
        }
    }
}