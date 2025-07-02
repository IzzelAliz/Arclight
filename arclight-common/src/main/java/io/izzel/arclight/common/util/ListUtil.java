package io.izzel.arclight.common.util;

import java.util.ArrayList;
import java.util.Collections;

public class ListUtil {
    @SafeVarargs
    public static <T> ArrayList<T> asMutableList(T... o) {
        var result = new ArrayList<T>();
        Collections.addAll(result, o);
        return result;
    }
}
