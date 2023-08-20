package org.embeddedt.blacksmith.impl;

import java.util.function.Supplier;

public class Utils {
    public static Class<?> classForName(String name) {
        try {
            return Class.forName(name);
        } catch(ClassNotFoundException e) {
             throw new RuntimeException(e);
        }
    }

    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    public static <T> T unthrow(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void throwAsUnchecked(Exception exception) throws E {
        throw (E) exception;
    }
}
