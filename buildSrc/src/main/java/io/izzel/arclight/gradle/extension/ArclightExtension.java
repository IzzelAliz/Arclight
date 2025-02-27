package io.izzel.arclight.gradle.extension;

import io.izzel.arclight.gradle.api.extension.IArclightExtension;
import io.izzel.arclight.gradle.api.extension.IArclightMappingsExtension;
import org.gradle.api.Action;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.Path;

public class ArclightExtension implements IArclightExtension {
    private Path cacheDir;
    private String mcVersion;
    private String bukkitVersion;
    private File accessTransformer;
    private File extraMapping;
    private final IArclightMappingsExtension mappingsConfiguration = new ArclightMappingsExtension();

    public ArclightExtension(Project project) {
        this.cacheDir = project.getRootProject().getRootDir().toPath().resolve(".gradle/arclight");
    }

    @Override
    public Path getCacheDir() {
        return cacheDir;
    }

    @Override
    public void setCacheDir(Path path) {
        cacheDir = path;
    }

    @Override
    public String getMcVersion() {
        return mcVersion;
    }

    @Override
    public void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion;
    }

    @Override
    public String getBukkitVersion() {
        return bukkitVersion;
    }

    @Override
    public void setBukkitVersion(String bukkitVersion) {
        this.bukkitVersion = bukkitVersion;
    }

    @Override
    public File getAccessTransformer() {
        return accessTransformer;
    }

    @Override
    public void setAccessTransformer(File accessTransformer) {
        this.accessTransformer = accessTransformer;
    }

    @Override
    public File getExtraMapping() {
        return extraMapping;
    }

    @Override
    public void setExtraMapping(File extraMapping) {
        this.extraMapping = extraMapping;
    }

    @Override
    public IArclightMappingsExtension getMappingsConfiguration() {
        return mappingsConfiguration;
    }

    @Override
    public void mappings(Action<IArclightMappingsExtension> spec) {
        spec.execute(mappingsConfiguration);
    }
}
