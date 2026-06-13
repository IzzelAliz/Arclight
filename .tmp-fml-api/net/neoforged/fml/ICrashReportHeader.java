/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import org.jetbrains.annotations.Nullable;

/**
 * Supplies a header to add to crash reports.
 * 
 * @see CrashReportCallables#registerHeader(ICrashReportHeader)
 */
@FunctionalInterface
public interface ICrashReportHeader {
    /**
     * {@return the header to be displayed at the top of the crash report, or {@code null} if the header shouldn't be added}
     */
    @Nullable
    String getHeader();
}
