/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.i18n;

import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;

public class MavenVersionTranslator {
    public static String artifactVersionToString(ArtifactVersion artifactVersion) {
        return artifactVersion.toString();
    }

    public static String versionRangeToString(VersionRange range) {
        return range.getRestrictions().stream().map(MavenVersionTranslator::restrictionToString).collect(Collectors.joining(", "));
    }

    public static String restrictionToString(Restriction restriction) {
        if (restriction.getLowerBound() == null && restriction.getUpperBound() == null) {
            return FMLTranslations.parseMessage("fml.messages.version.restriction.any");
        } else if (restriction.getLowerBound() != null && restriction.getUpperBound() != null) {
            if (Objects.equals(artifactVersionToString(restriction.getLowerBound()), artifactVersionToString(restriction.getUpperBound()))) {
                return artifactVersionToString(restriction.getLowerBound());
            } else {
                if (restriction.isLowerBoundInclusive() && restriction.isUpperBoundInclusive()) {
                    return FMLTranslations.parseMessage("fml.messages.version.restriction.bounded.inclusive", restriction.getLowerBound(), restriction.getUpperBound());
                } else if (restriction.isLowerBoundInclusive()) {
                    return FMLTranslations.parseMessage("fml.messages.version.restriction.bounded.upperexclusive", restriction.getLowerBound(), restriction.getUpperBound());
                } else if (restriction.isUpperBoundInclusive()) {
                    return FMLTranslations.parseMessage("fml.messages.version.restriction.bounded.lowerexclusive", restriction.getLowerBound(), restriction.getUpperBound());
                } else {
                    return FMLTranslations.parseMessage("fml.messages.version.restriction.bounded.exclusive", restriction.getLowerBound(), restriction.getUpperBound());
                }
            }
        } else if (restriction.getLowerBound() != null) {
            if (restriction.isLowerBoundInclusive()) {
                return FMLTranslations.parseMessage("fml.messages.version.restriction.lower.inclusive", restriction.getLowerBound());
            } else {
                return FMLTranslations.parseMessage("fml.messages.version.restriction.lower.exclusive", restriction.getLowerBound());
            }
        } else {
            if (restriction.isUpperBoundInclusive()) {
                return FMLTranslations.parseMessage("fml.messages.version.restriction.upper.inclusive", restriction.getUpperBound());
            } else {
                return FMLTranslations.parseMessage("fml.messages.version.restriction.upper.exclusive", restriction.getUpperBound());
            }
        }
    }
}
