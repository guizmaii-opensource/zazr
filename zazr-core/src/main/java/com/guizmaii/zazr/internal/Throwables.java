package com.guizmaii.zazr.internal;

import org.jspecify.annotations.Nullable;

/**
 * Two internal cross-cutting helpers for checked-exception handling, used across
 * {@code com.guizmaii.zazr}, {@code com.guizmaii.zazr.control} and {@code com.guizmaii.zazr.collection}.
 * <p>
 * This class and its methods are {@code public} only because {@code module-info.java} does <strong>not</strong>
 * export {@code com.guizmaii.zazr.internal}, so nothing outside this module can see it under the module path;
 * on the classpath (an unnamed module, or an automatic module) it stays reachable regardless of the
 * {@code exports} declaration, which is an accepted, documented gap. Either way: <strong>this is not public
 * API</strong> and may change or disappear without notice. {@code zazr-test} and {@code zazr-benchmark} are
 * separate modules and must not depend on it.
 * <p>
 * {@link #sneakyThrow} implements the "sneaky throw" erasure trick: rethrows any {@link Throwable}, checked or
 * not, without the compiler requiring it to be declared or caught. Used wherever a checked computation has to
 * be exposed through a JDK functional interface, whose methods declare no checked exceptions.
 * <p>
 * {@link #isFatal} is {@link com.guizmaii.zazr.control.Try}'s policy for which throwables are never captured
 * as a {@code Failure} and instead always propagate: {@link InterruptedException}, {@link LinkageError},
 * {@link ThreadDeath} and {@link VirtualMachineError}. Anything a checked function's {@code recover} or a
 * lift-style adapter catches follows the same policy, so a fatal error is never handed to user recovery code.
 */
public interface Throwables {

    // DEV-NOTE: we do not plan to expose this as public API
    @SuppressWarnings("unchecked")
    static <T extends Throwable, R extends @Nullable Object> R sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }

    // DEV-NOTE: we do not plan to expose this as public API
    static boolean isFatal(Throwable throwable) {
        return throwable instanceof InterruptedException
                || throwable instanceof LinkageError
                || ThreadDeathResolver.isThreadDeath(throwable)
                || throwable instanceof VirtualMachineError;
    }

    class ThreadDeathResolver {
        static final @Nullable Class<?> THREAD_DEATH_CLASS = resolve();

        static boolean isThreadDeath(Throwable throwable) {
            return THREAD_DEATH_CLASS != null && THREAD_DEATH_CLASS.isInstance(throwable);
        }

        private static @Nullable Class<?> resolve() {
            try {
                return Class.forName("java.lang.ThreadDeath");
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }
}
