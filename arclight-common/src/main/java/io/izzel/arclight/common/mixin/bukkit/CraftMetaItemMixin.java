package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.izzel.arclight.common.bridge.bukkit.ItemMetaBridge;
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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;

@Mixin(value = CraftMetaItem.class, remap = false)
public class CraftMetaItemMixin implements ItemMetaBridge {

    // @formatter:off
    @Shadow(remap = false) @Final private Map<String, Tag> unhandledTags;
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

    @ModifyVariable(method = "<init>(Lnet/minecraft/nbt/CompoundTag;)V", at = @At(value = "INVOKE", target = "Lorg/bukkit/UnsafeValues;getDataVersion()I"))
    private CompoundTag arclight$provideTag(CompoundTag tag) {
        return tag == null ? new CompoundTag() : tag;
    }

    private CompoundTag forgeCaps;

    @Override
    public CompoundTag bridge$getForgeCaps() {
        return this.forgeCaps;
    }

    @Override
    public void bridge$setForgeCaps(CompoundTag nbt) {
        this.forgeCaps = nbt;
    }

    @Override
    public void bridge$offerUnhandledTags(CompoundTag nbt) {
        if (getClass().equals(CraftMetaItem.class)) {
            for (String s : nbt.getAllKeys()) {
                if (EXTEND_TAGS.contains(s)) {
                    this.unhandledTags.put(s, nbt.get(s));
                }
            }
        }
    }

    @Override
    public Map<String, Tag> bridge$getUnhandledTags() {
        return this.unhandledTags;
    }

    @Override
    public void bridge$setUnhandledTags(Map<String, Tag> tags) {
        this.unhandledTags.putAll(tags);
    }

    @Inject(method = "serialize(Lcom/google/common/collect/ImmutableMap$Builder;)Lcom/google/common/collect/ImmutableMap$Builder;", at = @At("RETURN"))
    private void arclight$serializeForgeCaps(ImmutableMap.Builder<String, Object> builder, CallbackInfoReturnable<ImmutableMap.Builder<String, Object>> cir) throws IOException {
        if (this.forgeCaps != null) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            NbtIo.writeCompressed(this.forgeCaps, buf);
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

    @Inject(method = "isEmpty", cancellable = true, at = @At("HEAD"))
    private void arclight$forgeCapsEmpty(CallbackInfoReturnable<Boolean> cir) {
        if (this.forgeCaps != null && !this.forgeCaps.isEmpty()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "equalsCommon", cancellable = true, at = @At("HEAD"))
    private void arclight$forgeCapsEquals(CraftMetaItem that, CallbackInfoReturnable<Boolean> cir) {
        CompoundTag forgeCaps = ((ItemMetaBridge) that).bridge$getForgeCaps();
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
                this.forgeCaps = NbtIo.readCompressed(buf);
            } catch (IOException e) {
                LogManager.getLogger(getClass()).error("Reading forge caps", e);
            }
        }
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    private void arclight$copyForgeCaps(CraftMetaItem meta, CallbackInfo ci) {
        if (meta != null) {
            CompoundTag forgeCaps = ((ItemMetaBridge) meta).bridge$getForgeCaps();
            if (forgeCaps != null) {
                this.forgeCaps = forgeCaps.copy();
            }
        }
    }
}
