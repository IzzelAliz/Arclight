/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.modscan;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;

public class Scanner {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ModFile fileToScan;

    public Scanner(ModFile fileToScan) {
        this.fileToScan = fileToScan;
    }

    public ModFileScanData scan() {
        ModFileScanData result = new ModFileScanData();
        result.addModFileInfo(fileToScan.getModFileInfo());
        fileToScan.getContents().visitContent((relativePath, resource) -> {
            if (relativePath.endsWith(".class")) {
                try (var in = resource.open()) {
                    ModClassVisitor mcv = new ModClassVisitor();
                    ClassReader cr = new ClassReader(in);
                    cr.accept(mcv, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
                    mcv.buildData(result.getClasses(), result.getAnnotations());
                } catch (IOException | IllegalArgumentException e) {
                    LOGGER.error(LogMarkers.SCAN, "Exception scanning {} path {}", fileToScan, relativePath, e);
                }
            }
        });
        return result;
    }
}
