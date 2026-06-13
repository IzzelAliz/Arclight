/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.lang.model.SourceVersion;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.neoforgespi.language.IModInfo;
import org.objectweb.asm.Type;

record EnumPrototype(String owningMod, String enumName, String fieldName, String ctorDesc, String fullCtorDesc, EnumParameters ctorParams) implements Comparable<EnumPrototype> {

    private static final String ENUM_CTOR_BASE_DESC = "Ljava/lang/String;I";
    private static final Gson GSON = new Gson();
    @Override
    public int compareTo(EnumPrototype other) {
        int comp = owningMod.compareTo(other.owningMod);
        return comp != 0 ? comp : fieldName.compareTo(other.fieldName);
    }

    static List<EnumPrototype> load(IModInfo mod, JarResource resource) {
        try (Reader reader = resource.bufferedReader()) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);

            JsonArray entries = json.getAsJsonArray("entries");
            List<EnumPrototype> prototypes = new ArrayList<>(entries.size());
            for (JsonElement entry : entries) {
                JsonObject entryObj = entry.getAsJsonObject();

                String enumName = entryObj.get("enum").getAsString();
                if (!isValidClassDescriptor(enumName)) {
                    error("fml.modloadingissue.enumextender.invalid_enum_name", mod, enumName);
                    continue;
                }

                String fieldName = entryObj.get("name").getAsString();
                if (!fieldName.toLowerCase(Locale.ROOT).startsWith(mod.getModId())) {
                    error("fml.modloadingissue.enumextender.field_name.missing_prefix", mod, fieldName, enumName);
                    continue;
                }
                if (!SourceVersion.isIdentifier(fieldName)) {
                    error("fml.modloadingissue.enumextender.field_name.invalid", mod, fieldName, enumName);
                    continue;
                }

                String ctorDesc = entryObj.get("constructor").getAsString();
                if (!isValidConstructorDescriptor(ctorDesc)) {
                    error("fml.modloadingissue.enumextender.invalid_constructor", mod, ctorDesc, enumName);
                    continue;
                }

                EnumParameters ctorParams;
                JsonElement paramElem = entryObj.get("parameters");
                if (paramElem.isJsonArray()) {
                    ctorParams = loadConstantParameters(mod, enumName, fieldName, ctorDesc, paramElem.getAsJsonArray());
                    if (ctorParams == null) {
                        continue;
                    }
                } else if (paramElem.isJsonObject()) {
                    JsonObject obj = paramElem.getAsJsonObject();
                    String className = obj.get("class").getAsString();
                    if (!isValidClassDescriptor(className)) {
                        error("fml.modloadingissue.enumextender.argument.reference.invalid_src_class", mod, className, fieldName, enumName);
                        continue;
                    }

                    if (obj.has("method")) {
                        String srcMethodName = obj.get("method").getAsString();
                        if (!SourceVersion.isIdentifier(srcMethodName)) {
                            error("fml.modloadingissue.enumextender.argument.reference.invalid_src_method", mod, srcMethodName, fieldName, enumName);
                            continue;
                        }
                        ctorParams = new EnumParameters.MethodReference(Type.getObjectType(className), srcMethodName);
                    } else if (obj.has("field")) {
                        String srcFieldName = obj.get("field").getAsString();
                        if (!SourceVersion.isIdentifier(srcFieldName)) {
                            error("fml.modloadingissue.enumextender.argument.reference.invalid_src_field", mod, srcFieldName, fieldName, enumName);
                            continue;
                        }
                        ctorParams = new EnumParameters.FieldReference(Type.getObjectType(className), srcFieldName);
                    } else {
                        error("fml.modloadingissue.enumextender.argument.reference.unexpected_decl", mod, paramElem, fieldName, enumName);
                        continue;
                    }
                } else {
                    error("fml.modloadingissue.enumextender.argument.unexpected_decl", mod, paramElem, fieldName, enumName);
                    continue;
                }

                // Prepend source-invisible field name and ordinal parameters after checking user-provided parameters
                String fullCtorDesc = "(" + ENUM_CTOR_BASE_DESC + ctorDesc.substring(1);
                prototypes.add(new EnumPrototype(mod.getModId(), enumName, fieldName, ctorDesc, fullCtorDesc, ctorParams));
            }
            return prototypes;
        } catch (Throwable e) {
            ModLoader.addLoadingIssue(ModLoadingIssue.error("fml.modloadingissue.enumextender.loading_error", resource)
                    .withAffectedMod(mod)
                    .withCause(e));
            return List.of();
        }
    }

    private static EnumParameters loadConstantParameters(IModInfo mod, String enumName, String fieldName, String ctorDesc, JsonArray obj) {
        List<Object> params = new ArrayList<>(obj.size());
        Type[] argTypes = Type.getArgumentTypes(ctorDesc);
        if (argTypes.length != obj.size()) {
            error("fml.modloadingissue.enumextender.argument.constant.count_mismatch", mod, obj.size(), argTypes.length, ctorDesc, fieldName, enumName);
            return null;
        }

        int idx = 0;
        for (JsonElement element : obj) {
            Type argType = argTypes[idx];
            switch (argType.getDescriptor()) {
                case "Z" -> params.add(element.getAsBoolean());
                case "C" -> {
                    String param = element.getAsString();
                    if (param.length() != 1) {
                        error("fml.modloadingissue.enumextender.argument.constant.invalid_char", mod, param, idx, fieldName, enumName);
                        return null;
                    }
                    params.add(param.charAt(0));
                }
                case "B" -> params.add(element.getAsByte());
                case "S" -> params.add(element.getAsShort());
                case "I" -> params.add(element.getAsInt());
                case "F" -> params.add(element.getAsFloat());
                case "J" -> params.add(element.getAsLong());
                case "D" -> params.add(element.getAsDouble());
                case "Ljava/lang/String;" -> params.add(element.isJsonNull() ? null : element.getAsString());
                default -> {
                    if (!element.isJsonNull()) {
                        error("fml.modloadingissue.enumextender.argument.constant.unsupported_type", mod, argType, idx, fieldName, enumName);
                        return null;
                    }
                    params.add(null);
                }
            }
            idx++;
        }
        return new EnumParameters.Constant(params);
    }

    private static boolean isValidClassDescriptor(String desc) {
        for (String part : desc.split("/", -1)) {
            if (!SourceVersion.isIdentifier(part)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidConstructorDescriptor(String desc) {
        if (!desc.startsWith("(")) {
            return false;
        }
        if (!desc.endsWith(")V")) {
            return false;
        }
        boolean pendingArray = false;
        for (int i = 1; i < desc.length() - 2; i++) {
            char c = desc.charAt(i);
            if ("ZCBSIFJD".indexOf(c) != -1) {
                pendingArray = false;
                continue;
            }
            if (c == '[') {
                pendingArray = true;
                continue;
            }
            if (c != 'L') {
                return false;
            }
            int semicolon = desc.indexOf(';', i);
            if (semicolon <= i) {
                return false;
            }
            String segment = desc.substring(i + 1, semicolon);
            if (!isValidClassDescriptor(segment)) {
                return false;
            }
            i = semicolon;
            pendingArray = false;
        }
        return !pendingArray;
    }

    private static void error(String message, IModInfo mod, Object... params) {
        ModLoader.addLoadingIssue(ModLoadingIssue.error(message, params).withAffectedMod(mod));
    }
}
