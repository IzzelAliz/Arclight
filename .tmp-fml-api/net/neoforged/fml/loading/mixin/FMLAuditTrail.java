/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.util.function.BiConsumer;
import org.spongepowered.asm.service.IMixinAuditTrail;

/**
 * Audit trail adapter for ModLauncher
 */
class FMLAuditTrail implements IMixinAuditTrail {
    private static final String APPLY_MIXIN_ACTIVITY = "APP";
    private static final String POST_PROCESS_ACTIVITY = "DEC";
    private static final String GENERATE_ACTIVITY = "GEN";

    /**
     * Current audit trail class name
     */
    private String currentClass;

    /**
     * Audit trail consumer
     */
    private BiConsumer<String, String[]> consumer;

    /**
     * @param className current class name
     * @param consumer  audit trail consumer which sinks audit trail actions
     */
    public void setConsumer(String className, BiConsumer<String, String[]> consumer) {
        this.currentClass = className;
        this.consumer = consumer;
    }

    @Override
    public void onApply(String className, String mixinName) {
        this.writeActivity(className, FMLAuditTrail.APPLY_MIXIN_ACTIVITY, mixinName);
    }

    @Override
    public void onPostProcess(String className) {
        this.writeActivity(className, FMLAuditTrail.POST_PROCESS_ACTIVITY);
    }

    @Override
    public void onGenerate(String className, String generatorName) {
        this.writeActivity(className, FMLAuditTrail.GENERATE_ACTIVITY);
    }

    private void writeActivity(String className, String activity, String... context) {
        if (this.consumer != null && className.equals(this.currentClass)) {
            this.consumer.accept(activity, context);
        }
    }
}
