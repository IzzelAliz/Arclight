package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.common.mod.server.ArclightServer;
import org.bukkit.craftbukkit.v.entity.CraftSpellcaster;
import org.bukkit.entity.Spellcaster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CraftSpellcaster.class, remap = false)
public class CraftSpellcasterMixin {

    // CraftBukkit's method name is stable and unique; its NMS parameter descriptor
    // differs across loader namespaces while this host has remap = false.
    @Inject(method = "toBukkitSpell", at = @At("HEAD"))
    private static void arclight$toBukkitSpell(@Coerce Enum<?> spell, CallbackInfoReturnable<Spellcaster.Spell> cir) {
        try {
            Spellcaster.Spell.valueOf(spell.name());
            return;
        } catch (IllegalArgumentException e) {
            var newTypes = new ArrayList<Spellcaster.Spell>();
            var nmsValues = spell.getDeclaringClass().getEnumConstants();
            for (var id = Spellcaster.Spell.values().length; id < nmsValues.length; id++) {
                var newSpell = EnumHelper.makeEnum(Spellcaster.Spell.class, nmsValues[id].name(), id, List.of(), List.of());
                newTypes.add(newSpell);
                ArclightServer.LOGGER.debug("Registered {} as illager spell {}", nmsValues[id].name(), newSpell);
            }
            EnumHelper.addEnums(Spellcaster.Spell.class, newTypes);
        }
    }
}
