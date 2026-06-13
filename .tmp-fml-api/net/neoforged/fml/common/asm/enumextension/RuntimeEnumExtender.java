/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.common.asm.ListGeneratorAdapter;
import net.neoforged.fml.jarcontents.JarResource;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Transforms enums implementing {@link IExtensibleEnum} to add additional entries loaded from files provided by mods
 */
@ApiStatus.Internal
public class RuntimeEnumExtender implements ClassProcessor {
    private static final Type MARKER_IFACE = Type.getType(IExtensibleEnum.class);
    private static final Type INDEXED_ANNOTATION = Type.getType(IndexedEnum.class);
    private static final Type NAMED_ANNOTATION = Type.getType(NamedEnum.class);
    private static final Type RESERVED_ANNOTATION = Type.getType(ReservedConstructor.class);
    private static final Type ENUM_PROXY = Type.getType(EnumProxy.class);
    private static final Type NET_CHECK = Type.getType(NetworkedEnum.NetworkCheck.class);
    private static final Type EXT_INFO = Type.getType(ExtensionInfo.class);
    private static final String EXT_INFO_GETTER_DESC = Type.getMethodDescriptor(EXT_INFO);
    private static final String EXT_INFO_CTOR_DESC = Type.getMethodDescriptor(
            Type.VOID_TYPE, Type.BOOLEAN_TYPE, Type.INT_TYPE, Type.INT_TYPE, NET_CHECK);
    private static final Type NETWORKED_ANNOTATION = Type.getType(NetworkedEnum.class);
    private static final Type EXTENDER = Type.getType(RuntimeEnumExtender.class);
    private static final Type ARRAYS = Type.getType("Ljava/util/Arrays;");
    private static final int ENUM_FLAGS = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM;
    private static final int ARRAY_FLAGS = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_SYNTHETIC;
    private static final int EXT_INFO_FLAGS = Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL;
    private static Map<String, List<EnumPrototype>> prototypes = Map.of();

    @Override
    public ProcessorName name() {
        return ClassProcessorIds.RUNTIME_ENUM_EXTENDER;
    }

    @Override
    public Set<ProcessorName> runsBefore() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public OrderingHint orderingHint() {
        return OrderingHint.EARLY;
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && prototypes.containsKey(context.type().getInternalName());
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        var classNode = context.node();
        var classType = context.type();
        if ((classNode.access & Opcodes.ACC_ENUM) == 0 || !classNode.interfaces.contains(MARKER_IFACE.getInternalName())) {
            throw new IllegalStateException("Tried to extend non-enum class or non-extensible enum: " + classType);
        }

        List<EnumPrototype> protos = prototypes.getOrDefault(classType.getInternalName(), List.of());
        if (protos.isEmpty()) {
            return ComputeFlags.NO_REWRITE;
        }

        MethodNode clinit = findMethod(classNode, mth -> mth.name.equals("<clinit>"));
        Optional<MethodNode> $valuesOpt = tryFindMethod(classNode, mth -> mth.name.equals("$values"));
        boolean $valuesPresent = $valuesOpt.isPresent();
        MethodNode getExtInfo = findMethod(classNode, mth -> mth.name.equals("getExtensionInfo") && mth.desc.equals(EXT_INFO_GETTER_DESC));
        Set<String> ctors = classNode.methods.stream()
                .filter(mth -> mth.name.equals("<init>"))
                .filter(RuntimeEnumExtender::isAllowedConstructor)
                .map(mth -> mth.desc)
                .collect(Collectors.toSet());

        int vanillaEntryCount = getVanillaEntryCount(classNode, classType);
        int idParamIdx = getParameterIndexFromAnnotation(classNode, INDEXED_ANNOTATION);
        int nameParamIdx = getParameterIndexFromAnnotation(classNode, NAMED_ANNOTATION);

        if (idParamIdx != -1 && idParamIdx == nameParamIdx) {
            throw new IllegalStateException("ID and name parameter cannot have the same index on enum " + classType);
        }

        FieldNode infoField = new FieldNode(EXT_INFO_FLAGS, "FML$ENUM_EXT_INFO", EXT_INFO.getDescriptor(), null, null);
        classNode.fields.add(infoField);

        clearMethod(getExtInfo);
        InsnList getExtInfoInsnList = getExtInfo.instructions;
        getExtInfoInsnList.add(new FieldInsnNode(Opcodes.GETSTATIC, classType.getInternalName(), infoField.name, infoField.desc));
        getExtInfoInsnList.add(new InsnNode(Opcodes.ARETURN));

        ListGeneratorAdapter clinitGenerator = new ListGeneratorAdapter(new InsnList());
        List<FieldNode> enumEntries = createEnumEntries(classType, clinitGenerator, ctors, idParamIdx, nameParamIdx, vanillaEntryCount, protos);
        if ($valuesPresent) { // javac
            MethodNode $values = $valuesOpt.get();
            MethodInsnNode $valuesInsn = findFirstStaticMethodCall(clinit, classType.getInternalName(), $values.name, $values.desc);
            clinit.instructions.insertBefore($valuesInsn, clinitGenerator.insnList);
        } else { // ECJ
            AbstractInsnNode firstValuesArrayInsn = findValuesArrayCreation(classType, clinit);
            clinit.instructions.insertBefore(firstValuesArrayInsn, clinitGenerator.insnList);
        }

        ListGeneratorAdapter clinitTailGenerator = new ListGeneratorAdapter(new InsnList());
        buildExtensionInfo(classNode, classType, clinitTailGenerator, infoField, vanillaEntryCount, protos.size());
        returnValuesToExtender(classType, clinitTailGenerator, protos, enumEntries);
        AbstractInsnNode clinitRetNode = findFirstInstructionBefore(clinit, Opcodes.RETURN, clinit.instructions.size() - 1);
        clinit.instructions.insertBefore(clinitRetNode, clinitTailGenerator.insnList);
        classNode.fields.addAll(vanillaEntryCount, enumEntries);

        ListGeneratorAdapter appendValuesGenerator = new ListGeneratorAdapter(new InsnList());
        appendValuesArray(classType, appendValuesGenerator, enumEntries);
        if ($valuesPresent) { // javac
            MethodNode $values = $valuesOpt.get();
            AbstractInsnNode $valuesAretInsn = findFirstInstructionBefore($values, Opcodes.ARETURN, $values.instructions.size() - 1);
            $values.instructions.insertBefore($valuesAretInsn, appendValuesGenerator.insnList);
        } else { // ECJ
            AbstractInsnNode putStaticInsn = findValuesArrayStore(classType, classNode, clinit, classType.getInternalName());
            clinit.instructions.insertBefore(putStaticInsn, appendValuesGenerator.insnList);
        }

        return ComputeFlags.COMPUTE_FRAMES;
    }

    /**
     * Finds the first static method call in the given method matching the given owner, name and descriptor
     *
     * @param method     the method to search in
     * @param owner      the method call's owner to search for
     * @param name       the method call's name
     * @param descriptor the method call's descriptor
     * @return the found method call node, null if none matched after the given index
     */
    public static MethodInsnNode findFirstStaticMethodCall(MethodNode method, String owner, String name, String descriptor) {
        for (int i = 0; i < method.instructions.size(); i++) {
            AbstractInsnNode node = method.instructions.get(i);
            if (node instanceof MethodInsnNode methodInsnNode && node.getOpcode() == Opcodes.INVOKESTATIC) {
                if (methodInsnNode.owner.equals(owner) &&
                        methodInsnNode.name.equals(name) &&
                        methodInsnNode.desc.equals(descriptor)) {
                    return methodInsnNode;
                }
            }
        }
        return null;
    }

    /**
     * Finds the first instruction with matching opcode before the given index in reverse search
     *
     * @param method     the method to search in
     * @param opCode     the opcode to search for
     * @param startIndex the index at which to start searching (inclusive)
     * @return the found instruction node or null if none matched before the given startIndex
     */
    public static AbstractInsnNode findFirstInstructionBefore(MethodNode method, int opCode, int startIndex) {
        for (int i = Math.max(method.instructions.size() - 1, startIndex); i >= 0; i--) {
            AbstractInsnNode ain = method.instructions.get(i);
            if (ain.getOpcode() == opCode) {
                return ain;
            }
        }
        return null;
    }

    private static Optional<MethodNode> tryFindMethod(ClassNode classNode, Predicate<MethodNode> predicate) {
        return classNode.methods.stream()
                .filter(predicate)
                .findFirst();
    }

    private static MethodNode findMethod(ClassNode classNode, Predicate<MethodNode> predicate) {
        return tryFindMethod(classNode, predicate)
                .orElseThrow();
    }

    private static FieldNode findField(ClassNode classNode, Predicate<FieldNode> predicate) {
        return classNode.fields.stream()
                .filter(predicate)
                .findFirst()
                .orElseThrow();
    }

    private static void clearMethod(MethodNode mth) {
        mth.instructions.clear();
        mth.localVariables.clear();
        if (mth.tryCatchBlocks != null) {
            mth.tryCatchBlocks.clear();
        }
        if (mth.visibleLocalVariableAnnotations != null) {
            mth.visibleLocalVariableAnnotations.clear();
        }
        if (mth.invisibleLocalVariableAnnotations != null) {
            mth.invisibleLocalVariableAnnotations.clear();
        }
    }

    private static int getVanillaEntryCount(ClassNode classNode, Type classType) {
        return (int) classNode.fields.stream()
                .takeWhile(field -> (field.access & ENUM_FLAGS) == ENUM_FLAGS && field.desc.equals(classType.getDescriptor()))
                .count();
    }

    private static int getParameterIndexFromAnnotation(ClassNode classNode, Type annoType) {
        if (classNode.invisibleAnnotations == null) {
            return -1;
        }

        AnnotationNode annotation = classNode.invisibleAnnotations.stream()
                .filter(anno -> anno.desc.equals(annoType.getDescriptor()))
                .findFirst()
                .orElse(null);
        if (annotation == null) {
            return -1;
        }
        if (annotation.values == null) {
            return 0; // No explicit value specified
        }

        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals("value")) {
                return (Integer) annotation.values.get(i + 1);
            }
        }
        return 0;
    }

    private static boolean isAllowedConstructor(MethodNode mth) {
        if (mth.invisibleAnnotations == null) {
            return true;
        }

        AnnotationNode annotation = mth.invisibleAnnotations.stream()
                .filter(anno -> anno.desc.equals(RESERVED_ANNOTATION.getDescriptor()))
                .findFirst()
                .orElse(null);
        return annotation == null;
    }

    private static AbstractInsnNode findValuesArrayCreation(Type classType, MethodNode clinit) {
        for (int i = 0; i < clinit.instructions.size(); i++) {
            AbstractInsnNode ain = clinit.instructions.get(i);
            if (ain.getOpcode() != Opcodes.ANEWARRAY || !(ain instanceof TypeInsnNode tin)) {
                continue;
            }

            if (tin.desc.equals(classType.getInternalName())) {
                // Return instruction loading the array size
                return tin.getPrevious();
            }
        }

        throw new NoSuchElementException("Failed to locate values array creation in enum " + classType);
    }

    private static FieldInsnNode findValuesArrayStore(Type classType, ClassNode classNode, MethodNode mth, String owner) {
        String arrayDesc = Type.getType("[" + classType.getDescriptor()).getDescriptor();
        FieldNode valuesArray = findField(classNode, field -> ((field.access & ARRAY_FLAGS) == ARRAY_FLAGS) && field.desc.equals(arrayDesc));
        for (int i = 0; i < mth.instructions.size(); i++) {
            AbstractInsnNode ain = mth.instructions.get(i);
            if (ain.getOpcode() != Opcodes.PUTSTATIC || !(ain instanceof FieldInsnNode fin)) {
                continue;
            }
            if (fin.desc.equals(valuesArray.desc) && fin.name.equals(valuesArray.name) && fin.owner.equals(owner)) {
                return fin;
            }
        }
        throw new NoSuchElementException();
    }

    private static List<FieldNode> createEnumEntries(
            Type classType,
            ListGeneratorAdapter generator,
            Set<String> ctors,
            int idParamIdx,
            int nameParamIdx,
            int vanillaEntryCount,
            List<EnumPrototype> prototypes) {
        List<FieldNode> enumFields = new ArrayList<>(prototypes.size());
        int ordinal = vanillaEntryCount;
        for (EnumPrototype proto : prototypes) {
            if (!ctors.contains(proto.fullCtorDesc())) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT,
                        "Invalid, non-existant or disallowed constructor '%s' for field '%s' in enum '%s' specified by mod '%s'",
                        proto.ctorDesc(),
                        proto.fieldName(),
                        proto.enumName(),
                        proto.owningMod()));
            }

            String fieldName = proto.fieldName();
            FieldNode field = new FieldNode(ENUM_FLAGS, fieldName, classType.getDescriptor(), null, null);
            enumFields.add(field);

            // NEW_FIELD = new EnumType(fieldName, ordinal, ...);
            generator.newInstance(classType);
            generator.dup();
            generator.push(fieldName);
            generator.push(ordinal);
            loadConstructorParams(generator, idParamIdx, nameParamIdx, ordinal, proto); // additional parameters
            generator.invokeConstructor(classType, new Method("<init>", proto.fullCtorDesc()));
            generator.putStatic(classType, field.name, classType);

            ordinal++;
        }
        return enumFields;
    }

    private static void loadConstructorParams(ListGeneratorAdapter generator, int idParamIdx, int nameParamIdx, int ordinal, EnumPrototype proto) {
        Type[] argTypes = Type.getType(proto.fullCtorDesc()).getArgumentTypes();
        switch (proto.ctorParams()) {
            case EnumParameters.FieldReference(Type owner, String fieldName) -> {
                for (int idx = 2; idx < argTypes.length; idx++) {
                    if (idx - 2 == idParamIdx) {
                        generator.push(ordinal);
                        continue;
                    }
                    generator.getStatic(owner, fieldName, ENUM_PROXY);
                    generator.push(idx - 2);
                    generator.invokeVirtual(ENUM_PROXY, new Method("getParameter", "(I)Ljava/lang/Object;"));
                    generator.unbox(argTypes[idx]);
                    if (idx - 2 == nameParamIdx) {
                        generator.push(proto.owningMod());
                        generator.invokeStatic(EXTENDER, new Method("validateNameParameter", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
                    }
                }
            }
            case EnumParameters.MethodReference(Type owner, String methodName) -> {
                for (int idx = 2; idx < argTypes.length; idx++) {
                    if (idx - 2 == idParamIdx) {
                        generator.push(ordinal);
                        continue;
                    }
                    generator.push(idx - 2);
                    generator.push(argTypes[idx]);
                    generator.invokeStatic(owner, new Method(methodName, "(ILjava/lang/Class;)Ljava/lang/Object;"));
                    generator.unbox(argTypes[idx]);
                    if (idx - 2 == nameParamIdx) {
                        generator.push(proto.owningMod());
                        generator.invokeStatic(EXTENDER, new Method("validateNameParameter", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;"));
                    }
                }
            }
            case EnumParameters.Constant(List<Object> paramList) -> {
                for (int idx = 2; idx < argTypes.length; idx++) {
                    if (idx - 2 == idParamIdx) {
                        if (!(paramList.get(idx - 2) instanceof Integer i) || i != -1) {
                            throw new IllegalArgumentException(String.format(
                                    Locale.ROOT,
                                    "Expected -1 as ID parameter at index %d in parameters for field '%s' in enum '%s' specified by mod '%s'",
                                    idx - 2,
                                    proto.fieldName(),
                                    proto.enumName(),
                                    proto.owningMod()));
                        }
                        generator.push(ordinal);
                        continue;
                    }
                    if (idx - 2 == nameParamIdx) {
                        if (!(paramList.get(idx - 2) instanceof String str)) {
                            throw new IllegalArgumentException(String.format(
                                    Locale.ROOT,
                                    "Expected String at index %d in parameters for field '%s' in enum '%s' specified by mod '%s'",
                                    idx - 2,
                                    proto.fieldName(),
                                    proto.enumName(),
                                    proto.owningMod()));
                        }
                        validateNameParameter(str, proto.owningMod());
                    }
                    switch (paramList.get(idx - 2)) {
                        case null -> generator.push((String) null);
                        case String string -> generator.push(string);
                        case Character ch -> generator.push(ch);
                        case Byte b -> generator.push(b);
                        case Short s -> generator.push(s);
                        case Integer i -> generator.push(i);
                        case Long l -> generator.push(l);
                        case Float f -> generator.push(f);
                        case Double d -> generator.push(d);
                        case Boolean bool -> generator.push(bool);
                        default -> throw new IllegalArgumentException(String.format(
                                Locale.ROOT,
                                "Unsupported constant type '%s' in parameters for field '%s' in enum '%s' specified by mod '%s'",
                                paramList.get(idx - 2).getClass(),
                                proto.fieldName(),
                                proto.enumName(),
                                proto.owningMod()));
                    }
                }
            }
        }
    }

    private static void buildExtensionInfo(ClassNode classNode, Type classType, ListGeneratorAdapter generator, FieldNode infoField, int vanillaCount, int moddedCount) {
        String netCheckValue = null;
        if (classNode.visibleAnnotations != null) {
            netCheckValue = classNode.visibleAnnotations.stream()
                    .filter(anno -> anno.desc.equals(NETWORKED_ANNOTATION.getDescriptor()))
                    .findFirst()
                    .flatMap(anno -> {
                        if (anno.values == null) {
                            throw new IllegalStateException("Expected values on NetworkedEnum annotation");
                        }
                        for (int i = 0; i < anno.values.size(); i += 2) {
                            if (anno.values.get(i).equals("value")) {
                                String[] value = (String[]) anno.values.get(i + 1);
                                return Optional.of(value[1]);
                            }
                        }
                        throw new IllegalStateException("Expected NetworkedEnum.NetworkCheck value on NetworkedEnum annotation");
                    })
                    .orElse(null);
        }

        generator.newInstance(EXT_INFO);
        generator.dup();
        generator.push(moddedCount > 0);
        generator.push(vanillaCount);
        generator.push(vanillaCount + moddedCount);
        if (netCheckValue != null) {
            generator.getStatic(NET_CHECK, netCheckValue, NET_CHECK);
        } else {
            generator.push((Type) null);
        }
        generator.invokeConstructor(EXT_INFO, new Method("<init>", EXT_INFO_CTOR_DESC));
        generator.putStatic(classType, infoField.name, EXT_INFO);
    }

    private static void returnValuesToExtender(Type classType, ListGeneratorAdapter generator, List<EnumPrototype> protos, List<FieldNode> entries) {
        for (int i = 0; i < protos.size(); i++) {
            EnumPrototype prototype = protos.get(i);
            if (!(prototype.ctorParams() instanceof EnumParameters.FieldReference(Type owner, String fieldName))) {
                continue;
            }

            FieldNode field = entries.get(i);
            generator.getStatic(owner, fieldName, ENUM_PROXY);
            generator.getStatic(classType, field.name, classType);
            generator.invokeVirtual(ENUM_PROXY, new Method("setValue", "(Ljava/lang/Enum;)V"));
        }
    }

    private static void appendValuesArray(Type classType, ListGeneratorAdapter generator, List<FieldNode> enumEntries) {
        // values = Arrays.copyOf(values, values.length + listSize);
        generator.dup();
        generator.arrayLength();
        generator.push(enumEntries.size());
        generator.math(GeneratorAdapter.ADD, Type.INT_TYPE);
        generator.invokeStatic(ARRAYS, new Method("copyOf", "([Ljava/lang/Object;I)[Ljava/lang/Object;"));
        generator.checkCast(Type.getType("[" + classType.getDescriptor()));

        // values[NEW_FIELD.ordinal()] = NEW_FIELD;
        for (FieldNode entry : enumEntries) {
            generator.dup();
            generator.getStatic(classType, entry.name, classType);
            generator.dup();
            generator.invokeVirtual(classType, new Method("ordinal", "()I"));
            generator.swap();
            generator.arrayStore(classType);
        }
    }

    public static void loadEnumPrototypes(Map<IModInfo, JarResource> paths) {
        prototypes = paths.entrySet()
                .stream()
                .map(entry -> EnumPrototype.load(entry.getKey(), entry.getValue()))
                .flatMap(List::stream)
                .sorted()
                .reduce(
                        new HashMap<>(),
                        (map, proto) -> {
                            map.computeIfAbsent(proto.enumName(), ignored -> new ArrayList<>()).add(proto);
                            return map;
                        },
                        (protoOne, protoTwo) -> {
                            throw new IllegalStateException("Duplicate EnumPrototype: " + protoOne);
                        });

        Set<String> erroredEnums = new HashSet<>();
        for (Map.Entry<String, List<EnumPrototype>> entry : prototypes.entrySet()) {
            Map<String, EnumPrototype> distinctPrototypes = new HashMap<>();
            boolean foundDupe = false;
            for (EnumPrototype proto : entry.getValue()) {
                EnumPrototype prevProto = distinctPrototypes.put(proto.fieldName(), proto);
                if (prevProto != null) {
                    foundDupe = true;
                    ModLoader.addLoadingIssue(ModLoadingIssue.error(
                            "fml.modloadingissue.enumextender.duplicate",
                            proto.fieldName(),
                            proto.enumName(),
                            proto.owningMod(),
                            prevProto.owningMod()));
                }
            }
            if (foundDupe) {
                erroredEnums.add(entry.getKey());
            }
        }
        erroredEnums.forEach(prototypes::remove);
    }

    @SuppressWarnings("UnusedReturnValue") // Return value used via transformer
    public static String validateNameParameter(String fieldName, String owningMod) {
        if (!fieldName.startsWith(owningMod + ":")) {
            throw new IllegalArgumentException(String.format(
                    Locale.ROOT, "Name parameter must be prefixed by mod ID: '%s' provided by mod '%s'", fieldName, owningMod));
        }
        return fieldName;
    }
}
