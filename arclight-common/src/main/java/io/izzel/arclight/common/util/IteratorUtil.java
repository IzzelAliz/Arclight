package io.izzel.arclight.common.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

public class IteratorUtil {

    public static <T> Iterator<T> filter(Iterator<T> iterator, Predicate<T> predicate) {
        return new FilterIterator<>(iterator, predicate);
    }

    private static class FilterIterator<T> implements Iterator<T> {

        private final Iterator<T> iterator;
        private final Predicate<T> predicate;

        private boolean nextComputed = false;
        private boolean hasNext = false;
        private T next;

        private FilterIterator(Iterator<T> iterator, Predicate<T> predicate) {
            this.iterator = iterator;
            this.predicate = predicate;
        }

        @Override
        public boolean hasNext() {
            if (!nextComputed) {
                this.computeNext();
            }
            return hasNext;
        }

        @Override
        public T next() {
            if (!nextComputed) {
                this.computeNext();
            }
            if (!hasNext) {
                throw new NoSuchElementException();
            }
            try {
                return this.next;
            } finally {
                nextComputed = false;
            }
        }

        private void computeNext() {
            nextComputed = true;
            while (iterator.hasNext()) {
                T next = iterator.next();
                if (predicate.test(next)) {
                    hasNext = true;
                    this.next = next;
                    return;
                }
            }
            hasNext = false;
            this.next = null;
        }

        @Override
        public void remove() {
            iterator.remove();
        }
    }
}
