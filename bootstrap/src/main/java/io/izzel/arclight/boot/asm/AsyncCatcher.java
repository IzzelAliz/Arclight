package io.izzel.arclight.boot.asm;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.i18n.conf.AsyncCatcherSpec;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.util.Constants;

import java.io.InputStreamReader;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.objectweb.asm.Type.getType;

public class AsyncCatcher implements Implementer {

    public static final AsyncCatcher INSTANCE = new AsyncCatcher();
    private static final Marker MARKER = MarkerManager.getMarker("ASYNC_CATCHER");
    private static final CallbackInfoReturnable<?> NOOP = new CallbackInfoReturnable<>("noop", false);
    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    private static final String LAMBDA_METAFACTORY_METHOD = Type.getMethodDescriptor(Type.getType(CallSite.class), Type.getType(MethodHandles.Lookup.class), Type.getType(String.class), Type.getType(MethodType.class), Type.getType(MethodType.class), getType(MethodHandle.class), Type.getType(MethodType.class));
    private static final Handle LAMBDA_BOOTSTRAP_HANDLE = new Handle(Opcodes.H_INVOKESTATIC, Type.getInternalName(LambdaMetafactory.class), "metafactory", LAMBDA_METAFACTORY_METHOD, false);

    private final boolean dump;
    private final boolean warn;
    private final AsyncCatcherSpec.Operation defaultOp;
    private final Map<String, Map<String, String>> reasons;

    public AsyncCatcher() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        this.reasons = gson.fromJson(
            new InputStreamReader(AsyncCatcher.class.getResourceAsStream("/async_catcher.json")),
            new TypeToken<Map<String, Map<String, String>>>() {}.getType()
        );
        this.defaultOp = ArclightConfig.spec().getAsyncCatcher().getDefaultOp();
        this.dump = ArclightConfig.spec().getAsyncCatcher().isDump();
        this.warn = ArclightConfig.spec().getAsyncCatcher().isWarn();
    }

    @Override
    public boolean processClass(ClassNode node) {
        Map<String, String> map = reasons.get(node.name);
        if (map != null) {
            boolean found = false;
            List<MethodNode> methods = node.methods;
            // concurrent modification
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
        Implementer.LOGGER.debug(MARKER, "Injecting {}/{}{} for reason {}", node.name, methodNode.name, methodNode.desc, reason);
        AsyncCatcherSpec.Operation operation = ArclightConfig.spec().getAsyncCatcher().getOverrides().getOrDefault(reason, defaultOp);
        InsnList insnList = new InsnList();
        LabelNode labelNode = new LabelNode(new Label());
        LabelNode labelNode1 = new LabelNode(new Label());
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/izzel/arclight/common/mod/server/ArclightServer", "isPrimaryThread", "()Z"));
        insnList.add(new JumpInsnNode(Opcodes.IFNE, labelNode));
        instantiateCallback(node, methodNode, insnList);
        insnList.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getType(AsyncCatcherSpec.Operation.class).getInternalName(), operation.name(), Type.getType(AsyncCatcherSpec.Operation.class).getDescriptor()));
        insnList.add(new LdcInsnNode(reason));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/izzel/arclight/common/mod/server/ArclightServer", "getMainThreadExecutor", "()Ljava/util/concurrent/Executor;", false));
        insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(AsyncCatcher.class).getInternalName(), "checkOp", "(Ljava/util/function/Supplier;Lio/izzel/arclight/i18n/conf/AsyncCatcherSpec$Operation;Ljava/lang/String;Ljava/util/concurrent/Executor;)Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;"));
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

        Type methodType = Type.getMethodType(methodNode.desc);
        Implementer.loadArgs(insnList, methodNode, methodType.getArgumentTypes(), 0);

        var argTypes = Type.getArgumentTypes(methodNode.desc);
        if (!Modifier.isStatic(methodNode.access)) {
            var newTypes = new Type[argTypes.length + 1];
            newTypes[0] = Type.getObjectType(node.name);
            System.arraycopy(argTypes, 0, newTypes, 1, argTypes.length);
            argTypes = newTypes;
        }

        insnList.add(new InvokeDynamicInsnNode(
            "get", Type.getMethodDescriptor(Type.getType(Supplier.class), argTypes),
            LAMBDA_BOOTSTRAP_HANDLE,
            Type.getMethodType(Type.getType(Object.class)),
            new Handle(
                Modifier.isStatic(bridge.access) ? Opcodes.H_INVOKESTATIC : Opcodes.H_INVOKEVIRTUAL,
                node.name,
                bridge.name,
                bridge.desc,
                false
            ),
            Type.getMethodType(Type.getType(Object.class))
        ));
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
        Implementer.loadArgs(ret.instructions, methodNode, methodType.getArgumentTypes(), 0);
        int invokeCode = Modifier.isStatic(methodNode.access) ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL;
        ret.instructions.add(new MethodInsnNode(invokeCode, node.name, methodNode.name, methodNode.desc));
        ret.instructions.add(new InsnNode(methodType.getReturnType().getOpcode(Opcodes.IRETURN)));
        node.methods.add(ret);
        Implementer.LOGGER.debug(MARKER, "Bridge method {}/{}{} created", node.name, ret.name, ret.desc);
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
    public static <T> CallbackInfoReturnable<T> checkOp(Supplier<T> method, AsyncCatcherSpec.Operation operation, String reason, Executor executor) throws Throwable {
        if (INSTANCE.warn) {
            Implementer.LOGGER.warn(MARKER, "Async " + reason);
        }
        IllegalStateException exception = new IllegalStateException("Asynchronous " + reason + "!");
        if (INSTANCE.dump) {
            Implementer.LOGGER.debug(MARKER, "Async " + reason, exception);
        }
        switch (operation) {
            case NONE:
                return (CallbackInfoReturnable<T>) NOOP;
            case EXCEPTION:
                throw exception;
            case BLOCK: {
                CallbackInfoReturnable<T> cir = new CallbackInfoReturnable<>(reason, true);
                CompletableFuture<T> future = CompletableFuture.supplyAsync(method, executor);
                try {
                    cir.setReturnValue(future.get(5, TimeUnit.SECONDS));
                } catch (TimeoutException e) {
                    var thread = ((Supplier<Thread>) executor).get();
                    var ex = new Exception("Server thread");
                    ex.setStackTrace(thread.getStackTrace());
                    Implementer.LOGGER.error(MARKER, "Async catcher timeout", ex);
                    throw e;
                }
                return cir;
            }
            case DISPATCH: {
                executor.execute(method::get);
                CallbackInfoReturnable<T> cir = new CallbackInfoReturnable<>(reason, true);
                cir.cancel();
                return cir;
            }
        }
        throw new IllegalStateException("how this can happen?");
    }
}
