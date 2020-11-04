package io.izzel.arclight.common.asm;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.conf.AsyncCatcherSpec;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.util.Constants;

import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class AsyncCatcher implements Implementer {

    public static final AsyncCatcher INSTANCE = new AsyncCatcher();
    private static final Marker MARKER = MarkerManager.getMarker("ASYNC_CATCHER");
    private static final CallbackInfoReturnable<?> NOOP = new CallbackInfoReturnable<>("noop", false);
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private final boolean dump;
    private final boolean warn;
    private final AsyncCatcherSpec.Operation defaultOp;
    private final Map<String, Map<String, String>> reasons;
    private final ClassLoader classLoader;

    public AsyncCatcher() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        this.reasons = gson.fromJson(
            new InputStreamReader(AsyncCatcher.class.getResourceAsStream("/async_catcher.json")),
            new TypeToken<Map<String, Map<String, String>>>() {}.getType()
        );
        this.defaultOp = ArclightConfig.spec().getAsyncCatcher().getDefaultOp();
        this.dump = ArclightConfig.spec().getAsyncCatcher().isDump();
        this.warn = ArclightConfig.spec().getAsyncCatcher().isWarn();
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    @Override
    public boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) {
        Map<String, String> map = reasons.get(node.name);
        if (map != null) {
            boolean found = false;
            List<MethodNode> methods = node.methods;
            for (int i = 0, methodsSize = methods.size(); i < methodsSize; i++) {
                MethodNode method = methods.get(i);
                String reason = map.get(method.name + method.desc);
                if (reason != null) {
                    found = true;
                    injectCheck(node, method, reason);
                }
            }
            return found;
        }
        return false;
    }

    private void injectCheck(ClassNode node, MethodNode methodNode, String reason) {
        ArclightImplementer.LOGGER.debug(MARKER, "Injecting {}/{}{} for reason {}", node.name, methodNode.name, methodNode.desc, reason);
        AsyncCatcherSpec.Operation operation = ArclightConfig.spec().getAsyncCatcher().getOverrides().getOrDefault(reason, defaultOp);
        InsnList insnList = new InsnList();
        LabelNode labelNode = new LabelNode(new Label());
        LabelNode labelNode1 = new LabelNode(new Label());
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/izzel/arclight/common/mod/server/ArclightServer", "isPrimaryThread", "()Z"));
        insnList.add(new JumpInsnNode(Opcodes.IFNE, labelNode));
        instantiateCallback(node, methodNode, insnList);
        insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getType(AsyncCatcherSpec.Operation.class).getInternalName(), operation.name(), Type.getType(AsyncCatcherSpec.Operation.class).getDescriptor()));
        insnList.add(new LdcInsnNode(reason));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(AsyncCatcher.class).getInternalName(), "checkOp", "(Ljava/util/function/Supplier;Lio/izzel/arclight/i18n/conf/AsyncCatcherSpec$Operation;Ljava/lang/String;)Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;"));
        Type returnType = Type.getMethodType(methodNode.desc).getReturnType();
        boolean hasReturn = !returnType.equals(Type.VOID_TYPE);
        if (hasReturn) {
            insnList.add(new InsnNode(Opcodes.DUP));
        }
        insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getType(CallbackInfoReturnable.class).getInternalName(), "isCancelled", "()Z"));
        insnList.add(new JumpInsnNode(Opcodes.IFEQ, hasReturn ? labelNode1 : labelNode));
        if (hasReturn) {
            insnList.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getType(CallbackInfoReturnable.class).getInternalName(), getReturnAccessor(returnType), getReturnDescriptor(returnType)));
            if (returnType.getSort() > Type.DOUBLE) {
                insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
            }
            insnList.add(new InsnNode(returnType.getOpcode(Opcodes.IRETURN)));
        } else {
            insnList.add(new InsnNode(Opcodes.RETURN));
        }
        insnList.add(labelNode1);
        insnList.add(new InsnNode(Opcodes.POP));
        insnList.add(labelNode);
        methodNode.instructions.insert(insnList);
    }

    private void instantiateCallback(ClassNode node, MethodNode methodNode, InsnList insnList) {
        MethodNode bridge;
        if (Modifier.isPrivate(methodNode.access)) {
            bridge = createBridge(node, methodNode);
        } else {
            bridge = methodNode;
        }

        ClassNode classNode = new ClassNode();
        String desc = createImplType(node, methodNode, classNode, bridge);

        insnList.add(new TypeInsnNode(Opcodes.NEW, classNode.name));
        insnList.add(new InsnNode(Opcodes.DUP));
        Type methodType = Type.getMethodType(methodNode.desc);
        ArclightImplementer.loadArgs(insnList, methodNode, methodType.getArgumentTypes(), 0);
        insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, classNode.name, "<init>", desc));
    }

    private String createImplType(ClassNode node, MethodNode methodNode, ClassNode classNode, MethodNode bridge) {
        classNode.version = Opcodes.V1_8;
        classNode.name = node.name + "$AsyncCatcher$" + COUNTER.getAndIncrement();
        classNode.access = Opcodes.ACC_SYNTHETIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL;
        classNode.superName = "java/lang/Object";
        classNode.interfaces.add(Type.getType(Supplier.class).getInternalName());
        List<Type> types = new ArrayList<>();
        if (!Modifier.isStatic(methodNode.access)) {
            types.add(Type.getObjectType(node.name));
        }
        types.addAll(Arrays.asList(Type.getArgumentTypes(methodNode.desc)));

        for (int i = 0; i < types.size(); i++) {
            FieldNode fieldNode = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "x" + i, types.get(i).getDescriptor(), null, null);
            classNode.fields.add(fieldNode);
        }

        MethodNode init = new MethodNode();
        init.name = "<init>";
        init.desc = Type.getMethodType(Type.VOID_TYPE, types.toArray(new Type[0])).getDescriptor();
        init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));

        int offset = 1;
        for (int i = 0; i < types.size(); i++) {
            init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            init.instructions.add(new VarInsnNode(types.get(i).getOpcode(Opcodes.ILOAD), offset));
            init.instructions.add(new FieldInsnNode(Opcodes.PUTFIELD, classNode.name, "x" + i, types.get(i).getDescriptor()));
            offset += types.get(i).getSize();
        }
        init.instructions.add(new InsnNode(Opcodes.RETURN));
        classNode.methods.add(init);

        MethodNode get = new MethodNode();
        get.name = "get";
        get.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL;
        get.desc = "()Ljava/lang/Object;";
        GeneratorAdapter adapter = new GeneratorAdapter(get, Opcodes.ACC_PUBLIC, get.name, get.desc);
        for (int i = 0; i < types.size(); i++) {
            adapter.loadThis();
            adapter.getField(Type.getObjectType(classNode.name), "x" + i, types.get(i));
        }
        get.instructions.add(new MethodInsnNode(
            Modifier.isStatic(methodNode.access) ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
            node.name, bridge.name, bridge.desc
        ));
        adapter.valueOf(Type.getReturnType(bridge.desc));
        adapter.returnValue();
        classNode.methods.add(get);
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        byte[] bytes = writer.toByteArray();
        Unsafe.defineClass(Type.getObjectType(classNode.name).getClassName(), bytes, 0, bytes.length, this.classLoader, AsyncCatcher.class.getProtectionDomain());
        ArclightImplementer.LOGGER.debug(MARKER, "Defined impl callback class {}", classNode.name);
        return init.desc;
    }

    private MethodNode createBridge(ClassNode node, MethodNode methodNode) {
        MethodNode ret = new MethodNode();
        ret.name = methodNode.name + "$asyncCatcher$" + COUNTER.getAndIncrement();
        ret.desc = methodNode.desc;
        ret.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC;
        if (Modifier.isStatic(methodNode.access)) {
            ret.access = ret.access | Opcodes.ACC_STATIC;
        }
        Type methodType = Type.getMethodType(methodNode.desc);
        ArclightImplementer.loadArgs(ret.instructions, methodNode, methodType.getArgumentTypes(), 0);
        int invokeCode = Modifier.isStatic(methodNode.access) ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL;
        ret.instructions.add(new MethodInsnNode(invokeCode, node.name, methodNode.name, methodNode.desc));
        ret.instructions.add(new InsnNode(methodType.getReturnType().getOpcode(Opcodes.IRETURN)));
        node.methods.add(ret);
        ArclightImplementer.LOGGER.debug(MARKER, "Bridge method {}/{}{} created", node.name, ret.name, ret.desc);
        return ret;
    }

    static String getReturnAccessor(org.objectweb.asm.Type returnType) {
        if (returnType.getSort() == org.objectweb.asm.Type.OBJECT || returnType.getSort() == org.objectweb.asm.Type.ARRAY) {
            return "getReturnValue";
        }
        return String.format("getReturnValue%s", returnType.getDescriptor());
    }

    static String getReturnDescriptor(org.objectweb.asm.Type returnType) {
        if (returnType.getSort() == org.objectweb.asm.Type.OBJECT || returnType.getSort() == org.objectweb.asm.Type.ARRAY) {
            return String.format("()%s", Constants.OBJECT_DESC);
        }
        return String.format("()%s", returnType.getDescriptor());
    }

    @SuppressWarnings("unchecked")
    public static <T> CallbackInfoReturnable<T> checkOp(Supplier<T> method, AsyncCatcherSpec.Operation operation, String reason) throws Throwable {
        if (INSTANCE.warn) {
            ArclightImplementer.LOGGER.warn(MARKER, "Async " + reason);
        }
        IllegalStateException exception = new IllegalStateException("Asynchronous " + reason + "!");
        if (INSTANCE.dump) {
            ArclightImplementer.LOGGER.debug(MARKER, "Async " + reason, exception);
        }
        switch (operation) {
            case NONE: return (CallbackInfoReturnable<T>) NOOP;
            case EXCEPTION: throw exception;
            case BLOCK: {
                CallbackInfoReturnable<T> cir = new CallbackInfoReturnable<>(reason, true);
                CompletableFuture<T> future = CompletableFuture.supplyAsync(method, ArclightServer.getMainThreadExecutor());
                cir.setReturnValue(future.get(5, TimeUnit.SECONDS));
                return cir;
            }
            case DISPATCH: {
                ArclightServer.executeOnMainThread(method::get);
                CallbackInfoReturnable<T> cir = new CallbackInfoReturnable<>(reason, true);
                cir.cancel();
                return cir;
            }
        }
        throw new IllegalStateException("how this can happen?");
    }
}
