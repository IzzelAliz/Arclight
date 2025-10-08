package ca.spottedleaf.concurrentutil.function;

@FunctionalInterface
public interface BiLong1Function<T, R> {

    public R apply(final long t1, final T t2);

}
