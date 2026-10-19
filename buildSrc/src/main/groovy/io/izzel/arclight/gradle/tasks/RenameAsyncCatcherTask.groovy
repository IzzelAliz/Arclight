package io.izzel.arclight.gradle.tasks

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
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
        if (mapping.get() != 'official') {
            throw new IllegalArgumentException('26.1.2 AsyncCatcher requires the official namespace')
        }
        // Both platforms use the source namespace; no mapping provider exists.
        def map = new JsonSlurper().parse(input) as Map<String, Map<String, String>>
        output.parentFile.mkdirs()
        output.text = JsonOutput.toJson(map)
    }
}