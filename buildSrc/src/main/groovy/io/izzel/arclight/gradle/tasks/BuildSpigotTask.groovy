package io.izzel.arclight.gradle.tasks

import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.*

import javax.inject.Inject
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.jar.JarFile

class BuildSpigotTask implements Runnable {

    private Project project

    @Inject
    BuildSpigotTask(Project project) {
        this.project = project
    }

    private String mcVersion

    @Input
    String getMcVersion() {
        return mcVersion
    }

    void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion
        this.outSpigot = new File(outputDir, "spigot-${mcVersion}.jar")
    }

    private File buildTools

    @InputFile
    File getBuildTools() {
        return buildTools
    }

    void setBuildTools(File buildTools) {
        this.buildTools = buildTools
    }

    private File outputDir

    @OutputDirectory
    File getOutputDir() {
        return outputDir
    }

    void setOutputDir(File outputDir) {
        this.outputDir = outputDir
    }

    private File workDir

    File getWorkDir() {
        return workDir
    }

    void setWorkDir(File workDir) {
        this.workDir = workDir
    }

    private File outSpigot

    @OutputFile
    File getOutSpigot() {
        return outSpigot
    }

    void setOutSpigot(File outSpigot) {
        this.outSpigot = outSpigot
    }

    @Override
    void run() {
        if (outSpigot.exists()) {
            return
        }
        outSpigot.parentFile.mkdirs()
        workDir.mkdirs()
        project.exec {
            workingDir = workDir
            commandLine = [
                    'java', '-jar', buildTools.canonicalPath, '--rev', mcVersion
            ]
            standardOutput = System.out
        }
        def spigot = new File(workDir, outSpigot.name)
        def bundler = new File(outputDir, 'bundler-' + outSpigot.name)
        Files.move(spigot.toPath(), bundler.toPath(), StandardCopyOption.REPLACE_EXISTING)
        new JarFile(bundler).with {
            it.stream().filter(ent -> ent.name.startsWith('META-INF/versions/') && ent.name.endsWith('.jar'))
                    .limit(1)
                    .forEach { ent ->
                        outSpigot.withOutputStream { out ->
                            it.getInputStream(ent).transferTo(out)
                        }
                    }
        }
    }
}
