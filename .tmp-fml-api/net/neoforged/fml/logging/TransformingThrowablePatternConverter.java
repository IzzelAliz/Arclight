/*
 * ModLauncher - for launching Java programs with in-flight transformation ability.
 * Copyright (C) 2017-2019 cpw
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.neoforged.fml.logging;

import java.util.Collections;
import joptsimple.internal.Strings;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.PatternConverter;
import org.apache.logging.log4j.core.pattern.PlainTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.apache.logging.log4j.core.pattern.ThrowablePatternConverter;

/**
 * Started as a copy of {@link org.apache.logging.log4j.core.pattern.ExtendedThrowablePatternConverter} because
 * there is no mechanism to hook additional data into that class, which is very rubbish.
 */
@Plugin(name = "TransformingThrowablePatternConverter", category = PatternConverter.CATEGORY)
@ConverterKeys({ "tEx" })
public class TransformingThrowablePatternConverter extends ThrowablePatternConverter {
    static final String SUFFIXFLAG = "☃☃☃☃☃SUFFIXFLAG☃☃☃☃☃";

    /**
     * @param name    Name of converter.
     * @param style   CSS style for output.
     * @param options options, may be null.
     * @param config  config.
     */
    protected TransformingThrowablePatternConverter(Configuration config, String[] options) {
        super("TransformingThrowable", "throwable", options, config);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        ThrowableProxy proxy = event.getThrownProxy();
        Throwable throwable = event.getThrown();
        if ((throwable != null || proxy != null) && options.anyLines()) {
            if (proxy == null) {
                super.format(event, toAppendTo);
                return;
            }
            int len = toAppendTo.length();
            if (len > 0 && !Character.isWhitespace(toAppendTo.charAt(len - 1))) {
                toAppendTo.append(' ');
            }
            TextRenderer textRenderer = new ExtraDataTextRenderer(options.getTextRenderer());
            proxy.formatExtendedStackTraceTo(toAppendTo, options.getIgnorePackages(),
                    textRenderer, SUFFIXFLAG, options.getSeparator());
        }
    }

    /**
     * Gets an instance of the class.
     *
     * @param config  The current Configuration.
     * @param options pattern options, may be null. If first element is "short",
     *                only the first line of the throwable will be formatted.
     * @return instance of class.
     */
    public static TransformingThrowablePatternConverter newInstance(Configuration config, String[] options) {
        return new TransformingThrowablePatternConverter(config, options);
    }

    public static String generateEnhancedStackTrace(Throwable throwable) {
        ThrowableProxy proxy = new ThrowableProxy(throwable);
        StringBuilder buffer = new StringBuilder();
        TextRenderer textRenderer = new ExtraDataTextRenderer(PlainTextRenderer.getInstance());
        proxy.formatExtendedStackTraceTo(buffer, Collections.emptyList(),
                textRenderer, SUFFIXFLAG, Strings.LINE_SEPARATOR);
        return buffer.toString();
    }
}
