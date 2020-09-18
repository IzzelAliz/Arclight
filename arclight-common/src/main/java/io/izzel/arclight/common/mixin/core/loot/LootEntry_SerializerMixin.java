package io.izzel.arclight.common.mixin.core.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.loot.LootEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LootEntry.Serializer.class)
public abstract class LootEntry_SerializerMixin<T extends LootEntry> {

    // @formatter:off
    @Shadow public abstract void serialize(JsonObject p_230424_1_, T p_230424_2_, JsonSerializationContext p_230424_3_);
    @Shadow public abstract T deserialize(JsonObject p_230423_1_, JsonDeserializationContext p_230423_2_);
    // @formatter:on

    public final void a(JsonObject object, T t0, JsonSerializationContext context) {
        this.serialize(object, t0, context);
    }

    public final T a(JsonObject object, JsonDeserializationContext context) {
        return this.deserialize(object, context);
    }
}
