/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi;

import net.neoforged.fml.ModLoadingIssue;

/**
 * Interface used to report issues to the game.
 */
public interface IIssueReporting {
    /**
     * Report an issue to the loader.
     */
    void addIssue(ModLoadingIssue issue);
}
