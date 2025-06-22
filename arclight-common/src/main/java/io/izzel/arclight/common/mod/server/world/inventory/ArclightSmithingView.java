package io.izzel.arclight.common.mod.server.world.inventory;

import net.minecraft.world.inventory.SmithingMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventorySmithing;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;

public class ArclightSmithingView extends CraftInventoryView<SmithingMenu, CraftInventorySmithing> {
    public ArclightSmithingView(HumanEntity player, CraftInventorySmithing viewing, SmithingMenu container) {
        super(player, viewing, container);
    }
}
