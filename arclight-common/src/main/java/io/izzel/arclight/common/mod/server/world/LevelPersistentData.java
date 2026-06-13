package io.izzel.arclight.common.mod.server.world;

import com.mojang.serialization.Codec;
import io.izzel.arclight.common.mod.ArclightConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.bukkit.craftbukkit.CraftWorld;
import org.jetbrains.annotations.NotNull;

public class LevelPersistentData extends SavedData {

    public static final Codec<LevelPersistentData> CODEC = CompoundTag.CODEC.xmap(LevelPersistentData::new, data -> data.tag);

    public static final SavedDataType<LevelPersistentData> TYPE = new SavedDataType<>(
        Identifier.withDefaultNamespace("bukkit_pdc"),
        LevelPersistentData::new,
        CODEC,
        ArclightConstants.BUKKIT_PDC
    );

    private CompoundTag tag;

    public LevelPersistentData() {
        this.tag = new CompoundTag();
    }

    public LevelPersistentData(CompoundTag tag) {
        this.tag = tag == null ? new CompoundTag() : tag;
    }

    public CompoundTag getTag() {
        return tag;
    }

    public void save(CraftWorld world) {
        this.tag = new CompoundTag();
        world.storeBukkitValues(this.tag);
        this.setDirty();
    }
}
