/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class InterModComms {
    /**
     *
     * @param senderModId     The modid of the sender. This is supplied by the caller, or by the active mod container context.
     *                        Consider it unreliable.
     * @param modId           The modid being sent to.
     * @param method          The method being sent to.
     * @param messageSupplier A {@link Supplier} of the message.
     */
    public record IMCMessage(String senderModId, String modId, String method, Supplier<?> messageSupplier) {}

    private static ConcurrentMap<String, ConcurrentLinkedQueue<IMCMessage>> containerQueues = new ConcurrentHashMap<>();

    /**
     * Send IMC to remote. Sender will default to the active modcontainer, or minecraft if not.
     *
     * @param modId  the mod id to send to
     * @param method the method name to send
     * @param thing  the thing associated with the method name
     * @return true if the message was enqueued for sending (the target modid is loaded)
     */
    public static boolean sendTo(String modId, String method, Supplier<?> thing) {
        if (!ModList.get().isLoaded(modId)) return false;
        containerQueues.computeIfAbsent(modId, k -> new ConcurrentLinkedQueue<>()).add(new IMCMessage(ModLoadingContext.get().getActiveContainer().getModId(), modId, method, thing));
        return true;
    }

    /**
     * Send IMC to remote.
     *
     * @param senderModId the mod id you are sending from
     * @param modId       the mod id to send to
     * @param method      the method name to send
     * @param thing       the thing associated with the method name
     * @return true if the message was enqueued for sending (the target modid is loaded)
     */
    public static boolean sendTo(String senderModId, String modId, String method, Supplier<?> thing) {
        if (!ModList.get().isLoaded(modId)) return false;
        containerQueues.computeIfAbsent(modId, k -> new ConcurrentLinkedQueue<>()).add(new IMCMessage(senderModId, modId, method, thing));
        return true;
    }

    /**
     * Retrieve pending messages for your modid. Use the predicate to filter the method name.
     *
     * @param modId         the modid you are querying for
     * @param methodMatcher a predicate for the method you are interested in
     * @return All messages passing the supplied method predicate
     */
    public static Stream<IMCMessage> getMessages(String modId, Predicate<String> methodMatcher) {
        ConcurrentLinkedQueue<IMCMessage> queue = containerQueues.get(modId);
        if (queue == null) return Stream.empty();
        return StreamSupport.stream(new QueueFilteringSpliterator(queue, methodMatcher), false);
    }

    /**
     * Retrieve all message for your modid.
     *
     * @param modId the modid you are querying for
     * @return All messages
     */
    public static Stream<IMCMessage> getMessages(String modId) {
        return getMessages(modId, s -> Boolean.TRUE);
    }

    private static class QueueFilteringSpliterator implements Spliterator<IMCMessage> {
        private final ConcurrentLinkedQueue<IMCMessage> queue;
        private final Predicate<String> methodFilter;
        private final Iterator<IMCMessage> iterator;

        public QueueFilteringSpliterator(ConcurrentLinkedQueue<IMCMessage> queue, Predicate<String> methodFilter) {
            this.queue = queue;
            this.iterator = queue.iterator();
            this.methodFilter = methodFilter;
        }

        @Override
        public int characteristics() {
            return Spliterator.CONCURRENT | Spliterator.NONNULL | Spliterator.ORDERED;
        }

        @Override
        public long estimateSize() {
            return queue.size();
        }

        @Override
        public boolean tryAdvance(Consumer<? super IMCMessage> action) {
            IMCMessage next;
            do {
                if (!iterator.hasNext()) {
                    return false;
                }
                next = this.iterator.next();
            } while (!methodFilter.test(next.method));
            action.accept(next);
            this.iterator.remove();
            return true;
        }

        @Override
        public Spliterator<IMCMessage> trySplit() {
            return null;
        }
    }
}
