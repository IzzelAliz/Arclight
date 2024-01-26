package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.world.entity.monster.SpellcasterIllager;
import org.bukkit.craftbukkit.v.entity.CraftSpellcaster;
import org.bukkit.entity.Spellcaster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CraftSpellcaster.class, remap = false)
public class CraftSpellcasterMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Spellcaster.Spell toBukkitSpell(SpellcasterIllager.IllagerSpell spell) {
        try {
            return Spellcaster.Spell.valueOf(spell.name());
        } catch (IllegalArgumentException e) {
            var newTypes = new ArrayList<Spellcaster.Spell>();
            var forgeCount = SpellcasterIllager.IllagerSpell.values().length;
            for (var id = Spellcaster.Spell.values().length; id < forgeCount; id++) {
                var name = SpellcasterIllager.IllagerSpell.values()[id].name();
                var newPhase = EnumHelper.makeEnum(Spellcaster.Spell.class, name, id, List.of(), List.of());
                newTypes.add(newPhase);
                ArclightServer.LOGGER.debug("Registered {} as illager spell {}", name, newPhase);
            }
            EnumHelper.addEnums(Spellcaster.Spell.class, newTypes);
            return toBukkitSpell(spell);
        }
    }
}
