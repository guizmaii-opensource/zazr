package com.guizmaii.zazr;

import org.jspecify.annotations.Nullable;

/**
 * The one place the "sneaky throw" erasure trick is implemented: rethrows any {@link Throwable}, checked or
 * not, without the compiler requiring it to be declared or caught. Used wherever a checked computation has to
 * be exposed through a JDK functional interface, whose methods declare no checked exceptions.
 */
interface Throwables {

    // DEV-NOTE: we do not plan to expose this as public API
    @SuppressWarnings("unchecked")
    static <T extends Throwable, R extends @Nullable Object> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
