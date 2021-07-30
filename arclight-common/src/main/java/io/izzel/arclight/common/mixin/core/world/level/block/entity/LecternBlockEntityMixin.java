package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.LecternContainerBridge;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.command.CraftBlockCommandSender;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntityMixin implements CommandSource, ICommandSourceBridge {

    // @formatter:off
    @Shadow @Final public Container bookAccess;
    @Shadow @Final private ContainerData dataAccess;
    // @formatter:on

    @Redirect(method = "createCommandSourceStack", at = @At(value = "NEW", target = "net/minecraft/commands/CommandSourceStack"))
    private CommandSourceStack arclight$source(CommandSource source, Vec3 vec3d, Vec2 vec2f, ServerLevel world, int i, String s, Component component, MinecraftServer server, @Nullable Entity entity) {
        return new CommandSourceStack(this, vec3d, vec2f, world, i, s, component, server, entity);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public AbstractContainerMenu createMenu(int i, Inventory playerInventory, Player entity) {
        LecternMenu container = new LecternMenu(i, this.bookAccess, this.dataAccess);
        ((LecternContainerBridge) container).bridge$setPlayerInventory(playerInventory);
        return container;
    }

    @Override
    public void sendMessage(@NotNull Component component, @NotNull UUID uuid) {
    }

    @Override
    public boolean acceptsSuccess() {
        return false;
    }

    @Override
    public boolean acceptsFailure() {
        return false;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return wrapper.getEntity() != null ? ((EntityBridge) wrapper.getEntity()).bridge$getBukkitSender(wrapper) : new CraftBlockCommandSender(wrapper, (BlockEntity) (Object) this);
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }
}
