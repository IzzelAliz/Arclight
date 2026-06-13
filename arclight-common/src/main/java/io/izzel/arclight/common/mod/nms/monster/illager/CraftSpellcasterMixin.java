package io.izzel.arclight.common.mod.nms.monster.illager;

import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import org.bukkit.craftbukkit.entity.CraftSpellcaster;
import org.bukkit.entity.Spellcaster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = CraftSpellcaster.class, remap = false)
public class CraftSpellcasterMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static Spellcaster.Spell toBukkitSpell(SpellcasterIllager.IllagerSpell spell) {
        return ArclightIllagerSpellHelper.toBukkit(spell);
    }
}
