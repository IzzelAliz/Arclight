package io.izzel.arclight.common.mod.server.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.bukkit.craftbukkit.v.CraftWorld;

public class LevelPersistentData extends SavedData {

    public static final Codec<LevelPersistentData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        CompoundTag.CODEC.optionalFieldOf("BukkitValues", new CompoundTag()).forGetter(LevelPersistentData::getBukkitValues)
    ).apply(instance, LevelPersistentData::new));

    public static final SavedDataType<LevelPersistentData> TYPE = new SavedDataType<>("bukkit_pdc", LevelPersistentData::new, CODEC, ArclightConstants.BUKKIT_PDC);

    private CompoundTag tag;

    public LevelPersistentData() {
        this(new CompoundTag());
    }

    public LevelPersistentData(CompoundTag tag) {
        this.tag = tag == null ? new CompoundTag() : tag;
    }

    public CompoundTag getTag() {
        return tag;
    }

    private CompoundTag getBukkitValues() {
        return this.tag.getCompound("BukkitValues").orElseGet(CompoundTag::new);
    }

    public void save(CraftWorld world) {
        this.tag = new CompoundTag();
        world.storeBukkitValues(this.tag);
        this.setDirty();
    }
}
