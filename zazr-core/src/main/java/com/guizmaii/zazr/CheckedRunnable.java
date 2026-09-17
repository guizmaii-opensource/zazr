package com.guizmaii.zazr;

import static com.guizmaii.zazr.Throwables.sneakyThrow;

/**
 * A {@linkplain Runnable} that is allowed to throw checked exceptions.
 */
@FunctionalInterface
public interface CheckedRunnable {

    /**
     * Creates a {@code CheckedRunnable} from the given method reference or lambda.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // class Evil { static void sideEffect() { ... } }
     * final CheckedRunnable checkedRunnable = CheckedRunnable.of(Evil::sideEffect);
     * final Runnable runnable = checkedRunnable.unchecked();
     *
     * // performs the side-effect; a checked exception must be declared or caught by the caller
     * checkedRunnable.run();
     *
     * // performs the side-effect; a checked exception is sneakily rethrown without being declared
     * runnable.run();
     * }</pre>
     *
     * @param methodReference typically a method reference, e.g. {@code Type::method}
     * @return the given {@code CheckedRunnable} unchanged (this method only aids type inference)
     * @see CheckedFunction1#of(CheckedFunction1)
     */
    static CheckedRunnable of(CheckedRunnable methodReference) {
        return methodReference;
    }

    /**
     * Executes the action, potentially performing side-effects.
     *
     * @throws Exception if an error occurs during execution
     */

    void run() throws Exception;

    /**
     * Returns an unchecked {@link Runnable} that <em>sneakily throws</em> any exception 
     * encountered during execution of this unit of work.
     *
     * @return a {@link Runnable} that may throw any {@link Throwable} without declaring it
     */
    default Runnable unchecked() {
        return () -> {
            try {
                run();
            } catch(Throwable x) {
                sneakyThrow(x);
            }
        };
    }
}
