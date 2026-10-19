package io.izzel.arclight.gradle.tasks

import io.izzel.arclight.gradle.util.AwWriter
import org.cadixdev.at.AccessTransformSet
import org.cadixdev.at.io.AccessTransformFormats
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory

import java.util.jar.JarFile

class ProcessMappingTask implements Runnable {

    private final Project project

    ProcessMappingTask(Project project) {
        this.project = project
    }

    private File buildData
    private File inJar
    private String mcVersion
    private String bukkitVersion
    private File outDir

    @Override
    void run() {
        // Official names are used end-to-end; only access metadata is generated.
        def classes = [] as HashSet<String>
        def jarFile = new JarFile(inJar)
        try {
            jarFile.entries().each { entry ->
                if (entry.name.startsWith('net/minecraft/') && entry.name.endsWith('.class')) {
                    classes.add(entry.name.substring(0, entry.name.length() - '.class'.length()))
                }
            }
        } finally {
            jarFile.close()
        }
        if (!outDir.isDirectory()) outDir.mkdirs()
        // BuildData ATs already use official names in this revision.
        def at = AccessTransformSet.create()
        new File(buildData, "mappings/bukkit.at").eachLine { line ->
            def trimmed = line.trim()
            if (!trimmed || trimmed.startsWith('#')) return
            def parts = trimmed.split(' ', 2)
            def member = parts[1]
            def paren = member.indexOf('(')
            def ownerEnd = paren >= 0 ? member.lastIndexOf('/', paren) : member.lastIndexOf('/')
            def className = member.substring(0, ownerEnd)
            def converted
            if (paren >= 0) {
                // A method owner ends at the slash immediately preceding its name.
                converted = className.replace('/', '.') + ' ' + member.substring(className.length() + 1)
            } else if (classes.contains(member)) {
                converted = member.replace('/', '.')
            } else {
                converted = className.replace('/', '.') + ' ' + member.substring(className.length() + 1)
            }
            AccessTransformFormats.FML.read(new StringReader(parts[0].replace('inal', '') + ' ' + converted + '\n'), at)
        }
        def namedAt = at
        new File(outDir, 'bukkit_at.at').withWriter { AccessTransformFormats.FML.write(it, namedAt) }
        new File(outDir, 'bukkit_aw.aw').withWriter { w ->
            new AwWriter(w, inJar).write(namedAt)
        }
    }

    @InputDirectory
    File getBuildData() {
        return buildData
    }

    void setBuildData(File buildData) {
        this.buildData = buildData
    }

    @InputFile
    File getInJar() {
        return inJar
    }

    void setInJar(File inJar) {
        this.inJar = inJar
    }

    @Input
    String getMcVersion() {
        return mcVersion
    }

    void setMcVersion(String mcVersion) {
        this.mcVersion = mcVersion
    }

    @Input
    String getBukkitVersion() {
        return bukkitVersion
    }

    void setBukkitVersion(String bukkitVersion) {
        this.bukkitVersion = bukkitVersion
    }

    @OutputDirectory
    File getOutDir() {
        return outDir
    }

    void setOutDir(File outDir) {
        this.outDir = outDir
    }
}
