package io.izzel.arclight.common.mod;

public class ArclightCommon {

    public interface Api {

        byte[] platformRemapClass(byte[] cl);
    }

    private static Api instance;

    public static Api api() {
        return instance;
    }

    public static void setInstance(Api instance) {
        ArclightCommon.instance = instance;
    }
}
