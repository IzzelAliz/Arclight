package io.izzel.arclight.common.bridge.tileentity;

import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface AbstractFurnaceTileEntityBridge {

    List<Recipe<?>> bridge$dropExp(Level world, Vec3 pos, Player entity, ItemStack itemStack, int amount);
}
