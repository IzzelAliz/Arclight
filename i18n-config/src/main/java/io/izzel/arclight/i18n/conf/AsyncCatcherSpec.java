package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Map;

@ConfigSerializable
public class AsyncCatcherSpec {

    @Setting("dump")
    private boolean dump;

    @Setting("warn")
    private boolean warn;

    @Setting("defaultOperation")
    private Operation defaultOp;

    @Setting("overrides")
    private Map<String, Operation> overrides;

    public boolean isDump() {
        return dump;
    }

    public boolean isWarn() {
        return warn;
    }

    public Operation getDefaultOp() {
        return defaultOp;
    }

    public Map<String, Operation> getOverrides() {
        return overrides;
    }

    public enum Operation {
        NONE, DISPATCH, BLOCK, EXCEPTION
    }
}
