/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.modscan;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Map;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

public class ModAnnotation {
    public static ModFileScanData.AnnotationData fromModAnnotation(Type clazz, ModAnnotation annotation) {
        return new ModFileScanData.AnnotationData(annotation.asmType, annotation.type, clazz, annotation.member, annotation.values);
    }

    public record EnumHolder(String desc, String value) {}

    private final ElementType type;
    private final Type asmType;
    private final String member;
    private final Map<String, Object> values = Maps.newHashMap();

    private ArrayList<Object> arrayList;
    private String arrayName;

    public ModAnnotation(ElementType type, Type asmType, String member) {
        this.type = type;
        this.asmType = asmType;
        this.member = member;
    }

    public ModAnnotation(Type asmType, ModAnnotation parent) {
        this.type = parent.type;
        this.asmType = asmType;
        this.member = parent.member;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper("Annotation")
                .add("type", type)
                .add("name", asmType.getClassName())
                .add("member", member)
                .add("values", values)
                .toString();
    }

    public ElementType getType() {
        return type;
    }

    public Type getASMType() {
        return asmType;
    }

    public String getMember() {
        return member;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void addArray(String name) {
        this.arrayList = Lists.newArrayList();
        this.arrayName = name;
    }

    public void addProperty(String key, Object value) {
        if (this.arrayList != null) {
            arrayList.add(value);
        } else {
            values.put(key, value);
        }
    }

    public void addEnumProperty(String key, String enumName, String value) {
        addProperty(key, new EnumHolder(enumName, value));
    }

    public void endArray() {
        values.put(arrayName, arrayList);
        arrayList = null;
    }

    public ModAnnotation addChildAnnotation(String name, String desc) {
        ModAnnotation child = new ModAnnotation(Type.getType(desc), this);
        addProperty(name, child.getValues());
        return child;
    }
}
