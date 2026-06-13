/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import java.nio.file.Path;
import java.util.List;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jetbrains.annotations.Nullable;

public record ModLoadingIssue(
        Severity severity,
        String translationKey,
        List<Object> translationArgs,
        @Nullable Throwable cause,
        @Nullable Path affectedPath,
        @Nullable IModFile affectedModFile,
        @Nullable IModInfo affectedMod) {

    public ModLoadingIssue(Severity severity, String translationKey, List<Object> translationArgs) {
        this(severity, translationKey, translationArgs, null, null, null, null);
    }

    public ModLoadingIssue {
        // Make sure it's easy for us to know which translation keys get access to the implicit args at indices 100 and above
        if (translationKey.startsWith("fml.") && !translationKey.startsWith("fml.modloadingissue.")) {
            throw new IllegalArgumentException("When using FML translation keys, only use fml.modloadingissue. keys for mod loading issues: " + translationKey);
        }
    }

    public static ModLoadingIssue error(String translationKey, Object... args) {
        return new ModLoadingIssue(Severity.ERROR, translationKey, List.of(args));
    }

    public static ModLoadingIssue warning(String translationKey, Object... args) {
        return new ModLoadingIssue(Severity.WARNING, translationKey, List.of(args));
    }

    public ModLoadingIssue withAffectedPath(Path affectedPath) {
        return new ModLoadingIssue(severity, translationKey, translationArgs, cause, affectedPath, null, null);
    }

    public ModLoadingIssue withAffectedModFile(IModFile affectedModFile) {
        var affectedPath = affectedModFile.getFilePath();
        return new ModLoadingIssue(severity, translationKey, translationArgs, cause, affectedPath, affectedModFile, null);
    }

    public ModLoadingIssue withAffectedMod(IModInfo affectedMod) {
        var affectedModFile = affectedMod.getOwningFile().getFile();
        var affectedPath = affectedModFile.getFilePath();
        return new ModLoadingIssue(severity, translationKey, translationArgs, cause, affectedPath, affectedModFile, affectedMod);
    }

    public ModLoadingIssue withCause(Throwable cause) {
        return new ModLoadingIssue(severity, translationKey, translationArgs, cause, affectedPath, affectedModFile, affectedMod);
    }

    public ModLoadingIssue withSeverity(Severity severity) {
        return new ModLoadingIssue(severity, translationKey, translationArgs, cause, affectedPath, affectedModFile, affectedMod);
    }

    @Override
    public String toString() {
        var result = new StringBuilder(severity + ": " + translationKey);
        if (!translationArgs.isEmpty()) {
            result.append(" [");
            for (int i = 0; i < translationArgs.size(); i++) {
                if (i > 0) {
                    result.append("; ");
                }
                result.append(translationArgs.get(i));
            }
            result.append("]");
        }
        if (cause != null) {
            result.append(" caused by ").append(cause);
        }
        return result.toString();
    }
    public enum Severity {
        WARNING,
        ERROR
    }
}
