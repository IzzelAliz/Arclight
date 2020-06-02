package io.izzel.arclight.api;

import java.util.Objects;

public class ArclightVersion {

    public static final ArclightVersion v1_14 = new ArclightVersion("1.14.4", 1140);
    public static final ArclightVersion v1_15 = new ArclightVersion("1.15.2", 1152);

    private final String name;
    private final int num;

    public ArclightVersion(String name, int num) {
        this.name = name;
        this.num = num;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ArclightVersion{" +
            "name='" + name + '\'' +
            ", num=" + num +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArclightVersion that = (ArclightVersion) o;
        return num == that.num &&
            Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, num);
    }

    private static ArclightVersion version;

    public static void setVersion(ArclightVersion version) {
        if (ArclightVersion.version != null) throw new IllegalStateException("Version is already set!");
        if (version == null) throw new IllegalArgumentException("Version cannot be null!");
        ArclightVersion.version = version;
    }

    public static boolean atLeast(ArclightVersion v) {
        return v.num <= version.num;
    }

    public static boolean lesserThan(ArclightVersion v) {
        return v.num > version.num;
    }
}
