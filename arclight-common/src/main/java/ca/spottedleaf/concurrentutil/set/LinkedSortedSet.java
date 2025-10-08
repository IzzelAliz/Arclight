package ca.spottedleaf.concurrentutil.set;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public final class LinkedSortedSet<E> implements Iterable<E> {

    public final Comparator<? super E> comparator;

    protected Link<E> head;
    protected Link<E> tail;

    public LinkedSortedSet() {
        this((Comparator)Comparator.naturalOrder());
    }

    public LinkedSortedSet(final Comparator<? super E> comparator) {
        this.comparator = comparator;
    }

    public void clear() {
        this.head = this.tail = null;
    }

    public boolean isEmpty() {
        return this.head == null;
    }

    public E first() {
        final Link<E> head = this.head;
        return head == null ? null : head.element;
    }

    public E last() {
        final Link<E> tail = this.tail;
        return tail == null ? null : tail.element;
    }

    public boolean containsFirst(final E element) {
        final Comparator<? super E> comparator = this.comparator;
        for (Link<E> curr = this.head; curr != null; curr = curr.next) {
            if (comparator.compare(element, curr.element) == 0) {
                return true;
            }
        }
        return false;
    }

    public boolean containsLast(final E element) {
        final Comparator<? super E> comparator = this.comparator;
        for (Link<E> curr = this.tail; curr != null; curr = curr.prev) {
            if (comparator.compare(element, curr.element) == 0) {
                return true;
            }
        }
        return false;
    }

    private void removeNode(final Link<E> node) {
        final Link<E> prev = node.prev;
        final Link<E> next = node.next;

        // help GC
        node.element = null;
        node.prev = null;
        node.next = null;

        if (prev == null) {
            this.head = next;
        } else {
            prev.next = next;
        }

        if (next == null) {
            this.tail = prev;
        } else {
            next.prev = prev;
        }
    }

    public boolean remove(final Link<E> link) {
        if (link.element == null) {
            return false;
        }

        this.removeNode(link);
        return true;
    }

    public boolean removeFirst(final E element) {
        final Comparator<? super E> comparator = this.comparator;
        for (Link<E> curr = this.head; curr != null; curr = curr.next) {
            if (comparator.compare(element, curr.element) == 0) {
                this.removeNode(curr);
                return true;
            }
        }
        return false;
    }

    public boolean removeLast(final E element) {
        final Comparator<? super E> comparator = this.comparator;
        for (Link<E> curr = this.tail; curr != null; curr = curr.prev) {
            if (comparator.compare(element, curr.element) == 0) {
                this.removeNode(curr);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<>() {
            private Link<E> next = LinkedSortedSet.this.head;

            @Override
            public boolean hasNext() {
                return this.next != null;
            }

            @Override
            public E next() {
                final Link<E> next = this.next;
                if (next == null) {
                    throw new NoSuchElementException();
                }
                this.next = next.next;
                return next.element;
            }
        };
    }

    public E pollFirst() {
        final Link<E> head = this.head;
        if (head == null) {
            return null;
        }

        final E ret = head.element;
        final Link<E> next = head.next;

        // unlink head
        this.head = next;
        if (next == null) {
            this.tail = null;
        } else {
            next.prev = null;
        }

        // help GC
        head.element = null;
        head.next = null;

        return ret;
    }

    public E pollLast() {
        final Link<E> tail = this.tail;
        if (tail == null) {
            return null;
        }

        final E ret = tail.element;
        final Link<E> prev = tail.prev;

        // unlink tail
        this.tail = prev;
        if (prev == null) {
            this.head = null;
        } else {
            prev.next = null;
        }

        // help GC
        tail.element = null;
        tail.prev = null;

        return ret;
    }

    public Link<E> addLast(final E element) {
        final Comparator<? super E> comparator = this.comparator;

        Link<E> curr = this.tail;
        if (curr != null) {
            int compare;

            while ((compare = comparator.compare(element, curr.element)) < 0) {
                Link<E> prev = curr;
                curr = curr.prev;
                if (curr != null) {
                    continue;
                }
                return this.head = prev.prev = new Link<>(element, null, prev);
            }

            if (compare != 0) {
                // insert after curr
                final Link<E> next = curr.next;
                final Link<E> insert = new Link<>(element, curr, next);
                curr.next = insert;

                if (next == null) {
                    this.tail = insert;
                } else {
                    next.prev = insert;
                }
                return insert;
            }

            return null;
        } else {
            return this.head = this.tail = new Link<>(element);
        }
    }

    public Link<E> addFirst(final E element) {
        final Comparator<? super E> comparator = this.comparator;

        Link<E> curr = this.head;
        if (curr != null) {
            int compare;

            while ((compare = comparator.compare(element, curr.element)) > 0) {
                Link<E> prev = curr;
                curr = curr.next;
                if (curr != null) {
                    continue;
                }
                return this.tail = prev.next = new Link<>(element, prev, null);
            }

            if (compare != 0) {
                // insert before curr
                final Link<E> prev = curr.prev;
                final Link<E> insert = new Link<>(element, prev, curr);
                curr.prev = insert;

                if (prev == null) {
                    this.head = insert;
                } else {
                    prev.next = insert;
                }
                return insert;
            }

            return null;
        } else {
            return this.head = this.tail = new Link<>(element);
        }
    }

    public static final class Link<E> {
        private E element;
        private Link<E> prev;
        private Link<E> next;

        private Link() {}

        private Link(final E element) {
            this.element = element;
        }

        private Link(final E element, final Link<E> prev, final Link<E> next) {
            this.element = element;
            this.prev = prev;
            this.next = next;
        }
    }
}
