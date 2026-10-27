package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.PosContainerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerLoomPatternSelectEvent;
import net.minecraft.core.Holder;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import java.util.List;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.LoomMenu;
import org.bukkit.craftbukkit.block.banner.CraftPatternType;
import org.bukkit.craftbukkit.inventory.CraftInventoryLoom;
import org.bukkit.craftbukkit.inventory.view.CraftLoomView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoomMenu.class)
public abstract class LoomMenuMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    // @formatter:off
    @Shadow @Final private Container inputContainer;
    @Shadow @Final private Container outputContainer;
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow private List<Holder<BannerPattern>> selectablePatterns;
    // @formatter:on

    @Decorate(method = "clickMenuButton", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/DataSlot;set(I)V"), require = 1)
    private void arclight$selectPattern(DataSlot selectedIndex, int buttonId, @Local(ordinal = 0) Player player,
                                       @Local(allocate = "arclightSelectedPattern") Holder<BannerPattern> selectedPattern) throws Throwable {
        var event = new PlayerLoomPatternSelectEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
            this.getBukkitView().getTopInventory(), CraftPatternType.minecraftHolderToBukkit(this.selectablePatterns.get(buttonId)));
        if (!event.callEvent()) {
            player.containerMenu.sendAllDataToRemote();
            DecorationOps.cancel().invoke(false);
            return;
        }

        Holder<BannerPattern> eventPattern = CraftPatternType.bukkitToMinecraftHolder(event.getPatternType());
        int patternIndex = -1;
        selectedPattern = eventPattern;
        for (int i = 0; i < this.selectablePatterns.size(); i++) {
            Holder<BannerPattern> holder = this.selectablePatterns.get(i);
            if (eventPattern.equals(holder)) {
                patternIndex = i;
                selectedPattern = holder;
                break;
            }
        }

        player.containerMenu.sendAllDataToRemote();
        DecorationOps.blackhole().invoke(selectedPattern);
        DecorationOps.callsite().invoke(selectedIndex, patternIndex);
    }

    @Decorate(method = "clickMenuButton", at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;"), require = 1)
    private Object arclight$selectedPattern(List<Holder<BannerPattern>> patterns, int buttonId,
                                           @Local(allocate = "arclightSelectedPattern") Holder<BannerPattern> selectedPattern) throws Throwable {
        // Keep the lookup continuation, but do not re-index the live list after a plugin edit.
        // An event-selected pattern need not be selectable (its published index is then -1).
        return DecorationOps.callsite().invoke(List.of(selectedPattern), 0);
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(method = "lambda$setupResultSlot$0", index = 0,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BannerPatternLayers$Builder;addAll(Lnet/minecraft/world/level/block/entity/BannerPatternLayers;)Lnet/minecraft/world/level/block/entity/BannerPatternLayers$Builder;"))
    private static BannerPatternLayers arclight$capExistingPatterns(BannerPatternLayers layers) {
        return layers.layers().size() > 20
            ? new BannerPatternLayers(List.copyOf(layers.layers().subList(0, 20))) : layers;
    }

    private CraftLoomView bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int id, Inventory playerInventory, ContainerLevelAccess worldCallable, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) {
            cir.setReturnValue(true);
        }
    }

    @Override
    public CraftLoomView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryLoom inventory = new CraftInventoryLoom(this.inputContainer, this.outputContainer);
        bukkitEntity = new CraftLoomView(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (LoomMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public ContainerLevelAccess bridge$getWorldPos() {
        return this.access;
    }
}
