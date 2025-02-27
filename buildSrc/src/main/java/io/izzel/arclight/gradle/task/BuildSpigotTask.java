package io.izzel.arclight.gradle.task;

import io.izzel.arclight.gradle.runnable.SpigotBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.nio.file.Path;

public abstract class BuildSpigotTask extends DefaultTask {

    private final Project project;

    @InputFile
    public abstract Property<Path> getBuildToolsJar();

    @InputDirectory
    public abstract Property<Path> getWorkDir();

    @InputDirectory
    public abstract Property<Path> getOutputDir();

    @Input
    public abstract Property<String> getMinecraftVersion();

    @Inject
    public BuildSpigotTask(Project project) {
        this.project = project;
    }

    @TaskAction
    public void build() {
        var builder = project.getObjects().newInstance(SpigotBuilder.class);
        builder.setBuildToolsJar(getBuildToolsJar().get());
        builder.setWorkDir(getWorkDir().get());
        builder.setOutputDir(getOutputDir().get());
        builder.setMinecraftVersion(getMinecraftVersion().get());
    }
}
