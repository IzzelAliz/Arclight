package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.izzel.arclight.common.bridge.bukkit.ItemMetaBridge;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.INBT;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.bukkit.craftbukkit.v.inventory.CraftMetaItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Mixin(value = CraftMetaItem.class, remap = false)
public class CraftMetaItemMixin implements ItemMetaBridge {

    // @formatter:off
    @Shadow(remap = false) @Final private Map<String, INBT> unhandledTags;
    // @formatter:on

    private static final Set<String> EXTEND_TAGS = ImmutableSet.of(
        "map_is_scaling",
        "map",
        "CustomPotionEffects",
        "Potion",
        "CustomPotionColor",
        "SkullOwner",
        "SkullProfile",
        "EntityTag",
        "BlockEntityTag",
        "title",
        "author",
        "pages",
        "resolved",
        "generation",
        "Fireworks",
        "StoredEnchantments",
        "Explosion",
        "Recipes",
        "BucketVariantTag",
        "Charged",
        "ChargedProjectiles",
        "Effects",
        "LodestoneDimension",
        "LodestonePos",
        "LodestoneTracked"
    );
    private CompoundNBT forgeCaps;

    @Override
    public CompoundNBT bridge$getForgeCaps() {
        return this.forgeCaps;
    }

    @Override
    public void bridge$setForgeCaps(CompoundNBT nbt) {
        this.forgeCaps = nbt;
    }

    @Override
    public void bridge$offerUnhandledTags(CompoundNBT nbt) {
        if (getClass().equals(CraftMetaItem.class)) {
            for (String s : nbt.keySet()) {
                if (EXTEND_TAGS.contains(s)) {
                    this.unhandledTags.put(s, nbt.get(s));
                }
            }
        }
    }

    @Override
    public Map<String, INBT> bridge$getUnhandledTags() {
        return this.unhandledTags;
    }

    @Override
    public void bridge$setUnhandledTags(Map<String, INBT> tags) {
        this.unhandledTags.putAll(tags);
    }

    @Inject(method = "serialize(Lcom/google/common/collect/ImmutableMap$Builder;)Lcom/google/common/collect/ImmutableMap$Builder;", at = @At("RETURN"))
    private void arclight$serializeForgeCaps(ImmutableMap.Builder<String, Object> builder, CallbackInfoReturnable<ImmutableMap.Builder<String, Object>> cir) throws IOException {
        if (this.forgeCaps != null) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            CompressedStreamTools.writeCompressed(this.forgeCaps, buf);
            builder.put("forgeCaps", Base64.encodeBase64String(buf.toByteArray()));
        }
    }

    @Inject(method = "clone", locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private void arclight$cloneTags(CallbackInfoReturnable<CraftMetaItem> cir, CraftMetaItem clone) {
        if (this.unhandledTags != null) {
            ((ItemMetaBridge) clone).bridge$getUnhandledTags().putAll(this.unhandledTags);
        }
        if (this.forgeCaps != null) {
            ((ItemMetaBridge) clone).bridge$setForgeCaps(this.forgeCaps.copy());
        }
    }

    @ModifyVariable(method = "applyHash", index = 1, at = @At("RETURN"))
    private int arclight$applyForgeCapsHash(int hash) {
        return 61 * hash + (this.forgeCaps != null ? this.forgeCaps.hashCode() : 0);
    }

    @Inject(method = "equalsCommon", cancellable = true, at = @At("HEAD"))
    private void arclight$forgeCapsEquals(CraftMetaItem that, CallbackInfoReturnable<Boolean> cir) {
        CompoundNBT forgeCaps = ((ItemMetaBridge) that).bridge$getForgeCaps();
        boolean ret;
        if (this.forgeCaps == null) {
            ret = forgeCaps != null && forgeCaps.size() != 0;
        } else {
            ret = forgeCaps == null ? this.forgeCaps.size() != 0 : !this.forgeCaps.equals(forgeCaps);
        }
        if (ret) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "<init>(Ljava/util/Map;)V", at = @At("RETURN"))
    private void arclight$extractForgeCaps(Map<String, Object> map, CallbackInfo ci) {
        if (map.containsKey("forgeCaps")) {
            Object forgeCaps = map.get("forgeCaps");
            try {
                ByteArrayInputStream buf = new ByteArrayInputStream(Base64.decodeBase64(forgeCaps.toString()));
                this.forgeCaps = CompressedStreamTools.readCompressed(buf);
            } catch (IOException e) {
                LogManager.getLogger(getClass()).error("Reading forge caps", e);
            }
        }
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void arclight$copyForgeCaps( CraftMetaItem meta, CallbackInfo ci) {
        CompoundNBT forgeCaps = ((ItemMetaBridge) meta).bridge$getForgeCaps();
        if (forgeCaps != null) {
            this.forgeCaps = forgeCaps.copy();
        }
    }
}
