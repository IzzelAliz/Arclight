package io.izzel.arclight.gradle.runnable;

import io.izzel.arclight.gradle.api.extension.IArclightSpigotExtension;
import io.izzel.arclight.gradle.util.GitOps;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.gradle.api.GradleException;
import org.gradle.process.ExecOperations;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.jar.JarFile;

public class SpigotBuilder implements Runnable {

    private final ExecOperations execOperations;

    @Getter
    @Setter
    private Path buildToolsJar;

    @Getter
    @Setter
    private Path workDir;

    @Getter
    @Setter
    private Path outputDir;

    /**
     * Minecraft version to build spigot.
     */
    @Getter
    @Setter
    private String minecraftVersion;

    /**
     * The specific commit refs.
     * Todo: add a task to use it.
     */
    @Getter
    @Setter
    @Nullable
    private IArclightSpigotExtension extension;

    @Getter
    @Setter
    private boolean forceRebuild = false;

    /**
     * Remove the work dir.
     */
    @Getter
    @Setter
    private boolean refreshCache = false;

    @Getter
    private Path outputJar;

    @Inject
    public SpigotBuilder(ExecOperations execOperations) {
        this.execOperations = execOperations;
    }

    @SneakyThrows
    @Override
    public void run() {
        Objects.requireNonNull(buildToolsJar);
        Objects.requireNonNull(workDir);
        Objects.requireNonNull(outputDir);
        Objects.requireNonNull(minecraftVersion);

        this.outputJar = outputDir.resolve("spigot-" + minecraftVersion + ".jar");

        if (forceRebuild) {
            Files.delete(outputDir);
        }

        Files.createDirectories(outputDir);

        if (Files.exists(workDir)) {
            if (refreshCache) {
                Files.delete(workDir);
            }
        }

        Files.createDirectories(workDir);

        if (extension != null) {
            checkout("Bukkit", "https://hub.spigotmc.org/stash/scm/spigot/bukkit.git", extension.getBukkitRef());
            checkout("CraftBukkit", "https://hub.spigotmc.org/stash/scm/spigot/craftbukkit.git", extension.getCraftBukkitRef());
            checkout("Spigot", "https://hub.spigotmc.org/stash/scm/spigot/spigot.git", extension.getSpigotRef());
            checkout("BuildData", "https://hub.spigotmc.org/stash/scm/spigot/builddata.git", extension.getBuildDataRef());
        }

        var exit = execOperations.exec(spec -> {
            spec.setWorkingDir(workDir.toFile());
            spec.setStandardOutput(System.out);

            if (extension == null) {
                spec.setCommandLine("java", "-jar", buildToolsJar.normalize().toString(), "--rev", minecraftVersion, "--compile-if-changed");
            } else {
                spec.setCommandLine("java", "-jar", buildToolsJar.normalize().toString(), "--dont-update", "--compile-if-changed");
            }

            spec.setIgnoreExitValue(true);
        }).getExitValue();

        if (exit == 0) {
            if (Files.exists(outputJar)) {
                Files.delete(outputJar);
            }
        } else if (exit == 2) {
            // No changes.
        } else {
            throw new GradleException("Failed to build spigot jar.");
        }

        var spigot = workDir.resolve("spigot-" + minecraftVersion + ".jar");
        var bundler = outputDir.resolve("spigot-" + minecraftVersion + "-bundler.jar");
        Files.move(spigot, bundler, StandardCopyOption.REPLACE_EXISTING);
        try (var jar = new JarFile(bundler.toFile())) {
            jar.stream().filter(e -> e.getName().startsWith("META-INF/versions/") && e.getName().endsWith(".jar"))
                    .limit(1)
                    .forEach(e -> {
                        try (var out = Files.newOutputStream(outputJar)) {
                            jar.getInputStream(e).transferTo(out);
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    });
        }
    }



    @SneakyThrows
    private void checkout(String dirName, String url, String refs) {
        var repo = workDir.resolve(dirName);

        if (!GitOps.isGitRepo(repo)) {
            Files.delete(repo);

            execOperations.exec(spec -> {
                spec.setWorkingDir(workDir);
                spec.setStandardOutput(System.out);
                spec.setCommandLine(GitOps.clone(repo, url));
            });
        }

        execOperations.exec(spec -> {
            spec.setWorkingDir(refs);
            spec.setStandardOutput(System.out);
            spec.setCommandLine(GitOps.checkout(refs));
        });
    }
}
