/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.i18n;

import java.nio.file.Path;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import net.neoforged.fml.Logging;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.StringUtils;
import net.neoforged.fml.util.PathPrettyPrinting;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.commons.lang3.text.ExtendedMessageFormat;
import org.apache.commons.lang3.text.FormatFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@SuppressWarnings("deprecation")
public class FMLTranslations {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, FormatFactory> CUSTOM_FACTORIES;
    private static final Pattern PATTERN_CONTROL_CODE = Pattern.compile("(?i)\\u00A7[0-9A-FK-OR]");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    static {
        CUSTOM_FACTORIES = new HashMap<>();
        // {0,modinfo,id} -> modid from ModInfo object; {0,modinfo,name} -> displayname from ModInfo object
        CUSTOM_FACTORIES.put("modinfo", new CustomFormat<>(IModInfo.class, FMLTranslations::formatModInfo));
        // {0,lower} -> lowercase supplied string
        CUSTOM_FACTORIES.put("lower", new CustomFormat<>(Object.class, (stringBuffer, value) -> stringBuffer.append(StringUtils.toLowerCase(String.valueOf(value)))));
        // {0,upper> -> uppercase supplied string
        CUSTOM_FACTORIES.put("upper", new CustomFormat<>(Object.class, (stringBuffer, value) -> stringBuffer.append(StringUtils.toUpperCase(String.valueOf(value)))));
        // {0,exc,cls} -> class of exception; {0,exc,msg} -> message from exception
        CUSTOM_FACTORIES.put("exc", new CustomFormat<>(Throwable.class, FMLTranslations::formatException));
        // {0,vr} -> transform VersionRange into cleartext string using fml.messages.version.restriction.* strings
        CUSTOM_FACTORIES.put("vr", new CustomFormat<>(VersionRange.class, FMLTranslations::formatVersionRange));
        // {0,featurebound} -> transform feature bound to cleartext string
        CUSTOM_FACTORIES.put("featurebound", new CustomFormat<>(ForgeFeature.Bound.class, FMLTranslations::formatFeatureBoundValue));
        // {0,i18n,fml.message} -> pass object to i18n string 'fml.message'
        CUSTOM_FACTORIES.put("i18n", new CustomFormat<>(Object.class, (stringBuffer, o, args) -> stringBuffer.append(parseMessage(args, o))));
        // {0,i18ntranslate} -> attempt to use the argument as a translation key
        CUSTOM_FACTORIES.put("i18ntranslate", new CustomFormat<>(String.class, (stringBuffer, o) -> stringBuffer.append(parseMessage(o))));
        // {0,ornull,fml.absent} -> append String value of o, or i18n string 'fml.absent' (message format transforms nulls into the string literal "null")
        CUSTOM_FACTORIES.put("ornull", new CustomFormat<>(Object.class, FMLTranslations::formatOrNull));
        // {0,optional,[prefix]} -> append String value of o if the optional is present, with an optional prefix at the start
        CUSTOM_FACTORIES.put("optional", new CustomFormat<>(Optional.class, FMLTranslations::formatOptional));
    }

    public static String getPattern(String patternName, Supplier<String> fallback) {
        var translated = I18nManager.currentLocale.get(patternName);
        return translated == null ? fallback.get() : translated;
    }

    public static String parseMessage(String i18nMessage, Object... args) {
        return parseMessageWithFallback(i18nMessage, () -> i18nMessage, args);
    }

    public static String parseMessageWithFallback(String i18nMessage, Supplier<String> fallback, Object... args) {
        String pattern = getPattern(i18nMessage, fallback);
        try {
            return parseFormat(pattern, args);
        } catch (IllegalArgumentException e) {
            LOGGER.error(Logging.CORE, "Illegal format found `{}`", pattern);
            return pattern;
        }
    }

    public static String parseEnglishMessage(String i18n, Object... args) {
        var translated = I18nManager.DEFAULT_TRANSLATIONS.getOrDefault(i18n, i18n);
        try {
            return parseFormat(translated, args);
        } catch (IllegalArgumentException e) {
            LOGGER.error(Logging.CORE, "Illegal format found `{}`", translated);
            return translated;
        }
    }

    public static String parseFormat(String format, Object... args) {
        AtomicInteger i = new AtomicInteger();
        // Converts Mojang translation format (%s) to the one used by Apache Commons ({0})
        format = FORMAT_PATTERN.matcher(format).replaceAll(matchResult -> {
            if (matchResult.group(0).equals("%%")) {
                return "%";
            }
            String groupIdx = matchResult.group(1);
            int index = groupIdx != null ? Integer.parseInt(groupIdx) - 1 : i.getAndIncrement();
            return "{" + index + "}";
        });
        ExtendedMessageFormat extendedMessageFormat = new ExtendedMessageFormat(format, CUSTOM_FACTORIES);
        return extendedMessageFormat.format(args);
    }

    public static String translateIssueEnglish(ModLoadingIssue issue) {
        var args = getTranslationArgs(issue);
        return parseEnglishMessage(issue.translationKey(), args);
    }

    public static String translateIssue(ModLoadingIssue issue) {
        var args = getTranslationArgs(issue);
        return parseMessage(issue.translationKey(), args);
    }

    private static Object[] getTranslationArgs(ModLoadingIssue issue) {
        var args = new ArrayList<>(103);
        args.addAll(issue.translationArgs());
        // Pad up to 100
        while (args.size() < 100) {
            args.add(null);
        }

        // Implicit arguments start at index 100
        args.add(getModInfo(issue)); // {100} = affected mod
        args.add(getAffectedPath(issue)); // {101} = affected file-path
        args.add(issue.cause()); // {102} = exception

        args.replaceAll(FMLTranslations::formatArg);

        return args.toArray(Object[]::new);
    }

    private static @Nullable IModInfo getModInfo(ModLoadingIssue issue) {
        var modInfo = issue.affectedMod();
        var file = issue.affectedModFile();
        while (modInfo == null && file != null) {
            if (!file.getModInfos().isEmpty()) {
                modInfo = file.getModInfos().getFirst();
            }
            file = file.getDiscoveryAttributes().parent();
        }
        return modInfo;
    }

    private static @Nullable Path getAffectedPath(ModLoadingIssue issue) {
        if (issue.affectedPath() != null) {
            return issue.affectedPath();
        } else if (issue.affectedModFile() != null) {
            return issue.affectedModFile().getFilePath();
        } else {
            return null;
        }
    }

    private static Object formatArg(Object arg) {
        if (arg instanceof IModFile modFile) {
            return PathPrettyPrinting.prettyPrint(modFile.getFilePath());
        } else if (arg instanceof Path path) {
            return PathPrettyPrinting.prettyPrint(path);
        } else {
            return arg;
        }
    }

    public static String stripControlCodes(String text) {
        return PATTERN_CONTROL_CODE.matcher(text).replaceAll("");
    }

    private static void formatException(StringBuffer stringBuffer, Throwable t, String args) {
        if (Objects.equals(args, "msg")) {
            stringBuffer.append(t.getClass().getName()).append(": ").append(t.getMessage());
        } else if (Objects.equals(args, "cls")) {
            stringBuffer.append(t.getClass().getName());
        }
    }

    private static void formatModInfo(StringBuffer stringBuffer, IModInfo info, String args) {
        if (Objects.equals(args, "id")) {
            stringBuffer.append(info.getModId());
        } else if (Objects.equals(args, "name")) {
            stringBuffer.append(info.getDisplayName());
        } else {
            LOGGER.warn("Cannot format unknown mod info property in translation: {}", args);
        }
    }

    private static void formatVersionRange(StringBuffer stringBuffer, VersionRange range) {
        stringBuffer.append(MavenVersionTranslator.versionRangeToString(range));
    }

    private static void formatFeatureBoundValue(StringBuffer stringBuffer, ForgeFeature.Bound bound) {
        stringBuffer.append(bound.featureName());
        if (bound.bound() instanceof Boolean b) {
            stringBuffer.append("=").append(b);
        } else if (bound.bound() instanceof VersionRange vr) {
            stringBuffer.append(" ").append(MavenVersionTranslator.versionRangeToString(vr));
        } else {
            stringBuffer.append("=\"").append(bound.featureBound()).append("\"");
        }
    }

    private static void formatOrNull(StringBuffer stringBuffer, Object o, String args) {
        stringBuffer.append(Objects.equals(String.valueOf(o), "null") ? parseMessage(args) : String.valueOf(o));
    }

    private static void formatOptional(StringBuffer stringBuffer, Optional<?> value, String args) {
        args = Objects.requireNonNullElse(args, "");
        if (value.isPresent()) {
            stringBuffer.append(args).append(value.get());
        }
    }

    private record CustomFormat<T>(Class<T> valueClass, FormatFunctionWithArgs<T> formatter) implements FormatFactory {

        public CustomFormat(Class<T> valueClass, FormatFunction<T> formatter) {
            this(valueClass, (toAppendTo, value, args) -> formatter.format(toAppendTo, value));
        }

        @Override
        public Format getFormat(String name, String arguments, Locale locale) {
            return new Format() {
                @Override
                public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
                    if (valueClass.isInstance(obj)) {
                        formatter.format(toAppendTo, valueClass.cast(obj), arguments);
                    } else if (obj != null) {
                        LOGGER.warn("Translation format {} expected type {}, but got: {}", name, valueClass, obj.getClass());
                    }
                    return toAppendTo;
                }

                @Override
                public Object parseObject(String source, ParsePosition pos) {
                    throw new UnsupportedOperationException("Parsing is not supported");
                }
            };
        }
        @FunctionalInterface
        interface FormatFunction<T> {
            void format(StringBuffer toAppendTo, T value);
        }

        @FunctionalInterface
        interface FormatFunctionWithArgs<T> {
            void format(StringBuffer toAppendTo, T value, String args);
        }
    }
}
