package io.izzel.arclight.gradle.api.extension;

import org.gradle.api.Action;

import java.io.File;
import java.nio.file.Path;

public interface IArclightExtension {
    Path getCacheDir();
    void setCacheDir(Path path);

    String getMcVersion();
    void setMcVersion(String mcVersion);

    String getBukkitVersion();
    void setBukkitVersion(String bukkitVersion);

    File getAccessTransformer();
    void setAccessTransformer(File accessTransformer);

    File getExtraMapping();
    void setExtraMapping(File extraMapping);

    IArclightMappingsExtension getMappingsConfiguration();

    void mappings(Action<IArclightMappingsExtension> spec);
}
