/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.javafmlmod;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.BuiltInLanguageLoader;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.IIssueReporting;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IModFile;

public class FMLJavaModLanguageProvider extends BuiltInLanguageLoader {
    public static final String NAME = "javafml";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public ModContainer loadMod(IModInfo info, ModFileScanData modFileScanResults, ModuleLayer layer) {
        var modClasses = modFileScanResults.getAnnotatedBy(Mod.class, ElementType.TYPE)
                .filter(data -> data.annotationData().get("value").equals(info.getModId()))
                .filter(ad -> AutomaticEventSubscriber.getSides(ad.annotationData().get("dist")).contains(FMLLoader.getCurrent().getDist()))
                .filter(ad -> getDepends(ad).stream().allMatch(otherMod -> FMLLoader.getCurrent().getLoadingModList().getModFileById(otherMod) != null))
                .sorted(Comparator.<ModFileScanData.AnnotationData>comparingInt(ad -> getDepends(ad).size())
                        .thenComparingInt(ad -> -AutomaticEventSubscriber.getSides(ad.annotationData().get("dist")).size()))
                .map(ad -> ad.clazz().getClassName())
                .toList();
        return new FMLModContainer(info, modClasses, modFileScanResults, layer);
    }

    @Override
    public void validate(IModFile file, Collection<ModContainer> loadedContainers, IIssueReporting reporter) {
        Set<String> modIds = new HashSet<>();
        for (IModInfo modInfo : file.getModInfos()) {
            if (modInfo.getLoader() == this) {
                modIds.add(modInfo.getModId());
            }
        }

        file.getScanResult().getAnnotatedBy(Mod.class, ElementType.TYPE)
                .filter(data -> !modIds.contains((String) data.annotationData().get("value")))
                .forEach(data -> {
                    var modId = data.annotationData().get("value");
                    var entrypointClass = data.clazz().getClassName();
                    var issue = ModLoadingIssue.error("fml.modloadingissue.javafml.dangling_entrypoint", modId, entrypointClass, file.getFilePath()).withAffectedModFile(file);
                    reporter.addIssue(issue);
                });
    }

    @SuppressWarnings("unchecked")
    private static List<String> getDepends(ModFileScanData.AnnotationData data) {
        var depends = data.annotationData().get("depends");
        return depends != null ? (List<String>) depends : Collections.emptyList();
    }
}
