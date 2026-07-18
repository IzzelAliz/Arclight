package ca.spottedleaf.concurrentutil.util;

import java.util.Collection;

public final class CollectionUtil {

    public static String toString(final Collection<?> collection, final String name) {
        return CollectionUtil.toString(collection, name, new StringBuilder(name.length() + 128)).toString();
    }

    public static StringBuilder toString(final Collection<?> collection, final String name, final StringBuilder builder) {
        builder.append(name).append("{elements={");

        boolean first = true;

        for (final Object element : collection) {
            if (!first) {
                builder.append(", ");
            }
            first = false;

            builder.append('"').append(element).append('"');
        }

        return builder.append("}}");
    }

    private CollectionUtil() {
        throw new RuntimeException();
    }
}
