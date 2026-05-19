package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.common.mod.server.ArclightServer;
import org.bukkit.craftbukkit.v.entity.CraftSpellcaster;
import org.bukkit.entity.Spellcaster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CraftSpellcaster.class, remap = false)
public class CraftSpellcasterMixin {

    @Redirect(method = "toBukkitSpell", at = @At(value = "INVOKE", target = "Lorg/bukkit/entity/Spellcaster$Spell;valueOf(Ljava/lang/String;)Lorg/bukkit/entity/Spellcaster$Spell;"))
    private static Spellcaster.Spell arclight$toBukkitSpell(String name) {
        try {
            return Spellcaster.Spell.valueOf(name);
        } catch (IllegalArgumentException e) {
            var newTypes = new ArrayList<Spellcaster.Spell>();
            var values = Spellcaster.Spell.values();
            var newPhase = EnumHelper.makeEnum(Spellcaster.Spell.class, name, values.length, List.of(), List.of());
            newTypes.add(newPhase);
            ArclightServer.LOGGER.debug("Registered {} as illager spell {}", name, newPhase);
            EnumHelper.addEnums(Spellcaster.Spell.class, newTypes);
            return Spellcaster.Spell.valueOf(name);
        }
    }
}
