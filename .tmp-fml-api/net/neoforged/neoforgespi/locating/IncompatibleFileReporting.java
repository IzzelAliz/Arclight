/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import net.neoforged.fml.ModLoadingIssue;
import org.slf4j.event.Level;

/**
 * Defines how files that are not handled are reported.
 */
public enum IncompatibleFileReporting {
    /**
     * Add a {@link net.neoforged.fml.ModLoadingIssue} of severity {@link net.neoforged.fml.ModLoadingIssue.Severity#ERROR} if the
     * file is not determined to be a compatible mod or explicit library.
     */
    ERROR(Level.ERROR, ModLoadingIssue.Severity.ERROR),
    /**
     * Add a {@link net.neoforged.fml.ModLoadingIssue} of severity {@link net.neoforged.fml.ModLoadingIssue.Severity#WARNING} if the
     * file is not determined to be a compatible mod or explicit library.
     */
    WARN_ALWAYS(Level.WARN, ModLoadingIssue.Severity.WARNING),
    /**
     * Add a {@link net.neoforged.fml.ModLoadingIssue} of severity {@link net.neoforged.fml.ModLoadingIssue.Severity#WARNING} if the
     * file is not determined to be a compatible mod or explicit library, and the file triggers the built-in detection for
     * incompatible modding systems ({@link net.neoforged.fml.loading.moddiscovery.IncompatibleModReason}).
     */
    WARN_ON_KNOWN_INCOMPATIBILITY(Level.WARN, ModLoadingIssue.Severity.WARNING),
    /**
     * Do nothing if the file is not detected as compatible.
     */
    IGNORE(Level.WARN, ModLoadingIssue.Severity.WARNING);

    private final Level logLevel;

    private final ModLoadingIssue.Severity issueSeverity;

    IncompatibleFileReporting(Level logLevel, ModLoadingIssue.Severity issueSeverity) {
        this.logLevel = logLevel;
        this.issueSeverity = issueSeverity;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public ModLoadingIssue.Severity getIssueSeverity() {
        return issueSeverity;
    }
}
