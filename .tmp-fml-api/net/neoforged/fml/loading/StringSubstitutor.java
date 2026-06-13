/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import net.neoforged.fml.loading.moddiscovery.ModFile;
import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

@SuppressWarnings("deprecation")
public class StringSubstitutor {
    public static String replace(String in, ModFile file) {
        return new StrSubstitutor(getStringLookup(file)).replace(in);
    }

    private static StrLookup<String> getStringLookup(ModFile file) {
        return new StrLookup<>() {
            @Override
            public String lookup(String key) {
                String[] parts = key.split("\\.");
                if (parts.length == 1) return key;
                String pfx = parts[0];
                if ("file".equals(pfx) && file != null) {
                    return String.valueOf(file.getSubstitutionMap().get().get(parts[1]));
                }
                return key;
            }
        };
    }
}
