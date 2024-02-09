package io.izzel.arclight.gradle.tasks

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.lorenztiny.TinyMappingsReader
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.tree.MemoryMappingTree
import org.cadixdev.bombe.type.signature.MethodSignature
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class RenameAsyncCatcherTask extends DefaultTask {

    @InputFile
    abstract RegularFileProperty getInputFile()

    @Input
    abstract Property<String> getMapping()

    @TaskAction
    void run() {
        def input = inputFile.get().asFile
        def output = outputs.files.singleFile
        def tree = new MemoryMappingTree()
        MappingReader.read(LoomGradleExtension.get(project.project(":arclight-common")).mappingConfiguration.tinyMappingsWithSrg, tree)
        def set = new TinyMappingsReader(tree, "named", mapping.get()).read()
        def map = new JsonSlurper().parse(input) as Map<String, Map<String, String>>
        def mapped = map.collectEntries { ent ->
            def clMap = set.getOrCreateClassMapping(ent.key)
            def cl = clMap.fullDeobfuscatedName
            def methods = ent.value.collectEntries {
                def sig = MethodSignature.of(it.key)
                def mappedSig = clMap.getOrCreateMethodMapping(sig)
                [(mappedSig.deobfuscatedSignature.toJvmsIdentifier()): it.value]
            }
            [(cl): methods]
        }
        output.text = JsonOutput.toJson(mapped)
    }
}