package io.izzel.arclight.common.mod.nms.monster.illager;

import io.izzel.arclight.api.EnumHelper;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import org.bukkit.entity.Spellcaster;

import java.util.ArrayList;
import java.util.List;

public final class ArclightIllagerSpellHelper {

    private ArclightIllagerSpellHelper() {
    }

    public static Spellcaster.Spell toBukkit(SpellcasterIllager.IllagerSpell spell) {
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
            return toBukkit(spell);
        }
    }
}
