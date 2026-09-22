package io.izzel.arclight.gradle.tasks

import io.izzel.arclight.gradle.util.AwWriter
import net.fabricmc.loom.configuration.providers.mappings.MappingConfiguration
import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.lorenztiny.TinyMappingsReader
import net.fabricmc.mappingio.MappingReader
import net.fabricmc.mappingio.tree.MemoryMappingTree
import net.md_5.specialsource.InheritanceMap
import net.md_5.specialsource.Jar
import net.md_5.specialsource.provider.JarProvider
import org.cadixdev.at.AccessTransformSet
import org.cadixdev.at.io.AccessTransformFormats
import org.cadixdev.lorenz.MappingSet
import org.cadixdev.lorenz.io.srg.SrgWriter
import org.cadixdev.lorenz.io.srg.csrg.CSrgReader
import org.cadixdev.lorenz.io.srg.tsrg.TSrgWriter
import org.cadixdev.lorenz.model.ClassMapping
import org.cadixdev.lorenz.model.FieldMapping
import org.objectweb.asm.Type
import org.cadixdev.lorenz.model.Mapping
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
        def loom = LoomGradleExtension.get(project)

        // BuildTools produces Spigot class names with obfuscated members. Both
        // platform maps therefore start at Loom's `official` namespace, which
        // is the left side of BuildData's obfuscated -> Spigot csrg file.
        def defaultTree = new MemoryMappingTree()
        MappingReader.read(loom.mappingConfiguration.tinyMappings, defaultTree)
        def intermediary = new TinyMappingsReader(defaultTree, "official", "intermediary").read()
        def named = new TinyMappingsReader(defaultTree, "official", "named").read()

        // This API generates official Mojang mappings on demand, including
        // while common is evaluated before either platform project.
        def mojmapTree = new MemoryMappingTree()
        MappingReader.read(MappingConfiguration.getMojmapSrgFileIfPossible(project), mojmapTree)
        def mojmap = new TinyMappingsReader(mojmapTree, "official", "named").read()
        // Join by the full official identity. Lorenz merge is not relational
        // composition: it can match renamed inner-class suffixes or field names.
        def mojmapToNamed = MappingSet.create()
        mojmap.topLevelClassMappings.each { joinNamedClass(it, named, mojmapToNamed) }

        if (!outDir.isDirectory()) {
            outDir.mkdirs()
        }
        // The historic filename is consumed by RemapSpigotTask.
        // The first remap has Mojang names already, so this is the
        // Mojang -> Loom named map, not an SRG -> named map.
        new File(outDir, "srg_to_named.srg").withWriter {
            new SrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .filter { !it.hasMappings() }
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                    super.write(mappings)
                }

                @Override
                protected void writeClassMapping(final ClassMapping<?, ?> mapping) {
                    if (!mapping.hasDeobfuscatedName()) {
                        this.writer.println(String.format("CL: %s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()))
                    }
                    super.writeClassMapping(mapping)
                }
            }.write(mojmapToNamed)
        }

        def csrg = MappingSet.create()
        def clFile = new File(buildData, "mappings/bukkit-$mcVersion-cl.csrg")
        clFile.withReader {
            new CSrgReader(it).read(csrg)
        }
        def spigotToOfficial = csrg.reverse()
        // Compose from the BuildData (Mojmap) side so methods retain the
        // descriptor stored in the hybrid BuildTools jar: Mojmap class names
        // plus obfuscated member names.
        def finalMap = spigotToOfficial.merge(mojmap)
        def neoforgeMap = finalMap
        def fabricMap = spigotToOfficial.merge(intermediary)

        def im = new InheritanceMap()
        def classes = [] as ArrayList<String>
        /*csrg.topLevelClassMappings.each {
            classes.add(it.fullDeobfuscatedName)
            it.innerClassMappings.each {
                classes.add(it.fullDeobfuscatedName)
            }
        }*/
        def jarFile = new JarFile(this.inJar)
        for (entry in jarFile.entries()) {
            var name = entry.name
            if (name.startsWith("net") && name.endsWith('.class')) {
                var internalName = name.substring(0, name.lastIndexOf('.'))
                //if (internalName.split('\\$').any { it.integer }) {
                //    continue
                //}
                classes.add(internalName)
            }
        }
        im.generate(new JarProvider(Jar.init(this.inJar)), classes)
        new File(outDir, 'inheritanceMap.txt').withWriter { w ->
            for (def className : classes) {
                def parents = im.getParents(className).collect { finalMap.getOrCreateClassMapping(it).fullDeobfuscatedName }
                if (!parents.isEmpty()) {
                    w.print(finalMap.getOrCreateClassMapping(className).fullDeobfuscatedName)
                    w.print(' ')
                    w.println(parents.join(' '))
                }
            }
        }
        new File(outDir, 'inheritanceMap_intermediary.txt').withWriter { w ->
            for (def className : classes) {
                def parents = im.getParents(className).collect { fabricMap.getOrCreateClassMapping(it).fullDeobfuscatedName }
                if (!parents.isEmpty()) {
                    w.print(fabricMap.getOrCreateClassMapping(className).fullDeobfuscatedName)
                    w.print(' ')
                    w.println(parents.join(' '))
                }
            }
        }
        new File(outDir, 'bukkit_srg.srg').withWriter {
            new TSrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                }

                @Override
                protected void writeClassMapping(ClassMapping<?, ?> mapping) {
                    if (!mapping.hasMappings()) {
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                    } else if (mapping.fullObfuscatedName.contains('/')) {
                        // hasDeobfuscatedName() has to be true for every mapping
                        // since we need to preserve identity() mapping
                        // Or else SpecialSource recognize classes not in mapping as not mapped
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));

                        mapping.getFieldsByName().values().stream().filter(Mapping::hasDeobfuscatedName).sorted(this.getConfig().getFieldMappingComparator()).forEach(this::writeFieldMapping);
                        mapping.getMethodMappings().stream().filter(Mapping::hasDeobfuscatedName).sorted(this.getConfig().getMethodMappingComparator()).forEach(this::writeMethodMapping);
                        mapping.getInnerClassMappings().stream().filter(ClassMapping::hasMappings).sorted(this.getConfig().getClassMappingComparator()).forEach(this::writeClassMapping);
                    }
                }

                @Override
                protected void writeFieldMapping(FieldMapping mapping) {
                    def type = Type.getType(mapping.signature.type.orElseThrow().toString()).className
                    this.writer.println("    $type ${mapping.deobfuscatedName} -> ${mapping.obfuscatedName}")
                }
            }.write(finalMap)
        }
        new File(outDir, 'bukkit_moj.srg').withWriter {
            new TSrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                }

                @Override
                protected void writeClassMapping(ClassMapping<?, ?> mapping) {
                    if (!mapping.hasMappings()) {
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                    } else if (mapping.fullObfuscatedName.contains('/')) {
                        // hasDeobfuscatedName() has to be true for every mapping
                        // since we need to preserve identity() mapping
                        // Or else SpecialSource recognize classes not in mapping as not mapped
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                        mapping.getFieldsByName().values().stream().filter(Mapping::hasDeobfuscatedName).sorted(this.getConfig().getFieldMappingComparator()).forEach(this::writeFieldMapping);
                        mapping.getMethodMappings().stream().filter(Mapping::hasDeobfuscatedName).sorted(this.getConfig().getMethodMappingComparator()).forEach(this::writeMethodMapping);
                        mapping.getInnerClassMappings().stream().filter(ClassMapping::hasMappings).sorted(this.getConfig().getClassMappingComparator()).forEach(this::writeClassMapping);
                    }
                }

                @Override
                protected void writeFieldMapping(FieldMapping mapping) {
                    def type = Type.getType(mapping.signature.type.orElseThrow().toString()).className
                    this.writer.println("    $type ${mapping.deobfuscatedName} -> ${mapping.obfuscatedName}")
                }
            }.write(neoforgeMap)
        }
        new File(outDir, 'bukkit_intermediary.srg').withWriter {
            new TSrgWriter(it) {
                @Override
                void write(final MappingSet mappings) {
                    mappings.getTopLevelClassMappings().stream()
                            .sorted(this.getConfig().getClassMappingComparator())
                            .forEach(this::writeClassMapping)
                }

                @Override
                protected void writeClassMapping(ClassMapping<?, ?> mapping) {
                    if (!mapping.hasMappings()) {
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                    } else if (mapping.fullObfuscatedName.contains('/')) {
                        // hasDeobfuscatedName() has to be true for every mapping
                        // since we need to preserve identity() mapping
                        // Or else SpecialSource recognize classes not in mapping as not mapped
                        this.writer.println(String.format("%s %s", mapping.getFullObfuscatedName(), mapping.getFullDeobfuscatedName()));
                        mapping.getFieldsByName().values().stream().filter(Mapping::hasDeobfuscatedName).sorted(this.getConfig().getFieldMappingComparator()).forEach(this::writeFieldMapping);
                        mapping.getMethodMappings().stream().filter(Mapping::hasDeobfuscatedName).sorted(this.getConfig().getMethodMappingComparator()).forEach(this::writeMethodMapping);
                        mapping.getInnerClassMappings().stream().filter(ClassMapping::hasMappings).sorted(this.getConfig().getClassMappingComparator()).forEach(this::writeClassMapping);
                    }
                }

                @Override
                protected void writeFieldMapping(FieldMapping mapping) {
                    def type = Type.getType(mapping.signature.type.orElseThrow().toString()).className
                    this.writer.println("    $type ${mapping.deobfuscatedName} -> ${mapping.obfuscatedName}")
                }
            }.write(fabricMap)
        }
        // BuildData ATs already use Mojang names in this revision.
        def at = AccessTransformSet.create()
        new File(buildData, "mappings/bukkit-${mcVersion}.at").eachLine { line ->
            def trimmed = line.trim()
            if (!trimmed || trimmed.startsWith('#')) return
            def parts = trimmed.split(' ', 2)
            def member = parts[1]
            def paren = member.indexOf('(')
            String converted
            if (paren >= 0) {
                def ownerEnd = member.lastIndexOf('/', paren)
                converted = member.substring(0, ownerEnd).replace('/', '.') + ' ' + member.substring(ownerEnd + 1)
            } else {
                def last = member.substring(member.lastIndexOf('/') + 1)
                converted = mojmapToNamed.getClassMapping(member).isPresent() ? member.replace('/', '.') :
                        member.substring(0, member.lastIndexOf('/')).replace('/', '.') + ' ' + last
            }
            AccessTransformFormats.FML.read(new StringReader(parts[0].replace('inal', '') + ' ' + converted + '\n'), at)
        }
        def namedAt = at.remap(mojmapToNamed)
        new File(outDir, 'bukkit_at.at').withWriter { AccessTransformFormats.FML.write(it, namedAt) }
        new File(outDir, 'bukkit_aw.aw').withWriter { w ->
            new AwWriter(w, loom.namedMinecraftProvider).write(namedAt)
        }
        new File(outDir, 'reobf_bukkit.srg').text = "PK: org/bukkit/craftbukkit/v org/bukkit/craftbukkit/$bukkitVersion"
    }

    private static void joinNamedClass(ClassMapping<?, ?> source, MappingSet named, MappingSet result) {
        def target = named.getClassMapping(source.fullObfuscatedName).orElse(null)
        def output = result.getOrCreateClassMapping(source.fullDeobfuscatedName)
        output.setDeobfuscatedName(target == null ? source.fullDeobfuscatedName : target.fullDeobfuscatedName)
        source.methodMappings.each { method ->
            def match = target == null ? null : target.getMethodMapping(method.signature).orElse(null)
            output.getOrCreateMethodMapping(method.deobfuscatedSignature)
                    .setDeobfuscatedName(match == null ? method.deobfuscatedName : match.deobfuscatedName)
        }
        source.fieldMappings.each { field ->
            def match = target == null ? null : target.getFieldMapping(field.signature).orElse(null)
            output.getOrCreateFieldMapping(field.deobfuscatedSignature)
                    .setDeobfuscatedName(match == null ? field.deobfuscatedName : match.deobfuscatedName)
        }
        source.innerClassMappings.each { joinNamedClass(it, named, result) }
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
