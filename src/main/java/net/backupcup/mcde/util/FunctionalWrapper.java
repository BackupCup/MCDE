package net.backupcup.mcde.util;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class FunctionalWrapper<T, D> {
    protected T original;
    protected D data;
    public FunctionalWrapper(T original, D data) {
        this.original = original;
        this.data = data;
    }
    public D getData() {
        return data;
    }

    public static <D> D getDataFromWrapper(Object obj) throws ClassCastException {
        @SuppressWarnings("unchecked")
        var wrapper = (FunctionalWrapper<?, D>)obj;
        return wrapper.getData();
    }

    public static <T, D> Consumer<T> wrapConsumer(Consumer<T> original, D data) {
        return new ConsumerWrapper<>(original, data);
    }
    public static class ConsumerWrapper<T, D> extends FunctionalWrapper<Consumer<T>, D> implements Consumer<T> {
        public ConsumerWrapper(Consumer<T> original, D data) {
            super(original, data);
        }
        @Override
        public void accept(T obj) {
            original.accept(obj);
        }
    }

    public static <T, D> Predicate<T> wrapPredicate(Predicate<T> original, D data) {
        return new PredicateWrapper<>(original, data);
    }
    public static class PredicateWrapper<T, D> extends FunctionalWrapper<Predicate<T>, D> implements Predicate<T> {
        public PredicateWrapper(Predicate<T> original, D data) {
            super(original, data);
        }
        @Override
        public boolean test(T arg0) {
            return original.test(arg0);
        }
    }
}

