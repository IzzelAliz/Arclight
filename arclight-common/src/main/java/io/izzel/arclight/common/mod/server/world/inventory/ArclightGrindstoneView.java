package io.izzel.arclight.common.mod.server.world.inventory;

import net.minecraft.world.inventory.GrindstoneMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryGrindstone;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;

public class ArclightGrindstoneView extends CraftInventoryView<GrindstoneMenu, CraftInventoryGrindstone> {
    public ArclightGrindstoneView(HumanEntity player, CraftInventoryGrindstone viewing, GrindstoneMenu container) {
        super(player, viewing, container);
    }
}
